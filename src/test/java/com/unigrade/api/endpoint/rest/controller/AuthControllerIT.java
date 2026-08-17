package com.unigrade.api.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpStatus.OK;

import com.fasterxml.jackson.databind.JsonNode;
import com.unigrade.api.conf.SecuredFacadeIT;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

class AuthControllerIT extends SecuredFacadeIT {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void login_validCredentials_returnsTokenAndUser() {
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity(
            "/auth/login",
            Map.of("userId", IT_ADMIN_ID, "password", "integration-test-password"),
            JsonNode.class);

    assertEquals(OK, response.getStatusCode());
    assertNotNull(response.getBody().get("accessToken"));
    assertEquals(IT_ADMIN_ID, response.getBody().get("userId").asText());
    assertEquals("ADMIN", response.getBody().get("role").asText());
  }
}
