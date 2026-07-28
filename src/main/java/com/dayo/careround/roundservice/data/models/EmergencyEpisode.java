package com.dayo.careround.roundservice.data.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "emergency_episodes")
public class EmergencyEpisode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID patientId;

    @Column(nullable = false)
    private UUID declaredBy;

    @Column(nullable = false, updatable = false)
    private Instant declaredAt = Instant.now();

    @Column
    private Instant resolvedAt;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;
}