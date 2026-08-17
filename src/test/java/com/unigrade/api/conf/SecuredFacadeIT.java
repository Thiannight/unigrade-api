package com.unigrade.api.conf;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.unigrade.api.model.Role;
import com.unigrade.api.repository.UserRepository;
import com.unigrade.api.repository.model.JUser;
import com.unigrade.api.security.JwtService;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(classes = SecuredFacadeIT.TestSecurityConfig.class)
public class SecuredFacadeIT extends FacadeIT {

  public static final String IT_ADMIN_ID = "MGR00000";
  public static final String IT_ADMIN_EMAIL = "it-admin@unigrade.test";

  @TestConfiguration
  static class TestSecurityConfig {

    @Bean
    RestTemplateBuilder itAuthenticatedRestTemplateBuilder(
        UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
      seedAdminIfMissing(userRepository, passwordEncoder);
      String token =
          jwtService.generateToken(
              IT_ADMIN_ID, Map.of("id", IT_ADMIN_ID, "role", Role.ADMIN.name()));
      return new RestTemplateBuilder()
          .additionalInterceptors(
              (request, body, execution) -> {
                request.getHeaders().add("Authorization", "Bearer " + token);
                return execution.execute(request, body);
              });
    }

    private void seedAdminIfMissing(
        UserRepository userRepository, PasswordEncoder passwordEncoder) {
      if (userRepository.existsById(IT_ADMIN_ID)) {
        return;
      }
      userRepository.save(
          JUser.builder()
              .id(IT_ADMIN_ID)
              .firstName("Integration")
              .lastName("Admin")
              .birthDate(LocalDate.of(1990, 1, 1))
              .email(IT_ADMIN_EMAIL)
              .password(passwordEncoder.encode("integration-test-password"))
              .isActive(true)
              .role(Role.ADMIN)
              .build());
    }
  }
}