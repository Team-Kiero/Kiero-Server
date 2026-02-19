package com.kiero.mission.application.port.out;

public interface ParentChildAccessPort {
	boolean existsByParentIdAndChildId(Long parentId, Long childId);
}
