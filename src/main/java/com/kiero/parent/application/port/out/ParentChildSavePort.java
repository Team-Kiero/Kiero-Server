package com.kiero.parent.application.port.out;

import com.kiero.parent.domain.ParentChild;

public interface ParentChildSavePort {
	ParentChild save(ParentChild parentChild);
}
