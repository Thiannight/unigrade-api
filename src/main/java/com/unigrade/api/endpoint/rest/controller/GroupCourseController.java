package com.unigrade.api.endpoint.rest.controller;

import com.unigrade.api.model.GroupCourse;
import com.unigrade.api.model.dto.GroupCourseAssignRequest;
import com.unigrade.api.model.dto.GroupCourseEndRequest;
import com.unigrade.api.service.GroupCourseService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groups/{groupId}/courses")
@RequiredArgsConstructor
public class GroupCourseController {

  private final GroupCourseService service;

  @GetMapping
  public List<GroupCourse> findActive(@PathVariable UUID groupId) {
    return service.findActiveByGroup(groupId);
  }

  @PostMapping
  public ResponseEntity<GroupCourse> assign(
      @PathVariable UUID groupId, @Valid @RequestBody GroupCourseAssignRequest request) {
    GroupCourse created = service.assign(groupId, request);
    return ResponseEntity.created(
            URI.create("/groups/" + groupId + "/courses/" + created.courseId()))
        .body(created);
  }

  @PutMapping("/{courseId}")
  public GroupCourse end(
      @PathVariable UUID groupId,
      @PathVariable UUID courseId,
      @Valid @RequestBody GroupCourseEndRequest request) {
    return service.end(groupId, courseId, request);
  }
}
