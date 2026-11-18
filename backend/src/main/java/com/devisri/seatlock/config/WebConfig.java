package com.devisri.seatlock.config;

import java.time.Clock;
import java.util.List;

import com.devisri.seatlock.hold.HoldStoreConfig;
import com.devisri.seatlock.ratelimit.InMemoryRateLimiter;
import com.devisri.seatlock.ratelimit.RateLimitFilter;
import com.devisri.seatlock.ratelimit.RateLimiter;
import com.devisri.seatlock.ratelimit.RedisRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/** Browser access (CORS) and the per-visitor rate limit, as servlet filters in front of everything else. */
@Configuration
public class WebConfig {

    /**
     * Lets the React page (on another domain, e.g. Vercel) call the API. Runs first, so even a 429 from the
     * rate limiter carries CORS headers and the page can show "please wait" instead of a network error.
     * Allowed origins come from FRONTEND_ORIGIN, comma-separated; wildcards like https://*.vercel.app work.
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter(@Value("${seatlock.cors.allowed-origins}") List<String> origins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOriginPatterns(origins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "X-Guest-Id", "Idempotency-Key"));
        cors.setExposedHeaders(List.of("Retry-After"));
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        source.registerCorsConfiguration("/actuator/health", cors);

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(RateLimiter limiter,
                                                                   @Value("${seatlock.rate-limit.requests-per-minute}") int perMinute) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(new RateLimitFilter(limiter, perMinute));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    @Bean
    @Conditional(HoldStoreConfig.RedisConfigured.class)
    public RateLimiter redisRateLimiter(StringRedisTemplate redis, Clock clock) {
        return new RedisRateLimiter(redis, clock);
    }

    @Bean
    @Conditional(HoldStoreConfig.NoRedisConfigured.class)
    public RateLimiter inMemoryRateLimiter(Clock clock) {
        return new InMemoryRateLimiter(clock);
    }
}
