package com.andy.redis.server;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.Jedis;

import static org.assertj.core.api.Assertions.assertThat;


/**
 *
 */
public class RedisServerTest {

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

    @Test
    public void test_expire() throws InterruptedException {
        jedis.setex("key2", 1, "test");
        Thread.sleep(1100);
        String key2 = jedis.get("key2");
        assertThat(key2).isNullOrEmpty();
    }

    @Test
    public void test_spop() {
        jedis.sadd("key3", "value1");
        jedis.sadd("key3", "value2");
        jedis.sadd("key3", "value3");

        jedis.spop("key3");
        int size = jedis.smembers("key3").size();
        assertThat(size).isEqualTo(2);
    }

    @AfterClass
    public static void execute() throws Exception {
        redisServer.stop();
    }

}