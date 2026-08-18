package com.unigrade.api.security;

import com.unigrade.api.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";
  private static final String TOKEN_COOKIE = "token";

  private final JwtService jwtService;
  private final UserService userDetailsService;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String token = resolveToken(request);

    if (token == null) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      authenticateIfValid(token);
    } catch (Exception e) {
      SecurityContextHolder.clearContext();
    }

    filterChain.doFilter(request, response);
  }

  private String resolveToken(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith(BEARER_PREFIX)) {
      return header.substring(BEARER_PREFIX.length());
    }
    if (request.getCookies() != null) {
      return Arrays.stream(request.getCookies())
          .filter(cookie -> TOKEN_COOKIE.equals(cookie.getName()))
          .map(Cookie::getValue)
          .findFirst()
          .orElse(null);
    }
    return null;
  }

  private void authenticateIfValid(String token) {
    if (SecurityContextHolder.getContext().getAuthentication() != null) {
      return;
    }
    String subject = jwtService.extractSubject(token);
    if (subject == null) {
      return;
    }
    UserDetails userDetails = userDetailsService.loadUserByUsername(subject);
    if (!userDetails.isEnabled() || !jwtService.isValid(token, userDetails.getUsername())) {
      return;
    }
    var authToken =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(authToken);
  }
}
