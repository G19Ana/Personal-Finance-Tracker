package com.finance.controller;

import com.finance.dto.BudgetReq;
import com.finance.dto.DashboardView;
import com.finance.dto.GoalReq;
import com.finance.dto.TxReq;
import com.finance.entity.Budget;
import com.finance.entity.Goal;
import com.finance.entity.Transaction;
import com.finance.service.FinanceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/finance")
@CrossOrigin(origins = "*")
public class FinanceController {

    private final FinanceService service;

    public FinanceController(FinanceService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public DashboardView getDashboard() {
        return service.getDashboard();
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction addTransaction(@RequestBody TxReq req) {
        return service.addTx(req);
    }

    @DeleteMapping("/transactions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransaction(@PathVariable Long id) {
        service.deleteTx(id);
    }

    @PostMapping("/budgets")
    public Budget setBudget(@RequestBody BudgetReq req) {
        return service.setBudget(req);
    }

    @PostMapping("/goals")
    @ResponseStatus(HttpStatus.CREATED)
    public Goal addGoal(@RequestBody GoalReq req) {
        return service.addGoal(req);
    }

    @PatchMapping("/goals/{id}")
    public void updateGoal(@PathVariable Long id, @RequestBody Map<String, BigDecimal> body) {
        service.updateGoalCurrent(id, body.get("currentAmount"));
    }

    @DeleteMapping("/goals/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGoal(@PathVariable Long id) {
        service.deleteGoal(id);
    }

    @PostMapping("/import-csv")
    public Map<String, Integer> importCsv(@RequestParam("file") MultipartFile file) throws Exception {
        int count = service.importCsv(file);
        return Map.of("imported", count);
    }
}
