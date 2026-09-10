package com.jikchin.jikchinbackend.domain.event.repository;

import com.jikchin.jikchinbackend.domain.event.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue, Long> {}
