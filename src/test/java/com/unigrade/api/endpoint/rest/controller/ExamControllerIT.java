package com.unigrade.api.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import com.fasterxml.jackson.databind.JsonNode;
import com.unigrade.api.conf.FacadeIT;
import com.unigrade.api.model.Course;
import com.unigrade.api.model.Exam;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

class ExamControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void crud_lifecycle() {
    UUID courseId = createCourse("CS-EX-101", "Exam Lifecycle Course");
    var toCreate =
        new Exam(
            null,
            Instant.parse("2026-06-01T09:00:00Z"),
            new BigDecimal("50.00"),
            courseId,
            (short) 2026);

    ResponseEntity<JsonNode> createResponse =
        restTemplate.postForEntity("/courses/" + courseId + "/exams", toCreate, JsonNode.class);
    assertEquals(CREATED, createResponse.getStatusCode());
    assertNotNull(createResponse.getBody());
    UUID createdId = UUID.fromString(createResponse.getBody().get("id").asText());

    ResponseEntity<Exam> getResponse =
        restTemplate.getForEntity("/courses/" + courseId + "/exams/" + createdId, Exam.class);
    assertEquals(OK, getResponse.getStatusCode());
    assertEquals(courseId, getResponse.getBody().courseId());
    assertEquals((short) 2026, getResponse.getBody().schoolYear());

    ResponseEntity<Exam[]> listResponse =
        restTemplate.getForEntity("/courses/" + courseId + "/exams", Exam[].class);
    assertEquals(OK, listResponse.getStatusCode());
    assertFalse(listResponse.getBody().length == 0);

    var toUpdate =
        new Exam(
            null,
            Instant.parse("2026-07-01T09:00:00Z"),
            new BigDecimal("75.00"),
            courseId,
            (short) 2026);
    restTemplate.put("/courses/" + courseId + "/exams/" + createdId, toUpdate);

    ResponseEntity<Exam> afterUpdate =
        restTemplate.getForEntity("/courses/" + courseId + "/exams/" + createdId, Exam.class);
    assertEquals(new BigDecimal("75.00"), afterUpdate.getBody().coefficient());

    restTemplate.delete("/courses/" + courseId + "/exams/" + createdId);

    ResponseEntity<String> afterDelete =
        restTemplate.getForEntity("/courses/" + courseId + "/exams/" + createdId, String.class);
    assertEquals(NOT_FOUND, afterDelete.getStatusCode());
  }

  @Test
  void create_missingCourse_returnsNotFound() {
    UUID missingCourseId = UUID.randomUUID();
    var invalid =
        new Exam(
            null,
            Instant.parse("2026-06-01T09:00:00Z"),
            new BigDecimal("50.00"),
            missingCourseId,
            (short) 2026);

    ResponseEntity<String> response =
        restTemplate.postForEntity("/courses/" + missingCourseId + "/exams", invalid, String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void create_coefficientOutOfRange_returnsBadRequest() {
    UUID courseId = createCourse("CS-EX-102", "Exam Range Course");
    var invalid =
        new Exam(
            null,
            Instant.parse("2026-06-01T09:00:00Z"),
            new BigDecimal("150.00"),
            courseId,
            (short) 2026);

    ResponseEntity<String> response =
        restTemplate.postForEntity("/courses/" + courseId + "/exams", invalid, String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void create_missingExamDate_returnsBadRequest() {
    UUID courseId = createCourse("CS-EX-103", "Exam Date Course");
    var invalid = new Exam(null, null, new BigDecimal("50.00"), courseId, (short) 2026);

    ResponseEntity<String> response =
        restTemplate.postForEntity("/courses/" + courseId + "/exams", invalid, String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void create_missingSchoolYear_returnsBadRequest() {
    UUID courseId = createCourse("CS-EX-112", "Exam No Year Course");
    var invalid =
        new Exam(
            null, Instant.parse("2026-06-01T09:00:00Z"), new BigDecimal("50.00"), courseId, null);

    ResponseEntity<String> response =
        restTemplate.postForEntity("/courses/" + courseId + "/exams", invalid, String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void create_totalCoefficientExceeds100SameYear_returnsBadRequest() {
    UUID courseId = createCourse("CS-EX-109", "Exam Total Course");
    var first =
        new Exam(
            null,
            Instant.parse("2026-06-01T09:00:00Z"),
            new BigDecimal("60.00"),
            courseId,
            (short) 2026);
    restTemplate.postForEntity("/courses/" + courseId + "/exams", first, JsonNode.class);

    var second =
        new Exam(
            null,
            Instant.parse("2026-06-15T09:00:00Z"),
            new BigDecimal("50.00"),
            courseId,
            (short) 2026);
    ResponseEntity<String> response =
        restTemplate.postForEntity("/courses/" + courseId + "/exams", second, String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void create_totalCoefficientExactly100_returnsCreated() {
    UUID courseId = createCourse("CS-EX-110", "Exam Total Exact Course");
    var first =
        new Exam(
            null,
            Instant.parse("2026-06-01T09:00:00Z"),
            new BigDecimal("60.00"),
            courseId,
            (short) 2026);
    restTemplate.postForEntity("/courses/" + courseId + "/exams", first, JsonNode.class);

    var second =
        new Exam(
            null,
            Instant.parse("2026-06-15T09:00:00Z"),
            new BigDecimal("40.00"),
            courseId,
            (short) 2026);
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity("/courses/" + courseId + "/exams", second, JsonNode.class);

    assertEquals(CREATED, response.getStatusCode());
  }

  @Test
  void create_sameCourseDifferentSchoolYear_doesNotShareCoefficientBudget() {
    UUID courseId = createCourse("CS-EX-113", "Exam Cross Year Course");
    var year2026 =
        new Exam(
            null,
            Instant.parse("2026-06-01T09:00:00Z"),
            new BigDecimal("60.00"),
            courseId,
            (short) 2026);
    ResponseEntity<JsonNode> first =
        restTemplate.postForEntity("/courses/" + courseId + "/exams", year2026, JsonNode.class);
    assertEquals(CREATED, first.getStatusCode());

    // Different school year — should NOT be blocked by 2026's 60% usage.
    var year2028 =
        new Exam(
            null,
            Instant.parse("2028-06-01T09:00:00Z"),
            new BigDecimal("60.00"),
            courseId,
            (short) 2028);
    ResponseEntity<JsonNode> second =
        restTemplate.postForEntity("/courses/" + courseId + "/exams", year2028, JsonNode.class);

    assertEquals(CREATED, second.getStatusCode());
  }

  @Test
  void findAll_filtersBySchoolYear() {
    UUID courseId = createCourse("CS-EX-114", "Exam Filter Course");
    var year2026 =
        new Exam(
            null,
            Instant.parse("2026-06-01T09:00:00Z"),
            new BigDecimal("50.00"),
            courseId,
            (short) 2026);
    restTemplate.postForEntity("/courses/" + courseId + "/exams", year2026, JsonNode.class);
    var year2028 =
        new Exam(
            null,
            Instant.parse("2028-06-01T09:00:00Z"),
            new BigDecimal("50.00"),
            courseId,
            (short) 2028);
    restTemplate.postForEntity("/courses/" + courseId + "/exams", year2028, JsonNode.class);

    ResponseEntity<Exam[]> response =
        restTemplate.getForEntity("/courses/" + courseId + "/exams?schoolYear=2028", Exam[].class);

    assertEquals(OK, response.getStatusCode());
    assertEquals(1, response.getBody().length);
    assertEquals((short) 2028, response.getBody()[0].schoolYear());
  }

  @Test
  void getById_missing_returnsNotFound() {
    UUID courseId = createCourse("CS-EX-105", "Exam Get Course");

    ResponseEntity<String> response =
        restTemplate.getForEntity(
            "/courses/" + courseId + "/exams/" + UUID.randomUUID(), String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void getById_wrongCourse_returnsNotFound() {
    UUID courseId = createCourse("CS-EX-106", "Exam Wrong Course A");
    UUID otherCourseId = createCourse("CS-EX-107", "Exam Wrong Course B");
    var toCreate =
        new Exam(
            null,
            Instant.parse("2026-06-01T09:00:00Z"),
            new BigDecimal("50.00"),
            courseId,
            (short) 2026);
    ResponseEntity<JsonNode> createResponse =
        restTemplate.postForEntity("/courses/" + courseId + "/exams", toCreate, JsonNode.class);
    UUID examId = UUID.fromString(createResponse.getBody().get("id").asText());

    ResponseEntity<String> response =
        restTemplate.getForEntity("/courses/" + otherCourseId + "/exams/" + examId, String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void update_missing_returnsNotFound() {
    UUID courseId = createCourse("CS-EX-104", "Exam Update Course");
    var toUpdate =
        new Exam(
            null,
            Instant.parse("2026-06-01T09:00:00Z"),
            new BigDecimal("50.00"),
            courseId,
            (short) 2026);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/courses/" + courseId + "/exams/" + UUID.randomUUID(),
            PUT,
            new HttpEntity<>(toUpdate),
            String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void update_totalCoefficientExceeds100_returnsBadRequest() {
    UUID courseId = createCourse("CS-EX-111", "Exam Update Total Course");
    var first =
        new Exam(
            null,
            Instant.parse("2026-06-01T09:00:00Z"),
            new BigDecimal("50.00"),
            courseId,
            (short) 2026);
    ResponseEntity<JsonNode> firstResponse =
        restTemplate.postForEntity("/courses/" + courseId + "/exams", first, JsonNode.class);
    UUID firstId = UUID.fromString(firstResponse.getBody().get("id").asText());

    var second =
        new Exam(
            null,
            Instant.parse("2026-06-15T09:00:00Z"),
            new BigDecimal("30.00"),
            courseId,
            (short) 2026);
    restTemplate.postForEntity("/courses/" + courseId + "/exams", second, JsonNode.class);

    var updateFirst =
        new Exam(
            null,
            Instant.parse("2026-06-01T09:00:00Z"),
            new BigDecimal("80.00"),
            courseId,
            (short) 2026);
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/courses/" + courseId + "/exams/" + firstId,
            PUT,
            new HttpEntity<>(updateFirst),
            String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void delete_missing_returnsNotFound() {
    UUID courseId = createCourse("CS-EX-108", "Exam Delete Course");

    ResponseEntity<Void> response =
        restTemplate.exchange(
            "/courses/" + courseId + "/exams/" + UUID.randomUUID(), DELETE, null, Void.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  private UUID createCourse(String reference, String title) {
    var course = new Course(null, reference, title, (short) 6);
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity("/courses", course, JsonNode.class);
    return UUID.fromString(response.getBody().get("id").asText());
  }
}
