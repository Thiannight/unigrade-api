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
import com.unigrade.api.conf.FacadeIT;
import com.unigrade.api.model.Promotion;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

class PromotionControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void crud_lifecycle() {
    var toCreate = new Promotion(null, "PROMO-IT-2030", (short) 2030, (short) 2033);

    ResponseEntity<JsonNode> createResponse =
        restTemplate.postForEntity("/promotions", toCreate, JsonNode.class);
    assertEquals(CREATED, createResponse.getStatusCode());
    assertNotNull(createResponse.getBody());
    UUID createdId = UUID.fromString(createResponse.getBody().get("id").asText());

    ResponseEntity<Promotion> getResponse =
        restTemplate.getForEntity("/promotions/" + createdId, Promotion.class);
    assertEquals(OK, getResponse.getStatusCode());
    assertEquals("PROMO-IT-2030", getResponse.getBody().reference());

    ResponseEntity<Promotion[]> listResponse =
        restTemplate.getForEntity("/promotions", Promotion[].class);
    assertEquals(OK, listResponse.getStatusCode());
    assertFalse(listResponse.getBody().length == 0);

    var toUpdate = new Promotion(null, "PROMO-IT-2030-BIS", (short) 2030, (short) 2034);
    restTemplate.put("/promotions/" + createdId, toUpdate);

    ResponseEntity<Promotion> afterUpdate =
        restTemplate.getForEntity("/promotions/" + createdId, Promotion.class);
    assertEquals("PROMO-IT-2030-BIS", afterUpdate.getBody().reference());
    assertEquals((short) 2034, afterUpdate.getBody().endYear());

    restTemplate.delete("/promotions/" + createdId);

    ResponseEntity<String> afterDelete =
        restTemplate.getForEntity("/promotions/" + createdId, String.class);
    assertEquals(NOT_FOUND, afterDelete.getStatusCode());
  }

  @Test
  void create_invalidYears_returnsBadRequest() {
    var invalid = new Promotion(null, "PROMO-IT-INVALID", (short) 2030, (short) 2020);

    ResponseEntity<String> response =
        restTemplate.postForEntity("/promotions", invalid, String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void create_blankReference_returnsBadRequest() {
    var invalid = new Promotion(null, "", (short) 2030, (short) 2033);

    ResponseEntity<String> response =
        restTemplate.postForEntity("/promotions", invalid, String.class);

    assertEquals(BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void create_duplicateReference_returnsConflict() {
    var first = new Promotion(null, "PROMO-IT-DUP", (short) 2040, (short) 2043);
    restTemplate.postForEntity("/promotions", first, Promotion.class);

    var duplicate = new Promotion(null, "PROMO-IT-DUP", (short) 2041, (short) 2044);
    ResponseEntity<String> response =
        restTemplate.postForEntity("/promotions", duplicate, String.class);

    assertEquals(CONFLICT, response.getStatusCode());
  }

  @Test
  void getById_missing_returnsNotFound() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/promotions/" + UUID.randomUUID(), String.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }

  @Test
  void delete_missing_returnsNotFound() {
    ResponseEntity<Void> response =
        restTemplate.exchange("/promotions/" + UUID.randomUUID(), DELETE, null, Void.class);

    assertEquals(NOT_FOUND, response.getStatusCode());
  }
}
