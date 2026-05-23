package com.fitness.module.workout.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ExerciseSetDTO {
    private String exerciseName;
    private Integer setNumber;
    private BigDecimal weightKg;
    private Integer reps;
    private BigDecimal rpe;
}
