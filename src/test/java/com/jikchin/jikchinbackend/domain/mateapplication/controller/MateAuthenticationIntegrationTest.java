package com.jikchin.jikchinbackend.domain.mateapplication.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jikchin.jikchinbackend.domain.mateapplication.dto.request.MateApplicationCreateRequest;
import com.jikchin.jikchinbackend.domain.mateapplication.entity.MateApplicationStatus;
import com.jikchin.jikchinbackend.domain.mateapplication.repository.MateApplicationRepository;
import com.jikchin.jikchinbackend.domain.mateapplication.service.MateApplicationService;
import com.jikchin.jikchinbackend.domain.matemember.repository.MateMemberRepository;
import com.jikchin.jikchinbackend.domain.matepost.dto.request.MatePostCreateRequest;
import com.jikchin.jikchinbackend.domain.matepost.repository.MatePostRepository;
import com.jikchin.jikchinbackend.domain.matepost.service.MatePostService;
import com.jikchin.jikchinbackend.domain.member.entity.Member;
import com.jikchin.jikchinbackend.domain.member.entity.Role;
import com.jikchin.jikchinbackend.domain.member.repository.MemberRepository;
import com.jikchin.jikchinbackend.global.security.jwt.JwtTokenProvider;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class MateAuthenticationIntegrationTest {

  private static final String POST_BODY =
      """
      {"eventId":10,"title":"직관 모집","content":"함께 응원해요","maxMembers":3}
      """;

  @Autowired private WebApplicationContext context;
  @Autowired private JwtTokenProvider jwtTokenProvider;
  @Autowired private MemberRepository memberRepository;
  @Autowired private MatePostRepository matePostRepository;
  @Autowired private MateMemberRepository mateMemberRepository;
  @Autowired private MateApplicationRepository mateApplicationRepository;
  @Autowired private MatePostService matePostService;
  @Autowired private MateApplicationService mateApplicationService;

  private MockMvc mvc;
  private Member owner;
  private Member applicant;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    owner = saveMember();
    applicant = saveMember();
  }

  @Test
  void createsPostAsJwtMemberEvenWithAnotherUsersHeader() throws Exception {
    mvc.perform(
            post("/api/mate-posts")
                .header("Authorization", bearer(owner))
                .header("X-User-Id", applicant.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(POST_BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.userId").value(owner.getId()));

    var post =
        matePostRepository.findAll().stream()
            .filter(value -> value.getUserId().equals(owner.getId()))
            .findFirst()
            .orElseThrow();
    assertThat(mateMemberRepository.existsByMatePost_IdAndUserId(post.getId(), owner.getId()))
        .isTrue();
    assertThat(mateMemberRepository.existsByMatePost_IdAndUserId(post.getId(), applicant.getId()))
        .isFalse();
  }

  @Test
  void appliesListsAcceptsAndRejectsUsingJwtWithoutUserIdHeader() throws Exception {
    Long postId = createPost();
    String applications = "/api/mate-posts/" + postId + "/applications";
    mvc.perform(
            post(applications)
                .header("Authorization", bearer(applicant))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"신청합니다\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.userId").value(applicant.getId()));
    Long applicationId =
        mateApplicationRepository
            .findAllByMatePost_IdOrderByCreatedAtAsc(postId)
            .getFirst()
            .getId();

    mvc.perform(get(applications).header("Authorization", bearer(owner)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].userId").value(applicant.getId()));
    mvc.perform(
            patch(applications + "/" + applicationId + "/accept")
                .header("Authorization", bearer(owner)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    assertThat(mateMemberRepository.existsByMatePost_IdAndUserId(postId, applicant.getId()))
        .isTrue();

    Member other = saveMember();
    var pending =
        mateApplicationService.apply(
            postId, other.getMemberKey(), new MateApplicationCreateRequest(null));
    mvc.perform(
            patch(applications + "/" + pending.id() + "/reject")
                .header("Authorization", bearer(owner)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("REJECTED"));
    assertThat(mateMemberRepository.existsByMatePost_IdAndUserId(postId, other.getId())).isFalse();
  }

  @Test
  void cannotImpersonateOwnerWithUserIdHeader() {
    Long postId = createPost();
    var pending =
        mateApplicationService.apply(
            postId, applicant.getMemberKey(), new MateApplicationCreateRequest(null));
    String applications = "/api/mate-posts/" + postId + "/applications";
    var requests =
        java.util.List.of(
            get(applications),
            patch(applications + "/" + pending.id() + "/accept"),
            patch(applications + "/" + pending.id() + "/reject"));
    for (var request : requests) {
      // Existing domain exceptions are not yet mapped by the global exception handler.
      assertThatThrownBy(
              () ->
                  mvc.perform(
                      request
                          .header("Authorization", bearer(applicant))
                          .header("X-User-Id", owner.getId())))
          .hasRootCauseInstanceOf(IllegalStateException.class)
          .hasRootCauseMessage("모집자만 참가 신청을 처리할 수 있습니다.");
    }
    assertThat(mateApplicationRepository.findById(pending.id()).orElseThrow().getStatus())
        .isEqualTo(MateApplicationStatus.PENDING);
    assertThat(mateMemberRepository.existsByMatePost_IdAndUserId(postId, applicant.getId()))
        .isFalse();
  }

  @Test
  void userIdHeaderAloneDoesNotAuthenticate() throws Exception {
    var requests =
        java.util.List.of(
            post("/api/mate-posts"),
            post("/api/mate-posts/1/applications"),
            get("/api/mate-posts/1/applications"),
            patch("/api/mate-posts/1/applications/1/accept"),
            patch("/api/mate-posts/1/applications/1/reject"));
    for (var request : requests) {
      mvc.perform(
              request
                  .header("X-User-Id", owner.getId())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(POST_BODY))
          .andExpect(status().isUnauthorized());
    }
  }

  @Test
  void unknownJwtMemberReturnsMemberNotFound() throws Exception {
    String token = jwtTokenProvider.issue(UUID.randomUUID(), Role.ROLE_USER).accessToken();
    mvc.perform(
            post("/api/mate-posts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(POST_BODY))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.errorCode").value("E1007"));
    mvc.perform(
            post("/api/mate-posts/" + createPost() + "/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.errorCode").value("E1007"));
  }

  private Long createPost() {
    return matePostService
        .create(
            owner.getMemberKey(),
            new MatePostCreateRequest(10L, "직관 모집", "함께 응원해요", 3, null, null, null, null))
        .id();
  }

  private Member saveMember() {
    String unique = UUID.randomUUID().toString();
    return memberRepository.save(
        Member.create(unique + "@test.com", "encoded", unique, null, null, null, null));
  }

  private String bearer(Member member) {
    return "Bearer "
        + jwtTokenProvider.issue(member.getMemberKey(), member.getRole()).accessToken();
  }
}
