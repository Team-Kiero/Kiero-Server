package com.kiero.parent.application.port.out;

import com.kiero.parent.domain.Parent;

public interface ParentSavePort {
	Parent save(Parent parent);
}
