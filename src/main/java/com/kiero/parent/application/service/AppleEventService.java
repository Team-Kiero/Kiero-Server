package com.kiero.parent.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.global.auth.client.apple.dto.AppleServerEvent;
import com.kiero.global.auth.client.enums.Provider;
import com.kiero.global.auth.enums.Role;
import com.kiero.global.auth.jwt.application.port.out.TokenCommandPort;
import com.kiero.parent.application.port.in.AppleEventUseCase;
import com.kiero.parent.application.port.in.ParentWithdrawUseCase;
import com.kiero.parent.application.port.out.AppleEventParsePort;
import com.kiero.parent.application.port.out.ParentLoadPort;
import com.kiero.parent.application.port.out.ParentSavePort;
import com.kiero.parent.domain.Parent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppleEventService implements AppleEventUseCase {

	private final AppleEventParsePort appleEventParsePort;
	private final ParentLoadPort parentLoadPort;
	private final ParentSavePort parentSavePort;
	private final TokenCommandPort tokenCommandPort;
	private final ParentWithdrawUseCase parentWithdrawUseCase;

	@Override
	@Transactional
	public void handle(String eventJwt) {
		AppleServerEvent event = appleEventParsePort.parse(eventJwt);
		log.info("Apple 서버 이벤트 수신: type={}, sub={}", event.type(), event.sub());

		switch (event.type()) {
			case EMAIL_ENABLED, EMAIL_DISABLED ->
				log.info("Apple 이메일 설정 변경 이벤트 수신 (처리 불필요): sub={}", event.sub());
			case CONSENT_REVOKED -> handleConsentRevoked(event.sub());
			case ACCOUNT_DELETE -> handleAccountDelete(event.sub());
			case UNKNOWN -> log.warn("알 수 없는 Apple 서버 이벤트 수신: sub={}", event.sub());
		}
	}

	private void handleConsentRevoked(String sub) {
		Parent parent = parentLoadPort.findParentBySocialIdAndProvider(sub, Provider.APPLE)
			.orElse(null);

		if (parent == null) {
			log.warn("Apple consent-revoked 이벤트: 해당 사용자를 찾을 수 없습니다. sub={}", sub);
			return;
		}

		parent.updateAppleRefreshToken(null);
		parentSavePort.save(parent);
		tokenCommandPort.deleteRefreshToken(parent.getId(), Role.PARENT);
		log.info("Apple 연동 해제 처리 완료: parentId={}", parent.getId());
	}

	private void handleAccountDelete(String sub) {
		Parent parent = parentLoadPort.findParentBySocialIdAndProvider(sub, Provider.APPLE)
			.orElse(null);

		if (parent == null) {
			log.warn("Apple account-delete 이벤트: 해당 사용자를 찾을 수 없습니다. sub={}", sub);
			return;
		}

		parentWithdrawUseCase.withdraw(parent.getId());
		log.info("Apple 계정 삭제 처리 완료: parentId={}", parent.getId());
	}
}
