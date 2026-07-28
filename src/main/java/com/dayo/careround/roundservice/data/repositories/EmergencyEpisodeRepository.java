package com.dayo.careround.roundservice.data.repositories;

import com.dayo.careround.roundservice.data.models.EmergencyEpisode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmergencyEpisodeRepository extends JpaRepository<EmergencyEpisode, UUID> {
    Optional<EmergencyEpisode> findByPatientIdAndResolvedAtIsNull(UUID patientId);
    List<EmergencyEpisode> findByPatientId(UUID patientId);
}