package com.kiero.parent.application.port.in;

import com.kiero.parent.domain.ParentChild;

public interface ParentChildSaveUseCase {
	ParentChild save(ParentChild parentChild);
}
