## 生活优选(Life Preference)
*Personal Development (Backend)*  
A SpringBoot-based lifestyle service platform implementing merchant reviews, coupon flash sales, friend following, and blogger dynamic push notifications.

- **Authentication & Security**: Implement login authentication using Redis for shared Session, combined with interceptors for verification
- **Caching & Performance**: Built Redis-based caching system using Cache Aside pattern with proactive updates and timeout elimination to ensure data consistency and reduce database pressure
- **Concurrency & Distribution**: Implement distributed locks via Redis to ensure data consistency and thread safety of coupon inventory in high-concurrency scenarios
- **Asynchronous Messaging**: Implement message queues using Redis's Stream data structure to achieve asynchronous flash sale order placement, improving system response speed and throughput
- **Advanced Features**: Employed Redis data structures for rich functionalities including BitMap for user check-ins, HyperLogLog for UV statistics to measure website traffic, and GeoHash for nearby merchant searches
