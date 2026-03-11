package com.kiero.feed.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.application.exception.ChildErrorCode;
import com.kiero.child.application.port.out.ChildLoadPort;
import com.kiero.child.domain.Child;
import com.kiero.feed.application.dto.FeedCursor;
import com.kiero.feed.application.dto.FeedGetResponse;
import com.kiero.feed.application.dto.FeedHasUnreadResponse;
import com.kiero.feed.application.dto.FeedItemDto;
import com.kiero.feed.application.port.in.FeedQueryUseCase;
import com.kiero.feed.application.port.out.FeedItemQueryPort;
import com.kiero.feed.domain.FeedItem;
import com.kiero.global.exception.KieroException;
import com.kiero.parent.application.exception.ParentErrorCode;
import com.kiero.parent.application.port.out.ParentChildAccessPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedQueryService implements FeedQueryUseCase {

	private final FeedItemQueryPort feedItemQueryPort;

	private final ParentChildAccessPort parentChildAccessPort;
	private final ChildLoadPort childLoadPort;

	private final FeedCommandService feedCommandService;

	@Override
	@Transactional
	public FeedGetResponse getFeedAndMarkAsRead(Long parentId, Long childId, Integer size, String cursor) {

		Child child = childLoadPort.findById(childId)
			.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

		isParentChildValid(parentId, childId);

		// 읽지 않은 피드 알림들 읽음 처리
		List<Long> unreadItemIds = feedItemQueryPort.findUnreadItemIdsByParentIdAndChildId(parentId, childId);
		feedCommandService.markAllAsRead(unreadItemIds);

		FeedCursor feedCursor = FeedCursor.parse(cursor);

		LocalDateTime cursorOccurredAt = (feedCursor == null ? null : feedCursor.occurredAt());
		Long cursorId = (feedCursor == null ? null : feedCursor.id());

		List<FeedItem> feedItems = feedItemQueryPort.findByCursor(
			parentId,
			childId,
			cursorOccurredAt,
			cursorId,
			PageRequest.of(0, size + 1)
		);

		boolean hasNext = feedItems.size() > size;
		if (hasNext) {
			feedItems = feedItems.subList(0, size);
		}

		List<FeedItemDto> items = feedItems.stream()
			.map(this::toItemDto)
			.toList();

		String nextCursor = null;
		if (hasNext && !feedItems.isEmpty()) {
			FeedItem lastFeedItem = feedItems.get(feedItems.size() - 1);
			nextCursor = new FeedCursor(lastFeedItem.getOccurredAt(), lastFeedItem.getId()).toCursorString();
		}

		return new FeedGetResponse(child.getFirstName(), items, nextCursor);
	}

	public FeedHasUnreadResponse getHasUnread(Long parentId) {
		List<FeedItem> unreadFeedItem = feedItemQueryPort.findUnreadFeedItem(parentId);

		boolean hasUnread = !unreadFeedItem.isEmpty();

		List<Long> childIds = unreadFeedItem.stream()
			.map(feedItem -> feedItem.getChild().getId())
			.distinct()
			.toList();

		return new FeedHasUnreadResponse(hasUnread, childIds);
	}

	private void isParentChildValid(Long parentId, Long childId) {
		boolean parentChildExists = parentChildAccessPort.existsByParentIdAndChildId(parentId, childId);
		if (!parentChildExists) {
			throw new KieroException(ParentErrorCode.NOT_ALLOWED_TO_CHILD);
		}
	}

	private FeedItemDto toItemDto(FeedItem feedItem) {
		return new FeedItemDto(
			feedItem.getEventType(),
			feedItem.getOccurredAt(),
			feedItem.getMetadata()
		);
	}
}
