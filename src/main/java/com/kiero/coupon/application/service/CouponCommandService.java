package com.kiero.coupon.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.application.exception.ChildErrorCode;
import com.kiero.child.application.port.out.ChildLoadPort;
import com.kiero.child.domain.Child;
import com.kiero.coupon.application.dto.CouponResponse;
import com.kiero.coupon.application.dto.CreateCouponCommand;
import com.kiero.coupon.application.exception.CouponErrorCode;
import com.kiero.coupon.application.port.in.CouponCommandUseCase;
import com.kiero.coupon.application.port.out.CouponLoadPort;
import com.kiero.coupon.domain.Coupon;
import com.kiero.global.exception.KieroException;
import com.kiero.parent.application.exception.ParentErrorCode;
import com.kiero.parent.application.port.out.ParentChildAccessPort;
import com.kiero.parent.application.port.out.ParentLoadPort;
import com.kiero.parent.domain.Parent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponCommandService implements CouponCommandUseCase {

	private final ParentChildAccessPort parentChildAccessPort;
	private final ParentLoadPort parentLoadPort;
	private final ChildLoadPort childLoadPort;
	private final CouponLoadPort couponLoadPort;

	@Override
	@Transactional
	public CouponResponse create(CreateCouponCommand command) {
		if (!parentChildAccessPort.existsByParentIdAndChildId(command.parentId(), command.childId())) {
			throw new KieroException(CouponErrorCode.NOT_YOUR_CHILD);
		}

		Parent parent = parentLoadPort.findById(command.parentId())
			.orElseThrow(() -> new KieroException(ParentErrorCode.PARENT_NOT_FOUND));

		Child child = childLoadPort.findById(command.childId())
			.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

		Coupon coupon = Coupon.create(command.name(), command.price(), parent, child);

		return CouponResponse.from(couponLoadPort.save(coupon));
	}
}
