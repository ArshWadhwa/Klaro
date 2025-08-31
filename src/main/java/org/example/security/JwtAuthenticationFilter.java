package org.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        System.out.println("Request URL: " + request.getRequestURL());
        System.out.println("Request URI: " + requestURI);

        String authHeader = request.getHeader("Authorization");
        System.out.println("Authorization header: " + authHeader);

        // Skip JWT processing entirely for public endpoints
        boolean isPublicEndpoint = requestURI.equals("/auth/signin") ||
                requestURI.equals("/auth/signup") ||
                requestURI.equals("/auth/user") ||
                requestURI.equals("/auth/refresh") ||
                requestURI.equals("/index.html") ||
                requestURI.equals("/signup.html") ||
                requestURI.equals("/welcome.html")||
                requestURI.equals("/styles.css") ||
                requestURI.equals("/script.js") ||
                requestURI.equals("/") ||
                requestURI.startsWith("/ai/");

        // For public endpoints, skip JWT processing entirely
        if (isPublicEndpoint) {
            System.out.println("Public endpoint detected, skipping JWT validation");
            filterChain.doFilter(request, response);
            return;
        }

        // For protected endpoints, validate JWT if present
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            System.out.println("Token: " + token);
            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.extractEmail(token);
                System.out.println("Token validated, email: " + email);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(email, null, null);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("Authentication set for email: " + email);
            } else {
                System.out.println("Token validation failed for token: " + token);
                // For protected endpoints with invalid tokens, let Spring Security handle the 401
            }
        } else {
            System.out.println("No Authorization header or incorrect format for protected endpoint");
            // For protected endpoints without tokens, let Spring Security handle the 401
        }

        filterChain.doFilter(request, response);
    }
}