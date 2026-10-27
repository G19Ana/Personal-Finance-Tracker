package com.finance.dto;

import java.math.BigDecimal;

public record BudgetReq(String category, BigDecimal monthlyLimit) {}
