package com.unigrade.api.repository.model;

import jakarta.persistence.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "course")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JCourse {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(length = 20, nullable = false)
  private String reference;

  @Column(length = 50, nullable = false)
  private String title;

  @Column(nullable = false)
  private Short credits;

  @ManyToMany
  @JoinTable(
      name = "group_course",
      joinColumns = @JoinColumn(name = "course_id"),
      inverseJoinColumns = @JoinColumn(name = "group_id"))
  private Set<JGroup> groups;

  @ManyToMany
  @JoinTable(
      name = "course_teacher",
      joinColumns = @JoinColumn(name = "course_id"),
      inverseJoinColumns = @JoinColumn(name = "teacher_id"))
  private Set<JUser> teachers;

  @OneToMany(mappedBy = "course")
  private List<JExam> exams;
}
