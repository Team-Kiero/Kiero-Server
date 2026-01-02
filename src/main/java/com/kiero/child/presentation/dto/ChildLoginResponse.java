package com.kiero.child.presentation.dto;

import com.kiero.global.auth.enums.Role;

public record ChildLoginResponse(
        String name,
        Role role,
        String accessToken,
        String refreshToken
) {
    public static ChildLoginResponse of(String name, Role role, String accessToken, String refreshToken) {
        return new ChildLoginResponse(name, role, accessToken, refreshToken);
    }
}
