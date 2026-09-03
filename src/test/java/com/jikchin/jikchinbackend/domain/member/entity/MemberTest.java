package com.jikchin.jikchinbackend.domain.member.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MemberTest {

  @Test
  void 회원을_생성하면_식별자와_기본값이_설정된다() {
    Member member =
        Member.create(
            "MEMBER@example.com",
            "encoded-password",
            "직관러",
            "https://example.com/profile.png",
            Gender.FEMALE,
            LocalDate.of(2000, 1, 1),
            "서울");

    assertThat(member.getMemberKey()).isNotNull();
    assertThat(member.getEmail()).isEqualTo("member@example.com");
    assertThat(member.getMannerScore()).isEqualByComparingTo(new BigDecimal("5.00"));
    assertThat(member.getRole()).isEqualTo(Role.ROLE_USER);
    assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
  }

  @Test
  void 회원마다_서로_다른_식별자를_생성한다() {
    Member first = createMember("first@example.com", "첫번째");
    Member second = createMember("second@example.com", "두번째");

    assertThat(first.getMemberKey()).isNotEqualTo(second.getMemberKey());
  }

  @Test
  void 필수값이_비어있으면_회원을_생성할_수_없다() {
    assertThatThrownBy(() -> createMember(" ", "닉네임"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("이메일은 필수입니다.");
  }

  private Member createMember(String email, String nickname) {
    return Member.create(email, "encoded-password", nickname, null, null, null, null);
  }
}
