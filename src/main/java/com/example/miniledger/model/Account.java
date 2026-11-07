package com.example.miniledger.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Version
    private Long version;

    protected Account() {}

    public Account(String owner, BigDecimal balance) {
        this.owner = owner;
        this.balance = balance;
    }

    public Long getId() { return id; }
    public String getOwner() { return owner; }
    public BigDecimal getBalance() { return balance; }
    public Long getVersion() { return version; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
