package com.unigrade.api.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "groups")
@Getter
@Setter
public class Group {

  @Id
  @Column(length = 50)
  private String id;

  @Column(length = 2, nullable = false)
  private String ref;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "promotion_id", nullable = false)
  private Promotion promotion;
}
