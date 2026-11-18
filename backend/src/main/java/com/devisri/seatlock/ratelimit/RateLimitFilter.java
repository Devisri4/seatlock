package com.devisri.seatlock.ratelimit;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * At most N API requests per visitor per minute; more get 429 Too Many Requests with a Retry-After header.
 * The visitor is the client IP. Behind Render's proxy, Tomcat reads it from X-Forwarded-For right to left
 * (server.forward-headers-strategy=native), so values a client adds itself on the left are ignored.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter limiter;
    private final int limitPerMinute;

    public RateLimitFilter(RateLimiter limiter, int limitPerMinute) {
        this.limiter = limiter;
        this.limitPerMinute = limitPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return limitPerMinute <= 0
                || !request.getRequestURI().startsWith("/api/")
                || "OPTIONS".equals(request.getMethod()); // CORS preflights are free
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        RateLimiter.Decision decision = limiter.tryAcquire(request.getRemoteAddr(), limitPerMinute);
        if (decision.allowed()) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
                {"type":"about:blank","title":"Too Many Requests","status":429,\
                "detail":"Too many requests. Please wait %d seconds and try again."}"""
                .formatted(decision.retryAfterSeconds()));
    }
}
