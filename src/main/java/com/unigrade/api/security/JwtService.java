package com.unigrade.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

  private final SecretKey key;
  private final long expirationMillis;

  public JwtService(
      @Value("${poja.security.jwt.secret}") String secret,
      @Value("${poja.security.jwt.expiration-minutes:60}") long expirationMinutes) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMillis = expirationMinutes * 60_000;
  }

  public String generateToken(String subjectId, Map<String, Object> extraClaims) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + expirationMillis);
    return Jwts.builder()
        .claims(extraClaims)
        .subject(subjectId)
        .issuedAt(now)
        .expiration(expiry)
        .signWith(key)
        .compact();
  }

  public String extractSubject(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public boolean isValid(String token, String expectedSubject) {
    try {
      Claims claims = parseClaims(token);
      return expectedSubject.equals(claims.getSubject())
          && claims.getExpiration().after(new Date());
    } catch (JwtException e) {
      return false;
    }
  }

  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  private <T> T extractClaim(String token, Function<Claims, T> resolver) {
    return resolver.apply(parseClaims(token));
  }
}
