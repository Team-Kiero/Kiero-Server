package com.kiero.feed.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService {

	private final FeedItemRepository feedItemRepository;
	private final ParentChildRepository parentChildRepository;
	private final ChildRepository childRepository;

	private final EntityManager em;
	private final ResourceLoader resourceLoader;

	@Transactional(readOnly = true)
	public FeedGetResponse getFeed(Long parentId, Long childId, Integer size, String cursor) {

		Child child = childRepository.findById(childId)
			.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

		isParentChildValid(parentId, childId);

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
		if (hasNext)
			feedItems = feedItems.subList(0, size);

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

	public void isParentChildValid(Long parentId, Long childId) {
		boolean parentChildExists = parentChildRepository.existsByParentIdAndChildId(parentId, childId);
		if (!parentChildExists)
			throw new KieroException(ParentErrorCode.NOT_ALLOWED_TO_CHILD);
	}

	/*
	데모데이용 임시 메서드
	 */
	@Transactional
	public void deleteFeedsByChildIds(List<Long> childIds) {
		feedItemRepository.deleteByChildIdIn(childIds);
	}
	/*
	 */

	/*
	솝트 데모데이 때 더미데이터를 넣기 위한 메서드
	 */
	@Transactional
	public void insertDummy(List<Long> parentIds, Long childId, String env) {
		String sqlPath = "sql/" + env + "_feed_item_insert_dummy.sql";
		String sql = loadSql(sqlPath);

		Query q = em.createNativeQuery(sql);

		for (Long parentId : parentIds) {
			q.setParameter("parentId", parentId);
			q.setParameter("childId", childId);
			q.executeUpdate();
		}
	}

	private String loadSql(String path) {
		try {
			Resource resource = resourceLoader.getResource("classpath:" + path);
			return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("더미 SQL 로딩 실패", e);
		}
	}
	/*
	 */

	private FeedItemDto toItemDto(FeedItem feedItem) {
		return new FeedItemDto(
			feedItem.getEventType(),
			feedItem.getCreatedAt(),
			feedItem.getMetadata()
		);
	}
}
