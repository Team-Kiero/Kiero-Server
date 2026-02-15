package com.kiero.child.adapter.out.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.kiero.child.application.port.out.ChildJoinedEventPort;
import com.kiero.child.application.dto.ChildJoinedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SpringEventPublisherAdapter implements ChildJoinedEventPort {

	private final ApplicationEventPublisher publisher;

	@Override
	public void publish(ChildJoinedEvent event) {
		publisher.publishEvent(event);
	}
}