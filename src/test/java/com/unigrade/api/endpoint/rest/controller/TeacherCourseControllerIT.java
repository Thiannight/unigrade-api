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
import com.unigrade.api.conf.FacadeIT;
import com.unigrade.api.model.Course;
import com.unigrade.api.model.TeacherCourse;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

class TeacherCourseControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void crud_lifecycle() {
    UUID courseId = createCourse("TC-IT-101", "Teacher Course IT 101");
    String first = createTeacher("tc-it-1-" + UUID.randomUUID() + "@unigrade.com", "Bob");
    String second = createTeacher("tc-it-2-" + UUID.randomUUID() + "@unigrade.com", "Alice");

    ResponseEntity<JsonNode> assignResponse =
        restTemplate.postForEntity(
            "/courses/" + courseId + "/teachers",
            Map.of("teacherId", first, "priority", 1),
            JsonNode.class);
    assertEquals(CREATED, assignResponse.getStatusCode());
    assertNotNull(assignResponse.getBody());
    assertEquals(first, assignResponse.getBody().get("teacherId").asText());
    assertEquals(1, assignResponse.getBody().get("priority").asInt());

    restTemplate.postForEntity(
        "/courses/" + courseId + "/teachers",
        Map.of("teacherId", second, "priority", 2),
        JsonNode.class);

    ResponseEntity<TeacherCourse[]> listResponse =
        restTemplate.getForEntity("/courses/" + courseId + "/teachers", TeacherCourse[].class);
    assertEquals(OK, listResponse.getStatusCode());
    assertEquals(2, listResponse.getBody().length);
    assertEquals(first, listResponse.getBody()[0].teacherId());
    assertEquals(second, listResponse.getBody()[1].teacherId());

    restTemplate.exchange(
        "/courses/" + courseId + "/teachers/" + first,
        PUT,
        new HttpEntity<>(Map.of("priority", 4)),
        Void.class);

    ResponseEntity<TeacherCourse[]> afterUpdate =
        restTemplate.getForEntity("/courses/" + courseId + "/teachers", TeacherCourse[].class);
    assertEquals(second, afterUpdate.getBody()[0].teacherId());
    assertEquals((byte) 4, afterUpdate.getBody()[1].priority());

    ResponseEntity<Void> deleteResponse =
        restTemplate.exchange(
            "/courses/" + courseId + "/teachers/" + second, DELETE, null, Void.class);
    assertEquals(NO_CONTENT, deleteResponse.getStatusCode());

    ResponseEntity<TeacherCourse[]> afterDelete =
        restTemplate.getForEntity("/courses/" + courseId + "/teachers", TeacherCourse[].class);
    assertEquals(1, afterDelete.getBody().length);
    assertEquals(first, afterDelete.getBody()[0].teacherId());
  }

  @Test
  void assign_duplicate_returnsConflict() {
    UUID courseId = createCourse("TC-IT-DUP", "Teacher Course IT Dup");
    String teacherId = createTeacher("tc-it-dup-" + UUID.randomUUID() + "@unigrade.com", "Bob");

    restTemplate.postForEntity(
        "/courses/" + courseId + "/teachers",
        Map.of("teacherId", teacherId, "priority", 1),
        JsonNode.class);

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/courses/" + courseId + "/teachers",
            Map.of("teacherId", teacherId, "priority", 2),
            String.class);

    assertEquals(CONFLICT, response.getStatusCode());
  }

  @Test
  void assign_missingTeacher_returnsNotFound() {
    UUID courseId = createCourse("TC-IT-MISS-T", "Teacher Course IT Missing Teacher");

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/courses/" + courseId + "/teachers",
            Map.of("teacherId", "TCR99999", "priority", 1),
            String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void assign_missingCourse_returnsNotFound() {
    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/courses/" + UUID.randomUUID() + "/teachers",
            Map.of("teacherId", "TCR00001", "priority", 1),
            String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void assign_notTeacher_returnsBadRequest() {
    UUID courseId = createCourse("TC-IT-STD", "Teacher Course IT Student");
    Map<String, Object> body =
        Map.of(
            "firstName",
            "Ada",
            "lastName",
            "Lovelace",
            "birthDate",
            "2000-01-01",
            "email",
            "tc-it-std-" + UUID.randomUUID() + "@unigrade.com",
            "password",
            "hashed-password",
            "isActive",
            true,
            "role",
            "STUDENT");
    ResponseEntity<JsonNode> createResponse =
        restTemplate.postForEntity("/users", body, JsonNode.class);
    String studentId = createResponse.getBody().get("id").asText();

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/courses/" + courseId + "/teachers",
            Map.of("teacherId", studentId, "priority", 1),
            String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void assign_invalidPriority_returnsBadRequest() {
    UUID courseId = createCourse("TC-IT-PRIO", "Teacher Course IT Priority");
    String teacherId = createTeacher("tc-it-prio-" + UUID.randomUUID() + "@unigrade.com", "Bob");

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/courses/" + courseId + "/teachers",
            Map.of("teacherId", teacherId, "priority", 6),
            String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void getByCourse_missingCourse_returnsNotFound() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/courses/" + UUID.randomUUID() + "/teachers", String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void updatePriority_missingAssignment_returnsNotFound() {
    UUID courseId = createCourse("TC-IT-NOASSIGN", "Teacher Course IT No Assign");
    String teacherId =
        createTeacher("tc-it-noassign-" + UUID.randomUUID() + "@unigrade.com", "Bob");

    ResponseEntity<Void> response =
        restTemplate.exchange(
            "/courses/" + courseId + "/teachers/" + teacherId,
            PUT,
            new HttpEntity<>(Map.of("priority", 3)),
            Void.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  private UUID createCourse(String reference, String title) {
    var course = new Course(null, reference, title, (short) 6);
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity("/courses", course, JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
    return UUID.fromString(response.getBody().get("id").asText());
  }

  private String createTeacher(String email, String firstName) {
    Map<String, Object> body =
        Map.of(
            "firstName",
            firstName,
            "lastName",
            "Smith",
            "birthDate",
            "1990-01-01",
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
    return response.getBody().get("id").asText();
  }
}
