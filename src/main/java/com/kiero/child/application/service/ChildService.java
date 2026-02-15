package com.kiero.child.application.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.application.exception.ChildErrorCode;
import com.kiero.child.application.dto.ChildJoinedEvent;
import com.kiero.child.application.dto.ChildLoginResponse;
import com.kiero.child.application.dto.ChildMeResponse;
import com.kiero.child.application.dto.ChildSignupRequest;
import com.kiero.child.application.port.in.ChildMeUseCase;
import com.kiero.child.application.port.in.ChildQueryUseCase;
import com.kiero.child.application.port.in.ChildSignupUseCase;
import com.kiero.child.application.port.out.AuthGeneratePort;
import com.kiero.child.application.port.out.ChildJoinedEventPort;
import com.kiero.child.application.port.out.ChildPersistencePort;
import com.kiero.child.application.port.out.InviteCodeValidatePort;
import com.kiero.child.application.port.out.ParentChildPersistencePort;
import com.kiero.child.application.port.out.ParentLoadPort;
import com.kiero.child.domain.Child;
import com.kiero.global.auth.enums.Role;
import com.kiero.global.exception.KieroException;
import com.kiero.invitation.domain.InviteCode;
import com.kiero.invitation.exception.InvitationErrorCode;
import com.kiero.parent.domain.Parent;
import com.kiero.parent.domain.ParentChild;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChildService implements ChildSignupUseCase, ChildMeUseCase, ChildQueryUseCase {

	private final InviteCodeValidatePort inviteCodeValidatePort;
	private final ParentLoadPort parentLoadPort;
	private final ChildPersistencePort childPersistencePort;
	private final ParentChildPersistencePort parentChildPersistencePort;
	private final AuthGeneratePort authGeneratePort;
	private final ChildJoinedEventPort childJoinedEventPort;

	@Override
	@Transactional
	public ChildLoginResponse signup(ChildSignupRequest request) {
		log.info("Child signup started: inviteCode={}, childName={} {}",
			request.inviteCode(), request.lastName(), request.firstName());

		// 1. 초대 코드 검증 및 삭제 (분산 락으로 원자적 처리)
		InviteCode inviteCode = inviteCodeValidatePort.validateAndDeleteWithLock(
			request.inviteCode(),
			request.lastName(),
			request.firstName()
		);

		log.info("Invite code validated. Searching for parent with ID: {}", inviteCode.getParentId());

		// 2. 부모 엔티티 조회
		Parent parent = parentLoadPort.findById(inviteCode.getParentId())
			.orElseThrow(() -> {
				log.error("Parent not found in DB with ID: {}", inviteCode.getParentId());
				return new KieroException(InvitationErrorCode.PARENT_NOT_FOUND);
			});

		log.info("Parent found: parentId={}, parentName={}", parent.getId(), parent.getName());

		// 3. 아이 엔티티 생성
		Child child = Child.create(request.lastName(), request.firstName(), Role.CHILD);
		Child savedChild = childPersistencePort.save(child);

		log.info("Child created: childId={}, childName={}", savedChild.getId(), savedChild.getFullName());

		// 4. ParentChild 관계 생성
		ParentChild parentChild = ParentChild.create(parent, savedChild);
		parentChildPersistencePort.save(parentChild);

		log.info("ParentChild relationship created: parentId={}, childId={}", parent.getId(), savedChild.getId());

		childJoinedEventPort.publish(new ChildJoinedEvent(
			parent.getId(),
			savedChild.getId()
		));

		// 5. 토큰 발급 및 로그인 응답 반환
		return authGeneratePort.generateLoginResponse(savedChild);
	}

	@Override
	@Transactional(readOnly = true)
	public ChildMeResponse getMyInfo(Long childId) {
		Child child = childPersistencePort.findById(childId)
			.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

		LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

		log.info("Retrieved child info: childId={}, name={}, coinAmount={}",
			child.getId(), child.getFullName(), child.getCoinAmount());

		return ChildMeResponse.from(child, today);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Child> findByIdWithLock(Long childId) {
		return childPersistencePort.findByIdWithLock(childId);
	}

}
