package com.kiero.global.sse.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SseEventType {

	// 부모 이벤트
	CHILD_JOINED("invite", "자녀 가입 완료"),
	FEED_ITEM_CREATED("feed", "피드 아이템 생성"),

	// 자녀 이벤트
	MISSION_CREATED("mission", "미션 생성"),
	SCHEDULE_MODIFIED("schedule", "스케줄 변경"),
	COUPON_CREATED("coupon", "쿠폰 생성"),

	// 부모 + 자녀 이벤트
	SCHEDULE_STATUS_UPDATED("schedule", "스케쥴 상태 업데이트"),
	;

	private final String eventName;
	private final String description;
}
