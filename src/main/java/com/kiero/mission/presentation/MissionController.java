package com.kiero.mission.presentation;

import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.auth.enums.Role;
import com.kiero.global.exception.KieroException;
import com.kiero.global.response.code.ErrorCode;
import com.kiero.global.response.dto.SuccessResponse;
import com.kiero.mission.exception.MissionSuccessCode;
import com.kiero.mission.presentation.dto.MissionCreateRequest;
import com.kiero.mission.presentation.dto.MissionResponse;
import com.kiero.mission.service.MissionService;
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
}
