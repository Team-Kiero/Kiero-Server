package com.kiero.invitation.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.kiero.invitation.domain.InviteCode;

public interface InviteCodeRepository extends CrudRepository<InviteCode, String> {

    Optional<InviteCode> findByParentKey(String parentKey);
}
