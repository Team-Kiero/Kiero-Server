package com.kiero.child.service;

import com.kiero.child.domain.Child;
import com.kiero.child.exception.ChildErrorCode;
import com.kiero.child.presentation.dto.ChildLoginResponse;
import com.kiero.child.presentation.dto.ChildMeResponse;
import com.kiero.child.presentation.dto.ChildSignupRequest;
import com.kiero.child.repository.ChildRepository;
import com.kiero.global.auth.enums.Role;
import com.kiero.global.auth.jwt.service.AuthService;
import com.kiero.global.exception.KieroException;
import com.kiero.invitation.domain.InviteCode;
import com.kiero.invitation.exception.InvitationErrorCode;
import com.kiero.invitation.service.InviteCodeService;
import com.kiero.parent.domain.Parent;
import com.kiero.parent.domain.ParentChild;
import com.kiero.parent.repository.ParentChildRepository;
import com.kiero.parent.repository.ParentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChildService {

    private final InviteCodeService inviteCodeService;
    private final ParentRepository parentRepository;
    private final ChildRepository childRepository;
    private final ParentChildRepository parentChildRepository;
    private final AuthService authService;

    @Transactional
    public ChildLoginResponse signup(ChildSignupRequest request) {
        log.info("Child signup started: inviteCode={}, childName={}", request.inviteCode(), request.name());

        // 1. 초대 코드 검증 및 삭제 (분산 락으로 원자적 처리)
        InviteCode inviteCode = inviteCodeService.validateAndDeleteWithLock(
                request.inviteCode(),
                request.name()
        );

        log.info("Invite code validated. Searching for parent with ID: {}", inviteCode.getParentId());

        // 2. 부모 엔티티 조회
        Parent parent = parentRepository.findById(inviteCode.getParentId())
                .orElseThrow(() -> {
                    log.error("Parent not found in DB with ID: {}", inviteCode.getParentId());
                    return new KieroException(InvitationErrorCode.PARENT_NOT_FOUND);
                });

        log.info("Parent found: parentId={}, parentName={}", parent.getId(), parent.getName());

        // 3. 아이 엔티티 생성
        Child child = Child.create(request.name(), Role.CHILD);
        Child savedChild = childRepository.save(child);

        log.info("Child created: childId={}, childName={}", savedChild.getId(), savedChild.getName());

        // 4. ParentChild 관계 생성
        ParentChild parentChild = ParentChild.create(parent, savedChild);
        parentChildRepository.save(parentChild);

        log.info("ParentChild relationship created: parentId={}, childId={}", parent.getId(), savedChild.getId());

        // 5. 토큰 발급 및 로그인 응답 반환
        return authService.generateLoginResponse(savedChild);
    }

    @Transactional(readOnly = true)
    public ChildMeResponse getMyInfo(Long childId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

        log.info("Retrieved child info: childId={}, name={}, coinAmount={}",
                child.getId(), child.getName(), child.getCoinAmount());

        return ChildMeResponse.from(child);
    }
}
