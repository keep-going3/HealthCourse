package com.fitness.module.workout.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CalendarItemResponse {
    private LocalDate sessionDate;
    private Long sessionId;
    private Integer totalSets;
    private Integer feelRating;
    private String bodyPartsSummary;
}
