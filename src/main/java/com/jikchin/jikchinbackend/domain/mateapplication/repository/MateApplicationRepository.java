package com.jikchin.jikchinbackend.domain.mateapplication.repository;

import com.jikchin.jikchinbackend.domain.mateapplication.entity.MateApplication;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MateApplicationRepository extends JpaRepository<MateApplication, Long> {

  boolean existsByMatePost_IdAndUserId(Long matePostId, Long userId);

  Optional<MateApplication> findByIdAndMatePost_Id(Long applicationId, Long matePostId);

  List<MateApplication> findAllByMatePost_IdOrderByCreatedAtAsc(Long matePostId);
}
