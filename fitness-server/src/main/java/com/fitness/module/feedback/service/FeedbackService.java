package com.fitness.module.feedback.service;

import com.fitness.module.feedback.entity.Feedback;
import com.fitness.module.feedback.mapper.FeedbackMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackMapper feedbackMapper;

    public void submit(Long userId, String content) {
        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setContent(content);
        feedbackMapper.insert(feedback);
    }
}
