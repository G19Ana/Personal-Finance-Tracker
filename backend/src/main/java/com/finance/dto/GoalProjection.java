package com.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalProjection(Long id, String name, BigDecimal targetAmount, BigDecimal currentAmount, LocalDate targetDate, BigDecimal progressPct, String projectedDate, boolean onTrack) {}
