package com.dayo.careround.roundservice.data.repositories;

import com.dayo.careround.roundservice.data.models.Round;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoundRepository extends JpaRepository<Round, UUID> {
    Optional<Round> findByPublicId(UUID publicId);
}