package com.example.BEcodetoconnect.service;

import com.example.BEcodetoconnect.model.LedgerTransaction;
import com.example.BEcodetoconnect.repository.LedgerTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@Slf4j
public class ReconciliationService {
    @Autowired
    LedgerTransactionRepository ledgerTransactionRepository;

    public void reconcileTransactions(List<LedgerTransaction> ledgerTransactions, List<LedgerTransaction> swiftTransactions) {

        // define rules for matching
        // all match account number
        // one-to-one:
        // 1. account
        // 2. transaction reference
        // 3. exact amount
        // one-to-many / many-to-many:
        // 1. account
        // 2. total sum

    }


}
