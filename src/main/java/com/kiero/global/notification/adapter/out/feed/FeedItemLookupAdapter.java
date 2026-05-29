package com.kiero.global.notification.adapter.out.feed;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kiero.feed.adapter.out.persistence.FeedItemRepository;
import com.kiero.feed.domain.enums.EventType;
import com.kiero.global.notification.application.port.out.FeedItemLookupPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FeedItemLookupAdapter implements FeedItemLookupPort {

	private final FeedItemRepository feedItemRepository;

	@Override
	public Optional<Long> findFeedIdByParentAndScheduleDetail(Long parentId, Long scheduleDetailId) {
		return feedItemRepository.findByParentIdAndScheduleDetailIdAndEventType(
				parentId, String.valueOf(scheduleDetailId), EventType.SCHEDULE.name())
			.map(fi -> fi.getId());
	}

	@Override
	public Optional<Long> findFeedIdByParentAndMission(Long parentId, Long missionId) {
		return feedItemRepository.findByParentIdAndMissionId(parentId, String.valueOf(missionId))
			.map(fi -> fi.getId());
	}

	@Override
	public Optional<Long> findFeedIdByParentAndCoupon(Long parentId, Long couponId) {
		return feedItemRepository.findByParentIdAndCouponId(parentId, String.valueOf(couponId))
			.map(fi -> fi.getId());
	}

	@Override
	public Optional<Long> findFeedIdByParentAndChildComplete(Long parentId, Long childId, LocalDate date) {
		return feedItemRepository.findByParentAndChildComplete(parentId, childId, date)
			.map(fi -> fi.getId());
	}
}
