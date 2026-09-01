package com.jikchin.jikchinbackend.domain.matepost.repository;

import com.jikchin.jikchinbackend.domain.matepost.entity.MatePost;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatePostRepository extends JpaRepository<MatePost, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select post from MatePost post where post.id = :postId")
    Optional<MatePost> findByIdForUpdate(@Param("postId") Long postId);
}
