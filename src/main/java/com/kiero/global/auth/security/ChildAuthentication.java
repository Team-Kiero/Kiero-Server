package com.kiero.global.auth.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class ChildAuthentication extends UsernamePasswordAuthenticationToken {

    public ChildAuthentication(Object principal, Object credentials,
        Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
    }

    public Long getId() {
        return (Long) getPrincipal();
    }
}