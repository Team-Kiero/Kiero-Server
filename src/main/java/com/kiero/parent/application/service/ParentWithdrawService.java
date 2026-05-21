package com.kiero.parent.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.global.auth.enums.Role;
import com.kiero.global.auth.jwt.application.port.out.TokenCommandPort;
import com.kiero.global.exception.KieroException;
import com.kiero.parent.application.dto.ParentWithdrawnEvent;
import com.kiero.parent.application.exception.ParentErrorCode;
import com.kiero.parent.application.port.in.ParentWithdrawUseCase;
import com.kiero.parent.application.port.out.ParentChildLoadPort;
import com.kiero.parent.application.port.out.ParentLoadPort;
import com.kiero.parent.application.port.out.ParentSavePort;
import com.kiero.parent.application.port.out.ParentChildDeletePort;
import com.kiero.parent.application.port.out.ParentWithdrawEventPort;
import com.kiero.parent.application.port.out.ParentWithdrawNotificationPort;
import com.kiero.parent.domain.Parent;
import com.kiero.child.application.port.out.ChildDeletePort;
import com.kiero.coupon.application.port.out.CouponDeletePort;
import com.kiero.feed.application.port.out.FeedItemDeletePort;
import com.kiero.mission.application.port.out.MissionDeletePort;
import com.kiero.schedule.application.port.out.ScheduleDeletePort;
import com.kiero.terms.application.port.out.TermsAgreementDeletePort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ParentWithdrawService implements ParentWithdrawUseCase {

	private final ParentLoadPort parentLoadPort;
	private final ParentSavePort parentSavePort;
	private final ParentChildLoadPort parentChildLoadPort;
	private final ParentChildDeletePort parentWithdrawDeletePort;
	private final ParentWithdrawEventPort parentWithdrawEventPort;
	private final ParentWithdrawNotificationPort parentWithdrawNotificationPort;
	private final TokenCommandPort tokenCommandPort;
	private final ScheduleDeletePort scheduleDeletePort;
	private final MissionDeletePort missionDeletePort;
	private final CouponDeletePort couponDeletePort;
	private final FeedItemDeletePort feedItemDeletePort;
	private final TermsAgreementDeletePort termsAgreementDeletePort;
	private final ChildDeletePort childDeletePort;

	@Override
	@Transactional
	public void withdraw(Long parentId) {
		Parent parent = parentLoadPort.findById(parentId)
			.orElseThrow(() -> new KieroException(ParentErrorCode.PARENT_NOT_FOUND));

		List<Long> childIds = parentChildLoadPort.findChildIdsByParentId(parentId);

		// 탈퇴하려는 부모가 유일한 부모인 아이 List
		List<Long> soloChildIds = childIds.stream()
			.filter(childId -> parentChildLoadPort.findParentsByChildId(childId).size() == 1)
			.toList();

		// 부모-자녀 관계 삭제
		parentWithdrawDeletePort.deleteAllParentChildRelationsByParentId(parentId);

		// 부모의 약관 동의 내역 삭제
		termsAgreementDeletePort.deleteAllByParentId(parentId);

		// 개인정보 익명화
		parent.withdraw();

		// 부모 refresh token 삭제
		tokenCommandPort.deleteRefreshToken(parentId, Role.PARENT);

		// 유일한 부모였던 아이: 관련 리소스 삭제 + 알림 + refresh token 삭제
		for (Long childId : soloChildIds) {
			scheduleDeletePort.deleteAllByChildId(childId);
			missionDeletePort.deleteAllByChildId(childId);
			couponDeletePort.deleteAllByChildId(childId);
			feedItemDeletePort.deleteAllByChildId(childId);

			parentWithdrawEventPort.publish(new ParentWithdrawnEvent(childId));
			parentWithdrawNotificationPort.storeWithdrawalMarker(childId);
			tokenCommandPort.deleteRefreshToken(childId, Role.CHILD);
			childDeletePort.deleteById(childId);
		}
	}
}
