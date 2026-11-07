package com.example.miniledger.dto;

import java.math.BigDecimal;

public record TransferRequest(Long fromAccountId, Long toAccountId, BigDecimal amount) {}
