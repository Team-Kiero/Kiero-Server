package com.kiero.coupon.application.service;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CouponCacheEvictHelper {

	private static final String CACHE_NAME = "coupons";

	private final CacheManager cacheManager;

	public void evictByChildId(Long childId) {
		Cache cache = cacheManager.getCache(CACHE_NAME);
		if (cache != null) {
			cache.evict(childId);
		}
	}
}
