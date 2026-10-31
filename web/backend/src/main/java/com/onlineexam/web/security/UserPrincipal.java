package com.onlineexam.web.security;

import java.io.Serializable;

/**
 * Principal object representing authenticated user in Spring Security context.
 */
public record UserPrincipal(Long id, String username, String role) implements Serializable {
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
