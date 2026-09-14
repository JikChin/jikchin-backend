package com.jikchin.jikchinbackend.domain.report.repository;

import com.jikchin.jikchinbackend.domain.report.entity.Report;
import com.jikchin.jikchinbackend.domain.report.entity.ReportReason;
import com.jikchin.jikchinbackend.domain.report.entity.ReportStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

  boolean existsByMatePost_IdAndReporterIdAndReportedUserIdAndReason(
      Long matePostId, Long reporterId, Long reportedUserId, ReportReason reason);

  /** 관리자 처리 큐는 오래된 신고가 먼저다. 같은 시각이면 id가 작은 쪽이 먼저. */
  List<Report> findAllByOrderByCreatedAtAscIdAsc(Pageable pageable);

  List<Report> findAllByStatusOrderByCreatedAtAscIdAsc(ReportStatus status, Pageable pageable);

  /** 커서 다음 페이지. (created_at, id) 키셋이므로 OFFSET 없이 이어 읽는다. */
  @Query(
      """
      select r from Report r
      where (r.createdAt > :cursorCreatedAt
          or (r.createdAt = :cursorCreatedAt and r.id > :cursorId))
      order by r.createdAt asc, r.id asc
      """)
  List<Report> findAfterCursor(
      @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
      @Param("cursorId") Long cursorId,
      Pageable pageable);

  /** 상태별 커서 다음 페이지. (status, created_at) 인덱스가 있으면 status 구간을 created_at 순으로 size건만 읽는다. */
  @Query(
      """
      select r from Report r
      where r.status = :status
        and (r.createdAt > :cursorCreatedAt
          or (r.createdAt = :cursorCreatedAt and r.id > :cursorId))
      order by r.createdAt asc, r.id asc
      """)
  List<Report> findByStatusAfterCursor(
      @Param("status") ReportStatus status,
      @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
      @Param("cursorId") Long cursorId,
      Pageable pageable);
}
