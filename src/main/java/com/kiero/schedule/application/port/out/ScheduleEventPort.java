package com.kiero.schedule.application.port.out;

public interface ScheduleEventPort {
	void publish(Object event);
}
