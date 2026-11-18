package com.devisri.seatlock.hold;

import java.time.Clock;
import java.time.Duration;

import io.lettuce.core.RedisURI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Chooses where seat holds live. With {@code REDIS_URL} set (Upstash gives {@code rediss://default:pass@host:6379}),
 * Redis is used; otherwise an in-memory store, so local runs and tests need no setup.
 * Spring Boot's own Redis auto-configuration is switched off in application.properties, so nothing tries
 * to reach a Redis on localhost when none is configured.
 */
@Configuration
public class HoldStoreConfig {

    private static final String REDIS_URL = "seatlock.holds.redis-url";

    @Bean
    @Conditional(RedisConfigured.class)
    public LettuceConnectionFactory redisConnectionFactory(HoldProperties properties) {
        RedisURI uri = RedisURI.create(properties.redisUrl().strip());
        LettuceClientConfiguration.LettuceClientConfigurationBuilder client = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(3));
        if (uri.isSsl()) {
            client.useSsl();
        }
        return new LettuceConnectionFactory(standalone(uri), client.build());
    }

    @Bean
    @Conditional(RedisConfigured.class)
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    @Bean
    @Conditional(RedisConfigured.class)
    public SeatHoldStore redisSeatHoldStore(StringRedisTemplate redis, Clock clock) {
        return new RedisSeatHoldStore(redis, clock);
    }

    @Bean
    @Conditional(NoRedisConfigured.class)
    public SeatHoldStore inMemorySeatHoldStore(Clock clock) {
        return new InMemorySeatHoldStore(clock);
    }

    /** Host, port, user and password out of a redis:// or rediss:// URL. */
    static RedisStandaloneConfiguration standalone(RedisURI uri) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(uri.getHost(), uri.getPort());
        if (uri.getUsername() != null) {
            config.setUsername(uri.getUsername());
        }
        if (uri.getPassword() != null && uri.getPassword().length > 0) {
            config.setPassword(RedisPassword.of(uri.getPassword()));
        }
        return config;
    }

    static class RedisConfigured implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String url = context.getEnvironment().getProperty(REDIS_URL, "");
            return !url.isBlank();
        }
    }

    static class NoRedisConfigured implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return !new RedisConfigured().matches(context, metadata);
        }
    }
}
