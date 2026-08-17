package com.unigrade.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.JwtException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private static final String SECRET = "unit-test-secret-key-must-be-at-least-32-bytes-long!!";

  @Test
  void generateToken_isValidForItsOwnSubject() {
    JwtService service = new JwtService(SECRET, 60);

    String token = service.generateToken("ada@unigrade.com", Map.of("id", "STD00001"));

    assertEquals("ada@unigrade.com", service.extractSubject(token));
    assertTrue(service.isValid(token, "ada@unigrade.com"));
  }

  @Test
  void isValid_wrongSubject_returnsFalse() {
    JwtService service = new JwtService(SECRET, 60);

    String token = service.generateToken("ada@unigrade.com", Map.of());

    assertFalse(service.isValid(token, "someone.else@unigrade.com"));
  }

  @Test
  void isValid_expiredToken_returnsFalse() {
    JwtService service = new JwtService(SECRET, 0);

    String token = service.generateToken("ada@unigrade.com", Map.of());

    assertFalse(service.isValid(token, "ada@unigrade.com"));
  }

  @Test
  void extractSubject_tamperedToken_throws() {
    JwtService service = new JwtService(SECRET, 60);
    String token = service.generateToken("ada@unigrade.com", Map.of());
    String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

    assertThrows(JwtException.class, () -> service.extractSubject(tampered));
  }

  @Test
  void extractSubject_differentSecret_throws() {
    JwtService issuer = new JwtService(SECRET, 60);
    JwtService verifier = new JwtService("a-completely-different-secret-key-of-32-bytes-min", 60);
    String token = issuer.generateToken("ada@unigrade.com", Map.of());

    assertThrows(JwtException.class, () -> verifier.extractSubject(token));
  }
}
