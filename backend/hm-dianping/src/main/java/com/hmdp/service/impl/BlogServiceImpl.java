package com.hmdp.service.impl;



import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.hmdp.utils.RedisConstants.FEED_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author lorendw7
 * @since 2025-10-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IFollowService followService;

    @Override
    public Result queryBlogById(Long id) {
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("博客不存在");
        }
        queryBlogUser(blog);
        if(UserHolder.getUser() != null) {
            isBlogLiked(blog);
        }
        return Result.ok(blog);
    }


    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            this.queryBlogUser(blog);
            if(UserHolder.getUser() != null) {
                isBlogLiked(blog);
            }
        });

        return Result.ok(records);
    }

    private void isBlogLiked(Blog blog) {
        // 获取登录用户
        Long userId = UserHolder.getUser().getId();
        // 判断当前登录用户是否点赞
        String key = BLOG_LIKED_KEY + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null) {
            blog.setIsLike(Boolean.FALSE);
        }else  {
            blog.setIsLike(Boolean.TRUE);
        }
    }


    @Override
    public Result likeBlog(Long id) {
        // 获取登录用户
        Long userId = UserHolder.getUser().getId();
        if(userId == null) return Result.fail("用户未登录");
        // 判断当前登录用户是否点赞
        String key = BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null) {
            // 如果未点赞，可以点赞
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            // 数据库点赞数加1，用户添加到Redis的sortedset集合中
            if(isSuccess) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        }else {
            // 如果已点赞，可以取消点赞
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            // 数据库点赞数减1，用户从Redis的sortedset集合中移除
            stringRedisTemplate.opsForZSet().remove(key, userId.toString());
        }

        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        String key = BLOG_LIKED_KEY + id;
        // 查询点赞top5的用户
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key,0, 4);
        if (top5 == null || top5.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        // 解析出其中用户的id
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String idStr = StrUtil.join(",", ids);
        // 根据用户id查询用户
        List<UserDTO> userDTOS = userService.query()
                .in("id", ids).last("order by field(id," + idStr + ")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOS);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean isSuccess = save(blog);
        if (!isSuccess) {
            return Result.fail("新增笔记失败");
        }

        // 查询笔记作者的所有粉丝
        List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
        // 推送笔记id给所有粉丝
        for (Follow follow : follows) {
            // 获取粉丝id
            Long userId = follow.getUserId();
            // 推送
            String key = FEED_KEY + userId;
            stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
        }
        // 返回id
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        // 获取当前用户
        Long userId = UserHolder.getUser().getId();

        // 查询收件箱
        String key = FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples =
                stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, 0, max, offset, 2);
        // 非空判断
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok();
        }

        // 解析数据: blogId、minTime、offset
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0;
        int os = 1;
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            ids.add(Long.valueOf(Objects.requireNonNull(tuple.getValue())));
            long time = Objects.requireNonNull(tuple.getScore()).longValue();

            if (time == minTime) {
                os += 1;
            }else {
                minTime = time;
                os = 1;
            }
        }

        // 查询blog
        String idStr = StrUtil.join(",", ids);
        List<Blog> blogList = query().in("id", ids).last("order by field(id," + idStr + ")").list();
        for (Blog blog : blogList) {
            // 查询blog相关的用户
            queryBlogUser(blog);
            // 查询blog是否被点赞
            isBlogLiked(blog);
        }
        // 封装并返回
        ScrollResult result = new ScrollResult();
        result.setList(blogList);
        result.setMinTime(minTime);
        result.setOffset(os);

        return Result.ok(result);
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
