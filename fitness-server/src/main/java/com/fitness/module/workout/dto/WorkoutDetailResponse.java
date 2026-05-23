package com.fitness.module.workout.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class WorkoutDetailResponse {
    private Long sessionId;
    private LocalDate sessionDate;
    private List<String> bodyParts;
    private Integer totalSets;
    private Integer feelRating;
    private String notes;
    private List<ExerciseDetail> exercises;

    @Data
    public static class ExerciseDetail {
        private String exerciseName;
        private Integer setNumber;
        private BigDecimal weightKg;
        private Integer reps;
        private BigDecimal rpe;
    }
}
