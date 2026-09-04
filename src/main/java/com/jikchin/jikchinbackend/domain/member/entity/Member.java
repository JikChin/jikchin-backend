package com.jikchin.jikchinbackend.domain.member.entity;

import com.jikchin.jikchinbackend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "members",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_members_member_key", columnNames = "member_key"),
      @UniqueConstraint(name = "uk_members_email", columnNames = "email"),
      @UniqueConstraint(name = "uk_members_nickname", columnNames = "nickname")
    })
public class Member extends BaseEntity {

  public static final String EMAIL_UNIQUE_CONSTRAINT = "uk_members_email";
  public static final String NICKNAME_UNIQUE_CONSTRAINT = "uk_members_nickname";

  private static final BigDecimal INITIAL_MANNER_SCORE = new BigDecimal("5.00");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "member_key", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
  private UUID memberKey;

  @Column(nullable = false, length = 100)
  private String email;

  @Column(nullable = false, length = 255)
  private String password;

  @Column(nullable = false, length = 50)
  private String nickname;

  @Column(name = "profile_image_url", length = 500)
  private String profileImageUrl;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private Gender gender;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @Column(length = 100)
  private String region;

  @Column(name = "manner_score", nullable = false, precision = 3, scale = 2)
  private BigDecimal mannerScore;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MemberStatus status;

  @Builder(access = AccessLevel.PRIVATE)
  private Member(
      UUID memberKey,
      String email,
      String password,
      String nickname,
      String profileImageUrl,
      Gender gender,
      LocalDate birthDate,
      String region) {
    this.memberKey = Objects.requireNonNull(memberKey, "회원 식별자는 필수입니다.");
    this.email = normalizeEmail(email);
    this.password = requireText(password, "비밀번호는 필수입니다.");
    this.nickname = requireText(nickname, "닉네임은 필수입니다.");
    this.profileImageUrl = profileImageUrl;
    this.gender = gender;
    this.birthDate = birthDate;
    this.region = region;
    this.mannerScore = INITIAL_MANNER_SCORE;
    this.role = Role.ROLE_USER;
    this.status = MemberStatus.ACTIVE;
  }

  public static Member create(
      String email,
      String encodedPassword,
      String nickname,
      String profileImageUrl,
      Gender gender,
      LocalDate birthDate,
      String region) {
    return Member.builder()
        .memberKey(UUID.randomUUID())
        .email(email)
        .password(encodedPassword)
        .nickname(nickname)
        .profileImageUrl(profileImageUrl)
        .gender(gender)
        .birthDate(birthDate)
        .region(region)
        .build();
  }

  private static String normalizeEmail(String email) {
    return requireText(email, "이메일은 필수입니다.").toLowerCase(Locale.ROOT);
  }

  private static String requireText(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(message);
    }
    return value.trim();
  }
}
