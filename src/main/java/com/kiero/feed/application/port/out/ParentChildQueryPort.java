package com.kiero.feed.application.port.out;

import java.util.List;

import com.kiero.parent.domain.Parent;

public interface ParentChildQueryPort {

	List<Parent> findParentsByChildId(Long childId);

}
