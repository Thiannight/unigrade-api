package com.unigrade.api.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import com.fasterxml.jackson.databind.JsonNode;
import com.unigrade.api.conf.FacadeIT;
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

class GradeControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void grade_historizationLifecycle() {
    UUID groupId = createGroup("GR-GRP-1", (short) 2110, (short) 2111);
    UUID courseId = createCourse("GR-IT-101", "Grade Course IT Lifecycle");
    assignCourse(groupId, courseId, "2024-01-01");
    String examId = createExam(groupId, courseId, "2024-05-01T09:00:00Z");
    String studentId = createStudent("gr-it-life-" + UUID.randomUUID() + "@unigrade.com");
    createMembership(groupId, studentId, "2024-01-01");

    ResponseEntity<JsonNode> first =
        restTemplate.postForEntity(
            gradesUrl(groupId, courseId, examId),
            Map.of(
                "score",
                12.0,
                "gradeDate",
                "2024-05-02T09:00:00Z",
                "reason",
                "Midterm",
                "studentId",
                studentId),
            JsonNode.class);
    assertEquals(CREATED, first.getStatusCode());

    ResponseEntity<JsonNode> second =
        restTemplate.postForEntity(
            gradesUrl(groupId, courseId, examId),
            Map.of(
                "score",
                16.5,
                "gradeDate",
                "2024-05-03T09:00:00Z",
                "reason",
                "Corrected",
                "studentId",
                studentId),
            JsonNode.class);
    assertEquals(CREATED, second.getStatusCode());

    ResponseEntity<JsonNode[]> listResponse =
        restTemplate.getForEntity(gradesUrl(groupId, courseId, examId), JsonNode[].class);
    assertEquals(OK, listResponse.getStatusCode());
    assertNotNull(listResponse.getBody());
    assertEquals(2, listResponse.getBody().length);
    assertEquals(12.0, listResponse.getBody()[0].get("score").decimalValue().doubleValue());
    assertEquals(16.5, listResponse.getBody()[1].get("score").decimalValue().doubleValue());

    ResponseEntity<JsonNode[]> filtered =
        restTemplate.getForEntity(
            gradesUrl(groupId, courseId, examId) + "?studentId=" + studentId, JsonNode[].class);
    assertNotNull(filtered.getBody());
    assertEquals(2, filtered.getBody().length);

    ResponseEntity<JsonNode[]> otherFiltered =
        restTemplate.getForEntity(
            gradesUrl(groupId, courseId, examId) + "?studentId=STD99999", JsonNode[].class);
    assertNotNull(otherFiltered.getBody());
    assertEquals(0, otherFiltered.getBody().length);
  }

  @Test
  void grade_notMember_returnsBadRequest() {
    UUID groupId = createGroup("GR-GRP-2", (short) 2112, (short) 2113);
    UUID courseId = createCourse("GR-IT-102", "Grade Course IT Not Member");
    assignCourse(groupId, courseId, "2024-01-01");
    String examId = createExam(groupId, courseId, "2024-05-01T09:00:00Z");
    String studentId = createStudent("gr-it-notmember-" + UUID.randomUUID() + "@unigrade.com");

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            gradesUrl(groupId, courseId, examId),
            Map.of(
                "score",
                12.0,
                "gradeDate",
                "2024-05-02T09:00:00Z",
                "reason",
                "Midterm",
                "studentId",
                studentId),
            String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void grade_missingStudent_returnsNotFound() {
    UUID groupId = createGroup("GR-GRP-3", (short) 2114, (short) 2115);
    UUID courseId = createCourse("GR-IT-103", "Grade Course IT Missing Student");
    assignCourse(groupId, courseId, "2024-01-01");
    String examId = createExam(groupId, courseId, "2024-05-01T09:00:00Z");

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            gradesUrl(groupId, courseId, examId),
            Map.of(
                "score", 12.0,
                "gradeDate", "2024-05-02T09:00:00Z",
                "reason", "Midterm",
                "studentId", "STD99999"),
            String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void grade_missingExam_returnsNotFound() {
    UUID groupId = createGroup("GR-GRP-4", (short) 2116, (short) 2117);
    UUID courseId = createCourse("GR-IT-104", "Grade Course IT Missing Exam");
    assignCourse(groupId, courseId, "2024-01-01");
    String studentId = createStudent("gr-it-miss-exam-" + UUID.randomUUID() + "@unigrade.com");
    createMembership(groupId, studentId, "2024-01-01");

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/groups/"
                + groupId
                + "/courses/"
                + courseId
                + "/exams/"
                + UUID.randomUUID()
                + "/grades",
            Map.of(
                "score",
                12.0,
                "gradeDate",
                "2024-05-02T09:00:00Z",
                "reason",
                "Midterm",
                "studentId",
                studentId),
            String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void grade_scoreAbove20_returnsBadRequest() {
    UUID groupId = createGroup("GR-GRP-5", (short) 2118, (short) 2119);
    UUID courseId = createCourse("GR-IT-105", "Grade Course IT Score Range");
    assignCourse(groupId, courseId, "2024-01-01");
    String examId = createExam(groupId, courseId, "2024-05-01T09:00:00Z");
    String studentId = createStudent("gr-it-score-" + UUID.randomUUID() + "@unigrade.com");
    createMembership(groupId, studentId, "2024-01-01");

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            gradesUrl(groupId, courseId, examId),
            Map.of(
                "score",
                21.0,
                "gradeDate",
                "2024-05-02T09:00:00Z",
                "reason",
                "Midterm",
                "studentId",
                studentId),
            String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void grade_invalidStudentId_returnsBadRequest() {
    UUID groupId = createGroup("GR-GRP-6", (short) 2120, (short) 2121);
    UUID courseId = createCourse("GR-IT-106", "Grade Course IT Invalid Id");
    assignCourse(groupId, courseId, "2024-01-01");
    String examId = createExam(groupId, courseId, "2024-05-01T09:00:00Z");

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            gradesUrl(groupId, courseId, examId),
            Map.of(
                "score", 12.0,
                "gradeDate", "2024-05-02T09:00:00Z",
                "reason", "Midterm",
                "studentId", "XXX"),
            String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void get_missingExam_returnsNotFound() {
    UUID groupId = createGroup("GR-GRP-7", (short) 2122, (short) 2123);
    UUID courseId = createCourse("GR-IT-107", "Grade Course IT Get Missing Exam");
    assignCourse(groupId, courseId, "2024-01-01");

    ResponseEntity<String> response =
        restTemplate.getForEntity(
            "/groups/"
                + groupId
                + "/courses/"
                + courseId
                + "/exams/"
                + UUID.randomUUID()
                + "/grades",
            String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void grade_afterAssignmentEnded_succeeds() {
    UUID groupId = createGroup("GR-GRP-8", (short) 2124, (short) 2125);
    UUID courseId = createCourse("GR-IT-108", "Grade Course IT Ended Assignment");
    assignCourse(groupId, courseId, "2024-01-01");
    String studentId = createStudent("gr-it-ended-" + UUID.randomUUID() + "@unigrade.com");
    createMembership(groupId, studentId, "2024-01-01");

    String examId = createExam(groupId, courseId, "2024-05-01T09:00:00Z");

    restTemplate.exchange(
        "/groups/" + groupId + "/courses/" + courseId,
        PUT,
        new HttpEntity<>(Map.of("endDate", "2024-06-01")),
        JsonNode.class);

    ResponseEntity<JsonNode> gradeResponse =
        restTemplate.postForEntity(
            gradesUrl(groupId, courseId, examId),
            Map.of(
                "score",
                12.0,
                "gradeDate",
                "2024-07-01T09:00:00Z",
                "reason",
                "Late grade",
                "studentId",
                studentId),
            JsonNode.class);
    assertEquals(CREATED, gradeResponse.getStatusCode());
  }

  private String createExam(UUID groupId, UUID courseId, String examDate) {
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/courses/" + courseId + "/exams",
            Map.of("examDate", examDate, "coefficient", 0.5),
            JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    return response.getBody().get("id").asText();
  }

  private String gradesUrl(UUID groupId, UUID courseId, String examId) {
    return "/groups/" + groupId + "/courses/" + courseId + "/exams/" + examId + "/grades";
  }

  private void assignCourse(UUID groupId, UUID courseId, String startDate) {
    restTemplate.postForEntity(
        "/groups/" + groupId + "/courses",
        Map.of("courseId", courseId, "semester", "S1", "startDate", startDate),
        JsonNode.class);
  }

  private void createMembership(UUID groupId, String studentId, String startDate) {
    restTemplate.postForEntity(
        "/groups/" + groupId + "/members",
        Map.of("studentId", studentId, "startDate", startDate),
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
    assertNotNull(response.getBody());
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
    assertNotNull(response.getBody());
    return UUID.fromString(response.getBody().get("id").asText());
  }

  private UUID createCourse(String reference, String title) {
    var course = new Course(null, reference, title, (short) 6);
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity("/courses", course, JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    return UUID.fromString(response.getBody().get("id").asText());
  }
}
