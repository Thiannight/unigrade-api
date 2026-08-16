package com.unigrade.api.endpoint.rest.controller;

import com.unigrade.api.model.Grade;
import com.unigrade.api.model.dto.GradeRequest;
import com.unigrade.api.service.GradeService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groups/{groupId}/courses/{courseId}/exams/{examId}/grades")
@RequiredArgsConstructor
public class GradeController {

  private final GradeService service;

  @GetMapping
  public List<Grade> findByExam(
      @PathVariable UUID groupId,
      @PathVariable UUID courseId,
      @PathVariable UUID examId,
      @RequestParam(required = false) String studentId) {
    return service.findByExam(groupId, courseId, examId, studentId);
  }

  @PostMapping
  public ResponseEntity<Grade> grade(
      @PathVariable UUID groupId,
      @PathVariable UUID courseId,
      @PathVariable UUID examId,
      @Valid @RequestBody GradeRequest request) {
    Grade created = service.grade(groupId, courseId, examId, request);
    return ResponseEntity.created(
            URI.create(
                "/groups/"
                    + groupId
                    + "/courses/"
                    + courseId
                    + "/exams/"
                    + examId
                    + "/grades/"
                    + created.id()))
        .body(created);
  }
}
