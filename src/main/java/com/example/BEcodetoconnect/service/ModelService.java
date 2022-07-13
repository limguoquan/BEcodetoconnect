package com.example.BEcodetoconnect.service;

import com.example.BEcodetoconnect.model.*;
import com.example.BEcodetoconnect.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ModelService {
    @Autowired
    SwiftMessageRepository swiftMessageRepository;

    @Autowired
    LedgerTransactionRepository ledgerTransactionRepository;

    @Autowired
    LedgerBalanceRepository ledgerBalanceRepository;

    @Autowired
    BalanceDiscrepancyRepository balanceDiscrepancyRepository;

    @Autowired
    UnreconciledTransactionRepository unreconciledTransactionRepository;

    public void saveSwiftMessage(SwiftMessage swiftMessage) {
        swiftMessageRepository.save(swiftMessage);
    }

    public void saveLedgerTransactions(List<LedgerTransaction> ledgerTransactions) {
        ledgerTransactionRepository.saveAll(ledgerTransactions);
    }

    public void saveLedgerBalances(List<LedgerBalance> ledgerBalances) {
        ledgerBalanceRepository.saveAll(ledgerBalances);
    }

    public void saveBalanceDiscrepancy(BalanceDiscrepancy balanceDiscrepancy) {
        balanceDiscrepancyRepository.save(balanceDiscrepancy);
    }

    public void saveUnreconciledTransaction(UnreconciledTransaction unreconciledTransaction) {
        unreconciledTransactionRepository.save(unreconciledTransaction);
    }

}
