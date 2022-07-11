package com.example.BEcodetoconnect.service.Reconciliation;

import com.example.BEcodetoconnect.model.LedgerTransaction;
import com.example.BEcodetoconnect.model.SwiftEntry;
import com.example.BEcodetoconnect.repository.LedgerTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ReconciliationService {
    @Autowired
    LedgerTransactionRepository ledgerTransactionRepository;

    public HashMap<String, List<LedgerTransaction>> reconcileTransactions(List<LedgerTransaction> ledgerTransactions, List<SwiftEntry> swiftTransactions) {

//        Collections.sort(ledgerTransactions, Comparator.comparingLong(LedgerTransaction ::getAmount).reversed());
//        Collections.sort(swiftTransactions, Comparator.comparingLong(SwiftEntry ::getTtlAmt).reversed());

        HashMap<String, List<LedgerTransaction>> reconciledResults = new HashMap<>();
        List<LedgerTransaction> reconciledTransactions = new ArrayList<LedgerTransaction>();
        List<LedgerTransaction> unreconciledTransactions = new ArrayList<LedgerTransaction>();

        Map<Long, List<LedgerTransaction>> ledgerAmounts = ledgerTransactions.stream().collect(
                Collectors.groupingBy(LedgerTransaction::getAmount));

        for (SwiftEntry swiftEntry : swiftTransactions) {
            Long amount = swiftEntry.getLedgerTransaction().getAmount();
            log.info("amount {}", amount);
            int numberOfTransactions = swiftEntry.getNbOfTxs();
            Long totalAmount = swiftEntry.getTtlAmt();

            List<LedgerTransaction> matchedLedgerTransactions = ledgerAmounts.getOrDefault(amount, null);

            if (matchedLedgerTransactions != null) {
                int counter = 0;
                for (LedgerTransaction ledgerTransaction : matchedLedgerTransactions) {
                    if (counter >= numberOfTransactions) {
                        break;
                    }
                    reconciledTransactions.add(ledgerTransaction);
                    counter++;
                }
            }

        }

        reconciledResults.put("reconciledTransactions", reconciledTransactions);
        reconciledResults.put("unreconciledTransactions", unreconciledTransactions);
        return reconciledResults;
    }


}
