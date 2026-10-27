package com.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalReq(String name, BigDecimal targetAmount, BigDecimal currentAmount, LocalDate targetDate) {}
