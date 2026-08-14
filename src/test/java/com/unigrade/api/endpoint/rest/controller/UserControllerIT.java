package com.unigrade.api.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import com.fasterxml.jackson.databind.JsonNode;
import com.unigrade.api.conf.FacadeIT;
import java.time.Year;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

class UserControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void crud_lifecycle() {
    String email = "ada-" + UUID.randomUUID() + "@unigrade.com";

    ResponseEntity<JsonNode> createResponse =
        restTemplate.postForEntity("/users", studentBody(email, "Ada"), JsonNode.class);
    assertEquals(CREATED, createResponse.getStatusCode());
    assertNotNull(createResponse.getBody());
    String createdId = createResponse.getBody().get("id").asText();
    String yy = String.format("%02d", Year.now().getValue() % 100);
    assertTrue(createdId.matches("STD" + yy + "\\d{3}"));

    ResponseEntity<JsonNode> getResponse =
        restTemplate.getForEntity("/users/" + createdId, JsonNode.class);
    assertEquals(OK, getResponse.getStatusCode());
    assertEquals("Ada", getResponse.getBody().get("firstName").asText());
    assertFalse(getResponse.getBody().has("password"));

    ResponseEntity<JsonNode[]> listResponse =
        restTemplate.getForEntity("/users?page=0&size=10", JsonNode[].class);
    assertEquals(OK, listResponse.getStatusCode());
    assertNotNull(listResponse.getBody());
    assertTrue(listResponse.getBody().length > 0);

    Map<String, Object> updateBody =
        Map.of(
            "id", createdId,
            "firstName", "Ada-Bis",
            "lastName", "Lovelace",
            "birthDate", "2000-01-01",
            "email", email,
            "password", "hashed-password",
            "isActive", true,
            "role", "STUDENT");
    ResponseEntity<JsonNode> updateResponse =
        restTemplate.exchange(
            "/users/" + createdId, PUT, new HttpEntity<>(updateBody), JsonNode.class);
    assertEquals(OK, updateResponse.getStatusCode());
    assertEquals("Ada-Bis", updateResponse.getBody().get("firstName").asText());

    restTemplate.delete("/users/" + createdId + "/hard");

    ResponseEntity<String> afterDelete =
        restTemplate.getForEntity("/users/" + createdId, String.class);
    assertEquals(NOT_FOUND, afterDelete.getStatusCode());
  }

  @Test
  void create_clientProvidedId_isIgnored() {
    String email = "ignored-" + UUID.randomUUID() + "@unigrade.com";

    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity("/users", studentBody(email, "Ada"), JsonNode.class);

    assertEquals(CREATED, response.getStatusCode());
    assertTrue(response.getBody().get("id").asText().startsWith("STD"));
  }

  @Test
  void create_teacher_generatesTeacherId() {
    Map<String, Object> body =
        Map.of(
            "firstName", "Bob",
            "lastName", "Smith",
            "birthDate", "1990-01-01",
            "email", "bob-" + UUID.randomUUID() + "@unigrade.com",
            "password", "hashed-password",
            "isActive", true,
            "role", "TEACHER");

    ResponseEntity<JsonNode> response = restTemplate.postForEntity("/users", body, JsonNode.class);

    assertEquals(CREATED, response.getStatusCode());
    assertTrue(response.getBody().get("id").asText().matches("TCR\\d{5}"));
  }

  @Test
  void create_invalidEmail_returnsBadRequest() {
    Map<String, Object> body =
        Map.of(
            "firstName", "Ada",
            "lastName", "Lovelace",
            "birthDate", "2000-01-01",
            "email", "not-an-email",
            "password", "hashed-password",
            "isActive", true,
            "role", "STUDENT");

    ResponseEntity<String> response = restTemplate.postForEntity("/users", body, String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void create_duplicateEmail_returnsConflict() {
    String email = "dup-" + UUID.randomUUID() + "@unigrade.com";
    restTemplate.postForEntity("/users", studentBody(email, "Ada"), JsonNode.class);

    ResponseEntity<String> response =
        restTemplate.postForEntity("/users", studentBody(email, "Bob"), String.class);

    assertEquals(CONFLICT, response.getStatusCode());
  }

  @Test
  void getById_missing_returnsNotFound() {
    ResponseEntity<String> response = restTemplate.getForEntity("/users/STD99999", String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void delete_missing_returnsNotFound() {
    ResponseEntity<Void> response =
        restTemplate.exchange("/users/STD99999", DELETE, null, Void.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void delete_default_softDeletes() {
    String email = "soft-" + UUID.randomUUID() + "@unigrade.com";
    ResponseEntity<JsonNode> createResponse =
        restTemplate.postForEntity("/users", studentBody(email, "Ada"), JsonNode.class);
    String createdId = createResponse.getBody().get("id").asText();

    ResponseEntity<Void> deleteResponse =
        restTemplate.exchange("/users/" + createdId, DELETE, null, Void.class);
    assertEquals(NO_CONTENT, deleteResponse.getStatusCode());

    ResponseEntity<JsonNode> getResponse =
        restTemplate.getForEntity("/users/" + createdId, JsonNode.class);
    assertEquals(OK, getResponse.getStatusCode());
    assertFalse(getResponse.getBody().get("isActive").asBoolean());
  }

  @Test
  void delete_hard_removes() {
    String email = "hard-" + UUID.randomUUID() + "@unigrade.com";
    ResponseEntity<JsonNode> createResponse =
        restTemplate.postForEntity("/users", studentBody(email, "Ada"), JsonNode.class);
    String createdId = createResponse.getBody().get("id").asText();

    ResponseEntity<Void> deleteResponse =
        restTemplate.exchange("/users/" + createdId + "/hard", DELETE, null, Void.class);
    assertEquals(NO_CONTENT, deleteResponse.getStatusCode());

    ResponseEntity<String> getResponse =
        restTemplate.getForEntity("/users/" + createdId, String.class);
    assertEquals(NOT_FOUND, getResponse.getStatusCode());
  }

  @Test
  void create_admin_generatesAdminId() {
    Map<String, Object> body =
        Map.of(
            "firstName", "Eve",
            "lastName", "Admin",
            "birthDate", "1985-12-01",
            "email", "eve-" + UUID.randomUUID() + "@unigrade.com",
            "password", "admin-pass",
            "isActive", true,
            "role", "ADMIN");

    ResponseEntity<JsonNode> response = restTemplate.postForEntity("/users", body, JsonNode.class);

    assertEquals(CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().get("id").asText().matches("MGR\\d{5}"));
  }

  @Test
  void create_blankFirstName_returnsBadRequest() {
    Map<String, Object> body =
        Map.of(
            "firstName", "",
            "lastName", "Lovelace",
            "birthDate", "2000-01-01",
            "email", "blank-" + UUID.randomUUID() + "@unigrade.com",
            "password", "hashed-password",
            "isActive", true,
            "role", "STUDENT");

    ResponseEntity<String> response = restTemplate.postForEntity("/users", body, String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void create_invalidRole_returnsBadRequest() {
    Map<String, Object> body =
        Map.of(
            "firstName", "Ada",
            "lastName", "Lovelace",
            "birthDate", "2000-01-01",
            "email", "role-" + UUID.randomUUID() + "@unigrade.com",
            "password", "hashed-password",
            "isActive", true,
            "role", "DEAN");

    ResponseEntity<String> response = restTemplate.postForEntity("/users", body, String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void update_changesEmail() {
    String email = "update-" + UUID.randomUUID() + "@unigrade.com";
    ResponseEntity<JsonNode> createResponse =
        restTemplate.postForEntity("/users", studentBody(email, "Ada"), JsonNode.class);
    String createdId = createResponse.getBody().get("id").asText();

    String newEmail = "changed-" + UUID.randomUUID() + "@unigrade.com";
    Map<String, Object> updateBody =
        Map.of(
            "firstName", "Ada",
            "lastName", "Lovelace",
            "birthDate", "2000-01-01",
            "email", newEmail,
            "password", "hashed-password",
            "isActive", true,
            "role", "STUDENT");
    restTemplate.put("/users/" + createdId, updateBody);

    ResponseEntity<JsonNode> getResponse =
        restTemplate.getForEntity("/users/" + createdId, JsonNode.class);
    assertEquals(OK, getResponse.getStatusCode());
    assertEquals(newEmail, getResponse.getBody().get("email").asText());
  }

  @Test
  void create_mixedCaseEmail_normalizesAndDuplicateConflict() {
    String mixed = "Mixed" + UUID.randomUUID() + "@unigrade.com";
    String lower = mixed.toLowerCase();

    ResponseEntity<JsonNode> createResponse =
        restTemplate.postForEntity("/users", studentBody(mixed, "Ada"), JsonNode.class);
    assertEquals(CREATED, createResponse.getStatusCode());
    assertNotNull(createResponse.getBody());
    assertEquals(lower, createResponse.getBody().get("email").asText());

    ResponseEntity<String> duplicateResponse =
        restTemplate.postForEntity("/users", studentBody(lower, "Bob"), String.class);
    assertEquals(CONFLICT, duplicateResponse.getStatusCode());
  }

  private Map<String, Object> studentBody(String email, String firstName) {
    return Map.of(
        "id", "MGR99999",
        "firstName", firstName,
        "lastName", "Lovelace",
        "birthDate", "2000-01-01",
        "email", email,
        "password", "hashed-password",
        "isActive", true,
        "role", "STUDENT");
  }
}
