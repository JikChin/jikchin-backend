package com.jikchin.jikchinbackend.domain.eventactivity;

import static org.assertj.core.api.Assertions.*;

import com.jikchin.jikchinbackend.domain.eventactivity.partition.PartitionPlan;
import com.jikchin.jikchinbackend.domain.eventactivity.partition.PartitionPlan.Boundary;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class PartitionPlanTest {
  private Boundary b(String name, String value) {
    return new Boundary(name, value, "RANGE COLUMNS", "`occurred_at`");
  }

  @Test
  void keepsBoundaryMonthAndCreatesFutureMonths() {
    var plan =
        PartitionPlan.from(
            List.of(
                b("p_before", "'2025-08-01'"),
                b("p202508", "'2025-09-01'"),
                b("p202509", "'2025-10-01 00:00:00'"),
                b("pmax", "MAXVALUE")),
            YearMonth.of(2026, 9));
    assertThat(plan.drop()).containsExactly("p202508");
    assertThat(plan.create())
        .contains(YearMonth.of(2026, 12))
        .doesNotContain(YearMonth.of(2027, 1));
  }

  @Test
  void octoberExpiresLastSeptember() {
    var plan =
        PartitionPlan.from(
            List.of(
                b("p_before", "'2025-08-01'"), b("p202509", "'2025-10-01'"), b("pmax", "MAXVALUE")),
            YearMonth.of(2026, 10));
    assertThat(plan.drop()).containsExactly("p202509");
    assertThat(plan.create().getLast()).isEqualTo(YearMonth.of(2027, 1));
  }

  @Test
  void rejectsMismatchedNameBeforeAnyDeletion() {
    assertThatThrownBy(
            () ->
                PartitionPlan.from(
                    List.of(
                        b("p_before", "'2025-01-01'"),
                        b("p202508", "'2025-10-01'"),
                        b("pmax", "MAXVALUE")),
                    YearMonth.of(2026, 9)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rejectsWrongPartitionKeyAndMissingCatchAll() {
    assertThatThrownBy(
            () ->
                PartitionPlan.from(
                    List.of(
                        new Boundary("p_before", "'2025-01-01'", "RANGE COLUMNS", "created_at"),
                        b("pmax", "MAXVALUE")),
                    YearMonth.of(2026, 9)))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                PartitionPlan.from(
                    List.of(b("p_before", "'2025-01-01'"), b("p202501", "'2025-02-01'")),
                    YearMonth.of(2026, 9)))
        .isInstanceOf(IllegalStateException.class);
  }
}
