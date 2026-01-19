package com.kiero.schedule.presentation.dto;

import java.time.LocalDateTime;

/**
 * 스케줄 생성 이벤트
 * 부모가 자녀에게 스케줄을 생성했을 때 발행
 */
public record ScheduleCreatedEvent(
	Long parentId,
	Long childId,
	String scheduleName,
	LocalDateTime occurredAt
) {
}
