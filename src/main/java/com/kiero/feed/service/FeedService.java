package com.kiero.feed.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.domain.Child;
import com.kiero.child.exception.ChildErrorCode;
import com.kiero.child.repository.ChildRepository;
import com.kiero.feed.domain.FeedItem;
import com.kiero.feed.presentation.dto.FeedCursor;
import com.kiero.feed.presentation.dto.FeedGetResponse;
import com.kiero.feed.presentation.dto.FeedItemDto;
import com.kiero.feed.repository.FeedItemRepository;
import com.kiero.global.exception.KieroException;
import com.kiero.parent.exception.ParentErrorCode;
import com.kiero.parent.repository.ParentChildRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedService {

	private final FeedItemRepository feedItemRepository;
	private final ParentChildRepository parentChildRepository;
	private final ChildRepository childRepository;

	@Transactional(readOnly = true)
	public FeedGetResponse getFeed(Long parentId, Long childId, Integer size, String cursor) {

		boolean parentChildExists = parentChildRepository.existsByParentIdAndChildId(parentId, childId);
		if (!parentChildExists) throw new KieroException(ParentErrorCode.NOT_ALLOWED_TO_CHILD);

		FeedCursor feedCursor = FeedCursor.parse(cursor);

		LocalDateTime cursorOccurredAt = (feedCursor == null ? null : feedCursor.occurredAt());
		Long cursorId = (feedCursor == null ? null : feedCursor.id());

		List<FeedItem> feedItems = feedItemRepository.findFeedItemsByCursor(
			parentId,
			childId,
			cursorOccurredAt,
			cursorId,
			PageRequest.of(0, size + 1)
		);

		boolean hasNext = feedItems.size() > size;
		if (hasNext) feedItems = feedItems.subList(0, size);

		List<FeedItemDto> items = feedItems.stream()
			.map(this::toItemDto)
			.toList();

		String nextCursor = null;
		if (hasNext && !feedItems.isEmpty()) {
			FeedItem lastFeedItem = feedItems.get(feedItems.size() - 1);
			nextCursor = new FeedCursor(lastFeedItem.getOccurredAt(), lastFeedItem.getId()).toCursorString();
		}

		return new FeedGetResponse(items, nextCursor);
	}

	private FeedItemDto toItemDto(FeedItem feedItem) {
		return new FeedItemDto(
			feedItem.getEventType(),
			feedItem.getCreatedAt(),
			feedItem.getMetadata()
		);
	}
}
