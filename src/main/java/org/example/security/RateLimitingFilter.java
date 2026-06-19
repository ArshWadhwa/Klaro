package org.example.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter implements Filter {

    private final ConcurrentHashMap<String, RequestTracker> trackers = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 100;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String ip = httpRequest.getRemoteAddr();
            String uri = httpRequest.getRequestURI();

            // Apply rate limiting to critical API endpoints prone to enumeration
            if (uri.startsWith("/groups") || uri.contains("/users/available") || uri.contains("/projects/groups")) {
                RequestTracker tracker = trackers.computeIfAbsent(ip, k -> new RequestTracker());
                if (!tracker.allowRequest()) {
                    HttpServletResponse httpResponse = (HttpServletResponse) response;
                    httpResponse.setStatus(429);
                    httpResponse.getWriter().write("Too many requests - Rate limit exceeded. Please try again in a minute.");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    private static class RequestTracker {
        private long windowStart = System.currentTimeMillis();
        private final AtomicInteger count = new AtomicInteger(0);

        public synchronized boolean allowRequest() {
            long now = System.currentTimeMillis();
            if (now - windowStart > 60000) {
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() <= MAX_REQUESTS_PER_MINUTE;
        }
    }
}
