package com.kiero.missions.application.port.out;

public interface MissionEventPort {
	void publish(Object event);
}
