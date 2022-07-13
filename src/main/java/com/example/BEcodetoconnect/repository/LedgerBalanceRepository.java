package com.example.BEcodetoconnect.repository;

import com.example.BEcodetoconnect.model.LedgerBalance;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LedgerBalanceRepository extends MongoRepository<LedgerBalance, String> {
}