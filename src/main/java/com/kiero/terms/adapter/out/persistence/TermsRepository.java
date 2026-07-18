package com.kiero.terms.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kiero.terms.domain.Terms;
import com.kiero.terms.domain.enums.TermsType;

public interface TermsRepository extends JpaRepository<Terms, Long> {
	List<Terms> findAllByIsRequiredTrueAndIsActiveTrue();
	List<Terms> findAllByTypeIn(List<TermsType> types);
}
