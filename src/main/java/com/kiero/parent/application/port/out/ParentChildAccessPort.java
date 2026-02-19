package com.kiero.parent.application.port.out;

import com.kiero.child.domain.Child;
import com.kiero.parent.domain.Parent;

public interface ParentChildAccessPort {
	boolean existsByParentIdAndChildId(Long parentId, Long childId);
	boolean existsByParentAndChild(Parent parent, Child child);
}
