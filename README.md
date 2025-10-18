## 生活优选(Life Preference)
*Personal Development (Backend)*  
A SpringBoot-based lifestyle service platform implementing merchant reviews, coupon flash sales, friend following, and blogger dynamic push notifications.

- **Authentication & Security**: Implement login authentication using Redis for shared Session, combined with interceptors for verification
- **Caching & Performance**: Built Redis-based caching system using Cache Aside pattern with proactive updates and timeout elimination to ensure data consistency and reduce database pressure
- **Concurrency & Distribution**: Utilized Redisson for distributed locks and "lock-validate-update" approach to design idempotent interfaces, ensuring inventory and order data consistency and thread safety in high-concurrency scenarios
- **Asynchronous Messaging**: Leveraged RabbitMQ with exchange and queue binding to implement asynchronous flash sale ordering, improving system responsiveness and throughput
- **Advanced Features**: Employed Redis data structures for rich functionalities including BitMap for user check-ins, HyperLogLog for UV statistics to measure website traffic, and GeoHash for nearby merchant searches
