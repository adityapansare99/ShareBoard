package com.cb.config;

import com.cb.security.UserPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


@Component
public class UserContext {

    public String getCurrentUserEmail() {
        UserPrincipal principal = principal();
        return principal != null ? principal.getEmail() : "anonymous";
    }

    public String getCurrentUserName() {
        UserPrincipal principal = principal();
        return principal != null ? principal.getName() : "Anonymous";
    }

    public String getCurrentUserAvatar() {
        return "";
    }

    public boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // AnonymousAuthenticationToken.isAuthenticated() returns true — must exclude it,
        // otherwise permitAll endpoints like /api/auth/me would report logged-in.
        return auth != null
            && auth.isAuthenticated()
            && !(auth instanceof AnonymousAuthenticationToken)
            && auth.getPrincipal() instanceof UserPrincipal;
    }

    private UserPrincipal principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            return up;
        }
        return null;
    }
}
