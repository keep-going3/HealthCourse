package com.fitness.module.workout.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("workout_session")
public class WorkoutSession {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private LocalDate sessionDate;
    private String bodyParts;
    private Integer totalSets;
    private Integer feelRating;
    private String notes;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
