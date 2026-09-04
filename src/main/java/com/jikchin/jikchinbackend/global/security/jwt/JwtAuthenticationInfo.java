package com.jikchin.jikchinbackend.global.security.jwt;

import com.jikchin.jikchinbackend.domain.member.entity.Role;
import java.util.UUID;

public record JwtAuthenticationInfo(UUID memberKey, Role role) {}
