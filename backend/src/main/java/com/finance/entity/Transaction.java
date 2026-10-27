package com.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_tx_date", columnList = "date"),
    @Index(name = "idx_tx_category", columnList = "category")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Type type;

    private boolean recurring = false;

    public enum Type { INCOME, EXPENSE }

    public Transaction() {}

    public Transaction(LocalDate date, String description, BigDecimal amount, String category, Type type) {
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.category = category.toUpperCase();
        this.type = type;
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public String getCategory() { return category; }
    public Type getType() { return type; }
    public boolean isRecurring() { return recurring; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }
}
