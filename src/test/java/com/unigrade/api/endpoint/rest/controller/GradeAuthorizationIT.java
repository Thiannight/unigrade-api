package com.unigrade.api.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.unigrade.api.conf.SecuredFacadeIT;
import com.unigrade.api.model.Course;
import com.unigrade.api.model.Promotion;
import com.unigrade.api.model.StudentGroup;
import com.unigrade.api.repository.UserRepository;
import com.unigrade.api.repository.model.JUser;
import com.unigrade.api.security.JwtService;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

class GradeAuthorizationIT extends SecuredFacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private JwtService jwtService;
  @LocalServerPort private int port;

  @Test
  void student_can_see_their_grades() {
    UUID groupId = createGroup("GAUTH-1", (short) 2140, (short) 2141);
    UUID courseId = createCourse("GAUTH-101", "Authorization Course 1");
    assignCourse(groupId, courseId, "2024-01-01");
    String examId = createExam(groupId, courseId, "2024-05-01T09:00:00Z");

    String studentA = createStudent("gauth-a-" + UUID.randomUUID() + "@unigrade.com");
    String studentB = createStudent("gauth-b-" + UUID.randomUUID() + "@unigrade.com");
    createMembership(groupId, studentA, "2024-01-01");
    createMembership(groupId, studentB, "2024-01-01");
    gradeAsAdmin(groupId, courseId, examId, studentA, 12.0);
    gradeAsAdmin(groupId, courseId, examId, studentB, 18.0);

    TestRestTemplate asStudentA = restTemplateFor(studentA);

    ResponseEntity<String> ownGrades =
        asStudentA.getForEntity(gradesUrl(groupId, courseId, examId), String.class);
    assertEquals(OK, ownGrades.getStatusCode());

    ResponseEntity<String> othersGrades =
        asStudentA.getForEntity(
            gradesUrl(groupId, courseId, examId) + "?studentId=" + studentB, String.class);
    assertEquals(FORBIDDEN, othersGrades.getStatusCode());
  }

  @Test
  void teacher_canOnlyGradeTheirOwnCourses() {
    UUID groupId = createGroup("GAUTH-2", (short) 2142, (short) 2143);
    UUID courseId = createCourse("GAUTH-102", "Authorization Course 2");
    assignCourse(groupId, courseId, "2024-01-01");
    String examId = createExam(groupId, courseId, "2024-05-01T09:00:00Z");
    String student = createStudent("gauth-c-" + UUID.randomUUID() + "@unigrade.com");
    createMembership(groupId, student, "2024-01-01");

    String unassignedTeacher =
        createTeacher("gauth-teach-un-" + UUID.randomUUID() + "@unigrade.com");
    String assignedTeacher = createTeacher("gauth-teach-as-" + UUID.randomUUID() + "@unigrade.com");
    restTemplate.postForEntity(
        "/courses/" + courseId + "/teachers",
        Map.of("teacherId", assignedTeacher, "priority", 1),
        JsonNode.class);

    Map<String, Object> gradeBody =
        Map.of(
            "score",
            14.0,
            "gradeDate",
            "2024-05-02T09:00:00Z",
            "reason",
            "Midterm",
            "studentId",
            student);

    ResponseEntity<String> deniedResponse =
        restTemplateFor(unassignedTeacher)
            .postForEntity(gradesUrl(groupId, courseId, examId), gradeBody, String.class);
    assertEquals(FORBIDDEN, deniedResponse.getStatusCode());

    ResponseEntity<JsonNode> allowedResponse =
        restTemplateFor(assignedTeacher)
            .postForEntity(gradesUrl(groupId, courseId, examId), gradeBody, JsonNode.class);
    assertEquals(CREATED, allowedResponse.getStatusCode());
  }

  private TestRestTemplate restTemplateFor(String userId) {
    JUser user = userRepository.findById(userId).orElseThrow();
    String token =
        jwtService.generateToken(
            user.getId(), Map.of("id", user.getId(), "role", user.getRole().name()));
    RestTemplateBuilder builder =
        new RestTemplateBuilder()
            .rootUri("http://localhost:" + port)
            .additionalInterceptors(
                (request, body, execution) -> {
                  request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                  return execution.execute(request, body);
                });
    return new TestRestTemplate(builder);
  }

  private void gradeAsAdmin(
      UUID groupId, UUID courseId, String examId, String studentId, double score) {
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity(
            gradesUrl(groupId, courseId, examId),
            Map.of(
                "score",
                score,
                "gradeDate",
                "2024-05-02T09:00:00Z",
                "reason",
                "Midterm",
                "studentId",
                studentId),
            JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
  }

  private String createExam(UUID groupId, UUID courseId, String examDate) {
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/courses/" + courseId + "/exams",
            Map.of("examDate", examDate, "coefficient", 0.5),
            JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
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
    restTemplate.exchange(
        "/students/" + studentId + "/transfer",
        HttpMethod.PUT,
        new HttpEntity<>(Map.of("newGroupId", groupId, "transferDate", startDate)),
        JsonNode.class);
  }

  private String createStudent(String email) {
    return createUser(email, "STUDENT");
  }

  private String createTeacher(String email) {
    return createUser(email, "TEACHER");
  }

  private String createUser(String email, String role) {
    Map<String, Object> body =
        Map.of(
            "firstName", "Ada",
            "lastName", "Lovelace",
            "birthDate", "2000-01-01",
            "email", email,
            "password", "hashed-password",
            "isActive", true,
            "role", role);
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
