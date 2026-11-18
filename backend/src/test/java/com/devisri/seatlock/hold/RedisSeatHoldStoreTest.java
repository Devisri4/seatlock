package com.devisri.seatlock.hold;

import java.time.Clock;
import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/** The same hold rules on a real Redis 7 (what Upstash runs). Needs Docker, so it runs in CI. Expiry really waits. */
@Testcontainers(disabledWithoutDocker = true)
class RedisSeatHoldStoreTest extends SeatHoldStoreContract {

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
    protected SeatHoldStore newStore() {
        return new RedisSeatHoldStore(redis, Clock.systemUTC());
    }

    @Override
    protected void advance(Duration time) throws InterruptedException {
        Thread.sleep(time.toMillis());
    }

    @Test
    void redisItselfDeletesTheKeyWhenTheHoldRunsOut() {
        store.hold(77, "guest-1", Duration.ofMinutes(5));

        Long secondsLeft = redis.getExpire(RedisSeatHoldStore.key(77));

        assertThat(secondsLeft).isBetween(298L, 300L);
        assertThat(redis.opsForValue().get(RedisSeatHoldStore.key(77))).startsWith("guest-1|");
    }
}
