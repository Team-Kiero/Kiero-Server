package com.kiero.coupon.application.service;

import com.kiero.coupon.application.dto.CouponCreateRequest;
import com.kiero.coupon.application.dto.CouponCreatedEvent;
import com.kiero.coupon.application.dto.CouponPurchaseEvent;
import com.kiero.coupon.application.dto.CouponUpdateRequest;
import com.kiero.coupon.application.port.out.CouponEventPort;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.application.port.out.ChildLoadPort;
import com.kiero.child.domain.Child;
import com.kiero.coupon.application.dto.CouponResponse;
import com.kiero.coupon.application.exception.CouponErrorCode;
import com.kiero.coupon.application.port.in.CouponCommandUseCase;
import com.kiero.coupon.application.port.out.CouponLoadPort;
import com.kiero.coupon.application.port.out.CouponPersistencePort;
import com.kiero.coupon.domain.Coupon;
import com.kiero.global.exception.KieroException;
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
	private final CouponPersistencePort couponPersistencePort;
	private final CouponEventPort couponEventPort;

	@Override
	@Transactional
	public CouponResponse createCoupon(Long parentId, Long childId, CouponCreateRequest request) {
		if (!parentChildAccessPort.existsByParentIdAndChildId(parentId, childId)) {
			throw new KieroException(CouponErrorCode.NOT_YOUR_CHILD);
		}

		Parent parent = parentLoadPort.findById(parentId)
			.orElseThrow(() -> new KieroException(CouponErrorCode.PARENT_NOT_FOUND));

		Child child = childLoadPort.findById(childId)
			.orElseThrow(() -> new KieroException(CouponErrorCode.CHILD_NOT_FOUND));

		Coupon coupon = Coupon.create(request.name(), request.price(), parent, child);
		Coupon saved = couponPersistencePort.save(coupon);

		couponEventPort.publish(new CouponCreatedEvent(childId, saved.getName(), saved.getPrice()));

		return CouponResponse.from(saved);
	}

	@Override
	@Transactional
	public CouponResponse updateCoupon(Long parentId, Long couponId, CouponUpdateRequest request) {
		Coupon coupon = couponLoadPort.findById(couponId)
			.orElseThrow(() -> new KieroException(CouponErrorCode.COUPON_NOT_FOUND));

		if (!coupon.getParent().getId().equals(parentId)) {
			throw new KieroException(CouponErrorCode.NOT_YOUR_COUPON);
		}

		coupon.update(request.name(), request.price());

		return CouponResponse.from(coupon);
	}

	@Override
	@Transactional
	public void deleteCoupon(Long parentId, Long couponId) {
		Coupon coupon = couponLoadPort.findById(couponId)
			.orElseThrow(() -> new KieroException(CouponErrorCode.COUPON_NOT_FOUND));

		if (!coupon.getParent().getId().equals(parentId)) {
			throw new KieroException(CouponErrorCode.NOT_YOUR_COUPON);
		}

		couponPersistencePort.delete(coupon);
	}

  @Override
  @Transactional
  public CouponResponse purchaseCoupon(Long childId, Long couponId) {

    Child child = childLoadPort.findByIdWithLock(childId)
        .orElseThrow(() -> new KieroException(CouponErrorCode.CHILD_NOT_FOUND));

    Coupon coupon = couponLoadPort.findById(couponId)
        .orElseThrow(() -> new KieroException(CouponErrorCode.COUPON_NOT_FOUND));

    if (!coupon.getChild().getId().equals(childId)) {
      throw new KieroException(CouponErrorCode.NOT_YOUR_COUPON);
    }

    if (!child.hasEnoughCoin(coupon.getPrice())) {
      throw new KieroException(CouponErrorCode.INSUFFICIENT_COINS);
    }

    child.deductCoin(coupon.getPrice());

    couponEventPort.publish(new CouponPurchaseEvent(
        child.getId(),
        coupon.getName(),
        coupon.getPrice(),
        LocalDateTime.now()
    ));

    return new CouponResponse(coupon.getId(), coupon.getName(), coupon.getPrice());
  }
}
