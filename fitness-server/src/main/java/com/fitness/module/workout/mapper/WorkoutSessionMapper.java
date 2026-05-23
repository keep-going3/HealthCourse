package com.fitness.module.workout.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.module.workout.entity.WorkoutSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkoutSessionMapper extends BaseMapper<WorkoutSession> {
}
