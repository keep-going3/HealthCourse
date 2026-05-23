package com.fitness.module.workout.controller;

import com.fitness.common.Result;
import com.fitness.module.workout.entity.ExerciseLibrary;
import com.fitness.module.workout.service.ExerciseLibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exercise")
@RequiredArgsConstructor
public class ExerciseLibraryController {

    private final ExerciseLibraryService exerciseLibraryService;

    @GetMapping("/library")
    public Result<List<ExerciseLibrary>> getAll(@RequestAttribute("userId") Long userId) {
        return Result.success(exerciseLibraryService.getAll(userId));
    }

    @GetMapping("/library/group")
    public Result<Map<String, List<ExerciseLibrary>>> getGrouped(@RequestAttribute("userId") Long userId) {
        return Result.success(exerciseLibraryService.getGrouped(userId));
    }

    @PostMapping("/custom")
    public Result<ExerciseLibrary> addCustom(@RequestAttribute("userId") Long userId,
                                              @RequestBody ExerciseLibrary exercise) {
        exercise.setUserId(userId);
        return Result.success(exerciseLibraryService.addCustom(exercise));
    }

    @PutMapping("/custom/{id}")
    public Result<Void> updateCustom(@RequestAttribute("userId") Long userId,
                                     @PathVariable Long id,
                                     @RequestBody ExerciseLibrary exercise) {
        exerciseLibraryService.updateCustom(id, userId, exercise);
        return Result.success();
    }

    @DeleteMapping("/custom/{id}")
    public Result<Void> deleteCustom(@RequestAttribute("userId") Long userId,
                                     @PathVariable Long id) {
        exerciseLibraryService.deleteCustom(id, userId);
        return Result.success();
    }
}
