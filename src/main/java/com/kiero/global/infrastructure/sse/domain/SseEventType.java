package com.kiero.global.infrastructure.sse.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SseEventType {

	// 부모 이벤트
	CHILD_JOINED("자녀 가입 완료"),
	MISSION_CREATED("미션 생성"),
	SCHEDULE_CREATED("스케줄 생성"),

	// 자녀 이벤트
	MISSION_COMPLETED("미션 완료"),
	COUPON_PURCHASED("쿠폰 구매"),
	SCHEDULE_COMPLETED("스케줄 완료"),
	FIRE_LIT("불 돌 사용");

	private final String description;
}
