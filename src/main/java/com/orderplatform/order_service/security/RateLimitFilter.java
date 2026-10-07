package com.orderplatform.order_service.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.Duration;
import org.springframework.http.HttpStatus;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${ratelimit.requests-per-minute}")
    private int limit;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String who = resolveClient(request);
        long minute = System.currentTimeMillis() / 60000;
        String key = "ratelimit:" + who + ":" + minute;

        Long count;
        try {
            count = stringRedisTemplate.opsForValue().increment(key);
            stringRedisTemplate.expire(key, Duration.ofSeconds(120));
        } catch (Exception e) {
            log.warn("Rate limiter skipped, Redis unavailable: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        if (count != null && count > limit) {
            long secondsLeft = 60 - (System.currentTimeMillis() / 1000 % 60);
            log.warn("Rate limit exceeded for {} ({} requests)", who, count);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(secondsLeft));
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Too many requests\",\"retryAfterSeconds\":" + secondsLeft + "}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClient(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return auth.getName();
        }
        return request.getRemoteAddr();
    }
}