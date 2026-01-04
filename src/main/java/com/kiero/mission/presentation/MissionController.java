package com.kiero.mission.presentation;

import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.auth.enums.Role;
import com.kiero.global.exception.KieroException;
import com.kiero.global.response.code.ErrorCode;
import com.kiero.global.response.dto.SuccessResponse;
import com.kiero.mission.exception.MissionSuccessCode;
import com.kiero.mission.presentation.dto.MissionBulkCreateRequest;
import com.kiero.mission.presentation.dto.MissionCreateRequest;
import com.kiero.mission.presentation.dto.MissionResponse;
import com.kiero.mission.presentation.dto.MissionSuggestionRequest;
import com.kiero.mission.presentation.dto.MissionSuggestionResponse;
import com.kiero.mission.service.MissionService;
import com.kiero.mission.service.MissionSuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/missions")
public class MissionController {
    private final MissionService missionService;
    private final MissionSuggestionService missionSuggestionService;

    @PostMapping
    public ResponseEntity<SuccessResponse<MissionResponse>> createMission(
            @CurrentMember CurrentAuth currentAuth,
            @Valid @RequestBody MissionCreateRequest request
    ) {
        MissionResponse response = missionService.createMission(currentAuth.memberId(), request);

        return ResponseEntity.ok()
                .body(SuccessResponse.of(MissionSuccessCode.MISSION_CREATED, response));
    }

    @GetMapping
    public ResponseEntity<SuccessResponse<List<MissionResponse>>> getMissions(
            @CurrentMember CurrentAuth currentAuth,
            @RequestParam(required = false) Long childId
    ) {
        List<MissionResponse> responses;

        if (currentAuth.role() == Role.PARENT) {
            responses = missionService.getMissionsByParent(currentAuth.memberId(), childId);
        } else if (currentAuth.role() == Role.CHILD) {
            responses = missionService.getMissionsByChild(currentAuth.memberId());
        } else {
            throw new KieroException(ErrorCode.ACCESS_DENIED);
        }

        return ResponseEntity.ok()
                .body(SuccessResponse.of(MissionSuccessCode.MISSIONS_RETRIEVED, responses));
    }

    @PatchMapping("/{missionId}/complete")
    public ResponseEntity<SuccessResponse<MissionResponse>> completeMission(
            @CurrentMember CurrentAuth currentAuth,
            @PathVariable Long missionId
    ) {
        if (currentAuth.role() != Role.CHILD) {
            throw new KieroException(ErrorCode.ACCESS_DENIED);
        }

        MissionResponse response = missionService.completeMission(currentAuth.memberId(), missionId);

        return ResponseEntity.ok()
                .body(SuccessResponse.of(MissionSuccessCode.MISSION_COMPLETED, response));
    }

    @PostMapping("/suggestions")
    public ResponseEntity<SuccessResponse<MissionSuggestionResponse>> suggestMissions(
            @CurrentMember CurrentAuth currentAuth,
            @Valid @RequestBody MissionSuggestionRequest request
    ) {
        if (currentAuth.role() != Role.PARENT) {
            throw new KieroException(ErrorCode.ACCESS_DENIED);
        }

        MissionSuggestionResponse response = missionSuggestionService.suggestMissions(request.noticeText());

        log.info("Mission suggestions generated for parentId={}", currentAuth.memberId());

        return ResponseEntity.ok()
                .body(SuccessResponse.of(MissionSuccessCode.MISSION_SUGGESTIONS_GENERATED, response));
    }

    @PostMapping("/bulk")
    public ResponseEntity<SuccessResponse<List<MissionResponse>>> bulkCreateMissions(
            @CurrentMember CurrentAuth currentAuth,
            @Valid @RequestBody MissionBulkCreateRequest request
    ) {
        if (currentAuth.role() != Role.PARENT) {
            throw new KieroException(ErrorCode.ACCESS_DENIED);
        }

        List<MissionResponse> responses = missionService.bulkCreateMissions(currentAuth.memberId(), request);

        return ResponseEntity.ok()
                .body(SuccessResponse.of(MissionSuccessCode.MISSIONS_BULK_CREATED, responses));
    }
}
