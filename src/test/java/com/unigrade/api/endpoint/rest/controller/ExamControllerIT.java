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
import com.unigrade.api.model.Exam;
import com.unigrade.api.repository.CourseRepository;
import com.unigrade.api.repository.GroupCourseRepository;
import com.unigrade.api.repository.PromotionRepository;
import com.unigrade.api.repository.StudentGroupRepository;
import com.unigrade.api.repository.model.JCourse;
import com.unigrade.api.repository.model.JGroupCourse;
import com.unigrade.api.repository.model.JPromotion;
import com.unigrade.api.repository.model.JStudentGroup;
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
  @Autowired private CourseRepository courseRepository;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private StudentGroupRepository studentGroupRepository;
  @Autowired private GroupCourseRepository groupCourseRepository;

  @Test
  void crud_lifecycle() {
    UUID groupCourseId = createGroupCourse("CS-EX-101", "PROMO-EX-101", "A1");
    var toCreate =
        new Exam(
            null, Instant.parse("2026-06-01T09:00:00Z"), new BigDecimal("0.5000"), groupCourseId);

    ResponseEntity<JsonNode> createResponse =
        restTemplate.postForEntity(
            "/group-courses/" + groupCourseId + "/exams", toCreate, JsonNode.class);
    assertEquals(CREATED, createResponse.getStatusCode());
    assertNotNull(createResponse.getBody());
    UUID createdId = UUID.fromString(createResponse.getBody().get("id").asText());

    ResponseEntity<Exam> getResponse =
        restTemplate.getForEntity(
            "/group-courses/" + groupCourseId + "/exams/" + createdId, Exam.class);
    assertEquals(OK, getResponse.getStatusCode());
    assertEquals(groupCourseId, getResponse.getBody().groupCourseId());

    ResponseEntity<Exam[]> listResponse =
        restTemplate.getForEntity("/group-courses/" + groupCourseId + "/exams", Exam[].class);
    assertEquals(OK, listResponse.getStatusCode());
    assertFalse(listResponse.getBody().length == 0);

    var toUpdate =
        new Exam(
            null, Instant.parse("2026-07-01T09:00:00Z"), new BigDecimal("0.7500"), groupCourseId);
    restTemplate.put("/group-courses/" + groupCourseId + "/exams/" + createdId, toUpdate);

    ResponseEntity<Exam> afterUpdate =
        restTemplate.getForEntity(
            "/group-courses/" + groupCourseId + "/exams/" + createdId, Exam.class);
    assertEquals(new BigDecimal("0.7500"), afterUpdate.getBody().coefficient());

    restTemplate.delete("/group-courses/" + groupCourseId + "/exams/" + createdId);

    ResponseEntity<String> afterDelete =
        restTemplate.getForEntity(
            "/group-courses/" + groupCourseId + "/exams/" + createdId, String.class);
    assertEquals(NOT_FOUND, afterDelete.getStatusCode());
  }

  @Test
  void create_missingGroupCourse_returnsNotFound() {
    var invalid =
        new Exam(
            null,
            Instant.parse("2026-06-01T09:00:00Z"),
            new BigDecimal("0.5000"),
            UUID.randomUUID());

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/group-courses/" + UUID.randomUUID() + "/exams", invalid, String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void create_coefficientOutOfRange_returnsBadRequest() {
    UUID groupCourseId = createGroupCourse("CS-EX-102", "PROMO-EX-102", "A1");
    var invalid =
        new Exam(
            null, Instant.parse("2026-06-01T09:00:00Z"), new BigDecimal("1.5000"), groupCourseId);

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/group-courses/" + groupCourseId + "/exams", invalid, String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void create_missingExamDate_returnsBadRequest() {
    UUID groupCourseId = createGroupCourse("CS-EX-103", "PROMO-EX-103", "A1");
    var invalid = new Exam(null, null, new BigDecimal("0.5000"), groupCourseId);

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/group-courses/" + groupCourseId + "/exams", invalid, String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void create_totalCoefficientExceeds1_returnsBadRequest() {
    UUID groupCourseId = createGroupCourse("CS-EX-109", "PROMO-EX-109", "A1");
    var first =
        new Exam(
            null, Instant.parse("2026-06-01T09:00:00Z"), new BigDecimal("0.6000"), groupCourseId);
    restTemplate.postForEntity("/group-courses/" + groupCourseId + "/exams", first, JsonNode.class);

    var second =
        new Exam(
            null, Instant.parse("2026-06-15T09:00:00Z"), new BigDecimal("0.5000"), groupCourseId);
    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/group-courses/" + groupCourseId + "/exams", second, String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void create_totalCoefficientExactly1_returnsCreated() {
    UUID groupCourseId = createGroupCourse("CS-EX-110", "PROMO-EX-110", "A1");
    var first =
        new Exam(
            null, Instant.parse("2026-06-01T09:00:00Z"), new BigDecimal("0.6000"), groupCourseId);
    restTemplate.postForEntity("/group-courses/" + groupCourseId + "/exams", first, JsonNode.class);

    var second =
        new Exam(
            null, Instant.parse("2026-06-15T09:00:00Z"), new BigDecimal("0.4000"), groupCourseId);
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity(
            "/group-courses/" + groupCourseId + "/exams", second, JsonNode.class);

    assertEquals(CREATED, response.getStatusCode());
  }

  @Test
  void differentGroupCourse_sameCourse_doesNotShareCoefficientBudget() {
    UUID courseId = createCourse("CS-EX-118");
    UUID promotionId = createPromotion("PROMO-EX-118");
    UUID groupCourseK1 =
        linkGroupCourse(courseId, createStudentGroup(promotionId, "K1"), (short) 2026, (short) 1);
    UUID groupCourseK2 =
        linkGroupCourse(courseId, createStudentGroup(promotionId, "K2"), (short) 2026, (short) 1);

    var examK1 =
        new Exam(
            null, Instant.parse("2026-06-01T09:00:00Z"), new BigDecimal("0.8000"), groupCourseK1);
    ResponseEntity<JsonNode> firstResponse =
        restTemplate.postForEntity(
            "/group-courses/" + groupCourseK1 + "/exams", examK1, JsonNode.class);
    assertEquals(CREATED, firstResponse.getStatusCode());

    // K2 est une offre distincte du même cours — ne doit pas être limité par les
    // 0.8 déjà pris par K1.
    var examK2 =
        new Exam(
            null, Instant.parse("2026-06-01T09:00:00Z"), new BigDecimal("0.8000"), groupCourseK2);
    ResponseEntity<JsonNode> secondResponse =
        restTemplate.postForEntity(
            "/group-courses/" + groupCourseK2 + "/exams", examK2, JsonNode.class);

    assertEquals(CREATED, secondResponse.getStatusCode());
  }

  @Test
  void getById_missing_returnsNotFound() {
    UUID groupCourseId = createGroupCourse("CS-EX-105", "PROMO-EX-105", "A1");

    ResponseEntity<String> response =
        restTemplate.getForEntity(
            "/group-courses/" + groupCourseId + "/exams/" + UUID.randomUUID(), String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void getById_wrongGroupCourse_returnsNotFound() {
    UUID groupCourseA = createGroupCourse("CS-EX-106", "PROMO-EX-106", "A1");
    UUID groupCourseB = createGroupCourse("CS-EX-107", "PROMO-EX-107", "A1");
    var toCreate =
        new Exam(
            null, Instant.parse("2026-06-01T09:00:00Z"), new BigDecimal("0.5000"), groupCourseA);
    ResponseEntity<JsonNode> createResponse =
        restTemplate.postForEntity(
            "/group-courses/" + groupCourseA + "/exams", toCreate, JsonNode.class);
    UUID examId = UUID.fromString(createResponse.getBody().get("id").asText());

    ResponseEntity<String> response =
        restTemplate.getForEntity(
            "/group-courses/" + groupCourseB + "/exams/" + examId, String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void update_missing_returnsNotFound() {
    UUID groupCourseId = createGroupCourse("CS-EX-104", "PROMO-EX-104", "A1");
    var toUpdate =
        new Exam(
            null, Instant.parse("2026-06-01T09:00:00Z"), new BigDecimal("0.5000"), groupCourseId);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/group-courses/" + groupCourseId + "/exams/" + UUID.randomUUID(),
            PUT,
            new HttpEntity<>(toUpdate),
            String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void delete_missing_returnsNotFound() {
    UUID groupCourseId = createGroupCourse("CS-EX-108", "PROMO-EX-108", "A1");

    ResponseEntity<Void> response =
        restTemplate.exchange(
            "/group-courses/" + groupCourseId + "/exams/" + UUID.randomUUID(),
            DELETE,
            null,
            Void.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  private static final java.util.concurrent.atomic.AtomicInteger YEAR_OFFSET =
      new java.util.concurrent.atomic.AtomicInteger(0);

  private UUID createCourse(String reference) {
    var course =
        JCourse.builder()
            .reference(reference)
            .title(reference + " title")
            .credits((short) 6)
            .build();
    return courseRepository.save(course).getId();
  }

  private UUID createPromotion(String reference) {
    short startYear = (short) (3000 + YEAR_OFFSET.getAndIncrement());
    var promotion =
        JPromotion.builder()
            .reference(reference)
            .startYear(startYear)
            .endYear((short) (startYear + 3))
            .build();
    return promotionRepository.save(promotion).getId();
  }

  private UUID createStudentGroup(UUID promotionId, String reference) {
    var promotion = promotionRepository.findById(promotionId).orElseThrow();
    var group = JStudentGroup.builder().reference(reference).promotion(promotion).build();
    return studentGroupRepository.save(group).getId();
  }

  private UUID linkGroupCourse(UUID courseId, UUID groupId, short schoolYear, short semester) {
    var course = courseRepository.findById(courseId).orElseThrow();
    var group = studentGroupRepository.findById(groupId).orElseThrow();
    var groupCourse =
        JGroupCourse.builder()
            .course(course)
            .group(group)
            .schoolYear(schoolYear)
            .semester(semester)
            .build();
    return groupCourseRepository.save(groupCourse).getId();
  }

  private UUID createGroupCourse(
      String courseReference, String promotionReference, String groupReference) {
    UUID courseId = createCourse(courseReference);
    UUID promotionId = createPromotion(promotionReference);
    UUID groupId = createStudentGroup(promotionId, groupReference);
    return linkGroupCourse(courseId, groupId, (short) 2026, (short) 1);
  }
}
