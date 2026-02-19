package com.kiero.feeds.application.port.out;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;

import com.kiero.feeds.domain.FeedItem;

public interface FeedItemQueryPort {

	List<FeedItem> findByCursor(
		Long parentId,
		Long childId,
		LocalDateTime cursorOccurredAt,
		Long cursorId,
		Pageable pageable
	);
}
