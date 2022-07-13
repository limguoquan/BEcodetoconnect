package com.example.BEcodetoconnect.controller;

import com.example.BEcodetoconnect.helper.FileHelper;
import com.example.BEcodetoconnect.model.LedgerTransaction;
import com.example.BEcodetoconnect.model.SwiftEntry;
import com.example.BEcodetoconnect.model.SwiftMessage;
import com.example.BEcodetoconnect.service.FileService;
import com.example.BEcodetoconnect.service.ReconciliationService;
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
    FileService fileService;
    @Autowired
    ReconciliationService reconciliationService;

    @PostMapping("/reconcile")
    public ResponseEntity<String> reconcile(@RequestParam("ledgerFile") MultipartFile ledgerFile,
                                              @RequestParam("swiftFile") MultipartFile swiftFile) {
        String message = "";
        if (FileHelper.hasCSVFormat(ledgerFile) && FileHelper.hasXMLFormat(swiftFile)) {
            try {
                List<LedgerTransaction> ledgerTransactions = fileService.CSVparseToPOJO(ledgerFile);
                SwiftMessage swiftMessage = fileService.XMLparseToPOJO(swiftFile);
                reconciliationService.save(swiftMessage);
//                HashMap<String, List<LedgerTransaction>> reconciledResults = reconciliationService.reconcileTransactions(ledgerTransactions, swiftMessage);
//
//                List<LedgerTransaction> reconciledTransactions = reconciledResults.get("reconciledTransactions");
//                Integer counter = 0;
//                for (LedgerTransaction ledgerTransaction : reconciledTransactions) {
//                    counter++;
//                    log.info("{}", ledgerTransaction);
//                }
//                log.info("reconciled {}", counter);
//
//                List<LedgerTransaction> unreconciledTransactions = reconciledResults.get("unreconciledTransactions");
//                counter = 0;
//                for (LedgerTransaction ledgerTransaction : unreconciledTransactions) {
//                    counter++;
//                    log.info("{}", ledgerTransaction);
//                }
//                log.info("unreconciled {}", counter);

                return ResponseEntity.status(HttpStatus.OK).body("Reconciliation successful!");
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(e.getMessage());
            }
        }
        message = "Please upload correct file formats (CSV, XML)!";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

}
