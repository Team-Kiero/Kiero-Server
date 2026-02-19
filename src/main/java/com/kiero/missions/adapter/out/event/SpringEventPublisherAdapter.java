package com.kiero.missions.adapter.out.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.kiero.missions.application.port.out.MissionEventPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SpringEventPublisherAdapter implements MissionEventPort {
	private final ApplicationEventPublisher publisher;

	@Override
	public void publish(Object event) {
		publisher.publishEvent(event);
	}
}
