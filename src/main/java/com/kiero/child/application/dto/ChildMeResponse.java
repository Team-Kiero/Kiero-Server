package com.kiero.child.application.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.kiero.child.domain.Child;

public record ChildMeResponse(
        String lastName,
        String firstName,
        int coinAmount,
        String today,
        boolean isPushNotificationEnabled
) {
    public static ChildMeResponse from(Child child, LocalDate today) {
        return new ChildMeResponse(
                child.getLastName(),
                child.getFirstName(),
                child.getCoinAmount(),
                today.format(FORMATTER),
                child.isPushNotificationEnabled()
        );
    }

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN);
}
