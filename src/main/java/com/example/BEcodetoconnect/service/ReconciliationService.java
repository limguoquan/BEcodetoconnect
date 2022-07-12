package com.example.BEcodetoconnect.service;

import com.example.BEcodetoconnect.model.LedgerTransaction;
import com.example.BEcodetoconnect.model.SwiftEntry;
import lombok.extern.slf4j.Slf4j;
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

        Map<Long, List<LedgerTransaction>> ledgerAmounts = ledgerTransactions.stream().collect(
                Collectors.groupingBy(LedgerTransaction::getAmount));

        for (SwiftEntry swiftEntry : swiftTransactions) {
            // singular amount (1..1 mapping)
            Long amount = swiftEntry.getLedgerTransaction().getAmount();
            int numberOfTransactions = swiftEntry.getNbOfTxs();
            Long totalAmount = swiftEntry.getTtlAmt();

            List<LedgerTransaction> matchedLedgerTransactions = ledgerAmounts.getOrDefault(amount, null);

            Iterator<LedgerTransaction> itr = matchedLedgerTransactions.iterator();
            int counter = 0;
            while (itr.hasNext()) {
                if (counter >= numberOfTransactions) {
                    // over
                    break;
                }
                reconciledTransactions.add(itr.next());
                itr.remove();
                counter++;
            }
        }

        for (Map.Entry<Long, List<LedgerTransaction>> entry : ledgerAmounts.entrySet()) {
            for (LedgerTransaction ledgerTransaction : entry.getValue()) {
                unreconciledTransactions.add(ledgerTransaction);
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
