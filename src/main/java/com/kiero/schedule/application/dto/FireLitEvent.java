package com.kiero.schedule.application.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.kiero.parent.domain.Parent;

public record FireLitEvent(
	List<Parent> parents,
	Long childId,
	Integer amount,
	LocalDateTime occurredAt
) {
}
