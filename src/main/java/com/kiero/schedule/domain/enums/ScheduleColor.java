package com.kiero.schedule.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleColor {
	SCHEDULE1("#CFFFFA"),
	SCHEDULE2("#FFFEE9"),
	SCHEDULE3("#BFFFE3"),
	SCHEDULE4("#34D9D3"),
	SCHEDULE5("#7BBDFF"),
	;

	private final String colorCode;

	public ScheduleColor next() {
		ScheduleColor[] values = ScheduleColor.values();
		int nextIndex = (this.ordinal() + 1) % values.length;
		return values[nextIndex];
	}
}
