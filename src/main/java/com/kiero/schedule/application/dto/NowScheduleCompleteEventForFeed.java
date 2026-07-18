package com.kiero.schedule.application.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.kiero.parent.domain.Parent;

public record NowScheduleCompleteEventForFeed(
	List<Parent> parents,
	Long childId,
	Long scheduleDetailId,
	String name,
	String imageUrl,
	LocalDateTime occurredAt
) {
}
