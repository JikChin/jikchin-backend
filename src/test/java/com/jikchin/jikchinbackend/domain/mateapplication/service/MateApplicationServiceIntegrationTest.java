package com.jikchin.jikchinbackend.domain.mateapplication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jikchin.jikchinbackend.domain.mateapplication.dto.request.MateApplicationCreateRequest;
import com.jikchin.jikchinbackend.domain.mateapplication.dto.response.MateApplicationResponse;
import com.jikchin.jikchinbackend.domain.mateapplication.entity.MateApplicationStatus;
import com.jikchin.jikchinbackend.domain.mateapplication.repository.MateApplicationRepository;
import com.jikchin.jikchinbackend.domain.matemember.repository.MateMemberRepository;
import com.jikchin.jikchinbackend.domain.matepost.dto.request.MatePostCreateRequest;
import com.jikchin.jikchinbackend.domain.matepost.dto.response.MatePostResponse;
import com.jikchin.jikchinbackend.domain.matepost.entity.MatePost;
import com.jikchin.jikchinbackend.domain.matepost.entity.MatePostStatus;
import com.jikchin.jikchinbackend.domain.matepost.repository.MatePostRepository;
import com.jikchin.jikchinbackend.domain.matepost.service.MatePostService;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MateApplicationServiceIntegrationTest {

  private static final Long OWNER_ID = 1L;

  @Autowired private MateApplicationService mateApplicationService;

  @Autowired private MatePostService matePostService;

  @Autowired private MateApplicationRepository mateApplicationRepository;

  @Autowired private MateMemberRepository mateMemberRepository;

  @Autowired private MatePostRepository matePostRepository;

  @BeforeEach
  void setUp() {
    cleanDatabase();
  }

  @AfterEach
  void tearDown() {
    cleanDatabase();
  }

  @Test
  void createsPendingApplication() {
    MatePostResponse post = createPost(3);

    MateApplicationResponse application = apply(post.id(), 2L);

    assertThat(application.matePostId()).isEqualTo(post.id());
    assertThat(application.userId()).isEqualTo(2L);
    assertThat(application.status()).isEqualTo(MateApplicationStatus.PENDING);
    assertThat(application.message()).isEqualTo("같이 응원하고 싶어요");
  }

  @Test
  void rejectsOwnerAndDuplicateApplication() {
    MatePostResponse post = createPost(3);

    assertThatThrownBy(() -> apply(post.id(), OWNER_ID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("모집자는 자신의 모집글에 신청할 수 없습니다.");

    apply(post.id(), 2L);
    assertThatThrownBy(() -> apply(post.id(), 2L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("이미 신청한 모집글입니다.");
  }

  @Test
  void ownerCanAcceptApplicationAndCreateMateMember() {
    MatePostResponse post = createPost(2);
    MateApplicationResponse application = apply(post.id(), 2L);

    MateApplicationResponse accepted =
        mateApplicationService.accept(post.id(), application.id(), OWNER_ID);

    MatePost savedPost = matePostRepository.findById(post.id()).orElseThrow();
    assertThat(accepted.status()).isEqualTo(MateApplicationStatus.ACCEPTED);
    assertThat(mateMemberRepository.existsByMatePost_IdAndUserId(post.id(), 2L)).isTrue();
    assertThat(savedPost.getCurrentMembers()).isEqualTo(2);
    assertThat(savedPost.getStatus()).isEqualTo(MatePostStatus.CLOSED);
  }

  @Test
  void nonOwnerCannotAcceptApplication() {
    MatePostResponse post = createPost(3);
    MateApplicationResponse application = apply(post.id(), 2L);

    assertThatThrownBy(() -> mateApplicationService.accept(post.id(), application.id(), 99L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("모집자만 참가 신청을 처리할 수 있습니다.");

    assertThat(mateApplicationRepository.findById(application.id()).orElseThrow().getStatus())
        .isEqualTo(MateApplicationStatus.PENDING);
    assertThat(mateMemberRepository.existsByMatePost_IdAndUserId(post.id(), 2L)).isFalse();
  }

  @Test
  void rejectingApplicationDoesNotCreateMateMember() {
    MatePostResponse post = createPost(3);
    MateApplicationResponse application = apply(post.id(), 2L);

    MateApplicationResponse rejected =
        mateApplicationService.reject(post.id(), application.id(), OWNER_ID);

    assertThat(rejected.status()).isEqualTo(MateApplicationStatus.REJECTED);
    assertThat(mateMemberRepository.existsByMatePost_IdAndUserId(post.id(), 2L)).isFalse();
    assertThat(matePostRepository.findById(post.id()).orElseThrow().getCurrentMembers())
        .isEqualTo(1);
  }

  @Test
  void ownerCanGetApplicationsInCreatedOrder() {
    MatePostResponse post = createPost(4);
    apply(post.id(), 2L);
    apply(post.id(), 3L);

    List<MateApplicationResponse> applications =
        mateApplicationService.getApplications(post.id(), OWNER_ID);

    assertThat(applications).extracting(MateApplicationResponse::userId).containsExactly(2L, 3L);
  }

  @Test
  void concurrentAcceptsForLastSeatNeverExceedCapacity() throws Exception {
    MatePostResponse post = createPost(2);
    MateApplicationResponse firstApplication = apply(post.id(), 2L);
    MateApplicationResponse secondApplication = apply(post.id(), 3L);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    try {
      Future<Boolean> first =
          submitAccept(executor, ready, start, post.id(), firstApplication.id());
      Future<Boolean> second =
          submitAccept(executor, ready, start, post.id(), secondApplication.id());
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();

      assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(true, false);

      MatePost savedPost = matePostRepository.findById(post.id()).orElseThrow();
      assertThat(savedPost.getCurrentMembers()).isEqualTo(2);
      assertThat(savedPost.getStatus()).isEqualTo(MatePostStatus.CLOSED);
      assertThat(mateMemberRepository.findAll()).hasSize(2);
      assertThat(
              mateApplicationRepository.findAll().stream()
                  .filter(application -> application.getStatus() == MateApplicationStatus.ACCEPTED))
          .hasSize(1);
    } finally {
      executor.shutdownNow();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  private Future<Boolean> submitAccept(
      ExecutorService executor,
      CountDownLatch ready,
      CountDownLatch start,
      Long matePostId,
      Long applicationId) {
    return executor.submit(
        () -> {
          ready.countDown();
          if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("동시 승인 시작 신호를 받지 못했습니다.");
          }
          try {
            mateApplicationService.accept(matePostId, applicationId, OWNER_ID);
            return true;
          } catch (IllegalStateException exception) {
            return false;
          }
        });
  }

  private MateApplicationResponse apply(Long matePostId, Long userId) {
    return mateApplicationService.apply(
        matePostId, userId, new MateApplicationCreateRequest("같이 응원하고 싶어요"));
  }

  private MatePostResponse createPost(int maxMembers) {
    return matePostService.create(
        OWNER_ID,
        new MatePostCreateRequest(
            10L, "잠실 경기 같이 봐요", "즐겁게 응원할 분을 모집합니다.", maxMembers, "ANY", 20, 40, "1루 네이비석"));
  }

  private void cleanDatabase() {
    mateApplicationRepository.deleteAll();
    mateMemberRepository.deleteAll();
    matePostRepository.deleteAll();
  }
}
