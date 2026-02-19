package com.kiero.missions.adapter.in.web;

import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.auth.enums.Role;
import com.kiero.global.exception.KieroException;
import com.kiero.global.response.code.ErrorCode;
import com.kiero.global.response.dto.SuccessResponse;
import com.kiero.missions.application.dto.MissionBulkCreateRequest;
import com.kiero.missions.application.dto.MissionCreateRequest;
import com.kiero.missions.application.dto.MissionResponse;
import com.kiero.missions.application.dto.MissionSuggestionRequest;
import com.kiero.missions.application.dto.MissionSuggestionResponse;
import com.kiero.missions.application.dto.MissionsByDateResponse;
import com.kiero.missions.application.exception.MissionSuccessCode;
import com.kiero.missions.application.service.MissionCommandService;
import com.kiero.missions.application.service.MissionQueryService;
import com.kiero.missions.application.service.MissionSuggestionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MissionController {
    private final MissionCommandService missionCommandService;
    private final MissionQueryService missionQueryService;
    private final MissionSuggestionService missionSuggestionService;

    @PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
    @PostMapping("/missions/{childId}")
    public ResponseEntity<SuccessResponse<MissionResponse>> createMission(
            @CurrentMember CurrentAuth currentAuth,
            @PathVariable Long childId,
            @Valid @RequestBody MissionCreateRequest request
    ) {
        MissionResponse response = missionCommandService.createMission(currentAuth.memberId(), childId, request);

        return ResponseEntity.ok()
                .body(SuccessResponse.of(MissionSuccessCode.MISSION_CREATED, response));
    }

    @PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
    @PostMapping("/missions/{childId}/bulk")
    public ResponseEntity<SuccessResponse<List<MissionResponse>>> bulkCreateMissions(
            @CurrentMember CurrentAuth currentAuth,
            @PathVariable Long childId,
            @Valid @RequestBody MissionBulkCreateRequest request
    ) {
        List<MissionResponse> responses = missionCommandService.bulkCreateMissions(currentAuth.memberId(), childId, request);

        return ResponseEntity.ok()
                .body(SuccessResponse.of(MissionSuccessCode.MISSIONS_BULK_CREATED, responses));
    }

    @PreAuthorize("hasAnyRole('CHILD', 'PARENT', 'ADMIN')")
    @GetMapping("/missions")
    public ResponseEntity<SuccessResponse<MissionsByDateResponse>> getMissions(
            @CurrentMember CurrentAuth currentAuth,
            @RequestParam(required = false) Long childId
    ) {
        List<MissionResponse> missions;

        if (currentAuth.role() == Role.PARENT) {
            missions = missionQueryService.getMissionsByParent(currentAuth.memberId(), childId);
        } else if (currentAuth.role() == Role.CHILD) {
            missions = missionQueryService.getMissionsByChild(currentAuth.memberId());
        } else {
            throw new KieroException(ErrorCode.ACCESS_DENIED);
        }

        MissionsByDateResponse response = MissionsByDateResponse.from(missions);

        return ResponseEntity.ok()
                .body(SuccessResponse.of(MissionSuccessCode.MISSIONS_RETRIEVED, response));
    }

    @PreAuthorize("hasAnyRole('CHILD', 'ADMIN')")
    @PatchMapping("/missions/{missionId}/complete")
    public ResponseEntity<SuccessResponse<MissionResponse>> completeMission(
            @CurrentMember CurrentAuth currentAuth,
            @PathVariable Long missionId
    ) {
        MissionResponse response = missionCommandService.completeMission(currentAuth.memberId(), missionId);

        return ResponseEntity.ok()
                .body(SuccessResponse.of(MissionSuccessCode.MISSION_COMPLETED, response));
    }

    @PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
    @PostMapping("/missions/suggestions")
    public ResponseEntity<SuccessResponse<MissionSuggestionResponse>> suggestMissions(
            @CurrentMember CurrentAuth currentAuth,
            @Valid @RequestBody MissionSuggestionRequest request
    ) {
        MissionSuggestionResponse response = missionSuggestionService.suggestMissions(request.noticeText());

        log.info("Mission suggestions generated for parentId={}", currentAuth.memberId());

        return ResponseEntity.ok()
                .body(SuccessResponse.of(MissionSuccessCode.MISSION_SUGGESTIONS_GENERATED, response));
    }
}
