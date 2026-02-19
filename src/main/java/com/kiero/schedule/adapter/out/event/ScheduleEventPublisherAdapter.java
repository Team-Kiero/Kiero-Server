package com.kiero.schedule.adapter.out.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.kiero.schedule.application.port.out.ScheduleEventPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ScheduleEventPublisherAdapter implements ScheduleEventPort {

	private final ApplicationEventPublisher publisher;

	@Override
	public void publish(Object event) { publisher.publishEvent(event); }
}