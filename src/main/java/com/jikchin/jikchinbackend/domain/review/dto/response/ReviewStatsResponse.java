package com.jikchin.jikchinbackend.domain.review.dto.response;

import com.jikchin.jikchinbackend.domain.review.entity.Review;
import com.jikchin.jikchinbackend.domain.review.entity.ReviewStats;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.TreeMap;

/**
 * 회원이 받은 리뷰의 별점 통계.
 *
 * @param scoreCounts 1~5점 각각의 리뷰 수. 리뷰가 없는 점수도 0으로 항상 포함한다.
 * @param averageScore 소수점 둘째 자리까지 반올림한 평균. 리뷰가 없으면 null.
 * @param mostFrequentScore 가장 많이 받은 별점. 동률이면 높은 점수를 우선하며, 리뷰가 없으면 null.
 */
public record ReviewStatsResponse(
    Long revieweeId,
    long totalCount,
    BigDecimal averageScore,
    Integer mostFrequentScore,
    Map<Integer, Long> scoreCounts) {

  private static final int AVERAGE_SCALE = 2;

  /** 리뷰를 한 번도 받지 않아 집계 행이 없는 회원의 통계. */
  public static ReviewStatsResponse empty(Long revieweeId) {
    Map<Integer, Long> scoreCounts = new TreeMap<>();
    for (int score = Review.MIN_SCORE; score <= Review.MAX_SCORE; score++) {
      scoreCounts.put(score, 0L);
    }
    return new ReviewStatsResponse(revieweeId, 0, null, null, scoreCounts);
  }

  public static ReviewStatsResponse from(ReviewStats stats) {
    Map<Integer, Long> scoreCounts = new TreeMap<>();
    // 오름차순으로 훑으며 >= 비교하므로 동률이면 높은 점수가 남는다.
    Integer mostFrequentScore = null;
    long mostFrequentCount = 0;
    for (int score = Review.MIN_SCORE; score <= Review.MAX_SCORE; score++) {
      long count = stats.countOf(score);
      scoreCounts.put(score, count);
      if (count > 0 && count >= mostFrequentCount) {
        mostFrequentScore = score;
        mostFrequentCount = count;
      }
    }

    BigDecimal averageScore =
        stats.getTotalCount() == 0
            ? null
            : BigDecimal.valueOf(stats.getScoreSum())
                .divide(
                    BigDecimal.valueOf(stats.getTotalCount()), AVERAGE_SCALE, RoundingMode.HALF_UP);
    return new ReviewStatsResponse(
        stats.getRevieweeId(), stats.getTotalCount(), averageScore, mostFrequentScore, scoreCounts);
  }
}
