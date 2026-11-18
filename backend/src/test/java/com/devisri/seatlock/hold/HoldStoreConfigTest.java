package com.devisri.seatlock.hold;

import java.time.Clock;
import java.time.Instant;

import io.lettuce.core.RedisURI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class HoldStoreConfigTest {

    @Configuration
    @EnableConfigurationProperties(HoldProperties.class)
    static class TestClock {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.EPOCH, java.time.ZoneOffset.UTC);
        }
    }

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(TestClock.class, HoldStoreConfig.class);

    @Test
    void withoutARedisUrlHoldsAreKeptInMemory() {
        context.run(ctx -> assertThat(ctx.getBean(SeatHoldStore.class)).isInstanceOf(InMemorySeatHoldStore.class));
        context.withPropertyValues("seatlock.holds.redis-url=  ")
                .run(ctx -> assertThat(ctx.getBean(SeatHoldStore.class)).isInstanceOf(InMemorySeatHoldStore.class));
    }

    @Test
    void withARedisUrlHoldsGoToRedis() {
        context.withPropertyValues("seatlock.holds.redis-url=redis://localhost:6390")
                .run(ctx -> assertThat(ctx.getBean(SeatHoldStore.class)).isInstanceOf(RedisSeatHoldStore.class));
    }

    @Test
    void readsAnUpstashStyleUrlWithUserPasswordAndTls() {
        RedisURI uri = RedisURI.create("rediss://default:s3cret@eu1-example.upstash.io:6379");

        RedisStandaloneConfiguration config = HoldStoreConfig.standalone(uri);

        assertThat(uri.isSsl()).isTrue();
        assertThat(config.getHostName()).isEqualTo("eu1-example.upstash.io");
        assertThat(config.getPort()).isEqualTo(6379);
        assertThat(config.getUsername()).isEqualTo("default");
        assertThat(new String(config.getPassword().get())).isEqualTo("s3cret");
    }

    @Test
    void holdsLastFiveMinutesByDefault() {
        context.run(ctx -> assertThat(ctx.getBean(HoldProperties.class).duration()).hasMinutes(5));
    }
}
