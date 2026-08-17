package com.unigrade.api.security;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unigrade.api.model.Role;
import com.unigrade.api.repository.model.JUser;
import com.unigrade.api.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class JwtAuthenticationFilterTest {

  private final JwtService jwtService = mock(JwtService.class);
  private final UserService userDetailsService = mock(UserService.class);
  private final JwtAuthenticationFilter filter =
      new JwtAuthenticationFilter(jwtService, userDetailsService);
  private final HttpServletRequest request = mock(HttpServletRequest.class);
  private final HttpServletResponse response = mock(HttpServletResponse.class);
  private final FilterChain chain = mock(FilterChain.class);

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void noHeader_passesThrough() throws Exception {
    when(request.getHeader("Authorization")).thenReturn(null);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void nonBearerHeader_passesThrough() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void validToken_authenticates() throws Exception {
    JUser principal = principal(true);
    when(request.getHeader("Authorization")).thenReturn("Bearer token");
    when(jwtService.extractSubject("token")).thenReturn("STD00001");
    when(userDetailsService.loadUserByUsername("STD00001")).thenReturn(principal);
    when(jwtService.isValid("token", "STD00001")).thenReturn(true);

    filter.doFilter(request, response, chain);

    assertSame(principal, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    verify(chain).doFilter(request, response);
  }

  @Test
  void lookupFailure_clearsContext() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Bearer token");
    when(jwtService.extractSubject("token")).thenReturn("STD00001");
    when(userDetailsService.loadUserByUsername("STD00001"))
        .thenThrow(new UsernameNotFoundException("gone"));

    filter.doFilter(request, response, chain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(chain).doFilter(request, response);
  }

  @Test
  void nullSubject_passesThroughUnauthenticated() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Bearer token");
    when(jwtService.extractSubject("token")).thenReturn(null);

    filter.doFilter(request, response, chain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(chain).doFilter(request, response);
  }

  @Test
  void disabledUser_passesThroughUnauthenticated() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Bearer token");
    when(jwtService.extractSubject("token")).thenReturn("STD00001");
    when(userDetailsService.loadUserByUsername("STD00001")).thenReturn(principal(false));

    filter.doFilter(request, response, chain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(chain).doFilter(request, response);
  }

  @Test
  void invalidToken_passesThroughUnauthenticated() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Bearer token");
    when(jwtService.extractSubject("token")).thenReturn("STD00001");
    when(userDetailsService.loadUserByUsername("STD00001")).thenReturn(principal(true));
    when(jwtService.isValid("token", "STD00001")).thenReturn(false);

    filter.doFilter(request, response, chain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(chain).doFilter(request, response);
  }

  @Test
  void existingAuthentication_skipsReAuthentication() throws Exception {
    JUser principal = principal(true);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    when(request.getHeader("Authorization")).thenReturn("Bearer token");

    filter.doFilter(request, response, chain);

    verify(jwtService, never()).extractSubject(anyString());
    verify(chain).doFilter(request, response);
  }

  private JUser principal(boolean active) {
    return JUser.builder()
        .id("STD00001")
        .firstName("Ada")
        .lastName("Lovelace")
        .birthDate(LocalDate.of(2000, 1, 1))
        .email("STD00001")
        .password("secret")
        .isActive(active)
        .role(Role.STUDENT)
        .build();
  }
}
