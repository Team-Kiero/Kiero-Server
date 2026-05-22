package com.kiero.terms.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.kiero.terms.application.port.out.LinkLoadPort;
import com.kiero.terms.domain.Link;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LinkPersistenceAdapter implements LinkLoadPort {

	private final LinkRepository linkRepository;

	@Override
	public List<Link> findAll() {
		return linkRepository.findAll();
	}
}
