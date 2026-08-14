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

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void crud_lifecycle() {
    UUID courseId = createCourse("CS-EX-101", "Exam Lifecycle Course");
    var toCreate = new Exam(null, Instant.parse("2026-06-01T09:00:00Z"), new BigDecimal("0.5000"), courseId);

    ResponseEntity<JsonNode> createResponse = restTemplate.postForEntity("/exams", toCreate, JsonNode.class);
    assertEquals(CREATED, createResponse.getStatusCode());
    assertNotNull(createResponse.getBody());
    UUID createdId = UUID.fromString(createResponse.getBody().get("id").asText());

    ResponseEntity<Exam> getResponse = restTemplate.getForEntity("/exams/" + createdId, Exam.class);
    assertEquals(OK, getResponse.getStatusCode());
    assertEquals(courseId, getResponse.getBody().courseId());

    ResponseEntity<Exam[]> listResponse = restTemplate.getForEntity("/exams", Exam[].class);
    assertEquals(OK, listResponse.getStatusCode());
    assertFalse(listResponse.getBody().length == 0);

    var toUpdate = new Exam(null, Instant.parse("2026-07-01T09:00:00Z"), new BigDecimal("0.7500"), courseId);
    restTemplate.put("/exams/" + createdId, toUpdate);

    ResponseEntity<Exam> afterUpdate = restTemplate.getForEntity("/exams/" + createdId, Exam.class);
    assertEquals(new BigDecimal("0.7500"), afterUpdate.getBody().coefficient());

    restTemplate.delete("/exams/" + createdId);

    ResponseEntity<String> afterDelete = restTemplate.getForEntity("/exams/" + createdId, String.class);
    assertEquals(NOT_FOUND, afterDelete.getStatusCode());
  }

  @Test
  void create_missingCourse_returnsNotFound() {
    var invalid = new Exam(null, Instant.parse("2026-06-01T09:00:00Z"), new BigDecimal("0.5000"), UUID.randomUUID());

    ResponseEntity<String> response = restTemplate.postForEntity("/exams", invalid, String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void create_coefficientOutOfRange_returnsBadRequest() {
    UUID courseId = createCourse("CS-EX-102", "Exam Range Course");
    var invalid = new Exam(null, Instant.parse("2026-06-01T09:00:00Z"), new BigDecimal("1.5000"), courseId);

    ResponseEntity<String> response = restTemplate.postForEntity("/exams", invalid, String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void create_missingExamDate_returnsBadRequest() {
    UUID courseId = createCourse("CS-EX-103", "Exam Date Course");
    var invalid = new Exam(null, null, new BigDecimal("0.5000"), courseId);

    ResponseEntity<String> response = restTemplate.postForEntity("/exams", invalid, String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void getById_missing_returnsNotFound() {
    ResponseEntity<String> response = restTemplate.getForEntity("/exams/" + UUID.randomUUID(), String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void update_missing_returnsNotFound() {
    UUID courseId = createCourse("CS-EX-104", "Exam Update Course");
    var toUpdate = new Exam(null, Instant.parse("2026-06-01T09:00:00Z"), new BigDecimal("0.5000"), courseId);

    ResponseEntity<String> response = restTemplate.exchange("/exams/" + UUID.randomUUID(), PUT,
        new HttpEntity<>(toUpdate), String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void delete_missing_returnsNotFound() {
    ResponseEntity<Void> response = restTemplate.exchange("/exams/" + UUID.randomUUID(), DELETE, null, Void.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  private UUID createCourse(String reference, String title) {
    var course = new Course(null, reference, title, (short) 6);
    ResponseEntity<JsonNode> response = restTemplate.postForEntity("/courses", course, JsonNode.class);
    return UUID.fromString(response.getBody().get("id").asText());
  }
}
