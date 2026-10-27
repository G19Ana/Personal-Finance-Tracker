package com.finance.dto;

import com.finance.entity.Transaction;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardView(
    BigDecimal totalIncome,
    BigDecimal totalExpense,
    BigDecimal netSavings,
    BigDecimal savingsRate,
    List<BudgetDiff> budgets,
    List<GoalProjection> goals,
    List<RecurringItem> recurring,
    List<MonthlyTrend> trends,
    Map<String, BigDecimal> categorySpending,
    List<Transaction> recentTransactions
) {}
