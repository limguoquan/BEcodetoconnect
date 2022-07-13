package com.example.BEcodetoconnect.repository;

import com.example.BEcodetoconnect.model.UnreconciledTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UnreconciledTransactionRepository extends MongoRepository<UnreconciledTransaction, String> {
}
