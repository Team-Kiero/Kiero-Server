package com.kiero.mission.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kiero.mission.presentation.dto.MissionSuggestionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MissionSuggestionService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public MissionSuggestionService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    private static final String MISSION_SUGGESTION_PROMPT = """
            당신은 초등학생 자녀를 둔 부모를 돕는 AI 어시스턴트입니다.
            학교 알림장 내용을 분석하여, 부모가 자녀에게 줄 수 있는 미션(할 일) 목록을 추천해주세요.

            오늘 날짜: {today}

            알림장 내용:
            {noticeText}

            요구사항:
            1. 알림장 내용에서 자녀가 해야 할 일들을 파악하세요
            2. 각 미션은 구체적이고 실행 가능해야 합니다
            3. 미션 이름은 간단명료하게 작성하세요 (15자 이내)
            4. 최대 10개의 미션을 추천하세요
            5. 중요: 하나의 할 일은 하나의 미션으로 만드세요
               - 예: "독후감 3편 써오기" → "독후감 3편 쓰기" (1개 미션)
               - 분리하지 마세요: "독후감 1편", "독후감 2편", "독후감 3편" (X)
            6. 마감일(dueAt) 설정 규칙 (중요):
               - "내일": 오늘 기준 +1일
               - "모레": 오늘 기준 +2일
               - "X월 X일": 해당 날짜 그대로
               - "주말": 이번 주 일요일
               - "다음주 월요일": 다음 주의 월요일을 찾으세요
                 예시: 오늘이 2026-01-05(일요일)이면
                 → 이번주는 1/6(월)~1/12(일)
                 → 다음주는 1/13(월)~1/19(일)
                 → 다음주 월요일 = 2026-01-13
               - "다음주 X요일": 다음 주의 해당 요일
               - 명시 안됨: 일반적으로 다음날 (+1일)
               - 주의: 알림장 제목의 날짜(예: "1월 5일 알림장")는 작성일이지 마감일이 아닙니다
               - 중요: 한국에서 주는 월요일에 시작해서 일요일에 끝납니다
            7. 보상(reward)은 항상 20으로 설정하세요
            8. 응답은 반드시 JSON 배열 형식으로만 작성하세요

            응답 형식:
            - JSON 배열 (대괄호로 시작/끝)
            - 각 객체: name(문자열), dueAt(YYYY-MM-DD), reward(숫자 20)

            JSON 배열만 응답하세요. 설명이나 마크다운 없이.
            """;

    public MissionSuggestionResponse suggestMissions(String noticeText) {
        log.info("Generating mission suggestions from notice text");

        // 1. 입력값 검증
        if (noticeText == null || noticeText.isBlank()) {
            log.warn("Empty notice text provided");
            return createFallbackResponse();
        }

        try {
            LocalDate today = LocalDate.now();

            PromptTemplate promptTemplate = new PromptTemplate(MISSION_SUGGESTION_PROMPT);
            Prompt prompt = promptTemplate.create(Map.of(
                    "noticeText", noticeText.trim(),
                    "today", today.toString()
            ));

            // 2. AI 호출
            String response = chatClient.prompt(prompt)
                    .call()
                    .content();

            // 3. AI 응답 검증
            if (response == null || response.isBlank()) {
                log.warn("AI returned empty response");
                return createFallbackResponse();
            }

            log.debug("AI Response: {}", response);

            // 4. 파싱 및 검증
            List<MissionSuggestionResponse.SuggestedMission> missions = parseMissions(response);

            if (missions.isEmpty()) {
                log.warn("No missions parsed from AI response");
                return createFallbackResponse();
            }

            log.info("Generated {} mission suggestions", missions.size());
            return MissionSuggestionResponse.of(missions);

        } catch (Exception e) {
            log.error("Failed to generate mission suggestions", e);
            return createFallbackResponse();
        }
    }

    private MissionSuggestionResponse createFallbackResponse() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        return MissionSuggestionResponse.of(List.of(
                new MissionSuggestionResponse.SuggestedMission("알림장 내용 확인하기", tomorrow, 20)
        ));
    }

    private List<MissionSuggestionResponse.SuggestedMission> parseMissions(String response) {
        try {
            // JSON 배열 추출
            String jsonContent = extractJsonArray(response);

            if (jsonContent == null || jsonContent.isBlank()) {
                log.warn("No JSON content extracted from response");
                return List.of();
            }

            // JSON 파싱
            List<Map<String, Object>> rawList = objectMapper.readValue(
                    jsonContent,
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            if (rawList == null || rawList.isEmpty()) {
                log.warn("Parsed JSON array is empty");
                return List.of();
            }

            List<MissionSuggestionResponse.SuggestedMission> missions = new ArrayList<>();
            LocalDate tomorrow = LocalDate.now().plusDays(1);

            for (Map<String, Object> item : rawList) {
                // 필드 검증
                String name = (String) item.get("name");

                // name이 null이거나 빈 경우 스킵
                if (name == null || name.isBlank()) {
                    log.warn("Mission name is null or empty, skipping");
                    continue;
                }

                // name 길이 제한
                if (name.length() > 50) {
                    name = name.substring(0, 50);
                }

                String dueAtStr = (String) item.get("dueAt");

                // reward는 20으로 고정
                int reward = 20;

                // 날짜 파싱 (실패 시 내일)
                LocalDate dueAt;
                try {
                    dueAt = LocalDate.parse(dueAtStr);

                    // 과거 날짜 방지 (오늘 이전이면 내일로)
                    if (dueAt.isBefore(LocalDate.now())) {
                        dueAt = tomorrow;
                    }

                    // 너무 먼 미래 방지 (1년 이후면 내일로)
                    if (dueAt.isAfter(LocalDate.now().plusYears(1))) {
                        dueAt = tomorrow;
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse date: {}, using tomorrow", dueAtStr);
                    dueAt = tomorrow;
                }

                missions.add(new MissionSuggestionResponse.SuggestedMission(
                        name.trim(),
                        dueAt,
                        reward
                ));
            }

            return missions.stream().limit(10).toList();

        } catch (Exception e) {
            log.error("Failed to parse AI response as JSON", e);
            return List.of();
        }
    }

    private String extractJsonArray(String response) {
        // JSON 배열 패턴 찾기
        Pattern pattern = Pattern.compile("\\[\\s*\\{.*?}\\s*]", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);

        if (matcher.find()) {
            return matcher.group();
        }

        // 패턴을 못 찾으면 원본 그대로 반환
        return response.trim();
    }
}
