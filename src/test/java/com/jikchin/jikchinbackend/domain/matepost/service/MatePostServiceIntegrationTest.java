package com.jikchin.jikchinbackend.domain.matepost.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jikchin.jikchinbackend.domain.matemember.entity.MateMemberStatus;
import com.jikchin.jikchinbackend.domain.matemember.repository.MateMemberRepository;
import com.jikchin.jikchinbackend.domain.matepost.dto.request.MatePostCreateRequest;
import com.jikchin.jikchinbackend.domain.matepost.dto.response.MatePostResponse;
import com.jikchin.jikchinbackend.domain.matepost.entity.MatePost;
import com.jikchin.jikchinbackend.domain.matepost.entity.MatePostStatus;
import com.jikchin.jikchinbackend.domain.matepost.repository.MatePostRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MatePostServiceIntegrationTest {

  @Autowired private MatePostService matePostService;

  @Autowired private MatePostRepository matePostRepository;

  @Autowired private MateMemberRepository mateMemberRepository;

  @BeforeEach
  void setUp() {
    mateMemberRepository.deleteAll();
    matePostRepository.deleteAll();
  }

  @Test
  void createsMatePostAndStartsWithOwnerAsFirstMember() {
    MatePostResponse response = matePostService.create(1L, createRequest(3));

    assertThat(response.id()).isNotNull();
    assertThat(response.currentMembers()).isEqualTo(1);
    assertThat(response.status()).isEqualTo(MatePostStatus.OPEN);
    assertThat(matePostRepository.findById(response.id())).isPresent();
    assertThat(
            mateMemberRepository.existsByMatePost_IdAndUserIdAndStatus(
                response.id(), 1L, MateMemberStatus.ACTIVE))
        .isTrue();
  }

  @Test
  void getsMatePostById() {
    MatePostResponse created = matePostService.create(1L, createRequest(3));

    MatePostResponse found = matePostService.getById(created.id());

    assertThat(found).isEqualTo(created);
  }

  @Test
  void throwsWhenMatePostDoesNotExist() {
    assertThatThrownBy(() -> matePostService.getById(999L))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("모집글을 찾을 수 없습니다.");
  }

  @Test
  void closesMatePostWhenLastMemberIsAdded() {
    MatePost matePost =
        MatePost.create(1L, 10L, "잠실 경기 같이 봐요", "함께 응원해요", 2, "ANY", 20, 40, "1루 네이비석");

    matePost.addMember();

    assertThat(matePost.getCurrentMembers()).isEqualTo(2);
    assertThat(matePost.getStatus()).isEqualTo(MatePostStatus.CLOSED);
    assertThatThrownBy(matePost::addMember)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("모집 정원이 마감되었습니다.");
  }

  private MatePostCreateRequest createRequest(int maxMembers) {
    return new MatePostCreateRequest(
        10L, "잠실 경기 같이 봐요", "즐겁게 응원할 분을 모집합니다.", maxMembers, "ANY", 20, 40, "1루 네이비석");
  }
}
