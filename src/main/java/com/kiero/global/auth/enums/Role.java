package com.kiero.global.auth.enums;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Role {
	PARENT("ROLE_PARENT"),
	CHILD("ROLE_CHILD"),
	ADMIN("ROLE_ADMIN"),
	;

	private final String roleName;

	public GrantedAuthority toGrantedAuthority() {
		return new SimpleGrantedAuthority(roleName);
	}
}
