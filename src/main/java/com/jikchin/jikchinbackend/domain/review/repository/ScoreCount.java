package com.jikchin.jikchinbackend.domain.review.repository;

/** 별점별 리뷰 수 집계 결과. GROUP BY score 쿼리의 프로젝션으로 사용한다. */
public interface ScoreCount {

  int getScore();

  long getCount();
}
