package com.kiero.coupon.adapter.out.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.kiero.coupon.application.dto.CouponPurchaseEvent;
import com.kiero.coupon.application.port.out.CouponPurchaseEventPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CouponEventPublisherAdapter implements CouponPurchaseEventPort {

	private final ApplicationEventPublisher publisher;

	@Override
	public void publish(CouponPurchaseEvent event) {
		publisher.publishEvent(event);
	}
}