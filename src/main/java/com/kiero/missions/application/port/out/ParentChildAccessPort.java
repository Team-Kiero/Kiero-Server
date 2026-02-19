package com.kiero.missions.application.port.out;

public interface ParentChildAccessPort {
	boolean existsByParentIdAndChildId(Long parentId, Long childId);
}
