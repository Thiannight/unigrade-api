package com.unigrade.api.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
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

class StudentReportIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void report_buildsLevelReport() {
    UUID groupId = createGroup("REP-P-1", (short) 2230, (short) 2231);
    UUID courseId = createCourse("REP-C-101", "Report Course Happy Path");
    assignCourse(groupId, courseId, "2024-01-01", "S3");
    String studentId = createStudent("rep-it-happy-" + UUID.randomUUID() + "@unigrade.com");
    createMembership(groupId, studentId, "2024-01-01");
    String exam1Id = createExam(groupId, courseId, "2024-05-01T09:00:00Z", 0.4);
    String exam2Id = createExam(groupId, courseId, "2024-06-01T09:00:00Z", 0.6);
    grade(groupId, courseId, exam1Id, studentId, 10.0, "2024-05-02T09:00:00Z");
    grade(groupId, courseId, exam1Id, studentId, 12.0, "2024-05-03T09:00:00Z");
    grade(groupId, courseId, exam2Id, studentId, 16.0, "2024-06-02T09:00:00Z");

    ResponseEntity<JsonNode> response =
        restTemplate.getForEntity("/students/" + studentId + "/report", JsonNode.class);

    assertEquals(OK, response.getStatusCode());
    JsonNode body = response.getBody();
    assertNotNull(body);
    assertEquals(studentId, body.get("studentId").asText());
    assertEquals(1, body.get("levels").size());
    assertEquals("L2", body.get("levels").get(0).get("level").asText());
    assertEquals(
        "REP-P-1",
        body.get("levels").get(0).get("courses").get(0).get("promotionReference").asText());
    assertEquals(
        "REP-C-101", body.get("levels").get(0).get("courses").get(0).get("reference").asText());
    assertEquals(6, body.get("levels").get(0).get("courses").get(0).get("credits").asInt());
    assertTrue(body.get("levels").get(0).get("courses").get(0).get("completed").asBoolean());
    assertEquals(2, body.get("levels").get(0).get("courses").get(0).get("exams").size());
    assertEquals(
        12.0,
        body.get("levels")
            .get(0)
            .get("courses")
            .get(0)
            .get("exams")
            .get(0)
            .get("score")
            .asDouble());
    assertEquals(
        16.0,
        body.get("levels")
            .get(0)
            .get("courses")
            .get(0)
            .get("exams")
            .get(1)
            .get("score")
            .asDouble());
    assertEquals(14.40, body.get("levels").get(0).get("courses").get(0).get("average").asDouble());
    assertEquals(14.40, body.get("levels").get(0).get("overallAverage").asDouble());
    assertEquals(14.40, body.get("overallAverage").asDouble());
  }

  @Test
  void report_repeat_hidesFailedYear() {
    UUID oldGroupId = createGroup("REP-P-2", (short) 2232, (short) 2233);
    UUID oldCourseId = createCourse("REP-C-201", "Report Course Failed Year");
    assignCourse(oldGroupId, oldCourseId, "2024-01-01", "S3");
    String studentId = createStudent("rep-it-repeat-" + UUID.randomUUID() + "@unigrade.com");
    createMembership(oldGroupId, studentId, "2024-01-01");
    String oldExamId = createExam(oldGroupId, oldCourseId, "2024-05-01T09:00:00Z", 1.0);
    grade(oldGroupId, oldCourseId, oldExamId, studentId, 8.0, "2024-05-02T09:00:00Z");

    UUID newGroupId = createGroup("REP-P-3", (short) 2234, (short) 2235);
    transfer(oldGroupId, studentId, newGroupId, "2025-01-01");
    UUID newCourseId = createCourse("REP-C-301", "Report Course Repeat Year");
    assignCourse(newGroupId, newCourseId, "2025-01-01", "S3");
    String newExamId = createExam(newGroupId, newCourseId, "2025-05-01T09:00:00Z", 1.0);
    grade(newGroupId, newCourseId, newExamId, studentId, 14.0, "2025-05-02T09:00:00Z");

    ResponseEntity<JsonNode> response =
        restTemplate.getForEntity("/students/" + studentId + "/report", JsonNode.class);

    assertEquals(OK, response.getStatusCode());
    JsonNode body = response.getBody();
    assertNotNull(body);
    assertEquals(1, body.get("levels").size());
    assertEquals(1, body.get("levels").get(0).get("courses").size());
    assertEquals("L2", body.get("levels").get(0).get("level").asText());
    assertEquals(
        "REP-C-301", body.get("levels").get(0).get("courses").get(0).get("reference").asText());
    assertEquals(
        "REP-P-3",
        body.get("levels").get(0).get("courses").get(0).get("promotionReference").asText());
    assertEquals(14.0, body.get("levels").get(0).get("courses").get(0).get("average").asDouble());
  }

  @Test
  void report_noMembership_returnsEmptyLevels() {
    String studentId = createStudent("rep-it-nomember-" + UUID.randomUUID() + "@unigrade.com");

    ResponseEntity<JsonNode> response =
        restTemplate.getForEntity("/students/" + studentId + "/report", JsonNode.class);

    assertEquals(OK, response.getStatusCode());
    JsonNode body = response.getBody();
    assertNotNull(body);
    assertEquals(0, body.get("levels").size());
    assertTrue(body.get("overallAverage").isNull());
  }

  @Test
  void report_missingStudent_returnsNotFound() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/students/STD99999/report", String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void report_nonStudent_returnsBadRequest() {
    String teacherId = createTeacher("rep-it-teacher-" + UUID.randomUUID() + "@unigrade.com");

    ResponseEntity<String> response =
        restTemplate.getForEntity("/students/" + teacherId + "/report", String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  private void transfer(UUID oldGroupId, String studentId, UUID newGroupId, String transferDate) {
    ResponseEntity<Void> response =
        restTemplate.exchange(
            "/groups/" + oldGroupId + "/members/" + studentId,
            PUT,
            new HttpEntity<>(Map.of("newGroupId", newGroupId, "transferDate", transferDate)),
            Void.class);
    assertTrue(response.getStatusCode().is2xxSuccessful());
  }

  private String createExam(UUID groupId, UUID courseId, String examDate, double coefficient) {
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/courses/" + courseId + "/exams",
            Map.of("examDate", examDate, "coefficient", coefficient),
            JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    return response.getBody().get("id").asText();
  }

  private void grade(
      UUID groupId,
      UUID courseId,
      String examId,
      String studentId,
      double score,
      String gradeDate) {
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/courses/" + courseId + "/exams/" + examId + "/grades",
            Map.of(
                "score",
                score,
                "gradeDate",
                gradeDate,
                "reason",
                "Midterm",
                "studentId",
                studentId),
            JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
  }

  private void assignCourse(UUID groupId, UUID courseId, String startDate, String semester) {
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/courses",
            Map.of("courseId", courseId, "semester", semester, "startDate", startDate),
            JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
  }

  private void createMembership(UUID groupId, String studentId, String startDate) {
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/members",
            Map.of("studentId", studentId, "startDate", startDate),
            JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
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

  private String createTeacher(String email) {
    Map<String, Object> body =
        Map.of(
            "firstName",
            "Grace",
            "lastName",
            "Hopper",
            "birthDate",
            "2000-01-01",
            "email",
            email,
            "password",
            "hashed-password",
            "isActive",
            true,
            "role",
            "TEACHER");
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
    assertNotNull(response.getBody());
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
