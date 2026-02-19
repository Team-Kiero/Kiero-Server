package com.kiero.invitation.adapter.out.lock;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import com.kiero.global.exception.KieroException;
import com.kiero.invitation.application.exception.InvitationErrorCode;
import com.kiero.invitation.application.port.out.InviteLockPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedissonInviteLockAdapter implements InviteLockPort {

	private final RedissonClient redissonClient;

	private static final long WAIT_SEC = 3L;
	private static final long LEASE_SEC = 5L;

	@Override
	public <T> T withInviteCodeLock(String code, Callable<T> action) {
		String lockKey = "lock:invite:" + code;
		RLock lock = redissonClient.getLock(lockKey);

		try {
			boolean acquired = lock.tryLock(WAIT_SEC, LEASE_SEC, TimeUnit.SECONDS);
			if (!acquired) {
				throw new KieroException(InvitationErrorCode.INVITE_CODE_PROCESSING);
			}
			return action.call();

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new KieroException(InvitationErrorCode.INVITE_CODE_PROCESSING);

		} catch (KieroException e) {
			throw e;

		} catch (Exception e) {
			throw new KieroException(InvitationErrorCode.INVITE_CODE_PROCESSING);

		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}
}