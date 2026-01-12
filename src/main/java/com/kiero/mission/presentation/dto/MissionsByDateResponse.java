package com.kiero.mission.presentation.dto;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record MissionsByDateResponse(
    List<DateGroupedMissions> missionsByDate
) {
    public record DateGroupedMissions(
        String dueAt,
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
            .sorted(Map.Entry.comparingByKey())  // 날짜 오름차순 (가장 빠른 마감일이 먼저)
            .map(entry -> new DateGroupedMissions(
                entry.getKey().toString(),
                entry.getValue()
            ))
            .toList();

        return new MissionsByDateResponse(result);
    }
}
