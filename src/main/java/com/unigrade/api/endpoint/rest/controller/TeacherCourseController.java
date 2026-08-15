package com.unigrade.api.endpoint.rest.controller;

import com.unigrade.api.model.TeacherCourse;
import com.unigrade.api.model.dto.TeacherAssignmentRequest;
import com.unigrade.api.model.dto.TeacherPriorityRequest;
import com.unigrade.api.service.TeacherCourseService;
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
@RequestMapping("/courses/{courseId}/teachers")
@RequiredArgsConstructor
public class TeacherCourseController {

  private final TeacherCourseService service;

  @GetMapping
  public List<TeacherCourse> findByCourse(@PathVariable UUID courseId) {
    return service.findByCourse(courseId);
  }

  @PostMapping
  public ResponseEntity<TeacherCourse> assign(
      @PathVariable UUID courseId, @Valid @RequestBody TeacherAssignmentRequest request) {
    TeacherCourse created = service.assign(courseId, request);
    return ResponseEntity.created(
            URI.create("/courses/" + courseId + "/teachers/" + created.teacherId()))
        .body(created);
  }

  @PutMapping("/{teacherId}")
  public TeacherCourse updatePriority(
      @PathVariable UUID courseId,
      @PathVariable String teacherId,
      @Valid @RequestBody TeacherPriorityRequest request) {
    return service.updatePriority(courseId, teacherId, request);
  }

  @DeleteMapping("/{teacherId}")
  public ResponseEntity<Void> remove(@PathVariable UUID courseId, @PathVariable String teacherId) {
    service.remove(courseId, teacherId);
    return ResponseEntity.noContent().build();
  }
}
