package com.kiero.child.presentation.dto;

import com.kiero.global.auth.enums.Role;

public record ChildLoginResponse(
        String lastName,
        String firstName,
        Role role,
        String accessToken,
        String refreshToken
) {
    public static ChildLoginResponse of(String lastName, String firstName, Role role, String accessToken, String refreshToken) {
        return new ChildLoginResponse(lastName, firstName, role, accessToken, refreshToken);
    }
}
