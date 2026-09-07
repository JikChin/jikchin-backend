package com.jikchin.jikchinbackend.domain.report.entity;

import com.jikchin.jikchinbackend.domain.matepost.entity.MatePost;
import com.jikchin.jikchinbackend.global.error.AppException;
import com.jikchin.jikchinbackend.global.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "reports",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_reports_once",
            columnNames = {"mate_post_id", "reporter_id", "reported_user_id", "reason"}),
    indexes = {
      @Index(name = "idx_reports_reported_user", columnList = "reported_user_id"),
      @Index(name = "idx_reports_status", columnList = "status"),
      @Index(name = "fk_reports_reporter", columnList = "reporter_id")
    })
public class Report {

  public static final int DETAIL_MAX_LENGTH = 1000;
  public static final int ENUM_MAX_LENGTH = 30;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "reporter_id", nullable = false)
  private Long reporterId;

  @Column(name = "reported_user_id", nullable = false)
  private Long reportedUserId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "mate_post_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_reports_post"))
  private MatePost matePost;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = ENUM_MAX_LENGTH)
  private ReportReason reason;

  @Column(length = DETAIL_MAX_LENGTH)
  private String detail;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = ENUM_MAX_LENGTH)
  private ReportStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "processed_at", nullable = false)
  private LocalDateTime processedAt;

  private Report(
      MatePost matePost, Long reporterId, Long reportedUserId, ReportReason reason, String detail) {
    this.matePost = matePost;
    this.reporterId = reporterId;
    this.reportedUserId = reportedUserId;
    this.reason = reason;
    this.detail = detail;
    this.status = ReportStatus.PENDING;
    this.createdAt = LocalDateTime.now();
    this.processedAt = this.createdAt;
  }

  public static Report create(
      MatePost matePost, Long reporterId, Long reportedUserId, ReportReason reason, String detail) {
    return new Report(matePost, reporterId, reportedUserId, reason, detail);
  }

  public void resolve() {
    requirePending();
    this.status = ReportStatus.RESOLVED;
    this.processedAt = LocalDateTime.now();
  }

  public void reject() {
    requirePending();
    this.status = ReportStatus.REJECTED;
    this.processedAt = LocalDateTime.now();
  }

  private void requirePending() {
    if (status != ReportStatus.PENDING) {
      throw new AppException(ErrorType.REPORT_ALREADY_PROCESSED);
    }
  }
}
