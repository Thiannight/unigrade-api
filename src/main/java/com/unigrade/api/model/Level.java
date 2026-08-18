package com.unigrade.api.model;

public enum Level {
  L1,
  L2,
  L3;

  public static final int PER_LEVEL_CREDIT = 60;

  public static int requiredCredits(int levelCount) {
    return levelCount * PER_LEVEL_CREDIT;
  }
}
