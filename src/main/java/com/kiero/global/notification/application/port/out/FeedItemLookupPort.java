package com.kiero.global.notification.application.port.out;

import java.time.LocalDate;
import java.util.Optional;

public interface FeedItemLookupPort {
	Optional<Long> findFeedItemIdByParentAndScheduleDetail(Long parentId, Long scheduleDetailId);
	Optional<Long> findFeedItemIdByParentAndMission(Long parentId, Long missionId);
	Optional<Long> findFeedItemIdByParentAndCoupon(Long parentId, Long couponId);
	Optional<Long> findFeedItemIdByParentAndChildComplete(Long parentId, Long childId, LocalDate date);
}
