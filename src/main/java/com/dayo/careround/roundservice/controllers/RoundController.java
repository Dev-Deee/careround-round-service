package com.dayo.careround.roundservice.controllers;

import com.dayo.careround.roundservice.dto.request.CreateRoundRequest;
import com.dayo.careround.roundservice.dto.response.RoundResponse;
import com.dayo.careround.roundservice.services.RoundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rounds")
@RequiredArgsConstructor
public class RoundController {

    private final RoundService roundService;

    @PostMapping
    public ResponseEntity<RoundResponse> create(@Valid @RequestBody CreateRoundRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roundService.create(request));
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<RoundResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(roundService.getByPublicId(publicId));
    }
}