package com.quiz.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("userSecurity")
public class UserSecurity {

    public boolean isSelfOrAdmin(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals)) {
            return true;
        }
        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        return principal.getId().equals(userId);
    }
}
