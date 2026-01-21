package com.kiero.mission.service;

import com.kiero.holiday.service.HolidayService;
import com.kiero.mission.presentation.dto.MissionSuggestionResponse;
import com.kiero.mission.presentation.dto.MissionSuggestionResponse.SuggestedMission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Supplier;

@Slf4j
@Service
public class MissionSuggestionService {

  private static final int MAX_MISSIONS = 10;
  private static final int REWARD = 20;
  private static final int MAX_NAME_LEN = 15;
  private static final int CALENDAR_REF_DAYS = 60;

  private final ChatClient chatClient;
  private final HolidayService holidayService;
  private final Clock clock;

  private record AiGeneratedMission(String name, String dueAt) {}

  private static final String MISSION_SUGGESTION_PROMPT = """
      당신은 알림장을 분석하여 초등학생 자녀의 미션(할 일)을 추출하는 AI입니다.

      [기준 데이터]
      - 오늘: {today} ({dayOfWeek})
      - 날짜 참조표 (이 표에 있는 날짜만 dueAt으로 사용 가능):
      {calendarRef}

      [알림장]
      {noticeText}

      [지시사항]
      1. 분류: 알림장/가정통신문이 아니거나(뉴스, 광고 등), 아이가 해야 할 일이 없으면 빈 배열([])만 반환.
      2. 추출: 아이가 실행 가능한 "구체적 행동"만 미션으로.
      3. 미션명: 15자 이내, 간결하게.

      4. 병합 및 분리 (핵심)
         - 문맥상 동일 사건/준비물은 반드시 1개로 병합.
           예) 준비물 '성금' + 안내 '성금 모금' -> '불우이웃돕기 성금 챙기기'
         - 단일 과목 숙제는 절대 쪼개지 말 것.
           예) '수학익힘 42~45쪽'은 1개 미션
         - 과목/주제가 완전히 다를 때만 분리.

      5. dueAt 추출 규칙 (매우 중요 - 엄격 적용)
         - 원칙: 해당 미션 항목의 **본문** 또는 **같은 항목 내의 바로 앞 문장**에 날짜/요일이 명시된 경우 그 날짜를 추출한다.
         - ★문맥 허용: "수요일 미술시간 준비물" 처럼, 특정 행사를 위한 준비물인 경우 그 행사의 날짜를 dueAt으로 잡는다.
         - ★절대 금지: '오늘의 숙제', '알림장' 같은 **문서 전체의 제목/헤더**를 보고 날짜를 추측하지 말 것.
         - ★결과: 위 조건에 맞는 날짜 정보가 없으면, 시스템이 처리하므로 **반드시 dueAt은 null**로 반환할 것.

      6. 날짜 형식 및 선택 제한 (매우 중요)
         - dueAt은 반드시 ISO 날짜 형식 "yyyy-MM-dd" 또는 null.
         - dueAt은 반드시 [날짜 참조표]에 존재하는 날짜 중 하나만 선택.
         - 참조표에 없는 날짜로 계산해야 한다면 dueAt은 null.

      [예시 데이터 - 이 패턴을 따를 것]
      입력: "1. 수학익힘책 40쪽 풀기"
      출력: [\\{"name": "수학익힘책 40쪽 풀기", "dueAt": null\\}]
      
      입력: "2. 다음주 월요일까지 독서록 제출"
      출력: [\\{"name": "독서록 제출", "dueAt": "2026-01-26"\\}]
      
      입력: "3. 다음주 수요일 미술시간. 준비물 붓 챙겨오기"
      출력: [\\{"name": "미술 준비물 붓 챙기기", "dueAt": "2026-01-28"\\}]

      [출력 포맷]
      - JSON 배열만 출력 (Markdown 금지).
      - 키: name(String), dueAt(String or null)
      """;

  public MissionSuggestionService(
      ChatClient.Builder chatClientBuilder,
      HolidayService holidayService,
      Clock clock
  ) {
    this.chatClient = chatClientBuilder.build();
    this.holidayService = holidayService;
    this.clock = clock;
  }

  public MissionSuggestionResponse suggestMissions(String noticeText) {
    if (noticeText == null || noticeText.isBlank()) {
      return MissionSuggestionResponse.of(Collections.emptyList());
    }

    try {
      LocalDate today = LocalDate.now(clock);
      String dayOfWeek = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);

      String calendarRef = createCalendarReference(today, CALENDAR_REF_DAYS);

      PromptTemplate promptTemplate = new PromptTemplate(MISSION_SUGGESTION_PROMPT);
      Prompt prompt = promptTemplate.create(Map.of(
          "noticeText", noticeText.trim(),
          "today", today.toString(),
          "dayOfWeek", dayOfWeek,
          "calendarRef", calendarRef
      ));

      List<AiGeneratedMission> rawMissions = chatClient.prompt(prompt)
          .call()
          .entity(new ParameterizedTypeReference<List<AiGeneratedMission>>() {});

      if (rawMissions == null || rawMissions.isEmpty()) {
        return MissionSuggestionResponse.of(Collections.emptyList());
      }

       log.info("AI raw missions: {}", rawMissions);

      List<SuggestedMission> processed = processMissions(rawMissions, today);
      return MissionSuggestionResponse.of(processed);

    } catch (Exception e) {
      log.error("Failed to generate mission suggestions", e);
      return MissionSuggestionResponse.of(Collections.emptyList());
    }
  }

  private List<SuggestedMission> processMissions(List<AiGeneratedMission> rawMissions, LocalDate today) {
    LocalDate oneYearLater = today.plusYears(1);

    Supplier<LocalDate> nextSchoolDaySupplier = new Supplier<>() {
      private LocalDate cached;
      @Override
      public LocalDate get() {
        if (cached == null) cached = calculateNextSchoolDay(today);
        return cached;
      }
    };

    return rawMissions.stream()
        .filter(m -> m != null && m.name() != null && !m.name().isBlank())
        .map(m -> {
          String name = sanitizeName(m.name());

          LocalDate dueAt = safeParseIsoDate(m.dueAt());

          boolean needsFallback =
              (dueAt == null) ||
                  (dueAt.isBefore(today)) ||
                  (dueAt.isAfter(oneYearLater));

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

    if (s.length() >= 10) {
      s = s.substring(0, 10);
    }

    try {
      return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  private LocalDate calculateNextSchoolDay(LocalDate startDate) {
    LocalDate endDate = startDate.plusDays(60);
    Set<LocalDate> holidays = holidayService.getHolidayDatesBetween(startDate, endDate);

    LocalDate candidate = startDate.plusDays(1);
    for (int i = 0; i < 60; i++) {
      DayOfWeek dow = candidate.getDayOfWeek();
      boolean isWeekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);
      boolean isHoliday = holidays.contains(candidate);

      if (!isWeekend && !isHoliday) {
        return candidate;
      }
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
