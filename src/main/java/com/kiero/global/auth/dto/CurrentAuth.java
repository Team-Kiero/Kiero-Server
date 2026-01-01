package com.kiero.global.auth.dto;

import com.kiero.global.auth.enums.Role;

public record CurrentAuth(Long memberId, Role role) {
}
