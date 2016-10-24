Redis Embedded Server
=====================

## redis server java 版本客户端,适用于集成测试.

>注意此版本只用于验证业务代码是否正确,不能用于测试性能

### 使用说明
- 引用maven依赖

```xml
<dependency>
	<groupId>com.andy.redis</groupId>
    <artifactId>redis-embedded-server</artifactId>
    <version>0.2.0</version>
    <scope>test</scope>
</dependency>
```
- 启动和停止redis服务端

```java
    private static RedisServer redisServer;
    private static Jedis jedis;

    @BeforeClass
    public static void setUp() {
        redisServer = RedisServer.newBuilder().port(6379).build();
        redisServer.start();

        jedis = new Jedis("127.0.0.1", 6379);
    }


    @Test
    public void test_set() throws Exception {
        jedis.set("key", "test");
        String key = jedis.get("key");
        assertThat(key).isEqualTo("test");
    }

    @AfterClass
    public static void execute() throws Exception {
        redisServer.stop();
    }
```

