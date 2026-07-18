package com.kiero.feed.application.port.out;

import java.time.LocalDate;
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

	Optional<FeedItem> findByParentIdAndMissionId(Long parentId, Long missionId);

	Optional<FeedItem> findByParentIdAndCouponId(Long parentId, Long couponId);

	Optional<FeedItem> findByParentAndChildComplete(Long parentId, Long childId, LocalDate date);

	List<FeedItem> findUnreadFeedItem(Long parentId);
}
