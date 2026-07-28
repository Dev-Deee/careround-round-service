package com.dayo.careround.roundservice.data.models;

import com.dayo.careround.roundservice.data.models.enums.RoundStatus;
import jakarta.persistence.*;
import lombok.Data;

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
}