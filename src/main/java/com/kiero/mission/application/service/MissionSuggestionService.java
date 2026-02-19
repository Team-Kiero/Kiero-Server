package com.kiero.mission.application.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import com.kiero.mission.application.port.in.MissionSuggestionUseCase;
import com.kiero.mission.application.port.out.HolidayQueryPort;
import com.kiero.mission.application.port.out.MissionSuggestionAiPort;
import com.kiero.mission.application.dto.MissionSuggestionResponse;
import com.kiero.mission.application.dto.MissionSuggestionResponse.SuggestedMission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionSuggestionService implements MissionSuggestionUseCase {

	private static final int MAX_MISSIONS = 10;
	private static final int REWARD = 20;
	private static final int MAX_NAME_LEN = 15;
	private static final int CALENDAR_REF_DAYS = 60;

	private final MissionSuggestionAiPort aiPort;
	private final HolidayQueryPort holidayQueryPort;
	private final Clock clock;

	@Override
	public MissionSuggestionResponse suggestMissions(String noticeText) {
		if (noticeText == null || noticeText.isBlank()) {
			return MissionSuggestionResponse.of(Collections.emptyList());
		}

		try {
			LocalDate today = LocalDate.now(clock);
			String dayOfWeek = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);
			String calendarRef = createCalendarReference(today, CALENDAR_REF_DAYS);

			List<MissionSuggestionAiPort.AiGeneratedMission> raw = aiPort.generate(
				noticeText.trim(),
				today.toString(),
				dayOfWeek,
				calendarRef
			);

			if (raw == null || raw.isEmpty()) {
				return MissionSuggestionResponse.of(Collections.emptyList());
			}

			log.info("AI raw missions: {}", raw);

			List<SuggestedMission> processed = processMissions(raw, today);
			return MissionSuggestionResponse.of(processed);

		} catch (Exception e) {
			log.error("Failed to generate mission suggestions", e);
			return MissionSuggestionResponse.of(Collections.emptyList());
		}
	}

	private List<SuggestedMission> processMissions(List<MissionSuggestionAiPort.AiGeneratedMission> raw, LocalDate today) {
		LocalDate oneYearLater = today.plusYears(1);

		Supplier<LocalDate> nextSchoolDaySupplier = new Supplier<>() {
			private LocalDate cached;
			@Override public LocalDate get() {
				if (cached == null) cached = calculateNextSchoolDay(today);
				return cached;
			}
		};

		return raw.stream()
			.filter(m -> m != null && m.name() != null && !m.name().isBlank())
			.map(m -> {
				String name = sanitizeName(m.name());
				LocalDate dueAt = safeParseIsoDate(m.dueAt());

				boolean needsFallback =
					(dueAt == null) ||
						dueAt.isBefore(today) ||
						dueAt.isAfter(oneYearLater);

				if (needsFallback) {
					dueAt = nextSchoolDaySupplier.get();
				}

				return new SuggestedMission(name, dueAt, REWARD);
			})
			.limit(MAX_MISSIONS)
			.toList();
	}

	private String sanitizeName(String raw) {
		String cleaned = raw.trim();
		if (cleaned.length() > MAX_NAME_LEN) {
			cleaned = cleaned.substring(0, MAX_NAME_LEN);
		}
		return cleaned;
	}

	private LocalDate safeParseIsoDate(String rawDueAt) {
		if (rawDueAt == null || rawDueAt.isBlank()) return null;
		String s = rawDueAt.trim();
		if (s.length() >= 10) s = s.substring(0, 10);

		try {
			return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	private LocalDate calculateNextSchoolDay(LocalDate startDate) {
		LocalDate endDate = startDate.plusDays(60);
		Set<LocalDate> holidays = holidayQueryPort.getHolidayDatesBetween(startDate, endDate);

		LocalDate candidate = startDate.plusDays(1);
		for (int i = 0; i < 60; i++) {
			DayOfWeek dow = candidate.getDayOfWeek();
			boolean isWeekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);
			boolean isHoliday = holidays.contains(candidate);

			if (!isWeekend && !isHoliday) return candidate;
			candidate = candidate.plusDays(1);
		}
		return startDate.plusDays(1);
	}

	private String createCalendarReference(LocalDate start, int days) {
		StringBuilder sb = new StringBuilder();
		DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

		for (int i = 0; i < days; i++) {
			LocalDate date = start.plusDays(i);
			String dayStr = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);

			sb.append("- ")
				.append(date.format(formatter))
				.append(" (")
				.append(dayStr)
				.append(")");

			if (i == 0) sb.append(" [오늘]");
			if (i == 1) sb.append(" [내일]");
			if (i == 2) sb.append(" [모레]");

			if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
				sb.append("  <-- (이번 주/다음 주 경계)");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}
