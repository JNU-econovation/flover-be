package com.plover.plover_be.crew.controller;

import com.plover.plover_be.crew.dto.CrewDto;
import com.plover.plover_be.crew.service.CrewService;
import com.plover.plover_be.global.auth.LoginUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Crew", description = "크루 API")
@RestController
@RequestMapping("/api/crews")
@RequiredArgsConstructor
public class CrewController {

    private final CrewService crewService;

    @Operation(summary = "크루 생성")
    @PostMapping
    public ResponseEntity<CrewDto.CrewResponse> createCrew(
            @LoginUserId Long userId,
            @RequestBody @Valid CrewDto.CreateRequest request
    ) {
        return ResponseEntity.ok(crewService.createCrew(userId, request.name()));
    }

    @Operation(summary = "참여 코드로 크루 가입")
    @PostMapping("/join")
    public ResponseEntity<CrewDto.CrewResponse> joinCrew(
            @LoginUserId Long userId,
            @RequestBody @Valid CrewDto.JoinRequest request
    ) {
        return ResponseEntity.ok(crewService.joinCrew(userId, request.joinCode()));
    }

    @Operation(summary = "내 크루 목록 조회")
    @GetMapping
    public ResponseEntity<CrewDto.CrewListResponse> getMyCrews(@LoginUserId Long userId) {
        return ResponseEntity.ok(crewService.findMyCrews(userId));
    }

    @Operation(summary = "크루 상세 조회")
    @GetMapping("/{crewId}")
    public ResponseEntity<CrewDto.CrewDetailResponse> getCrew(
            @LoginUserId Long userId,
            @PathVariable Long crewId
    ) {
        return ResponseEntity.ok(crewService.findCrew(userId, crewId));
    }
}
