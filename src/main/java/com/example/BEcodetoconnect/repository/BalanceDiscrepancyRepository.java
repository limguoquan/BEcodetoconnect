package com.example.BEcodetoconnect.repository;

import com.example.BEcodetoconnect.model.BalanceDiscrepancy;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BalanceDiscrepancyRepository extends MongoRepository<BalanceDiscrepancy, String> {
}
