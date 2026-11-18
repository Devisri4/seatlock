package com.devisri.seatlock.ratelimit;

import java.time.Clock;
import java.time.Duration;

import com.devisri.seatlock.hold.HoldProperties;
import com.devisri.seatlock.hold.HoldStoreConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/** The same counting rules on a real Redis 7 (what Upstash runs). Needs Docker, so it runs in CI. */
@Testcontainers(disabledWithoutDocker = true)
class RedisRateLimiterTest extends RateLimiterContract {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private static LettuceConnectionFactory connection;
    private static StringRedisTemplate redis;

    @BeforeAll
    static void connect() {
        String url = "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379);
        connection = new HoldStoreConfig().redisConnectionFactory(new HoldProperties(Duration.ofMinutes(5), url));
        connection.afterPropertiesSet();
        connection.start();
        redis = new StringRedisTemplate(connection);
        redis.afterPropertiesSet();
    }

    @AfterAll
    static void disconnect() {
        connection.destroy();
    }

    @Override
    protected RateLimiter limiter(Clock clock) {
        return new RedisRateLimiter(redis, clock);
    }

    @Test
    void countersDeleteThemselvesSoRedisNeverFillsUp() {
        new RedisRateLimiter(redis, CLOCK).tryAcquire("198.51.100.9", 10);

        String key = redis.keys(RedisRateLimiter.KEY_PREFIX + "198.51.100.9:*").iterator().next();

        assertThat(redis.getExpire(key)).isBetween(1L, 41L);
    }
}
