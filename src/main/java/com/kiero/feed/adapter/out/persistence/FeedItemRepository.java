package com.kiero.feed.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

	@Query("""
		select f.id
		from FeedItem f
		where f.isRead = false
		and f.child.id = :childId
		and f.parent.id = :parentId
		""")
	List<Long> findUnreadItemIdsByParentIdAndChildId(
		@Param("parentId") Long parentId,
		@Param("childId") Long childId
	);


	@Modifying(clearAutomatically = true)
	@Query("""
		update FeedItem f
		set f.isRead = true
		where f.id in :itemIds
		""")
	void markAllAsRead(
		@Param("itemIds") List<Long> itemIds
	);

	@Query(value = """
		SELECT *
		FROM feed_item f
		WHERE f.parent_id = :parentId
		  AND f.event_type = :eventType
		  AND JSON_UNQUOTE(JSON_EXTRACT(f.metadata, '$.scheduleDetailId')) = :scheduleDetailId
		LIMIT 1
	""", nativeQuery = true)
	Optional<FeedItem> findByParentIdAndScheduleDetailIdAndEventType(
		@Param("parentId") Long parentId,
		@Param("scheduleDetailId") String scheduleDetailId,
		@Param("eventType") String eventType
	);

	@Query("""
		select f
		from FeedItem f
		where f.parent.id = :parentId
		and f.isRead = false
""")
	List<FeedItem> findUnreadFeedItemByParentId(
		@Param("parentId") Long parentId
	);

	@Query(value = """
		SELECT *
		FROM feed_item f
		WHERE f.parent_id = :parentId
		  AND f.event_type = 'MISSION'
		  AND JSON_UNQUOTE(JSON_EXTRACT(f.metadata, '$.missionId')) = :missionId
		LIMIT 1
	""", nativeQuery = true)
	Optional<FeedItem> findByParentIdAndMissionId(
		@Param("parentId") Long parentId,
		@Param("missionId") String missionId
	);

	@Query(value = """
		SELECT *
		FROM feed_item f
		WHERE f.parent_id = :parentId
		  AND f.event_type = 'COUPON'
		  AND JSON_UNQUOTE(JSON_EXTRACT(f.metadata, '$.couponId')) = :couponId
		LIMIT 1
	""", nativeQuery = true)
	Optional<FeedItem> findByParentIdAndCouponId(
		@Param("parentId") Long parentId,
		@Param("couponId") String couponId
	);

	@Query(value = """
		SELECT *
		FROM feed_item f
		WHERE f.parent_id = :parentId
		  AND f.child_id = :childId
		  AND f.event_type = 'COMPLETE'
		  AND DATE(f.occurred_at) = :date
		LIMIT 1
	""", nativeQuery = true)
	Optional<FeedItem> findByParentAndChildComplete(
		@Param("parentId") Long parentId,
		@Param("childId") Long childId,
		@Param("date") java.time.LocalDate date
	);

	@Modifying
	@Query("DELETE FROM FeedItem f WHERE f.child.id = :childId")
	void deleteAllByChildId(@Param("childId") Long childId);

	@Modifying
	@Query("DELETE FROM FeedItem f WHERE f.parent.id = :parentId")
	void deleteAllByParentId(@Param("parentId") Long parentId);

	@Modifying
	@Query(value = "UPDATE feed_item SET parent_id = :newParentId WHERE parent_id = :oldParentId AND child_id = :childId", nativeQuery = true)
	void transferOwnershipByParentIdAndChildId(@Param("oldParentId") Long oldParentId, @Param("newParentId") Long newParentId, @Param("childId") Long childId);
}
