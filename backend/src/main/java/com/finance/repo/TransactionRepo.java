package com.finance.repo;

import com.finance.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepo extends JpaRepository<Transaction, Long> {
    List<Transaction> findByDateBetweenOrderByDateDesc(LocalDate start, LocalDate end);
    List<Transaction> findAllByOrderByDateDesc();
}
