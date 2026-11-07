package com.example.miniledger.controller;

import com.example.miniledger.dto.AccountResponse;
import com.example.miniledger.dto.CreateAccountRequest;
import com.example.miniledger.model.Account;
import com.example.miniledger.repository.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountRepository accountRepo;

    public AccountController(AccountRepository accountRepo) {
        this.accountRepo = accountRepo;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@RequestBody CreateAccountRequest req) {
        BigDecimal balance = req.balance() != null ? req.balance() : BigDecimal.ZERO;
        Account saved = accountRepo.save(new Account(req.owner(), balance));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AccountResponse(saved.getId(), saved.getOwner(), saved.getBalance()));
    }

    @GetMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<AccountResponse> get(@PathVariable Long id) {
        return accountRepo.findById(id)
                .map(a -> ResponseEntity.ok(new AccountResponse(a.getId(), a.getOwner(), a.getBalance())))
                .orElse(ResponseEntity.notFound().build());
    }
}
