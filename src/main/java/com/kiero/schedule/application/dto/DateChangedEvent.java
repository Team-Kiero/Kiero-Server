package com.kiero.schedule.application.dto;

import java.time.LocalDate;
import java.util.List;

public record DateChangedEvent(
	List<Long> parentIds,
	List<Long> childIds,
	LocalDate newDate
) {
}
