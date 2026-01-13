package com.kiero.mission.presentation.dto;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public record MissionsByDateResponse(
    List<DateGroupedMissions> missionsByDate
) {
    public record DateGroupedMissions(
        String dueAt,
        String dayOfWeek,
        List<MissionItemResponse> missions
    ) {
    }
    public static MissionsByDateResponse from(List<MissionResponse> missions) {
        Map<LocalDate, List<MissionItemResponse>> grouped = missions.stream()
            .collect(Collectors.groupingBy(
                MissionResponse::dueAt,
                LinkedHashMap::new,
                Collectors.mapping(
                    MissionItemResponse::from,
                    Collectors.toList()
                )
            ));

        List<DateGroupedMissions> result = grouped.entrySet().stream()
            .sorted(Map.Entry.comparingByKey()) // 날짜순
            .map(entry -> {
                LocalDate date = entry.getKey();

                List<MissionItemResponse> sortedMissions = entry.getValue().stream()
                    .sorted(Comparator.comparing(MissionItemResponse::isCompleted)) // 미완료순
                    .toList();

                String dayOfWeek = date.getDayOfWeek()
                        .getDisplayName(TextStyle.FULL, Locale.KOREAN);

                return new DateGroupedMissions(
                    date.toString(),
                    dayOfWeek,
                    sortedMissions
                );
            })
            .toList();

        return new MissionsByDateResponse(result);
    }
}
