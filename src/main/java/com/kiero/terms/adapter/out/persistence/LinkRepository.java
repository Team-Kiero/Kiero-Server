package com.kiero.terms.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kiero.terms.domain.Link;

public interface LinkRepository extends JpaRepository<Link, Long> {
}
