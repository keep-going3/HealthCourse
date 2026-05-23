package com.fitness.module.body.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class WeightListItem {
    private Long recordId;
    private LocalDate recordDate;
    private BigDecimal weightKg;
    private BigDecimal change; // 相比前一天的差值，第一天为 null
}
