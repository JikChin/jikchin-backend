package com.jikchin.jikchinbackend.domain.mateapplication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.jikchin.jikchinbackend.domain.mateapplication.dto.request.MateApplicationCreateRequest;
import com.jikchin.jikchinbackend.domain.mateapplication.dto.response.MateApplicationResponse;
import com.jikchin.jikchinbackend.domain.mateapplication.entity.MateApplication;
import com.jikchin.jikchinbackend.domain.mateapplication.entity.MateApplicationStatus;
import com.jikchin.jikchinbackend.domain.mateapplication.repository.MateApplicationRepository;
import com.jikchin.jikchinbackend.domain.matemember.entity.MateMember;
import com.jikchin.jikchinbackend.domain.matemember.entity.MateMemberStatus;
import com.jikchin.jikchinbackend.domain.matemember.repository.MateMemberRepository;
import com.jikchin.jikchinbackend.domain.matepost.dto.request.MatePostCreateRequest;
import com.jikchin.jikchinbackend.domain.matepost.entity.MatePostStatus;
import com.jikchin.jikchinbackend.domain.matepost.repository.MatePostRepository;
import com.jikchin.jikchinbackend.domain.matepost.service.MatePostService;
import com.jikchin.jikchinbackend.domain.member.entity.Member;
import com.jikchin.jikchinbackend.domain.member.repository.MemberRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

/** Service-level concurrency checks; mysqlConcurrencyTest uses the isolated Docker MySQL. */
@SpringBootTest(
    properties = {
      "spring.datasource.url=${mate.test.url:jdbc:h2:mem:mate-concurrency;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000}",
      "spring.datasource.driver-class-name=${mate.test.driver:org.h2.Driver}",
      "spring.datasource.username=${mate.test.username:sa}",
      "spring.datasource.password=${mate.test.password:}",
      "spring.datasource.hikari.maximum-pool-size=20",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MateApplicationConcurrencyTest {

  private static final int CONCURRENCY = 20;
  private static final String DUPLICATE = "이미 신청한 모집글입니다.";
  private static final String FULL = "모집 정원이 마감되었습니다.";
  private static final String PROCESSED = "이미 처리된 참가 신청입니다.";

  @Autowired private MateApplicationService applicationService;
  @Autowired private MatePostService postService;
  @Autowired private MateApplicationRepository applications;
  @Autowired private MateMemberRepository participants;
  @Autowired private MatePostRepository posts;
  @Autowired private MemberRepository members;

  private Member owner;
  private List<Member> applicants;

  @BeforeEach
  void setUp() {
    owner = saveMember();
    applicants = IntStream.range(0, CONCURRENCY).mapToObj(i -> saveMember()).toList();
  }

  @AfterEach
  void cleanUp() {
    applications.deleteAll();
    participants.deleteAll();
    posts.deleteAll();
    members.deleteAll();
  }

  @RepeatedTest(3)
  void simultaneousDuplicateApplicationsCreateOnlyOneRecord() throws Exception {
    Long postId = createPost(3);
    var actions =
        IntStream.range(0, CONCURRENCY)
            .<Callable<?>>mapToObj(i -> () -> apply(postId, applicants.getFirst()))
            .toList();

    assertOutcomes(postId, runTogether(actions), 1, DUPLICATE);
    assertThat(applications.findAllByMatePost_IdOrderByCreatedAtAsc(postId))
        .singleElement()
        .satisfies(
            application -> {
              assertThat(application.getUserId()).isEqualTo(applicants.getFirst().getId());
              assertThat(application.getStatus()).isEqualTo(MateApplicationStatus.PENDING);
            });
    assertConsistency(postId, 0);
  }

  @RepeatedTest(3)
  void distinctConcurrentApplicationsDoNotReserveSeats() throws Exception {
    Long postId = createPost(2);
    var actions =
        applicants.stream().<Callable<?>>map(member -> () -> apply(postId, member)).toList();

    assertOutcomes(postId, runTogether(actions), CONCURRENCY, null);
    assertThat(applications.findAllByMatePost_IdOrderByCreatedAtAsc(postId))
        .hasSize(CONCURRENCY)
        .allSatisfy(
            application ->
                assertThat(application.getStatus()).isEqualTo(MateApplicationStatus.PENDING));
    assertConsistency(postId, 0);
  }

  @RepeatedTest(3)
  void twentyAcceptsForLastSeatProduceOneWinner() throws Exception {
    Long postId = createPost(2);
    var pending = applicants.stream().map(member -> apply(postId, member)).toList();
    var actions =
        pending.stream()
            .<Callable<?>>map(application -> () -> accept(postId, application.id()))
            .toList();

    assertOutcomes(postId, runTogether(actions), 1, FULL);
    assertThat(
            applications.findAllByMatePost_IdOrderByCreatedAtAsc(postId).stream()
                .filter(MateApplication::isPending))
        .hasSize(CONCURRENCY - 1);
    assertConsistency(postId, 1);
  }

  @RepeatedTest(3)
  void concurrentAcceptsWithEnoughSeatsDoNotLoseMemberUpdates() throws Exception {
    Long postId = createPost(CONCURRENCY + 1);
    var pending = applicants.stream().map(member -> apply(postId, member)).toList();
    var actions =
        pending.stream()
            .<Callable<?>>map(application -> () -> accept(postId, application.id()))
            .toList();

    assertOutcomes(postId, runTogether(actions), CONCURRENCY, null);
    assertConsistency(postId, CONCURRENCY);
  }

  @RepeatedTest(3)
  void repeatedAcceptOfSameApplicationCreatesOnlyOneMember() throws Exception {
    Long postId = createPost(3);
    var pending = apply(postId, applicants.getFirst());
    var actions =
        IntStream.range(0, CONCURRENCY)
            .<Callable<?>>mapToObj(i -> () -> accept(postId, pending.id()))
            .toList();

    assertOutcomes(postId, runTogether(actions), 1, PROCESSED);
    assertConsistency(postId, 1);
  }

  @RepeatedTest(3)
  void racingAcceptAndRejectLeaveOneConsistentDecision() throws Exception {
    Long postId = createPost(3);
    var pending = apply(postId, applicants.getFirst());
    var actions =
        IntStream.range(0, CONCURRENCY)
            .<Callable<?>>mapToObj(
                i ->
                    () ->
                        i % 2 == 0
                            ? accept(postId, pending.id())
                            : applicationService.reject(postId, pending.id(), owner.getMemberKey()))
            .toList();

    assertOutcomes(postId, runTogether(actions), 1, PROCESSED);
    var status = applications.findById(pending.id()).orElseThrow().getStatus();
    assertThat(status).isIn(MateApplicationStatus.ACCEPTED, MateApplicationStatus.REJECTED);
    assertConsistency(postId, status == MateApplicationStatus.ACCEPTED ? 1 : 0);
  }

  private void assertConsistency(Long postId, int expectedAccepted) {
    var post = posts.findById(postId).orElseThrow();
    var active =
        participants.findAllByMatePost_IdAndStatusOrderByJoinedAtAsc(
            postId, MateMemberStatus.ACTIVE);
    var records = applications.findAllByMatePost_IdOrderByCreatedAtAsc(postId);
    var acceptedIds =
        records.stream()
            .filter(application -> application.getStatus() == MateApplicationStatus.ACCEPTED)
            .map(MateApplication::getUserId)
            .toList();

    assertThat(records).extracting(MateApplication::getUserId).doesNotHaveDuplicates();
    assertThat(acceptedIds).hasSize(expectedAccepted);
    assertThat(active)
        .extracting(MateMember::getUserId)
        .doesNotHaveDuplicates()
        .contains(owner.getId());
    assertThat(active.stream().map(MateMember::getUserId).filter(id -> !id.equals(owner.getId())))
        .containsExactlyInAnyOrderElementsOf(acceptedIds);
    assertThat(post.getCurrentMembers())
        .isEqualTo(active.size())
        .isEqualTo(expectedAccepted + 1)
        .isLessThanOrEqualTo(post.getMaxMembers());
    assertThat(post.getStatus())
        .isEqualTo(
            post.getCurrentMembers() == post.getMaxMembers()
                ? MatePostStatus.CLOSED
                : MatePostStatus.OPEN);
  }

  private List<Outcome> runTogether(List<Callable<?>> actions) throws Exception {
    var executor = Executors.newFixedThreadPool(CONCURRENCY);
    var ready = new CountDownLatch(actions.size());
    var start = new CountDownLatch(1);
    List<Future<Outcome>> futures = new ArrayList<>();
    try {
      for (var action : actions) {
        futures.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("동시 요청 시작 시간 초과");
                  }
                  try {
                    action.call();
                    return new Outcome(true, null);
                  } catch (IllegalStateException exception) {
                    return new Outcome(false, exception.getMessage());
                  } catch (Exception exception) {
                    return new Outcome(
                        false,
                        exception.getClass().getSimpleName() + ": " + exception.getMessage());
                  }
                }));
      }
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      List<Outcome> outcomes = new ArrayList<>();
      for (var future : futures) {
        outcomes.add(future.get(30, TimeUnit.SECONDS));
      }
      return outcomes;
    } finally {
      start.countDown();
      executor.shutdownNow();
      assertThat(executor.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
    }
  }

  private void assertOutcomes(
      Long postId, List<Outcome> outcomes, int successes, String expectedRejection) {
    // Check committed state even when an unexpected DB exception was observed.
    int accepted =
        (int)
            applications.findAllByMatePost_IdOrderByCreatedAtAsc(postId).stream()
                .filter(application -> application.getStatus() == MateApplicationStatus.ACCEPTED)
                .count();
    System.out.printf(
        "post=%d success=%d expectedSuccess=%d accepted=%d%n",
        postId, outcomes.stream().filter(Outcome::success).count(), successes, accepted);
    assertAll(
        () -> assertConsistency(postId, accepted),
        () -> {
          assertThat(outcomes).hasSize(CONCURRENCY);
          assertThat(outcomes.stream().filter(Outcome::success)).hasSize(successes);
          assertThat(outcomes.stream().filter(outcome -> !outcome.success()))
              .allSatisfy(outcome -> assertThat(outcome.reason()).isEqualTo(expectedRejection));
        });
  }

  private MateApplicationResponse apply(Long postId, Member member) {
    return applicationService.apply(
        postId, member.getMemberKey(), new MateApplicationCreateRequest("동시성 검증"));
  }

  private MateApplicationResponse accept(Long postId, Long applicationId) {
    return applicationService.accept(postId, applicationId, owner.getMemberKey());
  }

  private Long createPost(int capacity) {
    return postService
        .create(
            owner.getMemberKey(),
            new MatePostCreateRequest(10L, "동시성 검증", "테스트", capacity, null, null, null, null))
        .id();
  }

  private Member saveMember() {
    String unique = UUID.randomUUID().toString();
    return members.save(
        Member.create(unique + "@test.com", "encoded", unique, null, null, null, null));
  }

  private record Outcome(boolean success, String reason) {}
}
