package com.kiero.parent.application.dto;

public record InviteCodeCreateResponse(
        String code,
        String childLastName,
        String childFirstName
) {
    public static InviteCodeCreateResponse of(String code, String childLastName, String childFirstName) {
        return new InviteCodeCreateResponse(code, childLastName, childFirstName);
    }
}
