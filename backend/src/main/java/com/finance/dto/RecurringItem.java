package com.finance.dto;

import java.math.BigDecimal;

public record RecurringItem(String description, BigDecimal amount, String category, int count) {}
