package com.unigrade.api.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import com.fasterxml.jackson.databind.JsonNode;
import com.unigrade.api.conf.FacadeIT;
import com.unigrade.api.model.Promotion;
import com.unigrade.api.model.StudentGroup;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

class StudentGroupControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void crud_lifecycle() {
    UUID promotionId = createPromotion("GRP-LIFE-1", (short) 2020, (short) 2021);

    var toCreate = new StudentGroup(null, "A1", promotionId);

    ResponseEntity<JsonNode> createResponse =
        restTemplate.postForEntity("/groups", toCreate, JsonNode.class);
    assertEquals(CREATED, createResponse.getStatusCode());
    assertNotNull(createResponse.getBody());
    UUID createdId = UUID.fromString(createResponse.getBody().get("id").asText());

    ResponseEntity<StudentGroup> getResponse =
        restTemplate.getForEntity("/groups/" + createdId, StudentGroup.class);
    assertEquals(OK, getResponse.getStatusCode());
    assertNotNull(getResponse.getBody());
    assertEquals("A1", getResponse.getBody().reference());
    assertEquals(promotionId, getResponse.getBody().promotionId());

    ResponseEntity<StudentGroup[]> listResponse =
        restTemplate.getForEntity("/groups", StudentGroup[].class);
    assertEquals(OK, listResponse.getStatusCode());
    assertNotNull(listResponse.getBody());
    assertNotEquals(0, listResponse.getBody().length);

    var toUpdate = new StudentGroup(null, "C3", promotionId);
    restTemplate.put("/groups/" + createdId, toUpdate);

    ResponseEntity<StudentGroup> afterUpdate =
        restTemplate.getForEntity("/groups/" + createdId, StudentGroup.class);
    assertNotNull(afterUpdate.getBody());
    assertEquals("C3", afterUpdate.getBody().reference());

    restTemplate.delete("/groups/" + createdId);

    ResponseEntity<String> afterDelete =
        restTemplate.getForEntity("/groups/" + createdId, String.class);
    assertEquals(NOT_FOUND, afterDelete.getStatusCode());
  }

  @Test
  void create_blankReference_returnsBadRequest() {
    var invalid = new StudentGroup(null, "", UUID.randomUUID());

    ResponseEntity<String> response = restTemplate.postForEntity("/groups", invalid, String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void create_invalidReferencePattern_returnsBadRequest() {
    var invalid = new StudentGroup(null, "a1", UUID.randomUUID());

    ResponseEntity<String> response = restTemplate.postForEntity("/groups", invalid, String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void create_missingPromotion_returnsNotFound() {
    var group = new StudentGroup(null, "Z9", UUID.randomUUID());

    ResponseEntity<String> response = restTemplate.postForEntity("/groups", group, String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void create_duplicateReferenceInSamePromotion_returnsConflict() {
    UUID promotionId = createPromotion("GRP-DUP-1", (short) 2022, (short) 2023);

    var first = new StudentGroup(null, "B2", promotionId);
    restTemplate.postForEntity("/groups", first, StudentGroup.class);

    var duplicate = new StudentGroup(null, "B2", promotionId);
    ResponseEntity<String> response =
        restTemplate.postForEntity("/groups", duplicate, String.class);

    assertEquals(CONFLICT, response.getStatusCode());
  }

  @Test
  void getById_missing_returnsNotFound() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/groups/" + UUID.randomUUID(), String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void delete_missing_returnsNotFound() {
    ResponseEntity<Void> response =
        restTemplate.exchange("/groups/" + UUID.randomUUID(), DELETE, null, Void.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  private UUID createPromotion(String reference, Short startYear, Short endYear) {
    var promotion = new Promotion(null, reference, startYear, endYear);
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity("/promotions", promotion, JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
    return UUID.fromString(response.getBody().get("id").asText());
  }
}
