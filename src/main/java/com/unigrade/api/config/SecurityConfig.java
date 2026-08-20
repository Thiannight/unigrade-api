package com.unigrade.api.config;

import com.unigrade.api.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .httpBasic(basic -> basic.disable())
        .formLogin(form -> form.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/auth/login", "/login")
                    .permitAll()
                    .requestMatchers("/ping", "/health/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/groups/*/courses/*/exams/*/grades")
                    .authenticated()
                    .requestMatchers("/groups/*/courses/*/exams/*/grades")
                    .hasAnyRole("ADMIN", "TEACHER")
                    .requestMatchers(HttpMethod.GET, "/groups/*/courses/*/exams")
                    .hasAnyRole("ADMIN", "TEACHER")
                    .requestMatchers(HttpMethod.POST, "/groups/*/courses/*/exams")
                    .hasAnyRole("ADMIN", "TEACHER")
                    .requestMatchers("/groups/*/courses/*/exams/*")
                    .hasAnyRole("ADMIN", "TEACHER")
                    .requestMatchers(HttpMethod.POST, "/users")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/users/*")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/users/*/hard")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/users/*")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/users")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/students/*/transfer")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/students/*/memberships")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/students/*/report")
                    .hasAnyRole("ADMIN", "STUDENT")
                    .requestMatchers(HttpMethod.GET, "/courses/**", "/promotions/**", "/groups/**")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/courses/**", "/promotions/**", "/groups/**")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/courses/**", "/promotions/**", "/groups/**")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        HttpMethod.DELETE, "/courses/**", "/promotions/**", "/groups/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .hasRole("ADMIN"))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
