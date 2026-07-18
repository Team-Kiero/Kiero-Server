package com.kiero.child.application.port.out;

import com.kiero.child.application.dto.ChildJoinedEvent;

public interface ChildJoinedEventPort {
	void publish(ChildJoinedEvent event);
}
