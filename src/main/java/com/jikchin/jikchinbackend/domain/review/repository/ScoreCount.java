package com.jikchin.jikchinbackend.domain.review.repository;

/** 별점별 리뷰 수 집계 결과. 집계 행이 없을 때 쓰는 GROUP BY 쿼리의 프로젝션이다. */
public interface ScoreCount {

  int getScore();

  long getCount();
}
