package com.fitness.module.body.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class WeightRecordRequest {
    @NotNull(message = "体重不能为空")
    private BigDecimal weightKg;

    private String recordDate; // yyyy-MM-dd，默认当天
}
