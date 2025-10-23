package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.impl.FollowServiceImpl;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lorendw7
 * @since 2025-10-23
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private FollowServiceImpl followService;

    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId, @PathVariable("isFollow") Boolean isFollow) {
        return followService.follow(followUserId, isFollow);
    }

    @GetMapping("/or/not/{id}")
    public Result followOrNot(@PathVariable("id") Long followUserId) {
        return followService.followOrNot(followUserId);
    }
}
