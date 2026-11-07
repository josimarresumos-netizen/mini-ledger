package com.example.miniledger.dto;

import java.math.BigDecimal;

public record AccountResponse(Long id, String owner, BigDecimal balance) {}
