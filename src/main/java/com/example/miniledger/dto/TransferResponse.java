package com.example.miniledger.dto;

import com.example.miniledger.model.Transfer;
import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(Long id, Long fromAccountId, Long toAccountId,
                               BigDecimal amount, String status, Instant createdAt) {
    public static TransferResponse from(Transfer t) {
        return new TransferResponse(t.getId(), t.getFromAccountId(), t.getToAccountId(),
                t.getAmount(), t.getStatus().name(), t.getCreatedAt());
    }
}
