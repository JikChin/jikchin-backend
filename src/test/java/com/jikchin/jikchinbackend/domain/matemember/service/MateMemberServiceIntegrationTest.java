package com.jikchin.jikchinbackend.domain.matemember.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jikchin.jikchinbackend.domain.matemember.dto.response.MateMemberResponse;
import com.jikchin.jikchinbackend.domain.matemember.entity.MateMemberStatus;
import com.jikchin.jikchinbackend.domain.matemember.repository.MateMemberRepository;
import com.jikchin.jikchinbackend.domain.matepost.dto.request.MatePostCreateRequest;
import com.jikchin.jikchinbackend.domain.matepost.dto.response.MatePostResponse;
import com.jikchin.jikchinbackend.domain.matepost.entity.MatePost;
import com.jikchin.jikchinbackend.domain.matepost.entity.MatePostStatus;
import com.jikchin.jikchinbackend.domain.matepost.repository.MatePostRepository;
import com.jikchin.jikchinbackend.domain.matepost.service.MatePostService;
import com.jikchin.jikchinbackend.domain.member.entity.Member;
import com.jikchin.jikchinbackend.domain.member.repository.MemberRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MateMemberServiceIntegrationTest {
  @Autowired private MemberRepository memberRepository;
  private Member owner;
  private Member applicant;
  private Member other;

  @Autowired private MateMemberService mateMemberService;

  @Autowired private MatePostService matePostService;

  @Autowired private MateMemberRepository mateMemberRepository;

  @Autowired private MatePostRepository matePostRepository;

  @BeforeEach
  void setUp() {
    mateMemberRepository.deleteAll();
    matePostRepository.deleteAll();
    owner = saveMember();
    applicant = saveMember();
    other = saveMember();
  }

  @Test
  void addsUserToMatePostAndReturnsActiveMembers() {
    MatePostResponse post = createPost(3);

    MateMemberResponse joined = mateMemberService.addMember(post.id(), applicant.getId());
    List<MateMemberResponse> members = mateMemberService.getActiveMembers(post.id());

    MatePost savedPost = matePostRepository.findById(post.id()).orElseThrow();
    assertThat(joined.matePostId()).isEqualTo(post.id());
    assertThat(joined.userId()).isEqualTo(applicant.getId());
    assertThat(joined.status()).isEqualTo(MateMemberStatus.ACTIVE);
    assertThat(members)
        .extracting(MateMemberResponse::userId)
        .containsExactly(owner.getId(), applicant.getId());
    assertThat(savedPost.getCurrentMembers()).isEqualTo(2);
  }

  @Test
  void checksWhetherUserIsParticipating() {
    MatePostResponse post = createPost(3);

    assertThat(mateMemberService.isParticipating(post.id(), owner.getId())).isTrue();
    assertThat(mateMemberService.isParticipating(post.id(), other.getId())).isFalse();
  }

  @Test
  void rejectsDuplicateParticipationWithoutIncreasingMemberCount() {
    MatePostResponse post = createPost(3);
    mateMemberService.addMember(post.id(), applicant.getId());

    assertThatThrownBy(() -> mateMemberService.addMember(post.id(), applicant.getId()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("이미 참가 중이거나 참가 이력이 있는 사용자입니다.");

    assertThat(matePostRepository.findById(post.id()).orElseThrow().getCurrentMembers())
        .isEqualTo(2);
    assertThat(mateMemberRepository.findAll()).hasSize(2);
  }

  @Test
  void closesMatePostWhenLastMemberJoins() {
    MatePostResponse post = createPost(2);

    mateMemberService.addMember(post.id(), applicant.getId());

    MatePost savedPost = matePostRepository.findById(post.id()).orElseThrow();
    assertThat(savedPost.getCurrentMembers()).isEqualTo(2);
    assertThat(savedPost.getStatus()).isEqualTo(MatePostStatus.CLOSED);
    assertThatThrownBy(() -> mateMemberService.addMember(post.id(), other.getId()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("모집 정원이 마감되었습니다.");
  }

  @Test
  void concurrentJoinsForLastSeatNeverExceedCapacity() throws Exception {
    MatePostResponse post = createPost(2);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    try {
      Future<Boolean> first = submitJoin(executor, ready, start, post.id(), applicant.getId());
      Future<Boolean> second = submitJoin(executor, ready, start, post.id(), other.getId());
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();

      assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(true, false);

      MatePost savedPost = matePostRepository.findById(post.id()).orElseThrow();
      assertThat(savedPost.getCurrentMembers()).isEqualTo(2);
      assertThat(savedPost.getStatus()).isEqualTo(MatePostStatus.CLOSED);
      assertThat(mateMemberService.getActiveMembers(post.id())).hasSize(2);
    } finally {
      executor.shutdownNow();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  private Future<Boolean> submitJoin(
      ExecutorService executor,
      CountDownLatch ready,
      CountDownLatch start,
      Long matePostId,
      Long userId) {
    return executor.submit(
        () -> {
          ready.countDown();
          if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("동시 참가 시작 신호를 받지 못했습니다.");
          }
          try {
            mateMemberService.addMember(matePostId, userId);
            return true;
          } catch (IllegalStateException exception) {
            return false;
          }
        });
  }

  private Member saveMember() {
    String unique = UUID.randomUUID().toString();
    return memberRepository.save(
        Member.create(unique + "@test.com", "encoded", unique, null, null, null, null));
  }

  private MatePostResponse createPost(int maxMembers) {
    return matePostService.create(
        owner.getMemberKey(),
        new MatePostCreateRequest(
            10L, "잠실 경기 같이 봐요", "즐겁게 응원할 분을 모집합니다.", maxMembers, "ANY", 20, 40, "1루 네이비석"));
  }
}
