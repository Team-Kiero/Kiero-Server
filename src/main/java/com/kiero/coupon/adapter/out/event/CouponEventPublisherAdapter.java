package com.kiero.coupon.adapter.out.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.kiero.coupon.application.dto.CouponCreatedEvent;
import com.kiero.coupon.application.dto.CouponPurchaseEvent;
import com.kiero.coupon.application.port.out.CouponEventPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CouponEventPublisherAdapter implements CouponEventPort {

	private final ApplicationEventPublisher publisher;

	@Override
	public void publish(CouponCreatedEvent event) {
		publisher.publishEvent(event);
	}

	@Override
	public void publish(CouponPurchaseEvent event) {
		publisher.publishEvent(event);
	}
}