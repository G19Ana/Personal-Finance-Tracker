package com.finance.service;

import com.finance.dto.*;
import com.finance.entity.Budget;
import com.finance.entity.Goal;
import com.finance.entity.Transaction;
import com.finance.repo.BudgetRepo;
import com.finance.repo.GoalRepo;
import com.finance.repo.TransactionRepo;
import com.opencsv.CSVReader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FinanceService {

    private final TransactionRepo txRepo;
    private final BudgetRepo budgetRepo;
    private final GoalRepo goalRepo;

    public FinanceService(TransactionRepo txRepo, BudgetRepo budgetRepo, GoalRepo goalRepo) {
        this.txRepo = txRepo;
        this.budgetRepo = budgetRepo;
        this.goalRepo = goalRepo;
    }

    public Transaction addTx(TxReq req) {
        return txRepo.save(new Transaction(req.date(), req.description(), req.amount(), req.category(), req.type()));
    }

    public void deleteTx(Long id) {
        txRepo.deleteById(id);
    }

    public Budget setBudget(BudgetReq req) {
        Budget b = budgetRepo.findByCategory(req.category().toUpperCase())
                .orElse(new Budget(req.category(), req.monthlyLimit()));
        b.setMonthlyLimit(req.monthlyLimit());
        return budgetRepo.save(b);
    }

    public Goal addGoal(GoalReq req) {
        return goalRepo.save(new Goal(req.name(), req.targetAmount(), req.currentAmount(), req.targetDate()));
    }

    public void updateGoalCurrent(Long id, BigDecimal current) {
        goalRepo.findById(id).ifPresent(g -> {
            g.setCurrentAmount(current);
            goalRepo.save(g);
        });
    }

    public void deleteGoal(Long id) {
        goalRepo.deleteById(id);
    }

    public int importCsv(MultipartFile file) throws Exception {
        List<Transaction> list = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] line;
            boolean header = true;
            while ((line = reader.readNext()) != null) {
                if (header) { header = false; continue; }
                if (line.length < 3) continue;

                LocalDate date = LocalDate.parse(line[0].trim());
                String desc = line[1].trim();
                BigDecimal amt = new BigDecimal(line[2].trim());
                Transaction.Type type = amt.compareTo(BigDecimal.ZERO) < 0 ? Transaction.Type.EXPENSE : Transaction.Type.INCOME;
                String cat = line.length > 3 && !line[3].isBlank() ? line[3].trim() : "GENERAL";

                list.add(new Transaction(date, desc, amt.abs(), cat, type));
            }
        }
        txRepo.saveAll(list);
        detectRecurring();
        return list.size();
    }

    public void detectRecurring() {
        List<Transaction> all = txRepo.findAll();
        Map<String, List<Transaction>> grouped = all.stream()
                .filter(t -> t.getType() == Transaction.Type.EXPENSE)
                .collect(Collectors.groupingBy(t -> t.getDescription().toLowerCase().trim()));

        grouped.forEach((desc, txs) -> {
            if (txs.size() >= 2) {
                txs.forEach(t -> t.setRecurring(true));
                txRepo.saveAll(txs);
            }
        });
    }

    public DashboardView getDashboard() {
        YearMonth now = YearMonth.now();
        LocalDate start = now.atDay(1);
        LocalDate end = now.atEndOfMonth();

        List<Transaction> currentTxs = txRepo.findByDateBetweenOrderByDateDesc(start, end);
        List<Transaction> allTxs = txRepo.findAllByOrderByDateDesc();

        BigDecimal income = currentTxs.stream()
                .filter(t -> t.getType() == Transaction.Type.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expense = currentTxs.stream()
                .filter(t -> t.getType() == Transaction.Type.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal net = income.subtract(expense);
        BigDecimal rate = income.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : net.divide(income, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        Map<String, BigDecimal> catSpent = currentTxs.stream()
                .filter(t -> t.getType() == Transaction.Type.EXPENSE)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        List<BudgetDiff> bDiffs = budgetRepo.findAll().stream().map(b -> {
            BigDecimal spent = catSpent.getOrDefault(b.getCategory(), BigDecimal.ZERO);
            BigDecimal rem = b.getMonthlyLimit().subtract(spent);
            BigDecimal pct = b.getMonthlyLimit().compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : spent.divide(b.getMonthlyLimit(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            return new BudgetDiff(b.getCategory(), b.getMonthlyLimit(), spent, rem, pct, spent.compareTo(b.getMonthlyLimit()) > 0);
        }).toList();

        BigDecimal avgMonthlyNet = computeAvgMonthlyNet(allTxs);

        List<GoalProjection> gProjections = goalRepo.findAll().stream().map(g -> {
            BigDecimal pct = g.getTargetAmount().compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : g.getCurrentAmount().divide(g.getTargetAmount(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            BigDecimal rem = g.getTargetAmount().subtract(g.getCurrentAmount());

            String projDate = "N/A";
            boolean onTrack = false;
            if (rem.compareTo(BigDecimal.ZERO) <= 0) {
                projDate = "Goal Reached!";
                onTrack = true;
            } else if (avgMonthlyNet.compareTo(BigDecimal.ZERO) > 0) {
                int monthsNeeded = rem.divide(avgMonthlyNet, 0, RoundingMode.CEILING).intValue();
                LocalDate estimated = LocalDate.now().plusMonths(monthsNeeded);
                projDate = estimated.format(DateTimeFormatter.ofPattern("MMM yyyy"));
                onTrack = !estimated.isAfter(g.getTargetDate());
            }
            return new GoalProjection(g.getId(), g.getName(), g.getTargetAmount(), g.getCurrentAmount(), g.getTargetDate(), pct, projDate, onTrack);
        }).toList();

        List<RecurringItem> recurring = allTxs.stream()
                .filter(Transaction::isRecurring)
                .collect(Collectors.groupingBy(t -> t.getDescription().toLowerCase().trim()))
                .entrySet().stream()
                .map(e -> {
                    Transaction f = e.getValue().get(0);
                    return new RecurringItem(f.getDescription(), f.getAmount(), f.getCategory(), e.getValue().size());
                }).toList();

        List<MonthlyTrend> trends = computeTrends(allTxs);

        return new DashboardView(
                income.setScale(2, RoundingMode.HALF_UP),
                expense.setScale(2, RoundingMode.HALF_UP),
                net.setScale(2, RoundingMode.HALF_UP),
                rate.setScale(1, RoundingMode.HALF_UP),
                bDiffs,
                gProjections,
                recurring,
                trends,
                catSpent,
                allTxs.stream().limit(15).toList()
        );
    }

    private BigDecimal computeAvgMonthlyNet(List<Transaction> txs) {
        Map<YearMonth, BigDecimal> monthlyNets = txs.stream().collect(Collectors.groupingBy(
                t -> YearMonth.from(t.getDate()),
                Collectors.reducing(BigDecimal.ZERO, t -> t.getType() == Transaction.Type.INCOME ? t.getAmount() : t.getAmount().negate(), BigDecimal::add)
        ));
        if (monthlyNets.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = monthlyNets.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(monthlyNets.size()), 2, RoundingMode.HALF_UP);
    }

    private List<MonthlyTrend> computeTrends(List<Transaction> txs) {
        Map<YearMonth, List<Transaction>> map = txs.stream().collect(Collectors.groupingBy(t -> YearMonth.from(t.getDate())));
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    BigDecimal inc = e.getValue().stream().filter(t -> t.getType() == Transaction.Type.INCOME).map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal exp = e.getValue().stream().filter(t -> t.getType() == Transaction.Type.EXPENSE).map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new MonthlyTrend(e.getKey().toString(), inc, exp, inc.subtract(exp));
                }).toList();
    }
}
