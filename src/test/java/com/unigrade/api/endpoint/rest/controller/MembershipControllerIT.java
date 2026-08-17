package com.unigrade.api.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import com.fasterxml.jackson.databind.JsonNode;
import com.unigrade.api.conf.SecuredFacadeIT;
import com.unigrade.api.model.Membership;
import com.unigrade.api.model.Promotion;
import com.unigrade.api.model.StudentGroup;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

class MembershipControllerIT extends SecuredFacadeIT {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void assign_transfer_reassign_lifecycle() {
    UUID groupA = createGroup("MEM-LIFE-1", (short) 2050, (short) 2051);
    UUID groupB = createGroup("MEM-LIFE-2", (short) 2052, (short) 2053);
    String studentId = createUser("mem-life-" + UUID.randomUUID() + "@unigrade.com", "Ada");

    ResponseEntity<JsonNode> assignResponse =
        restTemplate.postForEntity(
            "/groups/" + groupA + "/members",
            Map.of("studentId", studentId, "startDate", "2024-01-01"),
            JsonNode.class);
    assertEquals(CREATED, assignResponse.getStatusCode());
    assertNotNull(assignResponse.getBody());
    assertEquals(studentId, assignResponse.getBody().get("studentId").asText());
    assertEquals("2024-01-01", assignResponse.getBody().get("startDate").asText());

    ResponseEntity<Membership[]> activeResponse =
        restTemplate.getForEntity("/groups/" + groupA + "/members", Membership[].class);
    assertEquals(OK, activeResponse.getStatusCode());
    assertEquals(1, activeResponse.getBody().length);

    ResponseEntity<Void> transferResponse =
        restTemplate.exchange(
            "/groups/" + groupA + "/members/" + studentId,
            PUT,
            new HttpEntity<>(
                Map.<String, Object>of("newGroupId", groupB, "transferDate", "2024-06-01")),
            Void.class);
    assertEquals(NO_CONTENT, transferResponse.getStatusCode());

    ResponseEntity<Membership[]> inOldGroupBefore =
        restTemplate.getForEntity(
            "/groups/" + groupA + "/members?at=2024-03-01", Membership[].class);
    assertEquals(1, inOldGroupBefore.getBody().length);

    ResponseEntity<Membership[]> inOldGroupAfter =
        restTemplate.getForEntity(
            "/groups/" + groupA + "/members?at=2024-07-01", Membership[].class);
    assertEquals(0, inOldGroupAfter.getBody().length);

    ResponseEntity<Membership[]> inNewGroup =
        restTemplate.getForEntity(
            "/groups/" + groupB + "/members?at=2024-07-01", Membership[].class);
    assertEquals(1, inNewGroup.getBody().length);

    ResponseEntity<Void> transferBackResponse =
        restTemplate.exchange(
            "/groups/" + groupB + "/members/" + studentId,
            PUT,
            new HttpEntity<>(
                Map.<String, Object>of("newGroupId", groupA, "transferDate", "2024-09-01")),
            Void.class);
    assertEquals(NO_CONTENT, transferBackResponse.getStatusCode());

    ResponseEntity<Membership[]> afterRejoin =
        restTemplate.getForEntity("/groups/" + groupA + "/members", Membership[].class);
    assertEquals(1, afterRejoin.getBody().length);
  }

  @Test
  void assign_notStudent_returnsBadRequest() {
    UUID groupId = createGroup("MEM-NS-1", (short) 2054, (short) 2055);
    String teacherId =
        createUser("mem-ns-" + UUID.randomUUID() + "@unigrade.com", "Bob", "TEACHER");

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/members",
            Map.of("studentId", teacherId, "startDate", "2024-01-01"),
            String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void assign_inactiveStudent_returnsBadRequest() {
    UUID groupId = createGroup("MEM-IN-1", (short) 2056, (short) 2057);
    String studentId = createUser("mem-in-" + UUID.randomUUID() + "@unigrade.com", "Ada");

    restTemplate.delete("/users/" + studentId);

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/members",
            Map.of("studentId", studentId, "startDate", "2024-01-01"),
            String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void assign_duplicate_returnsBadRequest() {
    UUID groupId = createGroup("MEM-DUP-1", (short) 2058, (short) 2059);
    String studentId = createUser("mem-dup-" + UUID.randomUUID() + "@unigrade.com", "Ada");

    restTemplate.postForEntity(
        "/groups/" + groupId + "/members",
        Map.of("studentId", studentId, "startDate", "2024-01-01"),
        JsonNode.class);

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/members",
            Map.of("studentId", studentId, "startDate", "2024-05-01"),
            String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void getMembersAt_missingGroup_returnsNotFound() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/groups/" + UUID.randomUUID() + "/members", String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void transfer_noActiveMembership_returnsNotFound() {
    UUID groupId = createGroup("MEM-NM-1", (short) 2060, (short) 2061);
    String studentId = createUser("mem-nm-" + UUID.randomUUID() + "@unigrade.com", "Ada");

    ResponseEntity<Void> response =
        restTemplate.exchange(
            "/groups/" + groupId + "/members/" + studentId,
            PUT,
            new HttpEntity<>(
                Map.<String, Object>of(
                    "newGroupId", UUID.randomUUID(), "transferDate", "2024-06-01")),
            Void.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void getMembersAt_excludesInactiveUnlessRequested() {
    UUID groupId = createGroup("MEM-IA-1", (short) 2062, (short) 2063);
    String studentId = createUser("mem-ia-" + UUID.randomUUID() + "@unigrade.com", "Ada");

    restTemplate.postForEntity(
        "/groups/" + groupId + "/members",
        Map.of("studentId", studentId, "startDate", "2024-01-01"),
        JsonNode.class);
    restTemplate.delete("/users/" + studentId);

    ResponseEntity<Membership[]> defaultResponse =
        restTemplate.getForEntity("/groups/" + groupId + "/members", Membership[].class);
    assertEquals(OK, defaultResponse.getStatusCode());
    assertEquals(0, defaultResponse.getBody().length);

    ResponseEntity<Membership[]> withInactive =
        restTemplate.getForEntity(
            "/groups/" + groupId + "/members?includeInactive=true", Membership[].class);
    assertEquals(OK, withInactive.getStatusCode());
    assertEquals(1, withInactive.getBody().length);
  }

  private UUID createPromotion(String reference, Short startYear, Short endYear) {
    var promotion = new Promotion(null, reference, startYear, endYear);
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity("/promotions", promotion, JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
    return UUID.fromString(response.getBody().get("id").asText());
  }

  private UUID createGroup(String reference, Short startYear, Short endYear) {
    UUID promotionId = createPromotion(reference, startYear, endYear);
    var group =
        new StudentGroup(null, "A" + (1 + (int) (LocalDate.now().toEpochDay() % 9)), promotionId);
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity("/groups", group, JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
    return UUID.fromString(response.getBody().get("id").asText());
  }

  private String createUser(String email, String firstName) {
    return createUser(email, firstName, "STUDENT");
  }

  private String createUser(String email, String firstName, String role) {
    Map<String, Object> body =
        Map.of(
            "firstName",
            firstName,
            "lastName",
            "Lovelace",
            "birthDate",
            "2000-01-01",
            "email",
            email,
            "password",
            "hashed-password",
            "isActive",
            true,
            "role",
            role);
    ResponseEntity<JsonNode> response = restTemplate.postForEntity("/users", body, JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
    return response.getBody().get("id").asText();
  }
}
