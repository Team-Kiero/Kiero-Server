package com.kiero.terms.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kiero.terms.domain.Terms;

public interface TermsRepository extends JpaRepository<Terms, Long> {
	List<Terms> findAllByIsRequiredTrueAndIsActiveTrue();
}
