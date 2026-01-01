package com.kiero.child.service;

import com.kiero.child.domain.Child;
import com.kiero.child.presentation.dto.ChildLoginResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // 1. 초대 코드 검증
        InviteCode inviteCode = inviteCodeService.validateCodeAndGetName(
                request.inviteCode(),
                request.name()
        );

        // 2. 부모 엔티티 조회
        Parent parent = parentRepository.findById(inviteCode.getParentId())
                .orElseThrow(() -> new KieroException(InvitationErrorCode.PARENT_NOT_FOUND));

        // 3. 아이 엔티티 생성
        Child child = Child.create(request.name(), Role.CHILD);
        Child savedChild = childRepository.save(child);

        // 4. ParentChild 관계 생성
        ParentChild parentChild = ParentChild.create(parent, savedChild);
        parentChildRepository.save(parentChild);

        // 5. 사용한 초대 코드 삭제
        inviteCodeService.deleteInviteCode(request.inviteCode());

        // 6. 토큰 발급 및 로그인 응답 반환
        return authService.generateLoginResponse(savedChild);
    }
}
