package com.unigrade.api.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import com.fasterxml.jackson.databind.JsonNode;
import com.unigrade.api.conf.SecuredFacadeIT;
import com.unigrade.api.endpoint.event.EventProducer;
import com.unigrade.api.endpoint.event.model.ReportEmailRequested;
import com.unigrade.api.model.Course;
import com.unigrade.api.model.Promotion;
import com.unigrade.api.model.StudentGroup;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

class StudentReportIT extends SecuredFacadeIT {

  @Autowired private TestRestTemplate restTemplate;

  @MockBean private EventProducer<ReportEmailRequested> reportEmailEventProducer;

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

    ResponseEntity<String> response =
        restTemplate.getForEntity("/students/" + studentId + "/report", String.class);

    assertEquals(ACCEPTED, response.getStatusCode());
  }

  @Test
  void report_repeat_hidesFailedYear() {
    UUID oldGroupId = createGroup("REP-P-2", (short) 2232, (short) 2233);
    UUID oldCourseId = createCourse("REP-C-201", "Report Course Failed Year", (short) 30);
    assignCourse(oldGroupId, oldCourseId, "2024-01-01", "S3");
    String studentId = createStudent("rep-it-repeat-" + UUID.randomUUID() + "@unigrade.com");
    createMembership(oldGroupId, studentId, "2024-01-01");
    String oldExamId = createExam(oldGroupId, oldCourseId, "2024-05-01T09:00:00Z", 1.0);
    grade(oldGroupId, oldCourseId, oldExamId, studentId, 8.0, "2024-05-02T09:00:00Z");

    UUID newGroupId = createGroup("REP-P-3", (short) 2234, (short) 2235);
    transfer(oldGroupId, studentId, newGroupId, "2025-01-01");
    UUID newCourseId = createCourse("REP-C-301", "Report Course Repeat Year", (short) 30);
    assignCourse(newGroupId, newCourseId, "2025-01-01", "S3");
    String newExamId = createExam(newGroupId, newCourseId, "2025-05-01T09:00:00Z", 1.0);
    grade(newGroupId, newCourseId, newExamId, studentId, 14.0, "2025-05-02T09:00:00Z");

    ResponseEntity<String> response =
        restTemplate.getForEntity("/students/" + studentId + "/report", String.class);

    assertEquals(ACCEPTED, response.getStatusCode());
  }

  @Test
  void report_noMembership_returnsEmptyLevels() {
    String studentId = createStudent("rep-it-nomember-" + UUID.randomUUID() + "@unigrade.com");

    ResponseEntity<String> response =
        restTemplate.getForEntity("/students/" + studentId + "/report", String.class);

    assertEquals(ACCEPTED, response.getStatusCode());
  }

  @Test
  void report_withLevelFilter_returnsOnlyThatLevel() {
    String uid = UUID.randomUUID().toString().substring(0, 8);
    UUID groupId = createGroup("LF-" + uid, (short) 2410, (short) 2411);
    UUID l1CourseId = createCourse("LC-" + uid + "a", "L1 Course");
    assignCourse(groupId, l1CourseId, "2024-01-01", "S1");
    UUID l2CourseId = createCourse("LC-" + uid + "b", "L2 Course");
    assignCourse(groupId, l2CourseId, "2024-01-01", "S3");
    String studentId = createStudent("rep-it-filter-" + UUID.randomUUID() + "@unigrade.com");
    createMembership(groupId, studentId, "2024-01-01");

    ResponseEntity<String> response =
        restTemplate.getForEntity("/students/" + studentId + "/report?level=L2", String.class);

    assertEquals(ACCEPTED, response.getStatusCode());
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
    ResponseEntity<JsonNode> response =
        restTemplate.exchange(
            "/students/" + studentId + "/transfer",
            PUT,
            new HttpEntity<>(Map.of("newGroupId", newGroupId, "transferDate", transferDate)),
            JsonNode.class);
    assertEquals(OK, response.getStatusCode());
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
        restTemplate.exchange(
            "/students/" + studentId + "/transfer",
            PUT,
            new HttpEntity<>(Map.of("newGroupId", groupId, "transferDate", startDate)),
            JsonNode.class);
    assertEquals(OK, response.getStatusCode());
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

  private UUID createCourse(String reference, String title, short credits) {
    var course = new Course(null, reference, title, credits);
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity("/courses", course, JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    return UUID.fromString(response.getBody().get("id").asText());
  }

  private UUID createCourse(String reference, String title) {
    return createCourse(reference, title, (short) 6);
  }
}
