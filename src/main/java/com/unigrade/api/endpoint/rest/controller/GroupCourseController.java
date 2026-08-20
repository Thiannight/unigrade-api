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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/groups/{groupId}/courses")
@RequiredArgsConstructor
public class GroupCourseController {

  private final GroupCourseService service;

  @GetMapping
  public List<GroupCourse> findActive(
      @PathVariable UUID groupId,
      @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
    return (activeOnly) ? service.findIncompleteByGroupId(groupId) : service.findByGroupId(groupId);
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

  @DeleteMapping("/{courseId}")
  public ResponseEntity<Void> delete(@PathVariable UUID groupId, @PathVariable UUID courseId) {
    service.delete(groupId, courseId);
    return ResponseEntity.noContent().build();
  }
}
