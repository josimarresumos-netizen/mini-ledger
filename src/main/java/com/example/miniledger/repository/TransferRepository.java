package com.example.miniledger.repository;

import com.example.miniledger.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);
}
