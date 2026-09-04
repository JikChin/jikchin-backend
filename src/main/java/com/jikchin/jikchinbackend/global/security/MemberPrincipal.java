package com.jikchin.jikchinbackend.global.security;

import com.jikchin.jikchinbackend.domain.member.entity.Member;
import com.jikchin.jikchinbackend.domain.member.entity.MemberStatus;
import com.jikchin.jikchinbackend.domain.member.entity.Role;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@RequiredArgsConstructor
public class MemberPrincipal implements UserDetails {

  private final UUID memberKey;
  private final String email;
  private final String password;
  private final Role role;
  private final MemberStatus status;

  public static MemberPrincipal from(Member member) {
    return new MemberPrincipal(
        member.getMemberKey(),
        member.getEmail(),
        member.getPassword(),
        member.getRole(),
        member.getStatus());
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority(role.name()));
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isEnabled() {
    return status == MemberStatus.ACTIVE;
  }
}
