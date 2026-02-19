package com.kiero.missions.application.port.out;

import java.util.List;

public interface MissionSuggestionAiPort {

	record AiGeneratedMission(
		String name,
		String dueAt
	) {}

	List<AiGeneratedMission> generate(String noticeText, String today, String dayOfWeek, String calendarRef);
}
