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
            5. 마감일(dueAt) 설정 규칙:
               - 알림장에서 날짜를 찾았다면 해당 날짜로 설정 (YYYY-MM-DD 형식)
               - 날짜를 찾지 못했다면 오늘 기준 다음날로 설정
               - "내일", "모레" 등의 표현도 날짜로 변환하세요
            6. 보상(reward)은 항상 20으로 설정하세요
            7. 응답은 반드시 JSON 배열 형식으로만 작성하세요:

            [
              {
                "name": "수학 숙제 완료하기",
                "dueAt": "2026-01-10",
                "reward": 20
              },
              {
                "name": "준비물 챙기기",
                "dueAt": "2026-01-11",
                "reward": 20
              }
            ]

            JSON 배열만 응답하세요. 다른 텍스트는 포함하지 마세요.
            """;

    public MissionSuggestionResponse suggestMissions(String noticeText) {
        log.info("Generating mission suggestions from notice text");

        try {
            LocalDate today = LocalDate.now();

            PromptTemplate promptTemplate = new PromptTemplate(MISSION_SUGGESTION_PROMPT);
            Prompt prompt = promptTemplate.create(Map.of(
                    "noticeText", noticeText,
                    "today", today.toString()
            ));

            String response = chatClient.prompt(prompt)
                    .call()
                    .content();

            log.debug("AI Response: {}", response);

            List<MissionSuggestionResponse.SuggestedMission> missions = parseMissions(response);

            log.info("Generated {} mission suggestions", missions.size());
            return MissionSuggestionResponse.of(missions);

        } catch (Exception e) {
            log.error("Failed to generate mission suggestions", e);
            // Fallback: 기본값 반환
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            return MissionSuggestionResponse.of(List.of(
                    new MissionSuggestionResponse.SuggestedMission("알림장 내용 확인하기", tomorrow, 20)
            ));
        }
    }

    private List<MissionSuggestionResponse.SuggestedMission> parseMissions(String response) {
        try {
            // JSON 배열 추출 (마크다운 코드블록이나 추가 텍스트 제거)
            String jsonContent = extractJsonArray(response);

            // JSON 파싱
            List<Map<String, Object>> rawList = objectMapper.readValue(
                    jsonContent,
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            List<MissionSuggestionResponse.SuggestedMission> missions = new ArrayList<>();
            LocalDate tomorrow = LocalDate.now().plusDays(1);

            for (Map<String, Object> item : rawList) {
                String name = (String) item.get("name");
                String dueAtStr = (String) item.get("dueAt");
                Integer reward = item.get("reward") != null ?
                        ((Number) item.get("reward")).intValue() : 20;

                LocalDate dueAt;
                try {
                    dueAt = LocalDate.parse(dueAtStr);
                } catch (Exception e) {
                    dueAt = tomorrow;
                }

                missions.add(new MissionSuggestionResponse.SuggestedMission(name, dueAt, reward));
            }

            return missions.stream().limit(10).toList();

        } catch (Exception e) {
            log.error("Failed to parse AI response as JSON", e);
            // Fallback: 기본값 반환
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            return List.of(
                    new MissionSuggestionResponse.SuggestedMission("알림장 내용 확인하기", tomorrow, 20)
            );
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
