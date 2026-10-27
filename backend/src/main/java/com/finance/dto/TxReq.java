package com.finance.dto;

import com.finance.entity.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TxReq(LocalDate date, String description, BigDecimal amount, String category, Transaction.Type type) {}
