package com.jikchin.jikchinbackend.domain.matemember.entity;

import java.util.Set;

public enum MateMemberStatus {
  PENDING,
  ACTIVE,
  LEFT;

  // 참여가 확정된 적 있는 상태. 후기·신고 자격 판단에 사용한다.
  public static final Set<MateMemberStatus> PARTICIPATED = Set.of(ACTIVE, LEFT);
}
