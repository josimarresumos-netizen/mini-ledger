package com.example.miniledger.controller;

import com.example.miniledger.dto.TransferRequest;
import com.example.miniledger.dto.TransferResponse;
import com.example.miniledger.service.TransferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> transfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody TransferRequest req) {
        var transfer = transferService.executeTransfer(
                req.fromAccountId(), req.toAccountId(), req.amount(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(TransferResponse.from(transfer));
    }
}
