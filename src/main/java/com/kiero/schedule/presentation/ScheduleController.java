package com.kiero.schedule.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.response.dto.SuccessResponse;
import com.kiero.schedule.exception.ScheduleSuccessCode;
import com.kiero.schedule.presentation.dto.ScheduleAddRequest;
import com.kiero.schedule.service.ScheduleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController {

	private final ScheduleService scheduleService;

	@PostMapping("/{childId}")
	public ResponseEntity<SuccessResponse<Void>> addSchedule(
		@Valid @RequestBody ScheduleAddRequest request,
		@PathVariable Long childId,
		@CurrentMember CurrentAuth currentAuth
	) {
		scheduleService.addSchedule(request, currentAuth.memberId(), childId);
		return ResponseEntity.ok()
			.body(SuccessResponse.of(ScheduleSuccessCode.SCHEDULE_CREATED));
	}
}
