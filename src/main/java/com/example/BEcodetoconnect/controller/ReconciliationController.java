package com.example.BEcodetoconnect.controller;

import com.example.BEcodetoconnect.helper.FileHelper;
import com.example.BEcodetoconnect.model.LedgerTransaction;
import com.example.BEcodetoconnect.model.SwiftEntry;
import com.example.BEcodetoconnect.service.LedgerTransaction.LedgerTransactionService;
import com.example.BEcodetoconnect.service.Reconciliation.ReconciliationService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/rec")
public class ReconciliationController {
    @Autowired
    LedgerTransactionService ledgerTransactionService;
    @Autowired
    ReconciliationService reconciliationService;

    @PostMapping("/reconcile")
    public ResponseEntity<String> reconcile(@RequestParam("ledgerFile") MultipartFile ledgerFile,
                                              @RequestParam("swiftFile") MultipartFile swiftFile) {
        String message = "";
        if (FileHelper.hasCSVFormat(ledgerFile) && FileHelper.hasXMLFormat(swiftFile)) {
            try {
                List<LedgerTransaction> ledgerTransactions = ledgerTransactionService.parseToPOJO(ledgerFile).stream()
                        .map(element -> (LedgerTransaction) element)
                        .collect(Collectors.toList());
                List<SwiftEntry> swiftTransactions = ledgerTransactionService.parseToPOJO(swiftFile).stream()
                        .map(element -> (SwiftEntry) element)
                        .collect(Collectors.toList());
                HashMap<String, List<LedgerTransaction>> reconciledResults = reconciliationService.reconcileTransactions(ledgerTransactions, swiftTransactions);

                List<LedgerTransaction> reconciledTransactions = reconciledResults.get("reconciledTransactions");
                Integer counter = 0;
                for (LedgerTransaction ledgerTransaction : reconciledTransactions) {
                    counter++;
                    log.info("{}", ledgerTransaction);
                }
                log.info("{}", counter);

                return ResponseEntity.status(HttpStatus.OK).body("Reconciliation successful!");
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(e.getMessage());
            }
        }
        message = "Please upload correct file formats (CSV, XML)!";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<LedgerTransaction>> getAllTransactionsbyTransactionReference(@RequestParam(name="transactionReference") String transactionReference) {
        try {
            List<LedgerTransaction> tutorials = ledgerTransactionService.getAllTransactionsByTransactionReference(transactionReference);
            if (tutorials.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(tutorials, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("")
//    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> find() {
        return new ResponseEntity<String>("Hi", HttpStatus.OK);
    }

}
