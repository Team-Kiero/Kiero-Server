package com.kiero.global.infrastructure.sse.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SseEventType {

	// 부모 이벤트
	CHILD_JOINED("invite", "자녀 가입 완료"),

	// 자녀 이벤트
	MISSION_CREATED("mission", "미션 생성"),
	SCHEDULE_CREATED("schedule", "스케줄 생성"),

	// 피드 이벤트 (부모에게 전송)
	MISSION_COMPLETED("feed", "미션 완료"),
	COUPON_PURCHASED("feed", "쿠폰 구매"),
	SCHEDULE_COMPLETED("feed", "스케줄 완료"),
	FIRE_LIT("feed", "불 돌 사용");

	private final String eventName;
	private final String description;
}
