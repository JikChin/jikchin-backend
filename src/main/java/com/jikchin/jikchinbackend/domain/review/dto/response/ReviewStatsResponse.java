package com.jikchin.jikchinbackend.domain.review.dto.response;

import com.jikchin.jikchinbackend.domain.review.entity.Review;
import com.jikchin.jikchinbackend.domain.review.entity.ReviewStats;
import com.jikchin.jikchinbackend.domain.review.repository.ScoreCount;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
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

  /** 정상 경로: review_stats 집계 행 1건으로 만든다. */
  public static ReviewStatsResponse from(ReviewStats stats) {
    Map<Integer, Long> scoreCounts = emptyCounts();
    for (int score = Review.MIN_SCORE; score <= Review.MAX_SCORE; score++) {
      scoreCounts.put(score, stats.countOf(score));
    }
    return build(stats.getRevieweeId(), stats.getTotalCount(), stats.getScoreSum(), scoreCounts);
  }

  /** 집계 행이 없는 회원(백필 전, 또는 리뷰를 받은 적 없음): GROUP BY score 결과로 같은 응답을 만든다. */
  public static ReviewStatsResponse of(Long revieweeId, Collection<ScoreCount> counts) {
    Map<Integer, Long> scoreCounts = emptyCounts();
    long totalCount = 0;
    long scoreSum = 0;
    for (ScoreCount count : counts) {
      scoreCounts.put(count.getScore(), count.getCount());
      totalCount += count.getCount();
      scoreSum += (long) count.getScore() * count.getCount();
    }
    return build(revieweeId, totalCount, scoreSum, scoreCounts);
  }

  private static Map<Integer, Long> emptyCounts() {
    Map<Integer, Long> scoreCounts = new TreeMap<>();
    for (int score = Review.MIN_SCORE; score <= Review.MAX_SCORE; score++) {
      scoreCounts.put(score, 0L);
    }
    return scoreCounts;
  }

  private static ReviewStatsResponse build(
      Long revieweeId, long totalCount, long scoreSum, Map<Integer, Long> scoreCounts) {
    // 오름차순으로 훑으며 >= 비교하므로 동률이면 높은 점수가 남는다.
    Integer mostFrequentScore = null;
    long mostFrequentCount = 0;
    for (Map.Entry<Integer, Long> entry : scoreCounts.entrySet()) {
      if (entry.getValue() > 0 && entry.getValue() >= mostFrequentCount) {
        mostFrequentScore = entry.getKey();
        mostFrequentCount = entry.getValue();
      }
    }
    BigDecimal averageScore =
        totalCount == 0
            ? null
            : BigDecimal.valueOf(scoreSum)
                .divide(BigDecimal.valueOf(totalCount), AVERAGE_SCALE, RoundingMode.HALF_UP);
    return new ReviewStatsResponse(
        revieweeId, totalCount, averageScore, mostFrequentScore, scoreCounts);
  }
}
