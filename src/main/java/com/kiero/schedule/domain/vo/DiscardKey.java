package com.kiero.schedule.domain.vo;

import java.time.LocalDate;

public record DiscardKey(
	Long scheduleId,
	LocalDate date
) {
}
