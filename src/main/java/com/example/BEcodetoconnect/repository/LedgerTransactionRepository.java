package com.example.BEcodetoconnect.repository;

import com.example.BEcodetoconnect.model.LedgerTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LedgerTransactionRepository extends MongoRepository<LedgerTransaction, String> {
    List<LedgerTransaction> findByTransactionReference(String transactionReference);
}
