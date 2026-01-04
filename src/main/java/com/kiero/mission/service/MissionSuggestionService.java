package com.kiero.mission.service;

import com.kiero.mission.presentation.dto.MissionSuggestionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MissionSuggestionService {

    private final ChatClient chatClient;

    public MissionSuggestionService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    private static final String MISSION_SUGGESTION_PROMPT = """
            당신은 초등학생 자녀를 둔 부모를 돕는 AI 어시스턴트입니다.
            학교 알림장 내용을 분석하여, 부모가 자녀에게 줄 수 있는 미션(할 일) 목록을 추천해주세요.

            알림장 내용:
            {noticeText}

            요구사항:
            1. 알림장 내용에서 자녀가 해야 할 일들을 파악하세요
            2. 각 미션은 구체적이고 실행 가능해야 합니다
            3. 미션은 간단명료하게 작성하세요 (15자 이내)
            4. 최대 5개의 미션을 추천하세요
            5. 응답은 반드시 다음 형식을 따르세요:
               - 각 미션은 새 줄로 구분
               - 번호나 불릿 포인트 없이 미션 제목만 작성
               - 예시:
               수학 숙제 완료하기
               준비물 챙기기

            미션 목록:
            """;

    public MissionSuggestionResponse suggestMissions(String noticeText) {
        log.info("Generating mission suggestions from notice text");

        try {
            PromptTemplate promptTemplate = new PromptTemplate(MISSION_SUGGESTION_PROMPT);
            Prompt prompt = promptTemplate.create(Map.of("noticeText", noticeText));

            String response = chatClient.prompt(prompt)
                    .call()
                    .content();

            log.debug("AI Response: {}", response);

            List<String> missions = parseMissions(response);

            log.info("Generated {} mission suggestions", missions.size());
            return MissionSuggestionResponse.of(missions);

        } catch (Exception e) {
            log.error("Failed to generate mission suggestions", e);
            return MissionSuggestionResponse.of(List.of("알림장 내용 확인하기"));
        }
    }

    private List<String> parseMissions(String response) {
        return Arrays.stream(response.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                // 불릿 포인트 제거 (-, *, •)
                .map(line -> line.replaceAll("^[-*•]\\s*", ""))
                // 숫자와 점 제거 (1. 2. 3.)
                .map(line -> line.replaceAll("^\\d+\\.\\s*", ""))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .limit(5)
                .collect(Collectors.toList());
    }
}
