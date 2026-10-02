package com.jikchin.jikchinbackend.domain.matepost.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MatePostCreateRequestTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    factory.close();
  }

  @Test
  void acceptsAgeRangeFromZeroToHundred() {
    assertThat(validator.validate(requestWithAges(0, 100))).isEmpty();
  }

  @Test
  void rejectsAgeOverHundred() {
    Set<ConstraintViolation<MatePostCreateRequest>> violations =
        validator.validate(requestWithAges(20, 101));

    assertThat(violations)
        .extracting(v -> v.getPropertyPath().toString())
        .containsExactly("maxAge");
  }

  @Test
  void rejectsNegativeAge() {
    Set<ConstraintViolation<MatePostCreateRequest>> violations =
        validator.validate(requestWithAges(-1, 40));

    assertThat(violations)
        .extracting(v -> v.getPropertyPath().toString())
        .containsExactly("minAge");
  }

  private MatePostCreateRequest requestWithAges(Integer minAge, Integer maxAge) {
    return new MatePostCreateRequest(
        10L, "잠실 경기 같이 봐요", "즐겁게 응원할 분을 모집합니다.", 3, "ANY", minAge, maxAge, "1루 네이비석");
  }
}
