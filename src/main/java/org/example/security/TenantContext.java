package org.example.security;

import lombok.Data;

/**
 * Multi-tenant context holder using ThreadLocal.
 * Stores current user's organization ID and membership role for the request lifecycle.
 * 
 * This is automatically populated by TenantFilter for each authenticated request
 * and cleared after request completion to prevent memory leaks.
 * 
 * Usage in services:
 * - Long orgId = TenantContext.getOrganizationId();
 * - String role = TenantContext.getRole();
 */
public class TenantContext {
    
    @Data
    public static class TenantInfo {
        private Long organizationId;
        private String role; // OWNER, ADMIN, MEMBER
        private Long userId;
    }
    
    private static final ThreadLocal<TenantInfo> currentTenant = new ThreadLocal<>();
    
    public static void setTenantInfo(Long organizationId, String role, Long userId) {
        TenantInfo info = new TenantInfo();
        info.setOrganizationId(organizationId);
        info.setRole(role);
        info.setUserId(userId);
        currentTenant.set(info);
    }
    
    public static Long getOrganizationId() {
        TenantInfo info = currentTenant.get();
        return info != null ? info.getOrganizationId() : null;
    }
    
    public static String getRole() {
        TenantInfo info = currentTenant.get();
        return info != null ? info.getRole() : null;
    }
    
    public static Long getUserId() {
        TenantInfo info = currentTenant.get();
        return info != null ? info.getUserId() : null;
    }
    
    public static TenantInfo getTenantInfo() {
        return currentTenant.get();
    }
    
    public static boolean isOwner() {
        return "OWNER".equals(getRole());
    }
    
    public static boolean isAdmin() {
        String role = getRole();
        return "OWNER".equals(role) || "ADMIN".equals(role);
    }
    
    public static void clear() {
        currentTenant.remove();
    }
}
