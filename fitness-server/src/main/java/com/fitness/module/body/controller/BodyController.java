package com.fitness.module.body.controller;

import com.fitness.common.Result;
import com.fitness.module.body.dto.*;
import com.fitness.module.body.entity.WeightRecord;
import com.fitness.module.body.service.BodyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/body")
@RequiredArgsConstructor
public class BodyController {

    private final BodyService bodyService;

    @GetMapping("/info")
    public Result<BodyInfoResponse> getBodyInfo(@RequestAttribute("userId") Long userId) {
        return Result.success(bodyService.getBodyInfo(userId));
    }

    @PutMapping("/info")
    public Result<Void> updateBodyInfo(@RequestAttribute("userId") Long userId,
                                        @RequestBody BodyInfoUpdateRequest req) {
        bodyService.updateBodyInfo(userId, req);
        return Result.success();
    }

    @PostMapping("/weight")
    public Result<WeightRecord> recordWeight(@RequestAttribute("userId") Long userId,
                                              @Valid @RequestBody WeightRecordRequest req) {
        return Result.success(bodyService.recordWeight(userId, req));
    }

    @GetMapping("/weight/list")
    public Result<List<WeightListItem>> getWeightList(@RequestAttribute("userId") Long userId,
                                                       @RequestParam(value = "months", required = false) Integer months) {
        return Result.success(bodyService.getWeightList(userId, months));
    }

    @PutMapping("/weight/{recordId}")
    public Result<Void> updateWeight(@RequestAttribute("userId") Long userId,
                                     @PathVariable Long recordId,
                                     @Valid @RequestBody WeightRecordRequest req) {
        bodyService.updateWeight(userId, recordId, req);
        return Result.success();
    }

    @DeleteMapping("/weight/{recordId}")
    public Result<Void> deleteWeight(@RequestAttribute("userId") Long userId,
                                     @PathVariable Long recordId) {
        bodyService.deleteWeight(userId, recordId);
        return Result.success();
    }
}
