package com.finance.dto;

import java.math.BigDecimal;

public record BudgetDiff(String category, BigDecimal monthlyLimit, BigDecimal spent, BigDecimal remaining, BigDecimal pct, boolean over) {}
