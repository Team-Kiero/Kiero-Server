package com.kiero.invitation.service;

import com.kiero.global.exception.KieroException;
import com.kiero.invitation.domain.InviteCode;
import com.kiero.invitation.exception.InvitationErrorCode;
import com.kiero.invitation.repository.InviteCodeRepository;
import com.kiero.invitation.util.InviteCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InviteCodeService {

    private final InviteCodeRepository inviteCodeRepository;
    private final InviteCodeGenerator inviteCodeGenerator;
    private static final int MAX_GENERATION_ATTEMPTS = 10;

    @Transactional
    public String createInviteCode(Long parentId, String childName) {
        String code = generateUniqueCode();

        InviteCode inviteCode = InviteCode.of(code, parentId, childName);
        inviteCodeRepository.save(inviteCode);

        return code;
    }

    public InviteCode validateCodeAndGetName(String code, String inputChildName) {
        InviteCode inviteCode = inviteCodeRepository.findById(code)
                .orElseThrow(() -> new KieroException(InvitationErrorCode.INVALID_OR_EXPIRED_INVITE_CODE));

        if (!inviteCode.getChildName().equals(inputChildName)) {
            throw new KieroException(InvitationErrorCode.INVITE_CODE_NAME_MISMATCH);
        }

        return inviteCode;
    }

    public void deleteInviteCode(String code) {
        inviteCodeRepository.deleteById(code);
    }

    private String generateUniqueCode() {
        for (int i = 0; i < MAX_GENERATION_ATTEMPTS; i++) {
            String code = inviteCodeGenerator.generate();

            if (!inviteCodeRepository.existsById(code)) {
                return code;
            }
        }

        throw new KieroException(InvitationErrorCode.INVITE_CODE_GENERATION_FAILED);
    }

}
