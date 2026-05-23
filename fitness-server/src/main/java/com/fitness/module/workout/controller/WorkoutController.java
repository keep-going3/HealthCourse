package com.fitness.module.workout.controller;

import com.fitness.common.Result;
import com.fitness.module.workout.dto.*;
import com.fitness.module.workout.service.WorkoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workout")
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutService workoutService;

    @PostMapping("/save")
    public Result<WorkoutDetailResponse> save(@RequestAttribute("userId") Long userId,
                                               @Valid @RequestBody WorkoutSaveRequest req) {
        return Result.success(workoutService.save(userId, req));
    }

    @PutMapping("/update")
    public Result<WorkoutDetailResponse> update(@RequestAttribute("userId") Long userId,
                                                 @Valid @RequestBody WorkoutSaveRequest req) {
        return Result.success(workoutService.update(userId, req));
    }

    @GetMapping("/detail")
    public Result<WorkoutDetailResponse> detail(@RequestAttribute("userId") Long userId,
                                                 @RequestParam("date") String date) {
        WorkoutDetailResponse resp = workoutService.getDetail(userId, date);
        if (resp == null) {
            return Result.success(null);
        }
        return Result.success(resp);
    }

    @GetMapping("/calendar")
    public Result<List<CalendarItemResponse>> calendar(@RequestAttribute("userId") Long userId,
                                                        @RequestParam("year") int year,
                                                        @RequestParam("month") int month) {
        return Result.success(workoutService.getCalendar(userId, year, month));
    }

    @DeleteMapping("/{sessionId}")
    public Result<Void> delete(@RequestAttribute("userId") Long userId,
                               @PathVariable Long sessionId) {
        workoutService.delete(userId, sessionId);
        return Result.success();
    }
}
