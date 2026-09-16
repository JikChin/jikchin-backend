package com.jikchin.jikchinbackend.domain.eventactivity;

public enum ActivityType {
  MATE_POST_CREATED(5, "MATE_POST"),
  MATE_MEMBER_ACCEPTED(2, "MATE_APPLICATION"),
  MATE_MEMBER_JOINED(2, "MATE_MEMBER"),
  REVIEW_CREATED(3, "REVIEW");
  private final int score;
  private final String sourceType;

  ActivityType(int score, String sourceType) {
    this.score = score;
    this.sourceType = sourceType;
  }

  public int score() {
    return score;
  }

  public String sourceType() {
    return sourceType;
  }
}
