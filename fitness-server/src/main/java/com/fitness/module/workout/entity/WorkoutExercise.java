package com.fitness.module.workout.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("workout_exercise")
public class WorkoutExercise {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;
    private String exerciseName;
    private Integer setNumber;
    private BigDecimal weightKg;
    private Integer reps;
    private BigDecimal rpe;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
