package com.kiero.child.adapter.out.redis;

import org.springframework.stereotype.Component;

import com.kiero.child.application.port.out.ParentWithdrawCheckPort;
import com.kiero.parent.adapter.out.redis.ParentWithdrawalRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ParentWithdrawCheckRedisAdapter implements ParentWithdrawCheckPort {

	private static final String KEY_PREFIX = "child:";

	private final ParentWithdrawalRepository parentWithdrawalRepository;

	@Override
	public boolean isParentWithdrawn(Long childId) {
		return parentWithdrawalRepository.existsById(KEY_PREFIX + childId);
	}
}
