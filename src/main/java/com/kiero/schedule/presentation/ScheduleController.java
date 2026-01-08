package com.kiero.schedule.presentation;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.response.dto.SuccessResponse;
import com.kiero.schedule.exception.ScheduleSuccessCode;
import com.kiero.schedule.presentation.dto.ScheduleAddRequest;
import com.kiero.schedule.presentation.dto.ScheduleTabResponse;
import com.kiero.schedule.presentation.dto.TodayScheduleResponse;
import com.kiero.schedule.service.ScheduleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
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

	@GetMapping("/{childId}")
	public ResponseEntity<SuccessResponse<ScheduleTabResponse>> getSchedules(
		@RequestParam LocalDate startDate,
		@RequestParam LocalDate endDate,
		@PathVariable Long childId,
		@CurrentMember CurrentAuth currentAuth
	) {
		ScheduleTabResponse response = scheduleService.getSchedules(startDate, endDate, currentAuth.memberId(), childId);
		return ResponseEntity.ok()
			.body(SuccessResponse.of(ScheduleSuccessCode.SCHEDULE_TAB_GET_SUCCESS, response));
	}

	@GetMapping("/today")
	public ResponseEntity<SuccessResponse<TodayScheduleResponse>> getTodaySchedule(
		@CurrentMember CurrentAuth currentAuth
	) {
		TodayScheduleResponse response = scheduleService.getTodaySchedule(currentAuth.memberId());
		return ResponseEntity.ok()
			.body(SuccessResponse.of(ScheduleSuccessCode.TODAY_SCHEDULE_GET_SUCCESS, response));
	}
}
