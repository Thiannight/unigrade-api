package com.unigrade.api.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import com.fasterxml.jackson.databind.JsonNode;
import com.unigrade.api.conf.SecuredFacadeIT;
import com.unigrade.api.model.Course;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

class CourseControllerIT extends SecuredFacadeIT {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void crud_lifecycle() {
    var toCreate = new Course(null, "CS-IT-101", "Intro to Testing", (short) 6);

    ResponseEntity<JsonNode> createResponse =
        restTemplate.postForEntity("/courses", toCreate, JsonNode.class);
    assertEquals(CREATED, createResponse.getStatusCode());
    assertNotNull(createResponse.getBody());
    UUID createdId = UUID.fromString(createResponse.getBody().get("id").asText());

    ResponseEntity<Course> getResponse =
        restTemplate.getForEntity("/courses/" + createdId, Course.class);
    assertEquals(OK, getResponse.getStatusCode());
    assertEquals("CS-IT-101", getResponse.getBody().reference());
    ResponseEntity<Course[]> listResponse = restTemplate.getForEntity("/courses", Course[].class);
    assertEquals(OK, listResponse.getStatusCode());
    assertFalse(listResponse.getBody().length == 0);

    var toUpdate = new Course(null, "CS-IT-101-BIS", "Intro to Testing v2", (short) 8);
    restTemplate.put("/courses/" + createdId, toUpdate);

    ResponseEntity<Course> afterUpdate =
        restTemplate.getForEntity("/courses/" + createdId, Course.class);
    assertEquals("CS-IT-101-BIS", afterUpdate.getBody().reference());
    assertEquals((short) 8, afterUpdate.getBody().credits());

    restTemplate.delete("/courses/" + createdId);

    ResponseEntity<String> afterDelete =
        restTemplate.getForEntity("/courses/" + createdId, String.class);
    assertEquals(NOT_FOUND, afterDelete.getStatusCode());
  }

  @Test
  void create_blankReference_returnsBadRequest() {
    var invalid = new Course(null, "", "Intro to Testing", (short) 6);

    ResponseEntity<String> response = restTemplate.postForEntity("/courses", invalid, String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void create_nonPositiveCredits_returnsBadRequest() {
    var invalid = new Course(null, "CS-IT-102", "Negative Credits", (short) 0);

    ResponseEntity<String> response = restTemplate.postForEntity("/courses", invalid, String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void create_duplicateReference_returnsConflict() {
    var first = new Course(null, "CS-IT-DUP", "First title", (short) 6);
    restTemplate.postForEntity("/courses", first, Course.class);

    var duplicate = new Course(null, "CS-IT-DUP", "Different title", (short) 4);
    ResponseEntity<String> response =
        restTemplate.postForEntity("/courses", duplicate, String.class);

    assertEquals(CONFLICT, response.getStatusCode());
  }

  @Test
  void create_duplicateTitle_returnsConflict() {
    var first = new Course(null, "CS-IT-DUPTITLE1", "Duplicate Title", (short) 6);
    restTemplate.postForEntity("/courses", first, Course.class);

    var duplicate = new Course(null, "CS-IT-DUPTITLE2", "Duplicate Title", (short) 4);
    ResponseEntity<String> response =
        restTemplate.postForEntity("/courses", duplicate, String.class);

    assertEquals(CONFLICT, response.getStatusCode());
  }

  @Test
  void getById_missing_returnsNotFound() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/courses/" + UUID.randomUUID(), String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void delete_missing_returnsNotFound() {
    ResponseEntity<Void> response =
        restTemplate.exchange("/courses/" + UUID.randomUUID(), DELETE, null, Void.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }
}
