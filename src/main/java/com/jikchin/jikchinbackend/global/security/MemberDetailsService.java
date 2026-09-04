package com.jikchin.jikchinbackend.global.security;

import com.jikchin.jikchinbackend.domain.member.entity.Member;
import com.jikchin.jikchinbackend.domain.member.repository.MemberRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberDetailsService implements UserDetailsService {

  private final MemberRepository memberRepository;

  @Override
  public UserDetails loadUserByUsername(String email) {
    Member member =
        memberRepository
            .findByEmail(email.toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new UsernameNotFoundException("회원을 찾을 수 없습니다."));

    return MemberPrincipal.from(member);
  }
}
