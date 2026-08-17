package com.unigrade.api.conf;

import static java.lang.Runtime.getRuntime;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.unigrade.api.PojaGenerated;
import com.unigrade.api.model.Role;
import com.unigrade.api.repository.UserRepository;
import com.unigrade.api.repository.model.JUser;
import com.unigrade.api.security.JwtService;
import java.time.LocalDate;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@PojaGenerated
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(classes = FacadeIT.TestSecurityConfig.class)
@Slf4j
public class FacadeIT {

  public static final String IT_ADMIN_ID = "MGR00000";
  public static final String IT_ADMIN_EMAIL = "it-admin@unigrade.test";

  private static final PostgresConf POSTGRES_CONF = new PostgresConf();

  @BeforeAll
  static void beforeAll() {
    POSTGRES_CONF.start();
    getRuntime().addShutdownHook(new Thread(POSTGRES_CONF::stop));
  }

  @SneakyThrows
  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    POSTGRES_CONF.configureProperties(registry);
    new BucketConf().configureProperties(registry);
    new EmailConf().configureProperties(registry);

    try {
      var envConfClazz = Class.forName("com.unigrade.api.conf.EnvConf");
      var envConfConfigureProperties =
          envConfClazz.getDeclaredMethod("configureProperties", DynamicPropertyRegistry.class);
      var envConf = envConfClazz.getConstructor().newInstance();
      envConfConfigureProperties.invoke(envConf, registry);
    } catch (ClassNotFoundException e) {
      log.warn("EnvConf missing: no project-specific test env vars will be set");
    }
  }

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
