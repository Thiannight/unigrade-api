package com.unigrade.api.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import com.fasterxml.jackson.databind.JsonNode;
import com.unigrade.api.conf.SecuredFacadeIT;
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
  void transfer_noActiveMembership_createsMembership() {
    UUID groupA = createGroup("MEM-CR-1", (short) 2050, (short) 2051);
    String studentId = createUser("mem-cr-" + UUID.randomUUID() + "@unigrade.com", "Ada");

    ResponseEntity<JsonNode> response =
        restTemplate.exchange(
            "/students/" + studentId + "/transfer",
            PUT,
            new HttpEntity<>(
                Map.<String, Object>of("newGroupId", groupA, "transferDate", "2024-01-01")),
            JsonNode.class);

    assertEquals(OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(studentId, response.getBody().get("studentId").asText());
    assertEquals(groupA.toString(), response.getBody().get("groupId").asText());

    ResponseEntity<JsonNode[]> members =
        restTemplate.getForEntity("/groups/" + groupA + "/members", JsonNode[].class);
    assertEquals(1, members.getBody().length);
  }

  @Test
  void transfer_incompleteCourses_returnsBadRequest() {
    UUID groupA = createGroup("MEM-INC-1", (short) 2052, (short) 2053);
    String studentId = createUser("mem-inc-" + UUID.randomUUID() + "@unigrade.com", "Ada");

    transferTo(studentId, groupA, "2024-01-01");

    UUID courseId = createCourse("INC-C1", "Incomplete Course");
    assignCourse(groupA, courseId, "S1", "2024-01-01");
    createExam(groupA, courseId, "2024-03-01T09:00:00Z", 1.0);

    UUID groupB = createGroup("MEM-INC-2", (short) 2054, (short) 2055);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/students/" + studentId + "/transfer",
            PUT,
            new HttpEntity<>(
                Map.<String, Object>of("newGroupId", groupB, "transferDate", "2024-06-01")),
            String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void transfer_insufficientCredits_returnsBadRequest() {
    UUID groupA = createGroup("MEM-CRD-1", (short) 2056, (short) 2057);
    String studentId = createUser("mem-crd-" + UUID.randomUUID() + "@unigrade.com", "Ada");

    transferTo(studentId, groupA, "2024-01-01");

    UUID courseId = createCourse("CRD-C1", "Some Course");
    assignCourse(groupA, courseId, "S1", "2024-01-01");
    UUID examId = createExam(groupA, courseId, "2024-03-01T09:00:00Z", 1.0);
    createGrade(groupA, courseId, examId, studentId, 12.0f);

    UUID groupB = createGroup("MEM-CRD-2", (short) 2058, (short) 2059);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/students/" + studentId + "/transfer",
            PUT,
            new HttpEntity<>(
                Map.<String, Object>of("newGroupId", groupB, "transferDate", "2024-06-01")),
            String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void transfer_fullLifecycle() {
    UUID groupA = createGroup("MEM-LF-1", (short) 2060, (short) 2061);
    UUID groupB = createGroup("MEM-LF-2", (short) 2062, (short) 2063);
    String studentId = createUser("mem-lf-" + UUID.randomUUID() + "@unigrade.com", "Ada");

    transferTo(studentId, groupA, "2024-01-01");

    setupCompleteCourses(groupA, studentId, "2024-01-01", "2024-03-01T09:00:00Z");

    ResponseEntity<JsonNode> transferResponse =
        restTemplate.exchange(
            "/students/" + studentId + "/transfer",
            PUT,
            new HttpEntity<>(
                Map.<String, Object>of("newGroupId", groupB, "transferDate", "2024-06-01")),
            JsonNode.class);
    assertEquals(OK, transferResponse.getStatusCode());
    assertEquals(groupB.toString(), transferResponse.getBody().get("groupId").asText());

    ResponseEntity<JsonNode[]> inOldGroup =
        restTemplate.getForEntity("/groups/" + groupA + "/members?at=2024-07-01", JsonNode[].class);
    assertEquals(0, inOldGroup.getBody().length);

    ResponseEntity<JsonNode[]> inNewGroup =
        restTemplate.getForEntity("/groups/" + groupB + "/members?at=2024-07-01", JsonNode[].class);
    assertEquals(1, inNewGroup.getBody().length);
  }

  @Test
  void transfer_toSameGroup_returnsBadRequest() {
    UUID groupA = createGroup("MEM-SG-1", (short) 2064, (short) 2065);
    String studentId = createUser("mem-sg-" + UUID.randomUUID() + "@unigrade.com", "Ada");

    transferTo(studentId, groupA, "2024-01-01");

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/students/" + studentId + "/transfer",
            PUT,
            new HttpEntity<>(
                Map.<String, Object>of("newGroupId", groupA, "transferDate", "2024-06-01")),
            String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void transfer_inactiveStudent_returnsBadRequest() {
    UUID groupA = createGroup("MEM-IA-1", (short) 2066, (short) 2067);
    UUID groupB = createGroup("MEM-IA-2", (short) 2068, (short) 2069);
    String studentId = createUser("mem-ia-" + UUID.randomUUID() + "@unigrade.com", "Ada");

    transferTo(studentId, groupA, "2024-01-01");
    restTemplate.delete("/users/" + studentId);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/students/" + studentId + "/transfer",
            PUT,
            new HttpEntity<>(
                Map.<String, Object>of("newGroupId", groupB, "transferDate", "2024-06-01")),
            String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void getMemberships_returnsList() {
    UUID groupA = createGroup("MEM-GM-1", (short) 3100, (short) 3101);
    String studentId = createUser("mem-gm-" + UUID.randomUUID() + "@unigrade.com", "Ada");

    transferTo(studentId, groupA, "2024-01-01");

    ResponseEntity<JsonNode[]> response =
        restTemplate.getForEntity("/students/" + studentId + "/memberships", JsonNode[].class);

    assertEquals(OK, response.getStatusCode());
    assertEquals(1, response.getBody().length);
    assertEquals(groupA.toString(), response.getBody()[0].get("groupId").asText());
  }

  @Test
  void getMembersAt_missingGroup_returnsNotFound() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/groups/" + UUID.randomUUID() + "/members", String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void getMembersAt_excludesInactiveUnlessRequested() {
    UUID groupId = createGroup("MEM-IE-1", (short) 3102, (short) 3103);
    String studentId = createUser("mem-ie-" + UUID.randomUUID() + "@unigrade.com", "Ada");

    transferTo(studentId, groupId, "2024-01-01");
    restTemplate.delete("/users/" + studentId);

    ResponseEntity<JsonNode[]> defaultResponse =
        restTemplate.getForEntity("/groups/" + groupId + "/members", JsonNode[].class);
    assertEquals(OK, defaultResponse.getStatusCode());
    assertEquals(0, defaultResponse.getBody().length);

    ResponseEntity<JsonNode[]> withInactive =
        restTemplate.getForEntity(
            "/groups/" + groupId + "/members?includeInactive=true", JsonNode[].class);
    assertEquals(OK, withInactive.getStatusCode());
    assertEquals(1, withInactive.getBody().length);
  }

  private void transferTo(String studentId, UUID groupId, String transferDate) {
    ResponseEntity<JsonNode> response =
        restTemplate.exchange(
            "/students/" + studentId + "/transfer",
            PUT,
            new HttpEntity<>(
                Map.<String, Object>of("newGroupId", groupId, "transferDate", transferDate)),
            JsonNode.class);
    assertEquals(OK, response.getStatusCode());
  }

  private void setupCompleteCourses(
      UUID groupId, String studentId, String courseStartDate, String examDate) {
    for (int i = 0; i < 5; i++) {
      UUID courseId = createCourse("LC-C" + i, "Course " + i);
      assignCourse(groupId, courseId, "S1", courseStartDate);
      UUID examId = createExam(groupId, courseId, examDate, 1.0);
      createGrade(groupId, courseId, examId, studentId, 12.0f);
    }
  }

  private UUID createCourse(String ref, String title) {
    Map<String, Object> body = Map.of("reference", ref, "title", title, "credits", (short) 6);
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity("/courses", body, JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
    return UUID.fromString(response.getBody().get("id").asText());
  }

  private void assignCourse(UUID groupId, UUID courseId, String semester, String startDate) {
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/courses",
            Map.of("courseId", courseId, "semester", semester, "startDate", startDate),
            JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
  }

  private UUID createExam(UUID groupId, UUID courseId, String examDate, double coefficient) {
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/courses/" + courseId + "/exams",
            Map.of("examDate", examDate, "coefficient", coefficient),
            JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
    return UUID.fromString(response.getBody().get("id").asText());
  }

  private void createGrade(
      UUID groupId, UUID courseId, UUID examId, String studentId, float score) {
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/courses/" + courseId + "/exams/" + examId + "/grades",
            Map.of(
                "studentId",
                studentId,
                "score",
                score,
                "gradeDate",
                "2024-03-02T09:00:00Z",
                "reason",
                "Exam"),
            JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
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
