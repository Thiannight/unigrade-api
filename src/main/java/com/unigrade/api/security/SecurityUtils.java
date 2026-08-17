package com.unigrade.api.security;

import com.unigrade.api.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

  private SecurityUtils() {}

  public static AppUserPrincipal currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
      throw new ForbiddenException("No authenticated user in context");
    }
    return principal;
  }
}
