package com.kiero.parent.presentation.dto;

public record InviteCodeCreateResponse(
        String code,
        String childName
) {
    public static InviteCodeCreateResponse of(String code, String childName) {
        return new InviteCodeCreateResponse(code, childName);
    }
}
