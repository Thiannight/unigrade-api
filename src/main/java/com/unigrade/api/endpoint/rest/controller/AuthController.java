package com.unigrade.api.endpoint.rest.controller;

import com.unigrade.api.model.dto.LoginRequest;
import com.unigrade.api.model.dto.LoginResponse;
import com.unigrade.api.repository.model.JUser;
import com.unigrade.api.security.JwtService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    var authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.userId(), request.password()));

    var principal = (JUser) authentication.getPrincipal();
    String token =
        jwtService.generateToken(
            principal.getUsername(),
            Map.of("id", principal.getId(), "role", principal.getRole().name()));

    return new LoginResponse(token, principal.getId(), principal.getRole().name());
  }
}
