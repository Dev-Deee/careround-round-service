package com.dayo.careround.roundservice.data.models;

import com.dayo.careround.roundservice.data.models.enums.RoundStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "rounds")
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(nullable = false)
    private UUID wardId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoundStatus status;

    // Who/when columns; null on rounds created before they existed.
    @Column(updatable = false)
    private UUID createdBy;

    // The doctor running the round: the creator if a doctor, otherwise the first doctor to resolve a review.
    @Column
    private UUID conductedBy;

    @Column(updatable = false)
    private Instant createdAt = Instant.now();

    @Column
    private Instant startedAt;

    @Column
    private Instant endedAt;
}