package com.kiero.parent.presentation.dto;

public record InviteStatusResponse(
        boolean isRegistered,
        Long childId
) {
    public static InviteStatusResponse registered(Long childId) {
        return new InviteStatusResponse(true, childId);
    }

    public static InviteStatusResponse notRegistered() {
        return new InviteStatusResponse(false, null);
    }
}
