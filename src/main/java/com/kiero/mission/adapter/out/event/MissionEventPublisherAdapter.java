package com.kiero.mission.adapter.out.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.kiero.mission.application.port.out.MissionEventPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MissionEventPublisherAdapter implements MissionEventPort {
	private final ApplicationEventPublisher publisher;

	@Override
	public void publish(Object event) {
		publisher.publishEvent(event);
	}
}
