package com.kiero.admin.adapter.in.web;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kiero.admin.application.dto.AdminChildDetailResponse;
import com.kiero.admin.application.dto.AdminChildSummaryResponse;
import com.kiero.admin.application.dto.AdminCouponResponse;
import com.kiero.admin.application.dto.AdminCouponUpdateRequest;
import com.kiero.admin.application.dto.AdminMissionResponse;
import com.kiero.admin.application.dto.AdminMissionUpdateRequest;
import com.kiero.admin.application.dto.AdminPageResponse;
import com.kiero.admin.application.dto.AdminParentDetailResponse;
import com.kiero.admin.application.dto.AdminParentSummaryResponse;
import com.kiero.admin.application.dto.AdminScheduleResponse;
import com.kiero.admin.application.exception.AdminSuccessCode;
import com.kiero.admin.application.port.in.AdminCouponUseCase;
import com.kiero.admin.application.port.in.AdminMissionUseCase;
import com.kiero.admin.application.port.in.AdminScheduleUseCase;
import com.kiero.admin.application.port.in.AdminUserCommandUseCase;
import com.kiero.admin.application.port.in.AdminUserQueryUseCase;
import com.kiero.global.response.dto.SuccessResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

	private final AdminUserQueryUseCase adminUserQueryUseCase;
	private final AdminUserCommandUseCase adminUserCommandUseCase;
	private final AdminMissionUseCase adminMissionUseCase;
	private final AdminScheduleUseCase adminScheduleUseCase;
	private final AdminCouponUseCase adminCouponUseCase;

	// ==================== 부모 관리 ====================

	@GetMapping("/parents")
	public ResponseEntity<SuccessResponse<AdminPageResponse<AdminParentSummaryResponse>>> getParents(
		@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		AdminPageResponse<AdminParentSummaryResponse> response = adminUserQueryUseCase.findAllParents(pageable);
		return ResponseEntity.ok(SuccessResponse.of(AdminSuccessCode.GET_PARENTS_SUCCESS, response));
	}

	@GetMapping("/parents/{parentId}")
	public ResponseEntity<SuccessResponse<AdminParentDetailResponse>> getParent(
		@PathVariable Long parentId
	) {
		AdminParentDetailResponse response = adminUserQueryUseCase.findParent(parentId);
		return ResponseEntity.ok(SuccessResponse.of(AdminSuccessCode.GET_PARENT_SUCCESS, response));
	}

	@DeleteMapping("/parents/{parentId}")
	public ResponseEntity<SuccessResponse<Void>> deleteParent(
		@PathVariable Long parentId
	) {
		adminUserCommandUseCase.deleteParent(parentId);
		return ResponseEntity.ok(SuccessResponse.of(AdminSuccessCode.DELETE_PARENT_SUCCESS, null));
	}

	// ==================== 아이 관리 ====================

	@GetMapping("/children")
	public ResponseEntity<SuccessResponse<AdminPageResponse<AdminChildSummaryResponse>>> getChildren(
		@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		AdminPageResponse<AdminChildSummaryResponse> response = adminUserQueryUseCase.findAllChildren(pageable);
		return ResponseEntity.ok(SuccessResponse.of(AdminSuccessCode.GET_CHILDREN_SUCCESS, response));
	}

	@GetMapping("/children/{childId}")
	public ResponseEntity<SuccessResponse<AdminChildDetailResponse>> getChild(
		@PathVariable Long childId
	) {
		AdminChildDetailResponse response = adminUserQueryUseCase.findChild(childId);
		return ResponseEntity.ok(SuccessResponse.of(AdminSuccessCode.GET_CHILD_SUCCESS, response));
	}

	@DeleteMapping("/children/{childId}")
	public ResponseEntity<SuccessResponse<Void>> deleteChild(
		@PathVariable Long childId
	) {
		adminUserCommandUseCase.deleteChild(childId);
		return ResponseEntity.ok(SuccessResponse.of(AdminSuccessCode.DELETE_CHILD_SUCCESS, null));
	}

	// ==================== 미션 관리 ====================

	@GetMapping("/children/{childId}/missions")
	public ResponseEntity<SuccessResponse<List<AdminMissionResponse>>> getMissions(
		@PathVariable Long childId
	) {
		List<AdminMissionResponse> response = adminMissionUseCase.findAllByChildId(childId);
		return ResponseEntity.ok(SuccessResponse.of(AdminSuccessCode.GET_MISSIONS_SUCCESS, response));
	}

	@PatchMapping("/missions/{missionId}")
	public ResponseEntity<SuccessResponse<AdminMissionResponse>> updateMission(
		@PathVariable Long missionId,
		@Valid @RequestBody AdminMissionUpdateRequest request
	) {
		AdminMissionResponse response = adminMissionUseCase.updateMission(missionId, request);
		return ResponseEntity.ok(SuccessResponse.of(AdminSuccessCode.UPDATE_MISSION_SUCCESS, response));
	}

	@DeleteMapping("/missions/{missionId}")
	public ResponseEntity<SuccessResponse<Void>> deleteMission(
		@PathVariable Long missionId
	) {
		adminMissionUseCase.deleteMission(missionId);
		return ResponseEntity.ok(SuccessResponse.of(AdminSuccessCode.DELETE_MISSION_SUCCESS, null));
	}

	// ==================== 일정 관리 ====================

	@GetMapping("/children/{childId}/schedules")
	public ResponseEntity<SuccessResponse<List<AdminScheduleResponse>>> getSchedules(
		@PathVariable Long childId
	) {
		List<AdminScheduleResponse> response = adminScheduleUseCase.findAllByChildId(childId);
		return ResponseEntity.ok(SuccessResponse.of(AdminSuccessCode.GET_SCHEDULES_SUCCESS, response));
	}

	@DeleteMapping("/schedules/{scheduleId}")
	public ResponseEntity<SuccessResponse<Void>> deleteSchedule(
		@PathVariable Long scheduleId
	) {
		adminScheduleUseCase.deleteSchedule(scheduleId);
		return ResponseEntity.ok(SuccessResponse.of(AdminSuccessCode.DELETE_SCHEDULE_SUCCESS, null));
	}

	// ==================== 쿠폰 관리 ====================

	@GetMapping("/children/{childId}/coupons")
	public ResponseEntity<SuccessResponse<List<AdminCouponResponse>>> getCoupons(
		@PathVariable Long childId
	) {
		List<AdminCouponResponse> response = adminCouponUseCase.findAllByChildId(childId);
		return ResponseEntity.ok(SuccessResponse.of(AdminSuccessCode.GET_COUPONS_SUCCESS, response));
	}

	@PatchMapping("/coupons/{couponId}")
	public ResponseEntity<SuccessResponse<AdminCouponResponse>> updateCoupon(
		@PathVariable Long couponId,
		@Valid @RequestBody AdminCouponUpdateRequest request
	) {
		AdminCouponResponse response = adminCouponUseCase.updateCoupon(couponId, request);
		return ResponseEntity.ok(SuccessResponse.of(AdminSuccessCode.UPDATE_COUPON_SUCCESS, response));
	}

	@DeleteMapping("/coupons/{couponId}")
	public ResponseEntity<SuccessResponse<Void>> deleteCoupon(
		@PathVariable Long couponId
	) {
		adminCouponUseCase.deleteCoupon(couponId);
		return ResponseEntity.ok(SuccessResponse.of(AdminSuccessCode.DELETE_COUPON_SUCCESS, null));
	}
}
