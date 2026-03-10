package org.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Membership;
import org.example.entity.User;
import org.example.repository.MembershipRepository;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Multi-tenant filter that automatically sets TenantContext for each authenticated request.
 * 
 * Flow:
 * 1. JWT Filter authenticates user and sets SecurityContext
 * 2. This filter runs AFTER JWT filter
 * 3. Extracts authenticated User from SecurityContext
 * 4. Loads user's active membership (for now, first ACTIVE membership)
 * 5. Sets TenantContext with organizationId and role
 * 6. Clears context after request completes
 * 
 * Future: Support organization selection via header (X-Organization-Id) for users in multiple orgs
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final MembershipRepository membershipRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        // � Tenant system disabled - this method is never called due to shouldNotFilter returning true
        // Code kept for reference but not executed
        filterChain.doFilter(request, response);
    }

    /**
     * Endpoints that are allowed even without an active organization membership.
     * These let users manage their invites.
     */
    private boolean isAllowedWithoutOrg(String path) {
        return path.startsWith("/api/organizations/my-invites")
                || path.startsWith("/api/organizations/accept-invite")
                || path.startsWith("/api/organizations/decline-invite");
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 🚫 Tenant system disabled — skip for ALL requests
        return true;
    }
}
