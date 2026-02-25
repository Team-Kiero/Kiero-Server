package com.kiero.schedule.adapter.in.web;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.response.dto.SuccessResponse;
import com.kiero.schedule.application.dto.DefaultScheduleContentResponse;
import com.kiero.schedule.application.dto.FireLitResponse;
import com.kiero.schedule.application.dto.NowScheduleCompleteRequest;
import com.kiero.schedule.application.dto.ScheduleAddRequest;
import com.kiero.schedule.application.dto.ScheduleTabResponse;
import com.kiero.schedule.application.dto.ScheduleUpdateRequest;
import com.kiero.schedule.application.dto.TodayScheduleResponse;
import com.kiero.schedule.application.exception.ScheduleSuccessCode;
import com.kiero.schedule.application.port.in.ScheduleCommandUseCase;
import com.kiero.schedule.application.port.in.ScheduleQueryUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController {

	private final ScheduleCommandUseCase scheduleCommandUseCase;
	private final ScheduleQueryUseCase scheduleQueryUseCase;

	@PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
	@PostMapping("/{childId}")
	public ResponseEntity<SuccessResponse<Void>> addSchedule(
		@Valid @RequestBody ScheduleAddRequest request,
		@PathVariable Long childId,
		@CurrentMember CurrentAuth currentAuth
	) {
		scheduleCommandUseCase.addSchedule(request, currentAuth.memberId(), childId);
		return ResponseEntity.ok()
			.body(SuccessResponse.of(ScheduleSuccessCode.SCHEDULE_CREATED));
	}

	@PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
	@GetMapping("/{childId}")
	public ResponseEntity<SuccessResponse<ScheduleTabResponse>> getSchedules(
		@RequestParam LocalDate startDate,
		@RequestParam LocalDate endDate,
		@PathVariable Long childId,
		@CurrentMember CurrentAuth currentAuth
	) {
		ScheduleTabResponse response = scheduleQueryUseCase.getSchedules(startDate, endDate, currentAuth.memberId(),
			childId);
		return ResponseEntity.ok()
			.body(SuccessResponse.of(ScheduleSuccessCode.SCHEDULE_TAB_GET_SUCCESS, response));
	}

	@PreAuthorize("hasAnyRole('CHILD', 'ADMIN')")
	@PatchMapping("/today")
	public ResponseEntity<SuccessResponse<TodayScheduleResponse>> updateAndGetTodaySchedule(
		@CurrentMember CurrentAuth currentAuth
	) {
		TodayScheduleResponse response = scheduleCommandUseCase.getTodaySchedule(currentAuth.memberId());
		return ResponseEntity.ok()
			.body(SuccessResponse.of(ScheduleSuccessCode.TODAY_SCHEDULE_GET_SUCCESS, response));
	}

	@PreAuthorize("hasAnyRole('CHILD', 'ADMIN')")
	@PatchMapping("/skip/{scheduleDetailId}")
	public ResponseEntity<SuccessResponse<Void>> skipNowSchedule(
		@PathVariable("scheduleDetailId") Long scheduleDetailId,
		@CurrentMember CurrentAuth currentAuth
	) {
		scheduleCommandUseCase.skipNowSchedule(currentAuth.memberId(), scheduleDetailId);
		return ResponseEntity.ok()
			.body(SuccessResponse.of(ScheduleSuccessCode.NOW_SCHEDULE_SKIP_SUCCESS));
	}

	@PreAuthorize("hasAnyRole('CHILD', 'ADMIN')")
	@PatchMapping("/{scheduleDetailId}")
	public ResponseEntity<SuccessResponse<Void>> completeNowSchedule(
		@Valid @RequestBody NowScheduleCompleteRequest request,
		@PathVariable("scheduleDetailId") Long scheduleDetailId,
		@CurrentMember CurrentAuth currentAuth
	) {
		scheduleCommandUseCase.completeNowSchedule(currentAuth.memberId(), scheduleDetailId, request);
		return ResponseEntity.ok()
			.body(SuccessResponse.of(ScheduleSuccessCode.NOW_SCHEDULE_COMPLETE_SUCCESS));
	}

	@PreAuthorize("hasAnyRole('CHILD', 'ADMIN')")
	@PatchMapping("/fire-lit")
	public ResponseEntity<SuccessResponse<FireLitResponse>> fireLit(
		@CurrentMember CurrentAuth currentAuth
	) {
		FireLitResponse response = scheduleCommandUseCase.fireLit(currentAuth.memberId());
		return ResponseEntity.ok()
			.body(SuccessResponse.of(ScheduleSuccessCode.FIRE_LIT_SUCCESS, response));
	}

	@PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
	@GetMapping("/{childId}/default")
	public ResponseEntity<SuccessResponse<DefaultScheduleContentResponse>> getDefaultScheduleContent(
		@PathVariable("childId") Long childId,
		@CurrentMember CurrentAuth currentAuth
	) {
		DefaultScheduleContentResponse response = scheduleQueryUseCase.getDefaultSchedule(currentAuth.memberId(), childId);
		return ResponseEntity.ok()
			.body(SuccessResponse.of(ScheduleSuccessCode.DEFAULT_CONTENT_GET_SUCCESS, response));
	}

	@PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
	@PatchMapping("/{scheduleId}")
	public ResponseEntity<SuccessResponse<Void>> updateSchedule(
		@PathVariable("scheduleId") Long scheduleId,
		@RequestParam("selectedDate") LocalDate selectedDate,
		@RequestBody ScheduleUpdateRequest request,
		@CurrentMember CurrentAuth currentAuth
	) {
		scheduleCommandUseCase.updateSchedule(currentAuth.memberId(), scheduleId, selectedDate, request);
		return ResponseEntity.ok()
			.body(SuccessResponse.of(ScheduleSuccessCode.SCHEDULE_UPDATE_SUCCESS));
	}
}
