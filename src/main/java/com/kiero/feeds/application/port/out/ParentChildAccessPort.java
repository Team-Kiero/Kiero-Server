package com.kiero.feeds.application.port.out;

public interface ParentChildAccessPort {

	boolean existsByParentIdAndChildId(Long parentId, Long childId);

}
