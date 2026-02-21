package com.kiero.invitation.adapter.out.persistence;

import org.springframework.data.repository.CrudRepository;

import com.kiero.invitation.domain.InviteCode;

public interface InviteCodeRepository extends CrudRepository<InviteCode, String> {

}
