package com.fitness.module.body.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BodyInfoResponse {
    private BigDecimal heightCm;
    private BigDecimal targetWeightKg;
    private String goal;
    private BigDecimal latestWeight;
    private String latestWeightDate;
}
