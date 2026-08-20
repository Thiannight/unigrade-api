package com.unigrade.api.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import com.fasterxml.jackson.databind.JsonNode;
import com.unigrade.api.conf.SecuredFacadeIT;
import com.unigrade.api.model.Course;
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

class ExamControllerIT extends SecuredFacadeIT {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void exam_crud_lifecycle() {
    UUID groupId = createGroup("EX-GRP-1", (short) 2090, (short) 2091);
    UUID courseId = createCourse("EX-IT-101", "Exam Course IT Lifecycle");
    assignCourse(groupId, courseId, "2024-01-01");

    ResponseEntity<JsonNode> createResponse =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/courses/" + courseId + "/exams",
            Map.of("examDate", "2024-05-01T09:00:00Z", "coefficient", 0.5),
            JsonNode.class);
    assertEquals(CREATED, createResponse.getStatusCode());
    assertNotNull(createResponse.getBody());
    String examId = createResponse.getBody().get("id").asText();
    assertEquals(0.5, createResponse.getBody().get("coefficient").decimalValue().doubleValue());

    ResponseEntity<JsonNode[]> listResponse =
        restTemplate.getForEntity(
            "/groups/" + groupId + "/courses/" + courseId + "/exams", JsonNode[].class);
    assertEquals(OK, listResponse.getStatusCode());
    assertEquals(1, listResponse.getBody().length);

    restTemplate.exchange(
        "/groups/" + groupId + "/courses/" + courseId + "/exams/" + examId,
        PUT,
        new HttpEntity<>(Map.of("examDate", "2024-05-02T09:00:00Z", "coefficient", 0.3)),
        Void.class);

    ResponseEntity<JsonNode[]> afterUpdate =
        restTemplate.getForEntity(
            "/groups/" + groupId + "/courses/" + courseId + "/exams", JsonNode[].class);
    assertEquals("2024-05-02T09:00:00Z", afterUpdate.getBody()[0].get("examDate").asText());
    assertEquals(0.3, afterUpdate.getBody()[0].get("coefficient").decimalValue().doubleValue());

    ResponseEntity<Void> deleteResponse =
        restTemplate.exchange(
            "/groups/" + groupId + "/courses/" + courseId + "/exams/" + examId,
            DELETE,
            null,
            Void.class);
    assertEquals(NO_CONTENT, deleteResponse.getStatusCode());

    ResponseEntity<JsonNode[]> afterDelete =
        restTemplate.getForEntity(
            "/groups/" + groupId + "/courses/" + courseId + "/exams", JsonNode[].class);
    assertEquals(0, afterDelete.getBody().length);
  }

  @Test
  void getById_returnsExam() {
    UUID groupId = createGroup("EX-GET-1", (short) 2130, (short) 2131);
    UUID courseId = createCourse("EX-IT-GET", "Exam Course IT GetById");
    assignCourse(groupId, courseId, "2024-01-01");
    String examId = createExam(groupId, courseId, "2024-05-01T09:00:00Z", 0.5);

    ResponseEntity<JsonNode> response =
        restTemplate.getForEntity(
            "/groups/" + groupId + "/courses/" + courseId + "/exams/" + examId, JsonNode.class);

    assertEquals(OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(examId, response.getBody().get("id").asText());
    assertEquals(0.5, response.getBody().get("coefficient").decimalValue().doubleValue());
  }

  @Test
  void getById_missingExam_returnsNotFound() {
    UUID groupId = createGroup("EX-GET-2", (short) 2132, (short) 2133);
    UUID courseId = createCourse("EX-IT-GET2", "Exam Course IT GetById Missing");
    assignCourse(groupId, courseId, "2024-01-01");

    ResponseEntity<String> response =
        restTemplate.getForEntity(
            "/groups/" + groupId + "/courses/" + courseId + "/exams/" + UUID.randomUUID(),
            String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void getById_noActiveAssignment_returnsNotFound() {
    UUID groupId = createGroup("EX-GET-3", (short) 2134, (short) 2135);
    UUID courseId = createCourse("EX-IT-GET3", "Exam Course IT GetById No Assignment");

    ResponseEntity<String> response =
        restTemplate.getForEntity(
            "/groups/" + groupId + "/courses/" + courseId + "/exams/" + UUID.randomUUID(),
            String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void create_zeroCoefficient_returnsBadRequest() {
    UUID groupId = createGroup("EX-GRP-ZERO", (short) 2136, (short) 2137);
    UUID courseId = createCourse("EX-IT-ZERO", "Exam Course IT Zero Coefficient");
    assignCourse(groupId, courseId, "2024-01-01");

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/courses/" + courseId + "/exams",
            Map.of("examDate", "2024-05-01T09:00:00Z", "coefficient", 0),
            String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void create_noActiveAssignment_returnsNotFound() {
    UUID groupId = createGroup("EX-GRP-2", (short) 2092, (short) 2093);
    UUID courseId = createCourse("EX-IT-102", "Exam Course IT No Assignment");

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/courses/" + courseId + "/exams",
            Map.of("examDate", "2024-05-01T09:00:00Z", "coefficient", 0.5),
            String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void create_missingGroup_returnsNotFound() {
    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/groups/" + UUID.randomUUID() + "/courses/" + UUID.randomUUID() + "/exams",
            Map.of("examDate", "2024-05-01T09:00:00Z", "coefficient", 0.5),
            String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void create_dateBeforeStart_returnsBadRequest() {
    UUID groupId = createGroup("EX-GRP-3", (short) 2094, (short) 2095);
    UUID courseId = createCourse("EX-IT-103", "Exam Course IT Window");
    assignCourse(groupId, courseId, "2024-01-01");

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/courses/" + courseId + "/exams",
            Map.of("examDate", "2023-12-01T09:00:00Z", "coefficient", 0.5),
            String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void create_onEndedAssignment_succeeds() {
    UUID groupId = createGroup("EX-GRP-4", (short) 2096, (short) 2097);
    UUID courseId = createCourse("EX-IT-104", "Exam Course IT Ended");
    assignCourse(groupId, courseId, "2024-01-01");
    restTemplate.exchange(
        "/groups/" + groupId + "/courses/" + courseId,
        PUT,
        new HttpEntity<>(Map.of("endDate", "2024-06-01")),
        Void.class);

    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/courses/" + courseId + "/exams",
            Map.of("examDate", "2024-05-01T09:00:00Z", "coefficient", 0.5),
            JsonNode.class);

    assertEquals(CREATED, response.getStatusCode());
  }

  @Test
  void create_examsCoefficientExceedingOne_returnsBadRequest() {
    UUID groupId = createGroup("EX-GRP-7", (short) 2102, (short) 2103);
    UUID courseId = createCourse("EX-IT-107", "Exam Course IT Coefficient Budget");
    assignCourse(groupId, courseId, "2024-01-01");
    createExam(groupId, courseId, "2024-05-01T09:00:00Z", 0.6);

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/courses/" + courseId + "/exams",
            Map.of("examDate", "2024-05-02T09:00:00Z", "coefficient", 0.5),
            String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void update_examCoefficientExceedingOne_returnsBadRequest() {
    UUID groupId = createGroup("EX-GRP-8", (short) 2104, (short) 2105);
    UUID courseId = createCourse("EX-IT-108", "Exam Course IT Update Budget");
    assignCourse(groupId, courseId, "2024-01-01");
    createExam(groupId, courseId, "2024-05-01T09:00:00Z", 0.6);
    String examId = createExam(groupId, courseId, "2024-05-02T09:00:00Z", 0.4);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/groups/" + groupId + "/courses/" + courseId + "/exams/" + examId,
            PUT,
            new HttpEntity<>(Map.of("examDate", "2024-05-02T09:00:00Z", "coefficient", 0.5)),
            String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void update_sameExamCoefficientUnchanged_succeeds() {
    UUID groupId = createGroup("EX-GRP-9", (short) 2106, (short) 2107);
    UUID courseId = createCourse("EX-IT-109", "Exam Course IT Update Same Coefficient");
    assignCourse(groupId, courseId, "2024-01-01");
    String examId = createExam(groupId, courseId, "2024-05-01T09:00:00Z", 1.0);

    ResponseEntity<JsonNode> response =
        restTemplate.exchange(
            "/groups/" + groupId + "/courses/" + courseId + "/exams/" + examId,
            PUT,
            new HttpEntity<>(Map.of("examDate", "2024-05-03T09:00:00Z", "coefficient", 1.0)),
            JsonNode.class);

    assertEquals(OK, response.getStatusCode());
    assertEquals("2024-05-03T09:00:00Z", response.getBody().get("examDate").asText());
  }

  @Test
  void update_missingExam_returnsNotFound() {
    UUID groupId = createGroup("EX-GRP-5", (short) 2098, (short) 2099);
    UUID courseId = createCourse("EX-IT-105", "Exam Course IT Missing Exam");
    assignCourse(groupId, courseId, "2024-01-01");

    ResponseEntity<Void> response =
        restTemplate.exchange(
            "/groups/" + groupId + "/courses/" + courseId + "/exams/" + UUID.randomUUID(),
            PUT,
            new HttpEntity<>(Map.of("examDate", "2024-05-01T09:00:00Z", "coefficient", 0.5)),
            Void.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void delete_withGrades_returnsConflict() {
    UUID groupId = createGroup("EX-GRP-6", (short) 2100, (short) 2101);
    UUID courseId = createCourse("EX-IT-106", "Exam Course IT Graded");
    assignCourse(groupId, courseId, "2024-01-01");
    String examId = createExam(groupId, courseId, "2024-05-01T09:00:00Z", 0.5);
    String studentId = createStudent("ex-it-graded-" + UUID.randomUUID() + "@unigrade.com");
    createMembership(groupId, studentId, "2024-01-01");
    restTemplate.postForEntity(
        "/groups/" + groupId + "/courses/" + courseId + "/exams/" + examId + "/grades",
        Map.of(
            "score",
            15.5,
            "gradeDate",
            "2024-05-02T09:00:00Z",
            "reason",
            "Midterm",
            "studentId",
            studentId),
        JsonNode.class);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/groups/" + groupId + "/courses/" + courseId + "/exams/" + examId,
            DELETE,
            null,
            String.class);

    assertEquals(CONFLICT, response.getStatusCode());
  }

  private String createExam(UUID groupId, UUID courseId, String examDate, double coefficient) {
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/courses/" + courseId + "/exams",
            Map.of("examDate", examDate, "coefficient", coefficient),
            JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
    return response.getBody().get("id").asText();
  }

  private void assignCourse(UUID groupId, UUID courseId, String startDate) {
    restTemplate.postForEntity(
        "/groups/" + groupId + "/courses",
        Map.of("courseId", courseId, "semester", "S1", "startDate", startDate),
        JsonNode.class);
  }

  private void createMembership(UUID groupId, String studentId, String startDate) {
    restTemplate.exchange(
        "/students/" + studentId + "/transfer",
        PUT,
        new HttpEntity<>(Map.of("newGroupId", groupId, "transferDate", startDate)),
        JsonNode.class);
  }

  private String createStudent(String email) {
    Map<String, Object> body =
        Map.of(
            "firstName",
            "Ada",
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
            "STUDENT");
    ResponseEntity<JsonNode> response = restTemplate.postForEntity("/users", body, JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
    return response.getBody().get("id").asText();
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

  private UUID createCourse(String reference, String title) {
    var course = new Course(null, reference, title, (short) 6);
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity("/courses", course, JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
    return UUID.fromString(response.getBody().get("id").asText());
  }
}
