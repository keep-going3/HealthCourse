package com.fitness.module.feedback.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fitness.module.feedback.entity.Feedback;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {
}
