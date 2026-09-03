package com.jikchin.jikchinbackend.global.security;

import java.util.Collection;
import java.util.UUID;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class MemberAuthenticationToken extends AbstractAuthenticationToken {

  private final UUID memberKey;

  public MemberAuthenticationToken(
      UUID memberKey, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.memberKey = memberKey;
    super.setAuthenticated(true);
  }

  @Override
  public Object getPrincipal() {
    return memberKey;
  }

  @Override
  public Object getCredentials() {
    return null;
  }
}
