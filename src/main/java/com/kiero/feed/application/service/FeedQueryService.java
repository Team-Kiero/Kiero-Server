package com.kiero.feed.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.application.exception.ChildErrorCode;
import com.kiero.child.application.port.in.ChildByIdUseCase;
import com.kiero.child.application.port.in.ChildQueryUseCase;
import com.kiero.child.domain.Child;
import com.kiero.feed.application.dto.FeedCursor;
import com.kiero.feed.application.dto.FeedGetResponse;
import com.kiero.feed.application.dto.FeedItemDto;
import com.kiero.feed.application.port.in.FeedQueryUseCase;
import com.kiero.feed.application.port.out.FeedItemQueryPort;
import com.kiero.feed.domain.FeedItem;
import com.kiero.global.exception.KieroException;
import com.kiero.parent.application.exception.ParentErrorCode;
import com.kiero.parent.application.port.in.ParentChildAccessUseCase;
import com.kiero.parent.application.port.in.ParentChildQueryUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedQueryService implements FeedQueryUseCase {

	private final FeedItemQueryPort feedItemQueryPort;

	private final ParentChildAccessUseCase parentChildAccessUseCase;
	private final ChildByIdUseCase childByIdUseCase;

	@Override
	@Transactional(readOnly = true)
	public FeedGetResponse getFeed(Long parentId, Long childId, Integer size, String cursor) {

		Child child = childByIdUseCase.findById(childId)
			.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

		isParentChildValid(parentId, childId);

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

	private void isParentChildValid(Long parentId, Long childId) {
		boolean parentChildExists = parentChildAccessUseCase.existsByParentIdAndChildId(parentId, childId);
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
