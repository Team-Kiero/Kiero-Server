package com.kiero.coupon.application.service;

import java.time.LocalDate;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CouponCacheEvictHelper {

	private static final String CACHE_NAME = "coupons";

	private final CacheManager cacheManager;

	public void evictByChildId(Long childId) {
		// 현재 스레드에 활성화된 트랜잭션이 있는지 확인
		if (TransactionSynchronizationManager.isActualTransactionActive()) {
			// 트랜잭션이 커밋 성공했을 때 콜백(doEvict) 실행
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					doEvict(childId);
				}
			});
		} else {
			doEvict(childId);
		}
	}

	private void doEvict(Long childId) {
		Cache cache = cacheManager.getCache(CACHE_NAME);
		if (cache != null) {
			cache.evict(childId + ":" + LocalDate.now());
		}
	}
}
