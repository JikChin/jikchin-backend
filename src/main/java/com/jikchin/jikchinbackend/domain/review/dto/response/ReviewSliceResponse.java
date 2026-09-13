package com.jikchin.jikchinbackend.domain.review.dto.response;

import com.jikchin.jikchinbackend.domain.review.entity.Review;
import java.util.List;

/**
 * 받은 리뷰 목록 한 페이지.
 *
 * @param nextCursor 다음 페이지를 요청할 때 {@code cursor}로 넘길 마지막 리뷰 id. 다음 페이지가 없으면 null.
 */
public record ReviewSliceResponse(List<ReviewResponse> reviews, boolean hasNext, Long nextCursor) {

  /** size + 1건을 조회한 결과를 받아 다음 페이지 존재 여부를 판단하고 size건만 돌려준다. */
  public static ReviewSliceResponse of(List<Review> fetched, int size) {
    boolean hasNext = fetched.size() > size;
    List<Review> page = hasNext ? fetched.subList(0, size) : fetched;
    List<ReviewResponse> reviews = page.stream().map(ReviewResponse::from).toList();
    Long nextCursor = hasNext ? page.getLast().getId() : null;
    return new ReviewSliceResponse(reviews, hasNext, nextCursor);
  }
}
