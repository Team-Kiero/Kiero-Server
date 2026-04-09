package com.kiero.mission.adapter.out.ai;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import com.kiero.mission.application.port.out.MissionSuggestionAiPort;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SpringAiMissionSuggestionAdapter implements MissionSuggestionAiPort {

	private final ChatClient chatClient;

	public SpringAiMissionSuggestionAdapter(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder.build();
	}

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
		   3. 미션명: 15자 이내, 반드시 "~하기" / "~챙기기" 형태의 명사형으로 작성.
	      예) "수학 숙제 풀기", "미술 준비물 챙기기", "독서록 제출하기"

		   4. 병합 및 분리 (핵심)
		      - 문맥상 동일 사건/준비물은 반드시 1개로 병합.
		        예) 준비물 '성금' + 안내 '성금 모금' -> '불우이웃돕기 성금 챙기기'
		      - 단일 과목 숙제는 절대 쪼개지 말 것.
		        예) '수학익힘 42~45쪽'은 1개 미션
		      - 과목/주제가 완전히 다를 때만 분리.
		
		   5. dueAt 추출 규칙 (매우 중요 - 엄격 적용)
		      - 원칙: 해당 미션 항목의 **본문** 또는 **같은 항목 내의 바로 앞 문장**에 날짜/요일이 명시된 경우 그 날짜를 추출한다.
		      - ★문맥 허용: "수요일 미술시간 준비물" 처럼, 특정 행사를 위한 준비물인 경우 그 행사의 날짜를 dueAt으로 잡는다.
		      - ★오늘 표현: "오늘까지", "오늘", "당일" 같은 표현이 본문에 명시된 경우 → 참조표에서 [오늘] 레이블이 붙은 날짜를 선택.
		      - ★절대 금지: '오늘의 숙제', '알림장' 같은 **문서 전체의 제목/헤더**를 보고 날짜를 추측하지 말 것.
		      - ★결과: 위 조건에 맞는 날짜 정보가 없으면, 시스템이 처리하므로 **반드시 dueAt은 null**로 반환할 것.

		      - ★주차 해석 규칙 (매우 중요):
		        [날짜 참조표]의 각 날짜에는 [이번주]/[다음주]/[다다음주] 레이블이 붙어있다.
		        · "다음주 [요일]" → 참조표에서 [다음주] 레이블이 붙은 날짜 중 해당 요일을 선택
		        · "이번주 [요일]" → 참조표에서 [이번주] 레이블이 붙은 날짜 중 해당 요일을 선택
		        · "[요일]만 단독 언급" → 오늘 이후 가장 가까운 해당 요일
		        예) 오늘이 월요일이고 "다음주 화요일" → [다음주] 레이블이 붙은 화요일 날짜를 선택 (절대 [이번주] 화요일 선택 금지)
		
		   6. 날짜 형식 및 선택 제한 (매우 중요)
		      - dueAt은 반드시 ISO 날짜 형식 "yyyy-MM-dd" 또는 null.
		      - dueAt은 반드시 [날짜 참조표]에 존재하는 날짜 중 하나만 선택.
		      - 참조표에 없는 날짜로 계산해야 한다면 dueAt은 null.
		
		 [예시 데이터 - 이 패턴을 따를 것]
		 입력: "1. 수학익힘책 40쪽 풀기"
		 출력: [\\\\{"name": "수학익힘책 40쪽 풀기", "dueAt": null\\\\}]
		\s
		 입력: "2. 다음주 월요일까지 독서록 제출"
		 출력: [\\\\{"name": "독서록 제출하기", "dueAt": "2026-01-26"\\\\}]
		\s
		 입력: "3. 다음주 수요일 미술시간. 준비물 붓 챙겨오기"
		 출력: [\\\\{"name": "미술 준비물 붓 챙기기", "dueAt": "2026-01-28"\\\\}]
		\s
		 입력: "오늘까지 수학 익힘책을 풀어주세요."
		 출력: [\\\\{"name": "수학익힘책 풀기", "dueAt": "{today}"\\\\}]
		
		   [출력 포맷]
		   - JSON 배열만 출력 (Markdown 금지).
		   - 키: name(String), dueAt(String or null)
		""";

	@Override
	public List<AiGeneratedMission> generate(String noticeText, String today, String dayOfWeek, String calendarRef) {
		String rendered = MISSION_SUGGESTION_PROMPT
			.replace("{noticeText}", noticeText)
			.replace("{today}", today)
			.replace("{dayOfWeek}", dayOfWeek)
			.replace("{calendarRef}", calendarRef);
		Prompt prompt = new Prompt(rendered);

		return chatClient.prompt(prompt)
			.call()
			.entity(new ParameterizedTypeReference<List<AiGeneratedMission>>() {
			});
	}
}