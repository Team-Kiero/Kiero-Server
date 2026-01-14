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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class MissionSuggestionService {

    private final ChatClient chatClient;
    private final HolidayService holidayService;

    private static final LocalDate UNSPECIFIED_DATE = LocalDate.of(1900, 1, 1);

    public MissionSuggestionService(ChatClient.Builder chatClientBuilder, HolidayService holidayService) {
        this.chatClient = chatClientBuilder.build();
        this.holidayService = holidayService;
    }

    private static final String MISSION_SUGGESTION_PROMPT = """
              당신은 초등학생 자녀를 둔 부모를 돕는 AI 어시스턴트입니다.
              학교 알림장 내용을 분석하여, 부모가 자녀에게 줄 수 있는 미션(할 일) 목록을 추천해주세요.

              [오늘 날짜]
              {today} ({dayOfWeek})
              
              [날짜 참조표 (이 표를 보고 날짜를 찾으세요)]
              {calendarRef}

              [알림장 내용]
              {noticeText}

              [요구사항]
              1. 입력 텍스트가 학교 알림장, 가정통신문, 학원 공지사항 등 '자녀의 할 일'이 명시된 글인지 검토하세요.
              2. 만약 뉴스 기사, 단순 정보 전달, 광고, 혹은 할 일을 도출할 수 없는 일반적인 글이라면 미션을 생성하지 말고 반드시 빈 JSON 배열([])만 응답하세요.
              3. 알림장 내용에서 자녀가 해야 할 일들을 파악하세요.
              4. 각 미션은 구체적이고 실행 가능해야 합니다.
              5. 미션 이름은 반드시 15자 이내로 간결하게 작성하세요.
              
              6. **중복 및 연관 항목 병합 (매우 중요)**:
                 - '가방 확인', '준비물', '할 일' 등 서로 다른 섹션에 있더라도, **문맥상 같은 사건이나 물건을 지칭한다면 반드시 하나의 미션으로 합치세요.**
                 - **나쁜 예**: 
                    1. 미션: 성금 봉투 챙기기 (가방 확인 섹션에서 추출)
                    2. 미션: 성금 가져오기 (준비물 섹션에서 추출)
                 - **좋은 예**: 
                    1. 미션: 불우이웃돕기 성금 챙기기 (두 내용을 합쳐서 하나로 만듦)
                 - 준비물(예: 리코더)과 수업(예: 음악시간)이 같이 언급되면 '리코더 챙기기' 하나로 만드세요.

              7. 미션 분리 기준:
                 - 서로 완전히 다른 과목이나 주제인 경우에만 분리하세요.
                 - 예: 독후감 3편 써오기는 1개 미션으로 만드세요 (분리 금지)

              8. 마감일(dueAt) 설정 규칙:
                 **AI는 날짜를 직접 계산하지 말고, 반드시 위 [날짜 참조표]에서 해당하는 날짜를 찾아 입력해야 합니다.**

                 **규칙 A. 날짜가 명시된 경우 (우선순위 높음)**:
                 - 텍스트에 "내일", "모레", "이번주 금요일", "1월 19일까지" 등 날짜 표현이 있다면, **[날짜 참조표]에서 해당 요일의 날짜(YYYY-MM-DD)를 찾아 그대로 적으세요.**
                 - 병합된 미션의 경우, 언급된 날짜 중 가장 빠른 날짜(또는 명시된 마감일)를 따르세요.

                 **규칙 B. 날짜가 명시되지 않은 경우**:
                 - 알림장에 특정 마감 기한이 없다면, 반드시 "1900-01-01"로 설정하세요. (시스템이 자동으로 처리합니다)

              9. 보상(reward)은 항상 20으로 설정하세요.
              10. 최대 10개의 미션을 추천하세요.

              [응답 형식]
              - 반드시 마크다운 없이 JSON 배열 형식으로만 응답하세요.
              - 필드: name(문자열), dueAt(YYYY-MM-DD), reward(20)
              """;

    public MissionSuggestionResponse suggestMissions(String noticeText) {
        log.info("Generating mission suggestions from notice text");

        if (noticeText == null || noticeText.isBlank()) {
            return createFallbackResponse();
        }

        try {
            LocalDate today = LocalDate.now();
            String dayOfWeek = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);

            String calendarRef = createCalendarReference(today);

            PromptTemplate promptTemplate = new PromptTemplate(MISSION_SUGGESTION_PROMPT);
            Prompt prompt = promptTemplate.create(Map.of(
                    "noticeText", noticeText.trim(),
                    "today", today.toString(),
                    "dayOfWeek", dayOfWeek,
                    "calendarRef", calendarRef
            ));

            List<SuggestedMission> missions = chatClient.prompt(prompt)
                    .call()
                    .entity(new ParameterizedTypeReference<List<SuggestedMission>>() {});

            if (missions == null || missions.isEmpty()) {
                return createFallbackResponse();
            }

            return MissionSuggestionResponse.of(validateAndCleanMissions(missions));

        } catch (Exception e) {
            log.error("Failed to generate mission suggestions", e);
            return createFallbackResponse();
        }
    }

    private String createCalendarReference(LocalDate start) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 0; i < 14; i++) {
            LocalDate date = start.plusDays(i);
            String dayStr = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);

            sb.append(String.format("- %s (%s)", date.format(formatter), dayStr));

            if (i == 0) sb.append(" [오늘]");
            if (i == 1) sb.append(" [내일]");
            if (i == 2) sb.append(" [모레]");

            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                sb.append("  <-- (여기까지 이번 주, 아래부터 다음 주) -->");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private List<SuggestedMission> validateAndCleanMissions(List<SuggestedMission> missions) {
        LocalDate today = LocalDate.now();
        LocalDate oneYearLater = today.plusYears(1);

        return missions.stream()
                .filter(m -> m.name() != null && !m.name().isBlank())
                .map(m -> {
                    String cleanedName = m.name().trim();
                    if (cleanedName.length() > 15) {
                        cleanedName = cleanedName.substring(0, 15);
                    }

                    LocalDate dueAt = m.dueAt();

                    // 1. 날짜 미지정 -> 다음 등교일 계산
                    if (UNSPECIFIED_DATE.equals(dueAt)) {
                        dueAt = getNextSchoolDay(today);
                    }
                    // 2. 유효하지 않은 날짜 -> 다음 등교일 계산
                    else if (dueAt == null || dueAt.isBefore(today) || dueAt.isAfter(oneYearLater)) {
                        dueAt = getNextSchoolDay(today);
                    }
                    // 3. 명시적 날짜 -> 그대로 사용

                    return new SuggestedMission(cleanedName, dueAt, 20);
                })
                .limit(10)
                .toList();
    }

    private LocalDate getNextSchoolDay(LocalDate startDate) {
        LocalDate candidate = startDate.plusDays(1);
        for (int i = 0; i < 30; i++) {
            DayOfWeek dayOfWeek = candidate.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY
                    && dayOfWeek != DayOfWeek.SUNDAY
                    && !holidayService.isHoliday(candidate)) {
                return candidate;
            }
            candidate = candidate.plusDays(1);
        }
        return startDate.plusDays(1);
    }

    private MissionSuggestionResponse createFallbackResponse() {
        LocalDate nextSchoolDay = getNextSchoolDay(LocalDate.now());
        return MissionSuggestionResponse.of(List.of(
                new SuggestedMission("알림장 내용 확인하기", nextSchoolDay, 20)
        ));
    }
}