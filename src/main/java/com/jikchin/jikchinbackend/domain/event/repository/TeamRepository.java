package com.jikchin.jikchinbackend.domain.event.repository;

import com.jikchin.jikchinbackend.domain.event.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {}
