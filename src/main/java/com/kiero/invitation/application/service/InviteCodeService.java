package com.kiero.invitation.application.service;

// ------ 앱 심사용 로직 시작 ------

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.global.exception.KieroException;
import com.kiero.invitation.application.exception.InvitationErrorCode;
import com.kiero.invitation.application.port.in.InviteCodeUseCase;
import com.kiero.invitation.application.port.out.InviteCodeCommandPort;
import com.kiero.invitation.application.port.out.InviteCodeGeneratorPort;
import com.kiero.invitation.application.port.out.InviteCodeQueryPort;
import com.kiero.invitation.application.port.out.InviteLockPort;
import com.kiero.invitation.domain.InviteCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InviteCodeService implements InviteCodeUseCase {

    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private final InviteCodeQueryPort inviteCodeQueryPort;
    private final InviteCodeCommandPort inviteCodeCommandPort;
    private final InviteCodeGeneratorPort inviteCodeGeneratorPort;
    private final InviteLockPort inviteLockPort;

    // ------ 앱 심사용 로직 시작 ------
    @Value("${review.invite-code:}")
    private String reviewInviteCode;

    @Value("${review.parent-id:0}")
    private Long reviewParentId;

    @Value("${review.child-last-name:}")
    private String reviewChildLastName;

    @Value("${review.child-first-name:}")
    private String reviewChildFirstName;
    // ------ 앱 심사용 로직 종료 ------
    @Override
    @Transactional
    public String createInviteCode(Long parentId, String childLastName, String childFirstName) {
        inviteCodeQueryPort.findByParentKey(parentId.toString())
            .ifPresent(existing -> {
                inviteCodeCommandPort.deleteByCode(existing.getCode());
                log.info("Invalidated previous invite code: {}, parentId: {}, childName: {} {}",
                    existing.getCode(), parentId, existing.getChildLastName(), existing.getChildFirstName());
            });

        String code = generateUniqueCode();

        InviteCode inviteCode = InviteCode.of(code, parentId, childLastName, childFirstName);
        inviteCodeCommandPort.save(inviteCode);

        log.info("Created invite code: {}, parentId: {}, childName: {} {}",
            code, parentId, childLastName, childFirstName);

        return code;
    }

    @Override
    @Transactional
    public InviteCode validateAndConsume(String code, String inputLastName, String inputFirstName) {
        // ------ 앱 심사용 로직 시작 ------
        if (isReviewInviteCode(code)) {
            if (!reviewChildLastName.equals(inputLastName) || !reviewChildFirstName.equals(inputFirstName)) {
                throw new KieroException(InvitationErrorCode.INVITE_CODE_NAME_MISMATCH);
            }
            log.info("Review invite code used (not consumed): parentId={}", reviewParentId);
            return InviteCode.of(reviewInviteCode, reviewParentId, reviewChildLastName, reviewChildFirstName);
        }
        // ------ 앱 심사용 로직 종료 ------
        // 핵심: 락 잡고 -> 검증 -> 삭제(consume)
        return inviteLockPort.withInviteCodeLock(code, () -> {

            InviteCode inviteCode = inviteCodeQueryPort.findByCode(code)
                .orElseThrow(() -> new KieroException(InvitationErrorCode.INVALID_OR_EXPIRED_INVITE_CODE));

            if (!inviteCode.getChildLastName().equals(inputLastName)
                || !inviteCode.getChildFirstName().equals(inputFirstName)) {
                throw new KieroException(InvitationErrorCode.INVITE_CODE_NAME_MISMATCH);
            }

            log.info("Retrieved invite code: code={}, parentId={}, childName={} {}",
                code, inviteCode.getParentId(), inviteCode.getChildLastName(), inviteCode.getChildFirstName());

            inviteCodeCommandPort.deleteByCode(code);

            log.info("Deleted invite code: {}", code);

            return inviteCode;
        });
    }

    // ------ 앱 심사용 로직 시작 ------
    private boolean isReviewInviteCode(String code) {
        return reviewInviteCode != null && !reviewInviteCode.isBlank() && reviewInviteCode.equals(code);
    }
    // ------ 앱 심사용 로직 종료 ------
    private String generateUniqueCode() {
        for (int i = 0; i < MAX_GENERATION_ATTEMPTS; i++) {
            String code = inviteCodeGeneratorPort.generate();
            if (!inviteCodeQueryPort.existsByCode(code)) {
                return code;
            }
        }
        throw new KieroException(InvitationErrorCode.INVITE_CODE_GENERATION_FAILED);
    }
}