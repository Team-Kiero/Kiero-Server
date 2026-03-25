package com.kiero.mission.application.service;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MissionCacheEvictHelper {

	private static final String CACHE_NAME = "missions";

	private final CacheManager cacheManager;

	public void evictByChildId(Long childId) {
		Cache cache = cacheManager.getCache(CACHE_NAME);
		if (cache != null) {
			cache.evict(childId);
		}
	}
}
