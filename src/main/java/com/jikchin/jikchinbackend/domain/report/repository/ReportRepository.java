package com.jikchin.jikchinbackend.domain.report.repository;

import com.jikchin.jikchinbackend.domain.report.entity.Report;
import com.jikchin.jikchinbackend.domain.report.entity.ReportReason;
import com.jikchin.jikchinbackend.domain.report.entity.ReportStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

  boolean existsByMatePost_IdAndReporterIdAndReportedUserIdAndReason(
      Long matePostId, Long reporterId, Long reportedUserId, ReportReason reason);

  List<Report> findAllByStatusOrderByCreatedAtAsc(ReportStatus status);

  List<Report> findAllByOrderByCreatedAtAsc();
}
