package com.kiero.global.notification.application.port.out;

import java.time.LocalDate;
import java.util.Optional;

public interface FeedItemLookupPort {
	Optional<Long> findFeedIdByParentAndScheduleDetail(Long parentId, Long scheduleDetailId);
	Optional<Long> findFeedIdByParentAndMission(Long parentId, Long missionId);
	Optional<Long> findFeedIdByParentAndCoupon(Long parentId, Long couponId);
	Optional<Long> findFeedIdByParentAndChildComplete(Long parentId, Long childId, LocalDate date);
}
