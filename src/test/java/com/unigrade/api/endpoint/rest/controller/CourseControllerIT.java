package com.unigrade.api.endpoint.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import com.unigrade.api.conf.FacadeIT;
import com.unigrade.api.model.Course;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

class CourseControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void crud_lifecycle() {
    var toCreate = new Course(null, "CS-IT-101", "Intro to Testing", (short) 6);

    ResponseEntity<Course> createResponse =
        restTemplate.postForEntity("/courses", toCreate, Course.class);
    assertThat(createResponse.getStatusCode()).isEqualTo(CREATED);
    Course created = createResponse.getBody();
    assertThat(created).isNotNull();
    assertThat(created.id()).isNotNull();

    ResponseEntity<Course> getResponse =
        restTemplate.getForEntity("/courses/" + created.id(), Course.class);
    assertThat(getResponse.getStatusCode()).isEqualTo(OK);
    assertThat(getResponse.getBody().reference()).isEqualTo("CS-IT-101");

    ResponseEntity<Course[]> listResponse = restTemplate.getForEntity("/courses", Course[].class);
    assertThat(listResponse.getStatusCode()).isEqualTo(OK);
    assertThat(listResponse.getBody()).isNotEmpty();

    var toUpdate = new Course(created.id(), "CS-IT-101-BIS", "Intro to Testing v2", (short) 8);
    restTemplate.put("/courses/" + created.id(), toUpdate);

    ResponseEntity<Course> afterUpdate =
        restTemplate.getForEntity("/courses/" + created.id(), Course.class);
    assertThat(afterUpdate.getBody().reference()).isEqualTo("CS-IT-101-BIS");
    assertThat(afterUpdate.getBody().credits()).isEqualTo((short) 8);

    restTemplate.delete("/courses/" + created.id());

    ResponseEntity<String> afterDelete =
        restTemplate.getForEntity("/courses/" + created.id(), String.class);
    assertThat(afterDelete.getStatusCode()).isEqualTo(NOT_FOUND);
  }

  @Test
  void create_blankReference_returnsBadRequest() {
    var invalid = new Course(null, "", "Intro to Testing", (short) 6);

    ResponseEntity<String> response = restTemplate.postForEntity("/courses", invalid, String.class);

    assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  @Test
  void create_nonPositiveCredits_returnsBadRequest() {
    var invalid = new Course(null, "CS-IT-102", "Negative Credits", (short) 0);

    ResponseEntity<String> response = restTemplate.postForEntity("/courses", invalid, String.class);

    assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  @Test
  void create_duplicateReference_returnsConflict() {
    var first = new Course(null, "CS-IT-DUP", "First title", (short) 6);
    restTemplate.postForEntity("/courses", first, Course.class);

    var duplicate = new Course(null, "CS-IT-DUP", "Different title", (short) 4);
    ResponseEntity<String> response =
        restTemplate.postForEntity("/courses", duplicate, String.class);

    assertThat(response.getStatusCode()).isEqualTo(CONFLICT);
  }

  @Test
  void create_duplicateTitle_returnsConflict() {
    var first = new Course(null, "CS-IT-DUPTITLE1", "Duplicate Title", (short) 6);
    restTemplate.postForEntity("/courses", first, Course.class);

    var duplicate = new Course(null, "CS-IT-DUPTITLE2", "Duplicate Title", (short) 4);
    ResponseEntity<String> response =
        restTemplate.postForEntity("/courses", duplicate, String.class);

    assertThat(response.getStatusCode()).isEqualTo(CONFLICT);
  }

  @Test
  void getById_missing_returnsNotFound() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/courses/" + UUID.randomUUID(), String.class);

    assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
  }

  @Test
  void delete_missing_returnsNotFound() {
    ResponseEntity<Void> response =
        restTemplate.exchange("/courses/" + UUID.randomUUID(), DELETE, null, Void.class);

    assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
  }
}
