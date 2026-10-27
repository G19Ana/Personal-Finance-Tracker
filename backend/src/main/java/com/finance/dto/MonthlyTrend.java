package com.finance.dto;

import java.math.BigDecimal;

public record MonthlyTrend(String month, BigDecimal income, BigDecimal expense, BigDecimal net) {}
