package com.unigrade.api.endpoint.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

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

    ResponseEntity<Promotion> createResponse =
        restTemplate.postForEntity("/promotions", toCreate, Promotion.class);
    assertThat(createResponse.getStatusCode()).isEqualTo(CREATED);
    Promotion created = createResponse.getBody();
    assertThat(created).isNotNull();
    assertThat(created.id()).isNotNull();

    ResponseEntity<Promotion> getResponse =
        restTemplate.getForEntity("/promotions/" + created.id(), Promotion.class);
    assertThat(getResponse.getStatusCode()).isEqualTo(OK);
    assertThat(getResponse.getBody().reference()).isEqualTo("PROMO-IT-2030");

    ResponseEntity<Promotion[]> listResponse =
        restTemplate.getForEntity("/promotions", Promotion[].class);
    assertThat(listResponse.getStatusCode()).isEqualTo(OK);
    assertThat(listResponse.getBody()).isNotEmpty();

    var toUpdate = new Promotion(created.id(), "PROMO-IT-2030-BIS", (short) 2030, (short) 2034);
    restTemplate.put("/promotions/" + created.id(), toUpdate);

    ResponseEntity<Promotion> afterUpdate =
        restTemplate.getForEntity("/promotions/" + created.id(), Promotion.class);
    assertThat(afterUpdate.getBody().reference()).isEqualTo("PROMO-IT-2030-BIS");
    assertThat(afterUpdate.getBody().endYear()).isEqualTo((short) 2034);

    restTemplate.delete("/promotions/" + created.id());

    ResponseEntity<String> afterDelete =
        restTemplate.getForEntity("/promotions/" + created.id(), String.class);
    assertThat(afterDelete.getStatusCode()).isEqualTo(NOT_FOUND);
  }

  @Test
  void create_invalidYears_returnsBadRequest() {
    var invalid = new Promotion(null, "PROMO-IT-INVALID", (short) 2030, (short) 2020);

    ResponseEntity<String> response =
        restTemplate.postForEntity("/promotions", invalid, String.class);

    assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  @Test
  void create_blankReference_returnsBadRequest() {
    var invalid = new Promotion(null, "", (short) 2030, (short) 2033);

    ResponseEntity<String> response =
        restTemplate.postForEntity("/promotions", invalid, String.class);

    assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  @Test
  void create_duplicateReference_returnsConflict() {
    var first = new Promotion(null, "PROMO-IT-DUP", (short) 2040, (short) 2043);
    restTemplate.postForEntity("/promotions", first, Promotion.class);

    var duplicate = new Promotion(null, "PROMO-IT-DUP", (short) 2041, (short) 2044);
    ResponseEntity<String> response =
        restTemplate.postForEntity("/promotions", duplicate, String.class);

    assertThat(response.getStatusCode()).isEqualTo(CONFLICT);
  }

  @Test
  void getById_missing_returnsNotFound() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/promotions/" + UUID.randomUUID(), String.class);

    assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
  }

  @Test
  void delete_missing_returnsNotFound() {
    ResponseEntity<Void> response =
        restTemplate.exchange("/promotions/" + UUID.randomUUID(), DELETE, null, Void.class);

    assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
  }
}
