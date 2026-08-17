package com.unigrade.api.model;

public enum Semester {
  S1,
  S2,
  S3,
  S4,
  S5,
  S6;

  public Level level() {
    return switch (this) {
      case S1, S2 -> Level.L1;
      case S3, S4 -> Level.L2;
      case S5, S6 -> Level.L3;
    };
  }
}
