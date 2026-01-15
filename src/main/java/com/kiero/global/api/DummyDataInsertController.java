package com.kiero.global.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kiero.coupon.service.CouponService;
import com.kiero.feed.service.FeedService;
import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.mission.service.MissionService;
import com.kiero.schedule.service.ScheduleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/dummy")
@RestController
public class DummyDataInsertController {

	private final ScheduleService scheduleService;
	private final MissionService missionService;
	private final CouponService couponService;
	private final FeedService feedService;

	@PostMapping("/{childId}")
	ResponseEntity<Void> insertDummyData(
		@CurrentMember CurrentAuth currentAuth,
		@PathVariable Long childId
	) {

		log.info("해피해피!! 더미데이터를 삽입합니다. (ง •̀ω•́)ง✧");
		Long parentId = currentAuth.memberId();
		missionService.insertDummy(parentId, childId);
		couponService.insertDummy();
		feedService.insertDummy(parentId, childId);
		scheduleService.insertDummy(parentId, childId);

		return ResponseEntity.ok()
			.body(null);
	}
}
