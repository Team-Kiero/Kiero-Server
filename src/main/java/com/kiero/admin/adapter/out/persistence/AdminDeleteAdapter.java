package com.kiero.admin.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.kiero.admin.application.port.out.AdminDeletePort;
import com.kiero.child.adapter.out.persistence.ChildRepository;
import com.kiero.coupon.adapter.out.persistence.CouponHistoryRepository;
import com.kiero.coupon.adapter.out.persistence.CouponRepository;
import com.kiero.feed.adapter.out.persistence.FeedItemRepository;
import com.kiero.mission.adapter.out.persistence.MissionRepository;
import com.kiero.parent.adapter.out.persistence.ParentChildRepository;
import com.kiero.parent.adapter.out.persistence.ParentRepository;
import com.kiero.schedule.adapter.out.persistence.DiscardedScheduleRepository;
import com.kiero.schedule.adapter.out.persistence.ScheduleDetailRepository;
import com.kiero.schedule.adapter.out.persistence.ScheduleRepeatDaysRepository;
import com.kiero.schedule.adapter.out.persistence.ScheduleRepository;
import com.kiero.schedule.domain.Schedule;
import com.kiero.terms.adapter.out.persistence.TermsAgreementRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminDeleteAdapter implements AdminDeletePort {

	private final ScheduleRepository scheduleRepository;
	private final ScheduleDetailRepository scheduleDetailRepository;
	private final DiscardedScheduleRepository discardedScheduleRepository;
	private final ScheduleRepeatDaysRepository scheduleRepeatDaysRepository;
	private final MissionRepository missionRepository;
	private final CouponRepository couponRepository;
	private final CouponHistoryRepository couponHistoryRepository;
	private final FeedItemRepository feedItemRepository;
	private final ParentChildRepository parentChildRepository;
	private final ChildRepository childRepository;
	private final ParentRepository parentRepository;
	private final TermsAgreementRepository termsAgreementRepository;

	@Override
	public void deleteAllSchedulesByChildId(Long childId) {
		List<Schedule> schedules = scheduleRepository.findAllByChildId(childId);
		deleteScheduleCascade(schedules);
	}

	@Override
	public void deleteAllMissionsByChildId(Long childId) {
		missionRepository.deleteAllByChildId(childId);
	}

	@Override
	public void deleteAllCouponsByChildId(Long childId) {
		couponRepository.deleteAllByChildId(childId);
	}

	@Override
	public void deleteAllCouponHistoriesByChildId(Long childId) {
		couponHistoryRepository.deleteAllByChildId(childId);
	}

	@Override
	public void deleteAllFeedItemsByChildId(Long childId) {
		feedItemRepository.deleteAllByChildId(childId);
	}

	@Override
	public void deleteAllParentChildRelationsByChildId(Long childId) {
		parentChildRepository.deleteAllByChildId(childId);
	}

	@Override
	public void deleteChild(Long childId) {
		childRepository.deleteById(childId);
	}

	@Override
	public void deleteAllSchedulesByParentId(Long parentId) {
		List<Schedule> schedules = scheduleRepository.findAllByParentId(parentId);
		deleteScheduleCascade(schedules);
	}

	@Override
	public void deleteAllMissionsByParentId(Long parentId) {
		missionRepository.deleteAllByParentId(parentId);
	}

	@Override
	public void deleteAllCouponsByParentId(Long parentId) {
		couponRepository.deleteAllByParentId(parentId);
	}

	@Override
	public void deleteAllFeedItemsByParentId(Long parentId) {
		feedItemRepository.deleteAllByParentId(parentId);
	}

	@Override
	public void deleteAllTermsAgreementsByParentId(Long parentId) {
		termsAgreementRepository.deleteAllByParentId(parentId);
	}

	@Override
	public void deleteAllParentChildRelationsByParentId(Long parentId) {
		parentChildRepository.deleteAllByParentId(parentId);
	}

	@Override
	public void deleteParent(Long parentId) {
		parentRepository.deleteById(parentId);
	}

	@Override
	public void deleteScheduleById(Long scheduleId) {
		scheduleRepeatDaysRepository.deleteAllByScheduleId(scheduleId);
		discardedScheduleRepository.deleteByScheduleId(scheduleId);
		scheduleDetailRepository.deleteAllByScheduleId(scheduleId);
		scheduleRepository.deleteById(scheduleId);
	}

	@Override
	public void deleteMissionById(Long missionId) {
		missionRepository.deleteById(missionId);
	}

	@Override
	public void deleteCouponById(Long couponId) {
		couponRepository.deleteById(couponId);
	}

	private void deleteScheduleCascade(List<Schedule> schedules) {
		for (Schedule schedule : schedules) {
			scheduleRepeatDaysRepository.deleteAllByScheduleId(schedule.getId());
			discardedScheduleRepository.deleteByScheduleId(schedule.getId());
			scheduleDetailRepository.deleteAllByScheduleId(schedule.getId());
		}
		scheduleRepository.deleteAllById(schedules.stream().map(Schedule::getId).toList());
	}
}
