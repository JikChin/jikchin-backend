package com.jikchin.jikchinbackend.domain.report.dto.response;

import com.jikchin.jikchinbackend.domain.report.entity.Report;
import java.util.List;

/**
 * 신고 목록 한 페이지.
 *
 * @param nextCursor 다음 페이지를 요청할 때 {@code cursor}로 넘길 마지막 신고 id. 다음 페이지가 없으면 null.
 */
public record ReportSliceResponse(List<ReportResponse> reports, boolean hasNext, Long nextCursor) {

  /** size + 1건을 조회한 결과를 받아 다음 페이지 존재 여부를 판단하고 size건만 돌려준다. */
  public static ReportSliceResponse of(List<Report> fetched, int size) {
    boolean hasNext = fetched.size() > size;
    List<Report> page = hasNext ? fetched.subList(0, size) : fetched;
    List<ReportResponse> reports = page.stream().map(ReportResponse::from).toList();
    Long nextCursor = hasNext ? page.getLast().getId() : null;
    return new ReportSliceResponse(reports, hasNext, nextCursor);
  }
}
