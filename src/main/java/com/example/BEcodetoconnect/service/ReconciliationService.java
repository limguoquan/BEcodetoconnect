package com.example.BEcodetoconnect.service;

import com.example.BEcodetoconnect.model.*;
import com.example.BEcodetoconnect.repository.SwiftMessageRepository;
import com.example.BEcodetoconnect.utils.DateFormatter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ReconciliationService {

    @Autowired
    DateFormatter dateFormatter;

    @Autowired
    ModelService modelService;

    public String fullIntegrityCheck(List<LedgerTransaction> ledgerTransactions, SwiftMessage swiftMessage) {
        // By default, there are no unreconciled transactions
        String reconciliationMessage = "There are no unreconciled transactions from the full integrity check!";

        List<LedgerTransaction> reconciledTransactions = new ArrayList<>();
        List<LedgerTransaction> unreconciledTransactions = new ArrayList<>();

        // We make use of first map to perform matching algorithm
        // ********* First map *********
        // key is a pair of Transaction Reference and Ledger Transaction Amount
        // value is the number of transaction ledgers that have that certain transaction reference and amount
        // The intuition here is that we will decrement the number of times that ledger transaction appears by 1 if
        // it matches a swift entry transaction. And after checking all swift entries, if there are remaining ledgers
        // in this map, those are unreconciled.
        Map<Pair<String, Double>, Long> ledgerMapWithRecordCount = ledgerTransactions.stream()
                .collect(Collectors.groupingBy(p -> Pair.of(p.getTransactionReference(), p.getAmount()), Collectors.counting()));

        // We make use of second map to get the ledger transaction object because we want to
        // store this ledger transaction in MongoDB in the case of it being unreconciled
        // ********* Second map *********
        // key is a pair of Transaction Reference and Ledger Transaction Amount
        // value is the ledger transaction object
        Map<Pair<String, Double>, List<LedgerTransaction>> ledgerMapWithLedgerTransaction = ledgerTransactions.stream()
                .collect(Collectors.groupingBy(p -> Pair.of(p.getTransactionReference(), p.getAmount())));

        // We make use of third map to get swift entry object because we want to
        // store this swift entry in MongoDB in the case of it being unreconciled
        // ********* Third map *********
        // key is a Ledger Transaction amount (which is same as the singular amount found in each swift entry)
        // value is the swift entry object
        Map<Double, SwiftEntry> swiftEntryUnreconciled = new HashMap<>();

        for (SwiftEntry swiftEntry : swiftMessage.getSwiftEntries()) {
            String transactionReference = swiftEntry.getEndToEndId();
            Double amount = swiftEntry.getAmt();
            Integer numberOfTransactionsInSwift = swiftEntry.getNbOfTxs();

            // EDGE CASE
            // In the provided resource, in the first swift entry, there is TxDtls but no Btch
            // Which means even though there are no NbOfTxs fields, it is still considered as one transaction
            if (numberOfTransactionsInSwift == null) {
                numberOfTransactionsInSwift = 1;
            }

            int numberOfTransactionsInLedger;
            // Initialise key for less verbose code
            Pair<String, Double> key = Pair.of(transactionReference, amount);
            if (ledgerMapWithRecordCount.get(key) != null) {
                numberOfTransactionsInLedger = Math.toIntExact(ledgerMapWithRecordCount.get(key));
            } else {
                // In this else statement, we take care of a certain scenario (can be replicated with SampleSet2 ).
                // This scenario happens when there are no matching ledger transactions with swift entry transactions
                reconciliationMessage = "There are unreconciled transaction ledgers!";
                UnreconciledTransaction unreconciledTransaction =
                        unreconciledTransactionBuilder(
                                null,
                                swiftMessage,
                                swiftEntry
                        );
                swiftEntryUnreconciled.put(amount, swiftEntry);
                modelService.saveUnreconciledTransaction(unreconciledTransaction);
                unreconciledTransactions.add(ledgerMapWithLedgerTransaction.get(key).get(0));
                continue;
            }

            // If number of transactions in swift entry is less than number of ledger transactions with matching ( SampleSet 1 )
            // transaction reference and amount, we continue to decrement our map value
            if (numberOfTransactionsInSwift <= numberOfTransactionsInLedger) {
                for (int i=0; i<numberOfTransactionsInSwift; i++) {
                    reconciledTransactions.add(ledgerMapWithLedgerTransaction.get(key).get(0));
                    Long curCount = ledgerMapWithRecordCount.get(key);
                    ledgerMapWithRecordCount.put(key, curCount - 1);
                }
            } else {
                // But, if there are more swift entry transactions than ledger transactions, they are considered unreconciled transactions
                for (int i=0; i<numberOfTransactionsInLedger; i++) {
                    reconciledTransactions.add(ledgerMapWithLedgerTransaction.get(key).get(0));
                    Long curCount = ledgerMapWithRecordCount.get(key);
                    ledgerMapWithRecordCount.put(key, curCount - 1);
                }

                // Add it into unreconciledTransaction MongoDB collection
                for (int i=0; i<(numberOfTransactionsInSwift-numberOfTransactionsInLedger); i++) {
                    reconciliationMessage = "There are unreconciled transaction ledgers!";
                    UnreconciledTransaction unreconciledTransaction =
                            unreconciledTransactionBuilder(
                                    ledgerMapWithLedgerTransaction.get(key).get(0),
                                    swiftMessage,
                                    swiftEntry
                            );
                    swiftEntryUnreconciled.put(amount, swiftEntry);
                    modelService.saveUnreconciledTransaction(unreconciledTransaction);
                    unreconciledTransactions.add(ledgerMapWithLedgerTransaction.get(key).get(0));
                }
            }
        }

        // For each remaining ledger transaction in our map 1, they are considered unreconciled transactions
        for (Map.Entry<Pair<String, Double>, Long> entry : ledgerMapWithRecordCount.entrySet()) {
            if (entry.getValue() > 0) {
                reconciliationMessage = "There are unreconciled transaction ledgers!";
                SwiftEntry swiftEntry = swiftEntryUnreconciled.get(entry.getKey().getSecond());
                UnreconciledTransaction unreconciledTransaction =
                        unreconciledTransactionBuilder(
                                ledgerMapWithLedgerTransaction.get(entry.getKey()).get(0),
                                swiftMessage,
                                swiftEntry
                        );
                modelService.saveUnreconciledTransaction(unreconciledTransaction);
                unreconciledTransactions.add(ledgerMapWithLedgerTransaction.get(entry.getKey()).get(0));
            }
        }

        modelService.saveLedgerTransactions(reconciledTransactions);

        if (reconciliationMessage.equals("There are unreconciled transaction ledgers!")) {

//            String json = new Gson().toJson(unreconciledTransactions);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(unreconciledTransactions);
            log.info("{}", json);

            // return unreconciled transactions in format of json string for readability
            return json;
        } else {
            return reconciliationMessage;
        }
    }

    public String proofing(List<LedgerBalance> ledgerBalances, SwiftMessage swiftMessage) {
        // Convert times to LocalDate to compare by date (without specific hours and minutes)
        LocalDate chosenDate = dateFormatter.dateToLocalDate(swiftMessage.getCreDtTm());
        List<LedgerBalance> ledgerBalancesWithSameDate = ledgerBalances.stream()
                .filter(ledgerBalance -> dateFormatter.dateToLocalDate(ledgerBalance.getAsOfDateTS()).equals(chosenDate))
                .toList();

        LedgerBalance openingBalance = Collections.min(ledgerBalancesWithSameDate, Comparator.comparing(ledgerBalance -> ledgerBalance.getAsOfDateTS()));
        LedgerBalance closingBalance = Collections.max(ledgerBalancesWithSameDate, Comparator.comparing(ledgerBalance -> ledgerBalance.getAsOfDateTS()));

        Double swiftOpeningBalance = swiftMessage.getBalances().get(0).getAmt();
        Double swiftClosingBalance = swiftMessage.getBalances().get(1).getAmt();

        // Any discrepancy in balance, save it into our balance discrepancy object
        if (!openingBalance.getBalance().equals(swiftOpeningBalance) || !closingBalance.getBalance().equals(swiftClosingBalance)) {
            modelService.saveBalanceDiscrepancy(balanceDiscrepancyBuilder(
                    openingBalance,
                    closingBalance,
                    swiftMessage,
                    swiftOpeningBalance,
                    swiftClosingBalance
            ));
            return String.format("Ledger opening balance of %f and closing balance of %f is different from" +
                    " SWIFT opening balance of %f and closing balance of %f", openingBalance.getBalance(), closingBalance.getBalance(), swiftOpeningBalance, swiftClosingBalance);
        } else {
            modelService.saveLedgerBalances(ledgerBalances);
        }
        return "There are no discrepancies!";
    }

    public UnreconciledTransaction unreconciledTransactionBuilder(LedgerTransaction ledgerTransaction, SwiftMessage swiftMessage, SwiftEntry swiftEntry) {
        return new UnreconciledTransaction(
                ledgerTransaction,
                swiftMessage.getBicfiFrom(),
                swiftMessage.getBicfiTo(),
                swiftMessage.getMsgId(),
                swiftMessage.getCreDtTm(),
                swiftMessage.getStmtId(),
                swiftMessage.getPgNb(),
                swiftMessage.getLastPgInd(),
                swiftMessage.getLglSeqNb(),
                swiftMessage.getAcctId(),
                swiftMessage.getCcy(),
                swiftEntry
        );
    }

    public BalanceDiscrepancy balanceDiscrepancyBuilder(LedgerBalance openingBalance, LedgerBalance closingBalance, SwiftMessage swiftMessage, Double swiftOpeningBalance, Double swiftClosingBalance) {
        return new BalanceDiscrepancy(
                openingBalance.getAccount(),
                openingBalance.getAsOfDateTS(),
                swiftMessage.getBicfiFrom(),
                swiftMessage.getBicfiTo(),
                swiftMessage.getMsgId(),
                swiftMessage.getCreDtTm(),
                swiftMessage.getStmtId(),
                swiftMessage.getPgNb(),
                swiftMessage.getLastPgInd(),
                swiftMessage.getLglSeqNb(),
                swiftMessage.getAcctId(),
                swiftMessage.getCcy(),
                openingBalance.getBalance(),
                swiftOpeningBalance,
                closingBalance.getBalance(),
                swiftClosingBalance
        );
    }

}
