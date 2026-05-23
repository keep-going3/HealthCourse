package com.fitness.module.workout.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exercise_library")
public class ExerciseLibrary {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String name;
    private String targetMuscle;
    private String equipment;
    private String type;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
