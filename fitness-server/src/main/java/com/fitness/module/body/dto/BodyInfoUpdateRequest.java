package com.fitness.module.body.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BodyInfoUpdateRequest {
    private BigDecimal heightCm;
    private BigDecimal targetWeightKg;
    private String goal;
}
