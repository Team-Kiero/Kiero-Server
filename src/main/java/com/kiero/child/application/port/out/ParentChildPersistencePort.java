package com.kiero.child.application.port.out;

import com.kiero.parent.domain.ParentChild;

public interface ParentChildPersistencePort {
	ParentChild save(ParentChild parentChild);
}
