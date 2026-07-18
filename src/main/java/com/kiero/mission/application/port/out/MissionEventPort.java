package com.kiero.mission.application.port.out;

public interface MissionEventPort {
	void publish(Object event);
}
