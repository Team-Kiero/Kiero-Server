package com.kiero.invitation.service;

import com.kiero.global.exception.KieroException;
import com.kiero.invitation.domain.InviteCode;
import com.kiero.invitation.exception.InvitationErrorCode;
import com.kiero.invitation.repository.InviteCodeRepository;
import com.kiero.invitation.util.InviteCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class InviteCodeService {

    private final InviteCodeRepository inviteCodeRepository;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final RedissonClient redissonClient;
    private static final int MAX_GENERATION_ATTEMPTS = 10;

    @Transactional
    public String createInviteCode(Long parentId, String childLastName, String childFirstName) {
        String code = generateUniqueCode();

        InviteCode inviteCode = InviteCode.of(code, parentId, childLastName, childFirstName);
        inviteCodeRepository.save(inviteCode);

        log.info("Created invite code: {}, parentId: {}, childName: {} {}", code, parentId, childLastName, childFirstName);

        return code;
    }

    public InviteCode validateAndDeleteWithLock(String code, String inputLastName, String inputFirstName) {
        String lockKey = "lock:invite:" + code;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 락 획득 시도: 3초 대기, 5초 유지
            boolean acquired = lock.tryLock(3, 5, TimeUnit.SECONDS);
            if (!acquired) {
                throw new KieroException(InvitationErrorCode.INVITE_CODE_PROCESSING);
            }

            // 검증
            InviteCode inviteCode = validateCodeAndGetName(code, inputLastName, inputFirstName);

            log.info("Retrieved invite code from Redis: code={}, parentId={}, childName={} {}",
                    code, inviteCode.getParentId(), inviteCode.getChildLastName(), inviteCode.getChildFirstName());

            // 삭제
            deleteInviteCode(code);

            log.info("Deleted invite code from Redis: {}", code);

            return inviteCode;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KieroException(InvitationErrorCode.INVITE_CODE_PROCESSING);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private InviteCode validateCodeAndGetName(String code, String inputLastName, String inputFirstName) {
        InviteCode inviteCode = inviteCodeRepository.findById(code)
                .orElseThrow(() -> new KieroException(InvitationErrorCode.INVALID_OR_EXPIRED_INVITE_CODE));

        if (!inviteCode.getChildLastName().equals(inputLastName) || !inviteCode.getChildFirstName().equals(inputFirstName)) {
            throw new KieroException(InvitationErrorCode.INVITE_CODE_NAME_MISMATCH);
        }

        return inviteCode;
    }

    private void deleteInviteCode(String code) {
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
