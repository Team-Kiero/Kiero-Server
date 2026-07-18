package com.kiero.coupon.adapter.out.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.kiero.coupon.application.dto.CouponCreatedEvent;
import com.kiero.coupon.application.dto.CouponPurchaseEventForFeed;
import com.kiero.coupon.application.dto.CouponPurchasedEvent;
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
	public void publish(CouponPurchaseEventForFeed event) {
		publisher.publishEvent(event);
	}

	@Override
	public void publish(CouponPurchasedEvent event) {
		publisher.publishEvent(event);
	}
}