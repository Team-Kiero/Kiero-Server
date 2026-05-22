package com.kiero.terms.application.port.out;

import java.util.List;

import com.kiero.terms.domain.Link;

public interface LinkLoadPort {
	List<Link> findAll();
}
