package com.kiero.terms.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kiero.terms.domain.TermsAgreement;

public interface TermsAgreementRepository extends JpaRepository<TermsAgreement, Long> {
}
