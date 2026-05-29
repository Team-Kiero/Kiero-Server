package com.kiero.global.notification.adapter.out.feed;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kiero.feed.application.port.out.FeedItemQueryPort;
import com.kiero.feed.domain.enums.EventType;
import com.kiero.global.notification.application.port.out.FeedItemLookupPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FeedItemLookupAdapter implements FeedItemLookupPort {

	private final FeedItemQueryPort feedItemQueryPort;

	@Override
	public Optional<Long> findFeedItemIdByParentAndScheduleDetail(Long parentId, Long scheduleDetailId) {
		return feedItemQueryPort.findByParentIdAndScheduleDetailIdAndEventType(parentId, scheduleDetailId, EventType.SCHEDULE)
			.map(fi -> fi.getId());
	}

	@Override
	public Optional<Long> findFeedItemIdByParentAndMission(Long parentId, Long missionId) {
		return feedItemQueryPort.findByParentIdAndMissionId(parentId, missionId)
			.map(fi -> fi.getId());
	}

	@Override
	public Optional<Long> findFeedItemIdByParentAndCoupon(Long parentId, Long couponId) {
		return feedItemQueryPort.findByParentIdAndCouponId(parentId, couponId)
			.map(fi -> fi.getId());
	}

	@Override
	public Optional<Long> findFeedItemIdByParentAndChildComplete(Long parentId, Long childId, LocalDate date) {
		return feedItemQueryPort.findByParentAndChildComplete(parentId, childId, date)
			.map(fi -> fi.getId());
	}
}
