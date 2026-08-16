package com.unigrade.api.endpoint.rest.controller;

import com.unigrade.api.model.Exam;
import com.unigrade.api.model.dto.ExamRequest;
import com.unigrade.api.service.ExamService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groups/{groupId}/courses/{courseId}/exams")
@RequiredArgsConstructor
public class ExamController {

  private final ExamService service;

  @GetMapping
  public List<Exam> findByGroupAndCourse(@PathVariable UUID groupId, @PathVariable UUID courseId) {
    return service.findByGroupAndCourse(groupId, courseId);
  }

  @PostMapping
  public ResponseEntity<Exam> create(
      @PathVariable UUID groupId,
      @PathVariable UUID courseId,
      @Valid @RequestBody ExamRequest request) {
    Exam created = service.create(groupId, courseId, request);
    return ResponseEntity.created(
            URI.create("/groups/" + groupId + "/courses/" + courseId + "/exams/" + created.id()))
        .body(created);
  }

  @PutMapping("/{examId}")
  public Exam update(
      @PathVariable UUID groupId,
      @PathVariable UUID courseId,
      @PathVariable UUID examId,
      @Valid @RequestBody ExamRequest request) {
    return service.update(groupId, courseId, examId, request);
  }

  @DeleteMapping("/{examId}")
  public ResponseEntity<Void> delete(
      @PathVariable UUID groupId, @PathVariable UUID courseId, @PathVariable UUID examId) {
    service.delete(groupId, courseId, examId);
    return ResponseEntity.noContent().build();
  }
}
