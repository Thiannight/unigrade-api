package com.unigrade.api.security;

import com.unigrade.api.exception.ForbiddenException;
import com.unigrade.api.repository.model.JUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

  private SecurityUtils() {}

  public static JUser currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof JUser principal)) {
      throw new ForbiddenException("No authenticated user in context");
    }
    return principal;
  }
}
