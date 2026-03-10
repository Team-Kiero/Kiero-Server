package com.kiero.feed.application.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;

import com.kiero.feed.domain.FeedItem;
import com.kiero.feed.domain.enums.EventType;

public interface FeedItemQueryPort {

	List<FeedItem> findByCursor(
		Long parentId,
		Long childId,
		LocalDateTime cursorOccurredAt,
		Long cursorId,
		Pageable pageable
	);

	List<Long> findUnreadItemIdsByParentIdAndChildId(Long parentId, Long childId);

	Optional<FeedItem> findByParentIdAndScheduleDetailIdAndEventType(Long parentId, Long scheduleDetailId, EventType eventType);
}
