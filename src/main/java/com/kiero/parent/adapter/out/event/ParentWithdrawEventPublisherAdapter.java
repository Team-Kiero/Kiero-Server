package com.kiero.parent.adapter.out.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.kiero.parent.application.port.out.ParentWithdrawEventPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ParentWithdrawEventPublisherAdapter implements ParentWithdrawEventPort {

	private final ApplicationEventPublisher publisher;

	@Override
	public void publish(Object event) { publisher.publishEvent(event); }
}
