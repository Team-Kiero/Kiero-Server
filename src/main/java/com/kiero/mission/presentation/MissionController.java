package com.kiero.mission.presentation;

import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.response.dto.SuccessResponse;
import com.kiero.mission.presentation.dto.MissionCreateRequest;
import com.kiero.mission.presentation.dto.MissionResponse;
import com.kiero.mission.service.MissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/missions")
public class MissionController {
    private final MissionService missionService;

    @PostMapping
    public ResponseEntity<SuccessResponse<MissionResponse>> create(
            @CurrentMember CurrentAuth currentAuth,
            @RequestBody MissionCreateRequest request
    ) {
        return null;
    }
}
