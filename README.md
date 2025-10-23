## 生活优选(Life Preference)
 
A merchant review platform based on Spring Boot, featuring SMS login, merchant search, flash sale coupons, blogs, friend follows, and Feed stream notifications.

- **Authentication & Security**: Implements shared Session-based login authentication via Redis, validated with interceptors.
- **Caching & Performance**: Builds a Redis-based caching system using the Cache Aside pattern, with proactive updates and expiration to ensure data consistency and reduce database load.
- **Concurrency & Distribution**: Uses Redis for distributed locks to maintain data consistency and thread safety during high-concurrency coupon flash sales.
- **Asynchronous Messaging**: Leverages Redis Streams as a message queue for asynchronous order placement in flash sales, improving response speed and throughput.
- **Advanced Features**:  Utilizes Redis data structures for diverse functions—BitMap for user check-ins, HyperLogLog for UV statistics to measure traffic, and GEO for locating nearby merchants.
