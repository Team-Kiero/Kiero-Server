package com.kiero.child.application.dto;

import com.kiero.global.auth.enums.Role;

public record ChildLoginResponse(
        Long id,
        String lastName,
        String firstName,
        Role role,
        String accessToken,
        String refreshToken
) {
    public static ChildLoginResponse of(Long id, String lastName, String firstName, Role role, String accessToken, String refreshToken) {
        return new ChildLoginResponse(id, lastName, firstName, role, accessToken, refreshToken);
    }
}
