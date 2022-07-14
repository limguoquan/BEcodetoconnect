package com.example.BEcodetoconnect.service;

import com.example.BEcodetoconnect.model.*;
import com.example.BEcodetoconnect.repository.SwiftMessageRepository;
import com.example.BEcodetoconnect.utils.DateFormatter;
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
        String reconciliationMessage = "There are no unreconciled transactions from the full integrity check!";

        HashMap<String, List<LedgerTransaction>> reconciledResults = new HashMap<>();
        List<LedgerTransaction> reconciledTransactions = new ArrayList<>();
        List<LedgerTransaction> unreconciledTransactions = new ArrayList<>();

        Map<Pair<String, Double>, Long> ledgerMapWithRecordCount = ledgerTransactions.stream()
                .collect(Collectors.groupingBy(p -> Pair.of(p.getTransactionReference(), p.getAmount()), Collectors.counting()));

        Map<Pair<String, Double>, List<LedgerTransaction>> ledgerMapWithLedgerTransaction = ledgerTransactions.stream()
                .collect(Collectors.groupingBy(p -> Pair.of(p.getTransactionReference(), p.getAmount())));

        Map<Double, SwiftEntry> swiftEntryUnreconciled = new HashMap<>();

        for (SwiftEntry swiftEntry : swiftMessage.getSwiftEntries()) {
            String transactionReference = swiftEntry.getEndToEndId();
            Double amount = swiftEntry.getAmt();
            Integer numberOfTransactionsInSwift = swiftEntry.getNbOfTxs();

            if (numberOfTransactionsInSwift == null) {
                numberOfTransactionsInSwift = 1;
            }

            int numberOfTransactionsInLedger;
            Pair<String, Double> key = Pair.of(transactionReference, amount);
            if (ledgerMapWithRecordCount.get(key) != null) {
                numberOfTransactionsInLedger = Math.toIntExact(ledgerMapWithRecordCount.get(key));
            } else {
                reconciliationMessage = "There are unreconciled transaction ledgers!";
                UnreconciledTransaction unreconciledTransaction =
                        unreconciledTransactionBuilder(
                                null,
                                swiftMessage,
                                swiftEntry
                        );
                swiftEntryUnreconciled.put(amount, swiftEntry);
                modelService.saveUnreconciledTransaction(unreconciledTransaction);
                continue;
            }

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
                    reconciliationMessage = "There are unreconciled transaction ledgers!";
                    UnreconciledTransaction unreconciledTransaction =
                            unreconciledTransactionBuilder(
                                    ledgerMapWithLedgerTransaction.get(key).get(0),
                                    swiftMessage,
                                    swiftEntry
                            );
                    swiftEntryUnreconciled.put(amount, swiftEntry);
                    modelService.saveUnreconciledTransaction(unreconciledTransaction);
                }
            }
        }

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
        return reconciliationMessage;
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

        if (!openingBalance.getBalance().equals(swiftOpeningBalance) || !closingBalance.getBalance().equals(swiftClosingBalance)) {
            modelService.saveBalanceDiscrepancy(balanceDiscrepancyBuilder(
                    openingBalance,
                    closingBalance,
                    swiftMessage,
                    swiftOpeningBalance,
                    swiftClosingBalance
            ));
            return "There are discrepancies!";
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
