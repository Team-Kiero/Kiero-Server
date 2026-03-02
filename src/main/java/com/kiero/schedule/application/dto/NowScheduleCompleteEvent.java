package com.kiero.schedule.application.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.kiero.parent.domain.Parent;

public record NowScheduleCompleteEvent(
	List<Parent> parents,
	Long childId,
	String name,
	String imageUrl,
	LocalDateTime occurredAt
) {
}
