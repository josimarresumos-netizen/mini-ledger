package com.example.miniledger.service;

import com.example.miniledger.model.Account;
import com.example.miniledger.model.Transfer;
import com.example.miniledger.repository.AccountRepository;
import com.example.miniledger.repository.TransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class TransferService {

    private final AccountRepository accountRepo;
    private final TransferRepository transferRepo;

    public TransferService(AccountRepository accountRepo, TransferRepository transferRepo) {
        this.accountRepo = accountRepo;
        this.transferRepo = transferRepo;
    }

    /**
     * Atomicidade: @Transactional garante que débito + crédito + registro
     * do Transfer acontecem em UMA transação. Qualquer exceção não-checada
     * faz rollback de tudo.
     */
    @Transactional
    public Transfer executeTransfer(Long fromId, Long toId, BigDecimal amount, String idempotencyKey) {

        // -------- 1. Idempotência (barreira 1: aplicação) --------
        Optional<Transfer> existing = transferRepo.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        // -------- 2. Validações de negócio --------
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount deve ser > 0");
        }
        if (amount.scale() != 2) {
            throw new IllegalArgumentException("amount deve ter exatamente 2 casas decimais (ex: 10.00)");
        }
        if (fromId == null || toId == null || fromId.equals(toId)) {
            throw new IllegalArgumentException("fromAccountId e toAccountId devem ser diferentes");
        }

        // -------- 3. Lock pessimista com ordenação para evitar deadlock --------
        // Sempre tranca o menor ID primeiro, depois o maior.
        Long firstId  = Math.min(fromId, toId);
        Long secondId = Math.max(fromId, toId);

        Account first  = accountRepo.findByIdForUpdate(firstId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada: " + firstId));
        Account second = accountRepo.findByIdForUpdate(secondId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada: " + secondId));

        Account from = firstId.equals(fromId) ? first : second;
        Account to   = firstId.equals(toId)   ? first : second;

        // -------- 4. Regra de saldo --------
        if (from.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Saldo insuficiente");
        }

        // -------- 5. Débito e crédito (BigDecimal é imutável) --------
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        // -------- 6. Registro da transferência (barreira 2: UNIQUE no banco) --------
        Transfer transfer = new Transfer(fromId, toId, amount, idempotencyKey);
        return transferRepo.save(transfer);
    }
}
