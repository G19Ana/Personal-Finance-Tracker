package com.finance.repo;

import com.finance.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BudgetRepo extends JpaRepository<Budget, Long> {
    Optional<Budget> findByCategory(String category);
}
