package com.kiero.parent.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.application.port.out.ChildDeletePort;
import com.kiero.coupon.application.port.out.CouponDeletePort;
import com.kiero.coupon.application.port.out.CouponHistoryDeletePort;
import com.kiero.coupon.application.port.out.CouponTransferPort;
import com.kiero.feed.application.port.out.FeedItemDeletePort;
import com.kiero.feed.application.port.out.FeedItemTransferPort;
import com.kiero.global.auth.enums.Role;
import com.kiero.global.auth.jwt.application.port.out.TokenCommandPort;
import com.kiero.global.exception.KieroException;
import com.kiero.mission.application.port.out.MissionDeletePort;
import com.kiero.mission.application.port.out.MissionTransferPort;
import com.kiero.parent.application.dto.ParentWithdrawnEvent;
import com.kiero.parent.application.exception.ParentErrorCode;
import com.kiero.parent.application.port.in.ParentWithdrawUseCase;
import com.kiero.parent.application.port.out.ParentChildDeletePort;
import com.kiero.parent.application.port.out.ParentChildLoadPort;
import com.kiero.parent.application.port.out.ParentDeletePort;
import com.kiero.parent.application.port.out.ParentLoadPort;
import com.kiero.parent.application.port.out.ParentWithdrawEventPort;
import com.kiero.parent.application.port.out.ParentWithdrawNotificationPort;
import com.kiero.parent.domain.Parent;
import com.kiero.schedule.application.port.out.ScheduleDeletePort;
import com.kiero.schedule.application.port.out.ScheduleTransferPort;
import com.kiero.terms.application.port.out.TermsAgreementDeletePort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ParentWithdrawService implements ParentWithdrawUseCase {

	private final ParentLoadPort parentLoadPort;
	private final ParentChildLoadPort parentChildLoadPort;
	private final ParentChildDeletePort parentWithdrawDeletePort;
	private final ParentWithdrawEventPort parentWithdrawEventPort;
	private final ParentWithdrawNotificationPort parentWithdrawNotificationPort;
	private final ParentDeletePort parentDeletePort;
	private final TokenCommandPort tokenCommandPort;
	private final ScheduleDeletePort scheduleDeletePort;
	private final ScheduleTransferPort scheduleTransferPort;
	private final MissionDeletePort missionDeletePort;
	private final MissionTransferPort missionTransferPort;
	private final CouponDeletePort couponDeletePort;
	private final CouponHistoryDeletePort couponHistoryDeletePort;
	private final CouponTransferPort couponTransferPort;
	private final FeedItemDeletePort feedItemDeletePort;
	private final FeedItemTransferPort feedItemTransferPort;
	private final TermsAgreementDeletePort termsAgreementDeletePort;
	private final ChildDeletePort childDeletePort;

	@Override
	public void withdraw(Long parentId) {
		parentLoadPort.findById(parentId)
			.orElseThrow(() -> new KieroException(ParentErrorCode.PARENT_NOT_FOUND));

		List<Long> childIds = parentChildLoadPort.findChildIdsByParentId(parentId);

		// 아이별로 후계 부모 계산
		List<Long> soloChildIds = new ArrayList<>();
		Map<Long, Long> childToSuccessorMap = new HashMap<>(); // childId -> 후계 부모 ID

		for (Long childId : childIds) {
			List<Parent> remainingParents = parentChildLoadPort.findParentsByChildId(childId)
				.stream()
				.filter(p -> !p.getId().equals(parentId))
				.sorted(Comparator.comparing(Parent::getCreatedAt))
				.toList();

			if (remainingParents.isEmpty()) {
				soloChildIds.add(childId);
			} else {
				childToSuccessorMap.put(childId, remainingParents.get(0).getId()); // 가장 먼저 생성된 부모
			}
		}

		// 복수 부모 아이: 탈퇴 부모의 리소스를 후계 부모에게 이전
		for (Map.Entry<Long, Long> entry : childToSuccessorMap.entrySet()) {
			Long childId = entry.getKey();
			Long successorId = entry.getValue();
			scheduleTransferPort.transferOwnership(parentId, successorId, childId);
			missionTransferPort.transferOwnership(parentId, successorId, childId);
			couponTransferPort.transferOwnership(parentId, successorId, childId);
			feedItemTransferPort.transferOwnership(parentId, successorId, childId);
		}

		// 부모-자녀 관계 삭제
		parentWithdrawDeletePort.deleteAllParentChildRelationsByParentId(parentId);

		// 부모의 약관 동의 내역 삭제
		termsAgreementDeletePort.deleteAllByParentId(parentId);

		// 부모 refresh token 삭제
		tokenCommandPort.deleteRefreshToken(parentId, Role.PARENT);

		// 유일한 부모였던 아이: 관련 리소스 삭제 + 알림 + refresh token 삭제 + 아이 삭제
		for (Long childId : soloChildIds) {
			scheduleDeletePort.deleteAllByChildId(childId);
			missionDeletePort.deleteAllByChildId(childId);
			couponDeletePort.deleteAllByChildId(childId);
			feedItemDeletePort.deleteAllByChildId(childId);
			couponHistoryDeletePort.deleteAllByChildId(childId);

			parentWithdrawEventPort.publish(new ParentWithdrawnEvent(childId));
			parentWithdrawNotificationPort.storeWithdrawalMarker(childId);
			tokenCommandPort.deleteRefreshToken(childId, Role.CHILD);
			childDeletePort.deleteById(childId);
		}

		// 부모 하드딜리트 (soloChild 리소스 전부 정리 후 마지막에 삭제)
		parentDeletePort.deleteParent(parentId);
	}
}
