package com.jikchin.jikchinbackend.domain.event.repository;

import com.jikchin.jikchinbackend.domain.event.entity.Sport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SportRepository extends JpaRepository<Sport, Long> {}
