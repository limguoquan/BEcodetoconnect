package com.example.BEcodetoconnect.service;

import com.example.BEcodetoconnect.model.LedgerTransaction;
import com.example.BEcodetoconnect.model.SwiftEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ReconciliationService {

    public HashMap<String, List<LedgerTransaction>> reconcileTransactions(List<LedgerTransaction> ledgerTransactions, List<SwiftEntry> swiftTransactions) {

        HashMap<String, List<LedgerTransaction>> reconciledResults = new HashMap<>();
        List<LedgerTransaction> reconciledTransactions = new ArrayList<>();
        List<LedgerTransaction> unreconciledTransactions = new ArrayList<>();

        Map<Pair<String, Long>, Long> ledgerMapWithRecordCount = ledgerTransactions.stream()
                .collect(Collectors.groupingBy(p -> Pair.of(p.getTransactionReference(), p.getAmount()), Collectors.counting()));

        Map<Pair<String, Long>, List<LedgerTransaction>> ledgerMapWithLedgerTransaction = ledgerTransactions.stream()
                .collect(Collectors.groupingBy(p -> Pair.of(p.getTransactionReference(), p.getAmount())));

        for (SwiftEntry swiftEntry : swiftTransactions) {
            String transactionReference = swiftEntry.getEndToEndId();
            Long amount = swiftEntry.getLedgerTransactions().get(0).getAmount();
            Integer numberOfTransactionsInSwift = swiftEntry.getNbOfTxs();

            Pair<String, Long> key = Pair.of(transactionReference, amount);
            int numberOfTransactionsInLedger = Math.toIntExact(ledgerMapWithRecordCount.get(key));

            if (numberOfTransactionsInSwift <= numberOfTransactionsInLedger) {
                for (int i=0; i<numberOfTransactionsInSwift; i++) {
                    reconciledTransactions.add(ledgerMapWithLedgerTransaction.get(key).get(0));
                    Long curCount = ledgerMapWithRecordCount.get(key);
                    ledgerMapWithRecordCount.put(key, curCount - 1);
                }
            } else {
                for (int i=0; i<numberOfTransactionsInLedger; i++) {
                    reconciledTransactions.add(ledgerMapWithLedgerTransaction.get(key).get(0));
                    Long curCount = ledgerMapWithRecordCount.get(key);
                    ledgerMapWithRecordCount.put(key, curCount - 1);
                }

                for (int i=0; i<(numberOfTransactionsInSwift-numberOfTransactionsInLedger); i++) {
                    unreconciledTransactions.add(ledgerMapWithLedgerTransaction.get(key).get(0));
                }
            }
        }

        for (Map.Entry<Pair<String, Long>, Long> entry : ledgerMapWithRecordCount.entrySet()) {
            if (entry.getValue() > 0) {
                log.info("{}", entry.getValue());
                unreconciledTransactions.add(ledgerMapWithLedgerTransaction.get(entry.getKey()).get(0));
            }
        }


        // consider mock data for edge cases, consider the opposite way (swift > ledger transactions)
        // consider balance reconciliation
        // consider swift model store
        // refactor code for granular methods/modular design
        // make code more readable (e.g counter)
        // java 8
        // transaction reference key mapping


        reconciledResults.put("reconciledTransactions", reconciledTransactions);
        reconciledResults.put("unreconciledTransactions", unreconciledTransactions);
        return reconciledResults;
    }


}
