package com.kiero.child.application.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.application.dto.ChildJoinedEvent;
import com.kiero.child.application.dto.ChildLoginRequest;
import com.kiero.child.application.dto.ChildLoginResponse;
import com.kiero.child.application.dto.ChildMeResponse;
import com.kiero.child.application.exception.ChildErrorCode;
import com.kiero.child.application.port.in.ChildLoginUseCase;
import com.kiero.child.application.port.in.ChildQueryUseCase;
import com.kiero.child.application.port.out.AuthGeneratePort;
import com.kiero.child.application.port.out.ChildJoinedEventPort;
import com.kiero.child.application.port.out.ChildLoadPort;
import com.kiero.child.application.port.out.ChildSavePort;
import com.kiero.child.domain.Child;
import com.kiero.global.auth.enums.Role;
import com.kiero.global.exception.KieroException;
import com.kiero.invitation.application.exception.InvitationErrorCode;
import com.kiero.invitation.application.port.in.InviteCodeUseCase;
import com.kiero.invitation.domain.InviteCode;
import com.kiero.parent.application.port.in.ParentChildSaveUseCase;
import com.kiero.parent.application.port.out.ParentLoadPort;
import com.kiero.parent.domain.Parent;
import com.kiero.parent.domain.ParentChild;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChildService implements ChildLoginUseCase, ChildQueryUseCase {

	private final InviteCodeUseCase inviteCodeUseCase;
	private final ParentChildSaveUseCase parentChildSaveUseCase;

	private final ParentLoadPort parentLoadPort;

	private final ChildSavePort childSavePort;
	private final ChildLoadPort childLoadPort;
	private final AuthGeneratePort authGeneratePort;
	private final ChildJoinedEventPort childJoinedEventPort;

	@Override
	@Transactional
	public ChildLoginResponse login(ChildLoginRequest request) {
		log.info("Child login started: inviteCode={}, childName={} {}",
			request.inviteCode(), request.lastName(), request.firstName());

		// 1. 초대 코드 검증 및 삭제 (분산 락으로 원자적 처리)
		InviteCode inviteCode = inviteCodeUseCase.validateAndConsume(
			request.inviteCode(),
			request.lastName(),
			request.firstName()
		);

		// 2. 부모 엔티티 조회
		Parent parent = parentLoadPort.findById(inviteCode.getParentId())
			.orElseThrow(() -> {
				log.error("Parent not found in DB with ID: {}", inviteCode.getParentId());
				return new KieroException(InvitationErrorCode.PARENT_NOT_FOUND);
			});

		// 3. 해당 부모 하위에 동일 이름의 아이가 이미 존재하면 로그인, 없으면 신규 생성
		Child child = childLoadPort.findByParentIdAndName(parent.getId(), request.lastName(), request.firstName())
			.orElseGet(() -> {
				Child newChild = Child.create(request.lastName(), request.firstName(), Role.CHILD);
				Child savedChild = childSavePort.save(newChild);

				ParentChild parentChild = ParentChild.create(parent, savedChild);
				parentChildSaveUseCase.save(parentChild);

				log.info("New child created: childId={}, childName={}", savedChild.getId(), savedChild.getFullName());

				return savedChild;
			});

		// 4. 신규 가입 / 재로그인 모두 부모에게 SSE 알림 발행
		childJoinedEventPort.publish(new ChildJoinedEvent(parent.getId(), child.getId()));

		return authGeneratePort.generateLoginResponse(child);
	}

	@Override
	@Transactional(readOnly = true)
	public ChildMeResponse getMyInfo(Long childId) {
		Child child = childLoadPort.findById(childId)
			.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

		LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

		log.info("Retrieved child info: childId={}, name={}, coinAmount={}",
			child.getId(), child.getFullName(), child.getCoinAmount());

		return ChildMeResponse.from(child, today);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Child> findByIdWithLock(Long childId) {
		return childLoadPort.findByIdWithLock(childId);
	}


	@Override
	public Optional<Child> findById(Long childId) { return childLoadPort.findById(childId); }
}
