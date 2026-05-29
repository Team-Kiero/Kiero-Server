package com.kiero.schedule.application.dto;

import java.time.LocalDate;
import java.util.List;

public record FireLitEvent(
	List<Long> parentIds,
	Long childId,
	LocalDate occurredDate
) {
}
