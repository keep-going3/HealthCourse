package com.fitness.module.workout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class WorkoutSaveRequest {
    @NotBlank(message = "训练日期不能为空")
    private String sessionDate;

    private List<String> bodyParts;
    private String notes;
    private Integer feelRating;

    @NotEmpty(message = "训练动作不能为空")
    @Size(min = 1, message = "至少需要一个训练动作")
    private List<ExerciseSetDTO> exercises;
}
