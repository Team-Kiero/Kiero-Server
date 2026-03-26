package com.kiero.schedule.adapter.in.event;

import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.kiero.schedule.application.dto.FireLitEvent;
import com.kiero.schedule.application.dto.ScheduleCacheEvent;
import com.kiero.schedule.application.dto.ScheduleModifiedEvent;
import com.kiero.schedule.application.dto.ScheduleStatusUpdatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleCacheEvictListener {

	private static final String CACHE_KEY_PREFIX = "schedules::";

	private final StringRedisTemplate stringRedisTemplate;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ScheduleCacheEvent event) {
		evictByChildId(event.childId());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ScheduleModifiedEvent event) {
		evictByChildId(event.childId());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ScheduleStatusUpdatedEvent event) {
		evictByChildId(event.childId());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(FireLitEvent event) { evictByChildId(event.childId());}

	private void evictByChildId(Long childId) {
		String pattern = CACHE_KEY_PREFIX + childId + ":*";
		Set<String> keys = stringRedisTemplate.keys(pattern);
		if (!keys.isEmpty()) {
			stringRedisTemplate.delete(keys);
			log.debug("[ScheduleCacheEvict] childId={} 캐시 {}건 삭제", childId, keys.size());
		}
	}
}
