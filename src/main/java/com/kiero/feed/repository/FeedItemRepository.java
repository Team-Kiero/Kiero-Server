package com.kiero.feed.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kiero.feed.domain.FeedItem;

@Repository
public interface FeedItemRepository extends JpaRepository<FeedItem, Long> {

	@Query("""
		select f
		from FeedItem f
		where f.parent.id = :parentId
		and f.child.id = :childId
	    and (
				:cursorOccurredAt is null
				or f.occurredAt < :cursorOccurredAt
				or (f.occurredAt = :cursorOccurredAt and f.id < :cursorId)
		  )
		order by f.occurredAt desc, f.id desc
""")
	List<FeedItem> findFeedItemsByCursor(
		@Param("parentId") Long parentId,
		@Param("childId") Long childId,
		@Param("cursorOccurredAt") LocalDateTime cursorOccurredAt,
		@Param("cursorId") Long cursorId,
		Pageable pageable
	);

	/*
	데모데이용 임시 메서드
	 */
	void deleteByChildIdIn(List<Long> childIds);
	/*
	 */
}
