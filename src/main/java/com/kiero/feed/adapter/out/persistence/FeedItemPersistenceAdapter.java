package com.kiero.feed.adapter.out.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.kiero.feed.application.port.out.FeedItemCommandPort;
import com.kiero.feed.application.port.out.FeedItemTransferPort;
import com.kiero.feed.application.port.out.FeedItemDeletePort;
import com.kiero.feed.application.port.out.FeedItemQueryPort;
import com.kiero.feed.domain.FeedItem;
import com.kiero.feed.domain.enums.EventType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FeedItemPersistenceAdapter implements FeedItemCommandPort, FeedItemQueryPort, FeedItemDeletePort, FeedItemTransferPort {

	private final FeedItemRepository feedItemRepository;

	@Override
	public List<FeedItem> saveAll(List<FeedItem> feedItems) {
		return feedItemRepository.saveAll(feedItems);
	}

	@Override
	public List<FeedItem> findByCursor(
		Long parentId,
		Long childId,
		LocalDateTime cursorOccurredAt,
		Long cursorId,
		Pageable pageable
	) {
		return feedItemRepository.findFeedItemsByCursor(
			parentId,
			childId,
			cursorOccurredAt,
			cursorId,
			pageable
		);
	}

	@Override
	public List<Long> findUnreadItemIdsByParentIdAndChildId(Long parentId, Long childId) {
		return feedItemRepository.findUnreadItemIdsByParentIdAndChildId(parentId, childId);
	}

	@Override
	public void markAllAsRead(List<Long> itemIds) {
		feedItemRepository.markAllAsRead(itemIds);
	}

	@Override
	public Optional<FeedItem> findByParentIdAndScheduleDetailIdAndEventType(Long parentId, Long scheduleDetailId, EventType eventType) {
		return feedItemRepository.findByParentIdAndScheduleDetailIdAndEventType(parentId, String.valueOf(scheduleDetailId), eventType.name());
	}

	@Override
	public Optional<FeedItem> findByParentIdAndMissionId(Long parentId, Long missionId) {
		return feedItemRepository.findByParentIdAndMissionId(parentId, String.valueOf(missionId));
	}

	@Override
	public Optional<FeedItem> findByParentIdAndCouponId(Long parentId, Long couponId) {
		return feedItemRepository.findByParentIdAndCouponId(parentId, String.valueOf(couponId));
	}

	@Override
	public Optional<FeedItem> findByParentAndChildComplete(Long parentId, Long childId, LocalDate date) {
		return feedItemRepository.findByParentAndChildComplete(parentId, childId, date);
	}

	@Override
	public List<FeedItem> findUnreadFeedItem(Long parentId) {
		return feedItemRepository.findUnreadFeedItemByParentId(parentId);
	}

	@Override
	public void deleteAllByChildId(Long childId) {
		feedItemRepository.deleteAllByChildId(childId);
	}

	@Override
	public void transferOwnership(Long fromParentId, Long toParentId, Long childId) {
		feedItemRepository.transferOwnershipByParentIdAndChildId(fromParentId, toParentId, childId);
	}
}
