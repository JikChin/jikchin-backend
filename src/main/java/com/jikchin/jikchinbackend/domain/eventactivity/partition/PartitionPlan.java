package com.jikchin.jikchinbackend.domain.eventactivity.partition;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Validate the actual database boundaries before producing any destructive DDL. */
public record PartitionPlan(List<YearMonth> create, List<String> drop) {
  public record Boundary(String name, String value, String method, String expression) {}

  public static PartitionPlan from(List<Boundary> boundaries, YearMonth current) {
    if (boundaries.size() < 2) throw new IllegalStateException("파티션 초기 스키마가 없습니다.");
    List<String> drop = new ArrayList<>();
    LocalDateTime previous = null;
    LocalDateTime cutoff = current.minusMonths(12).atDay(1).atStartOfDay();
    for (int i = 0; i < boundaries.size(); i++) {
      Boundary b = boundaries.get(i);
      if (!"RANGE COLUMNS".equals(b.method())
          || b.expression() == null
          || !"occurred_at".equals(b.expression().replace("`", "").trim()))
        throw new IllegalStateException("지원하지 않는 파티션 구조입니다.");
      if (i == boundaries.size() - 1) {
        if (!"pmax".equals(b.name()) || !"MAXVALUE".equals(b.value()))
          throw new IllegalStateException("마지막 파티션은 pmax여야 합니다.");
        break;
      }
      String raw = b.value().replace("'", "").trim();
      LocalDateTime end =
          LocalDateTime.parse(raw.length() == 10 ? raw + "T00:00:00" : raw.replace(' ', 'T'));
      if (!end.equals(YearMonth.from(end).atDay(1).atStartOfDay())
          || (previous != null && !end.isAfter(previous)))
        throw new IllegalStateException("월 경계가 올바르지 않습니다.");
      if (i == 0) {
        if (!"p_before".equals(b.name())) throw new IllegalStateException("p_before가 필요합니다.");
      } else {
        if (b.name() == null || !b.name().matches("p[0-9]{6}"))
          throw new IllegalStateException("파티션 이름이 올바르지 않습니다.");
        YearMonth named =
            YearMonth.parse(b.name().substring(1), DateTimeFormatter.ofPattern("yyyyMM"));
        if (!named.plusMonths(1).atDay(1).atStartOfDay().equals(end))
          throw new IllegalStateException("파티션 이름과 실제 경계가 일치하지 않습니다.");
        // The first retained month may have a widened lower boundary after older months were
        // dropped.
        if (i > 1 && !previous.plusMonths(1).equals(end))
          throw new IllegalStateException("월 파티션 사이에 누락이 있습니다.");
        if (!end.isAfter(cutoff)) drop.add(b.name());
      }
      previous = end;
    }
    List<YearMonth> create = new ArrayList<>();
    for (YearMonth m = YearMonth.from(previous);
        !m.isAfter(current.plusMonths(3));
        m = m.plusMonths(1)) {
      if (create.size() >= 120) throw new IllegalStateException("120개월 이상 복구는 수동 검토가 필요합니다.");
      create.add(m);
    }
    return new PartitionPlan(List.copyOf(create), List.copyOf(drop));
  }
}
