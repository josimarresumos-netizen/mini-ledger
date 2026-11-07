package com.example.miniledger.dto;

import java.math.BigDecimal;

public record CreateAccountRequest(String owner, BigDecimal balance) {}
