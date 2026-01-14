package com.kiero.child.presentation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kiero.child.domain.Child;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public record ChildMeResponse(
        String lastName,
        String firstName,
        int coinAmount,
        String today
) {
    public static ChildMeResponse from(Child child, LocalDate today) {
        return new ChildMeResponse(
                child.getLastName(),
                child.getFirstName(),
                child.getCoinAmount(),
                today.format(FORMATTER)
        );
    }

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN);
}
