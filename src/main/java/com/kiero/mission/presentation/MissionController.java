package com.kiero.mission.presentation;

import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.auth.enums.Role;
import com.kiero.global.exception.KieroException;
import com.kiero.global.response.code.ErrorCode;
import com.kiero.global.response.dto.SuccessResponse;
import com.kiero.mission.exception.MissionSuccessCode;
import com.kiero.mission.presentation.dto.*;
import com.kiero.mission.service.MissionService;
import com.kiero.mission.service.MissionSuggestionService;
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
    private final MissionService missionService;
    private final MissionSuggestionService missionSuggestionService;

    @PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
    @PostMapping("/missions/{childId}")
    public ResponseEntity<SuccessResponse<MissionResponse>> createMission(
            @CurrentMember CurrentAuth currentAuth,
            @PathVariable Long childId,
            @Valid @RequestBody MissionCreateRequest request
    ) {
        MissionResponse response = missionService.createMission(currentAuth.memberId(), childId, request);

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
        List<MissionResponse> responses = missionService.bulkCreateMissions(currentAuth.memberId(), childId, request);

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
            missions = missionService.getMissionsByParent(currentAuth.memberId(), childId);
        } else if (currentAuth.role() == Role.CHILD) {
            missions = missionService.getMissionsByChild(currentAuth.memberId());
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
        MissionResponse response = missionService.completeMission(currentAuth.memberId(), missionId);

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
