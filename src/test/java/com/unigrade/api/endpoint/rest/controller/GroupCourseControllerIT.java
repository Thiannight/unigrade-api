package com.unigrade.api.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import com.fasterxml.jackson.databind.JsonNode;
import com.unigrade.api.conf.FacadeIT;
import com.unigrade.api.model.Course;
import com.unigrade.api.model.GroupCourse;
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

class GroupCourseControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void assign_list_end_lifecycle() {
    UUID groupId = createGroup("GC-LIFE-1", (short) 2070, (short) 2071);
    UUID courseId = createCourse("GC-LIFE-101", "Group Course Lifecycle");

    ResponseEntity<JsonNode> assignResponse =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/courses",
            Map.of("courseId", courseId, "startDate", "2024-01-01"),
            JsonNode.class);
    assertEquals(CREATED, assignResponse.getStatusCode());
    assertNotNull(assignResponse.getBody());
    assertEquals(courseId.toString(), assignResponse.getBody().get("courseId").asText());

    ResponseEntity<GroupCourse[]> activeResponse =
        restTemplate.getForEntity("/groups/" + groupId + "/courses", GroupCourse[].class);
    assertEquals(OK, activeResponse.getStatusCode());
    assertEquals(1, activeResponse.getBody().length);

    ResponseEntity<JsonNode> endResponse =
        restTemplate.exchange(
            "/groups/" + groupId + "/courses/" + courseId,
            PUT,
            new HttpEntity<>(Map.of("endDate", "2024-06-01")),
            JsonNode.class);
    assertEquals(OK, endResponse.getStatusCode());
    assertEquals("2024-06-01", endResponse.getBody().get("endDate").asText());

    ResponseEntity<GroupCourse[]> afterEndResponse =
        restTemplate.getForEntity("/groups/" + groupId + "/courses", GroupCourse[].class);
    assertEquals(OK, afterEndResponse.getStatusCode());
    assertEquals(0, afterEndResponse.getBody().length);
  }

  @Test
  void assign_missingGroup_returnsNotFound() {
    UUID courseId = createCourse("GC-NG-101", "No Group Course");

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/groups/" + UUID.randomUUID() + "/courses",
            Map.of("courseId", courseId, "startDate", "2024-01-01"),
            String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void assign_missingCourse_returnsNotFound() {
    UUID groupId = createGroup("GC-NC-1", (short) 2072, (short) 2073);

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/courses",
            Map.of("courseId", UUID.randomUUID(), "startDate", "2024-01-01"),
            String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void assign_duplicateActive_returnsConflict() {
    UUID groupId = createGroup("GC-DUP-1", (short) 2074, (short) 2075);
    UUID courseId = createCourse("GC-DUP-101", "Duplicate Active Course");

    restTemplate.postForEntity(
        "/groups/" + groupId + "/courses",
        Map.of("courseId", courseId, "startDate", "2024-01-01"),
        JsonNode.class);

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/courses",
            Map.of("courseId", courseId, "startDate", "2024-02-01"),
            String.class);

    assertEquals(CONFLICT, response.getStatusCode());
  }

  @Test
  void end_noActiveAssignment_returnsNotFound() {
    UUID groupId = createGroup("GC-NA-1", (short) 2076, (short) 2077);
    UUID courseId = createCourse("GC-NA-101", "Never Assigned Course");

    ResponseEntity<Void> response =
        restTemplate.exchange(
            "/groups/" + groupId + "/courses/" + courseId,
            PUT,
            new HttpEntity<>(Map.of("endDate", "2024-06-01")),
            Void.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void end_dateBeforeStart_returnsBadRequest() {
    UUID groupId = createGroup("GC-BS-1", (short) 2078, (short) 2079);
    UUID courseId = createCourse("GC-BS-101", "Bad Start Course");

    restTemplate.postForEntity(
        "/groups/" + groupId + "/courses",
        Map.of("courseId", courseId, "startDate", "2024-06-01"),
        JsonNode.class);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/groups/" + groupId + "/courses/" + courseId,
            PUT,
            new HttpEntity<>(Map.of("endDate", "2024-01-01")),
            String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void reassign_afterEnd_succeeds() {
    UUID groupId = createGroup("GC-REJ-1", (short) 2080, (short) 2081);
    UUID courseId = createCourse("GC-REJ-101", "Rejoinable Course");

    restTemplate.postForEntity(
        "/groups/" + groupId + "/courses",
        Map.of("courseId", courseId, "startDate", "2024-01-01"),
        JsonNode.class);

    restTemplate.exchange(
        "/groups/" + groupId + "/courses/" + courseId,
        PUT,
        new HttpEntity<>(Map.of("endDate", "2024-06-01")),
        Void.class);

    ResponseEntity<JsonNode> reassignResponse =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/courses",
            Map.of("courseId", courseId, "startDate", "2024-09-01"),
            JsonNode.class);

    assertEquals(CREATED, reassignResponse.getStatusCode());

    ResponseEntity<GroupCourse[]> activeResponse =
        restTemplate.getForEntity("/groups/" + groupId + "/courses", GroupCourse[].class);
    assertEquals(1, activeResponse.getBody().length);
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
