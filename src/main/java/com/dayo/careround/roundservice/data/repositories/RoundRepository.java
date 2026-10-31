package com.dayo.careround.roundservice.data.repositories;

import com.dayo.careround.roundservice.data.models.Round;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface RoundRepository extends JpaRepository<Round, UUID>, JpaSpecificationExecutor<Round> {
    Optional<Round> findByPublicId(UUID publicId);
}