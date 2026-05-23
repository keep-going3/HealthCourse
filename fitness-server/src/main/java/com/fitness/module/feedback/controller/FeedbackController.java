package com.fitness.module.feedback.controller;

import com.fitness.common.Result;
import com.fitness.module.feedback.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public Result<Void> submit(@RequestAttribute("userId") Long userId,
                                @RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "").trim();
        if (content.isEmpty()) {
            return Result.error(400, "反馈内容不能为空");
        }
        feedbackService.submit(userId, content);
        return Result.success();
    }
}
