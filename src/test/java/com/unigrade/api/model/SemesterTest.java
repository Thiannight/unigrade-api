package com.unigrade.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SemesterTest {

  @Test
  void level_mapsEachSemesterToItsLevel() {
    assertEquals(Level.L1, Semester.S1.level());
    assertEquals(Level.L1, Semester.S2.level());
    assertEquals(Level.L2, Semester.S3.level());
    assertEquals(Level.L2, Semester.S4.level());
    assertEquals(Level.L3, Semester.S5.level());
    assertEquals(Level.L3, Semester.S6.level());
  }
}
