package com.example.miniledger.service;

import com.example.miniledger.model.Account;
import com.example.miniledger.repository.AccountRepository;
import com.example.miniledger.repository.TransferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceUnitTest {

    @Mock AccountRepository accountRepo;
    @Mock TransferRepository transferRepo;
    @InjectMocks TransferService service;

    @Test
    void transferThatWouldLeaveNegativeBalanceIsRejected() {
        Long fromId = 1L, toId = 2L;
        Account from = new Account("Alice", new BigDecimal("50.00"));
        Account to   = new Account("Bob",   new BigDecimal("0.00"));
        ReflectionTestUtils.setField(from, "id", fromId);
        ReflectionTestUtils.setField(to,   "id", toId);

        when(transferRepo.findByIdempotencyKey("k1")).thenReturn(Optional.empty());
        when(accountRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(from));
        when(accountRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(to));

        assertThatThrownBy(() ->
                service.executeTransfer(fromId, toId, new BigDecimal("100.00"), "k1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Saldo insuficiente");

        verify(transferRepo, never()).save(any());
        verify(accountRepo, never()).save(any());
    }
}
