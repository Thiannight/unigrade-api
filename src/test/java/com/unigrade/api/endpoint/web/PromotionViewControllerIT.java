package com.unigrade.api.endpoint.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

import com.fasterxml.jackson.databind.JsonNode;
import com.unigrade.api.conf.SecuredFacadeIT;
import com.unigrade.api.model.Course;
import com.unigrade.api.model.Promotion;
import com.unigrade.api.model.StudentGroup;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

class PromotionViewControllerIT extends SecuredFacadeIT {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void view_rendersPromotionsPage() {
    createPromotion("VIEW-IT-1", (short) 2200, (short) 2201);

    ResponseEntity<String> response = restTemplate.getForEntity("/promotions/view", String.class);

    assertEquals(OK, response.getStatusCode());
    assertTrue(response.getBody().contains("VIEW-IT-1"));
    assertTrue(response.getBody().contains("/graduates?specialization=TN"));
    assertTrue(response.getBody().contains("/graduates?specialization=EL"));
  }

  @Test
  void graduatesXlsx_includesOnlyCompletedStudents() throws Exception {
    UUID promotionId = createPromotion("GRAD-IT-1", (short) 2210, (short) 2211);
    String studentId = createStudent("grad-it-" + UUID.randomUUID() + "@unigrade.com");

    // Build a full 3-level (L1..L3 / S1..S6), fully-graded curriculum for this one
    // student,
    // so the GraduationService marks them as graduates. A student holds only one
    // active
    // membership at a time, so we assign once then transfer for each following
    // semester.
    UUID previousGroupId = null;
    for (int semesterIndex = 1; semesterIndex <= 6; semesterIndex++) {
      UUID groupId = createGroup("GRAD-A" + semesterIndex, promotionId);
      String startDate = "2024-0" + semesterIndex + "-01";
      if (previousGroupId == null) {
        createMembership(groupId, studentId, startDate);
      } else {
        transferMembership(previousGroupId, studentId, groupId, startDate);
      }
      previousGroupId = groupId;

      String suffix = UUID.randomUUID().toString().substring(0, 6);
      UUID courseId = createCourse("GC" + semesterIndex + "-" + suffix, 30);
      assignCourse(groupId, courseId, "S" + semesterIndex, startDate);
      String examId =
          createExam(groupId, courseId, "2024-0" + semesterIndex + "-15T09:00:00Z", 1.0);
      grade(groupId, courseId, examId, studentId, 15.0, "2024-0" + semesterIndex + "-20T09:00:00Z");
    }

    ResponseEntity<byte[]> response =
        restTemplate.getForEntity(
            "/promotions/" + promotionId + "/graduates?specialization=TN", byte[].class);

    assertEquals(OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().length > 0);

    try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getBody()))) {
      Sheet sheet = workbook.getSheetAt(0);
      Row headerRow = sheet.getRow(0);
      assertEquals("Rank", headerRow.getCell(0).getStringCellValue());

      Row dataRow = sheet.getRow(1);
      assertNotNull(dataRow, "expected at least one graduate row");
      assertEquals(studentId, dataRow.getCell(1).getStringCellValue());
    }
  }

  private UUID createPromotion(String reference, Short startYear, Short endYear) {
    var promotion = new Promotion(null, reference, startYear, endYear);
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity("/promotions", promotion, JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
    return UUID.fromString(response.getBody().get("id").asText());
  }

  private UUID createGroup(String reference, UUID promotionId) {
    var group = new StudentGroup(null, reference.substring(reference.length() - 2), promotionId);
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity("/groups", group, JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
    return UUID.fromString(response.getBody().get("id").asText());
  }

  private UUID createCourse(String reference, int credits) {
    var course = new Course(null, reference, "Title " + reference, (short) credits);
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity("/courses", course, JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
    return UUID.fromString(response.getBody().get("id").asText());
  }

  private void assignCourse(UUID groupId, UUID courseId, String semester, String startDate) {
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/courses",
            Map.of("courseId", courseId, "semester", semester, "startDate", startDate),
            JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
  }

  private void createMembership(UUID groupId, String studentId, String startDate) {
    ResponseEntity<JsonNode> response =
        restTemplate.exchange(
            "/students/" + studentId + "/transfer",
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("newGroupId", groupId, "transferDate", startDate)),
            JsonNode.class);
    assertEquals(OK, response.getStatusCode());
  }

  private void transferMembership(
      UUID fromGroupId, String studentId, UUID toGroupId, String transferDate) {
    ResponseEntity<JsonNode> response =
        restTemplate.exchange(
            "/students/" + studentId + "/transfer",
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("newGroupId", toGroupId, "transferDate", transferDate)),
            JsonNode.class);
    assertEquals(OK, response.getStatusCode());
  }

  private String createExam(UUID groupId, UUID courseId, String examDate, double coefficient) {
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/courses/" + courseId + "/exams",
            Map.of("examDate", examDate, "coefficient", coefficient),
            JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
    return response.getBody().get("id").asText();
  }

  private void grade(
      UUID groupId,
      UUID courseId,
      String examId,
      String studentId,
      double score,
      String gradeDate) {
    ResponseEntity<JsonNode> response =
        restTemplate.postForEntity(
            "/groups/" + groupId + "/courses/" + courseId + "/exams/" + examId + "/grades",
            Map.of(
                "score", score,
                "gradeDate", gradeDate,
                "reason", "Final",
                "studentId", studentId),
            JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
  }

  private String createStudent(String email) {
    Map<String, Object> body =
        Map.of(
            "firstName", "Ada",
            "lastName", "Lovelace",
            "birthDate", "2000-01-01",
            "email", email,
            "password", "hashed-password",
            "isActive", true,
            "role", "STUDENT",
            "specialization", "TN");
    ResponseEntity<JsonNode> response = restTemplate.postForEntity("/users", body, JsonNode.class);
    assertEquals(CREATED, response.getStatusCode());
    return response.getBody().get("id").asText();
  }
}
