package com.example.BEcodetoconnect.controller;

import com.example.BEcodetoconnect.helper.FileHelper;
import com.example.BEcodetoconnect.model.BalanceDiscrepancy;
import com.example.BEcodetoconnect.model.LedgerBalance;
import com.example.BEcodetoconnect.model.LedgerTransaction;
import com.example.BEcodetoconnect.model.SwiftMessage;
import com.example.BEcodetoconnect.service.FileService;
import com.example.BEcodetoconnect.service.ModelService;
import com.example.BEcodetoconnect.service.ReconciliationService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/rec")
public class ReconciliationController {
    @Autowired
    FileService fileService;
    @Autowired
    ReconciliationService reconciliationService;
    @Autowired
    ModelService modelService;

    @PostMapping("/reconcile")
    public ResponseEntity<String> reconcile(@RequestParam("ledgerTransactionFile") MultipartFile ledgerTransactionFile,
                                              @RequestParam("ledgerBalanceFile") MultipartFile ledgerBalanceFile,
                                              @RequestParam("swiftFile") MultipartFile swiftFile) {
        String message = "";
        if (FileHelper.hasCSVFormat(ledgerTransactionFile) && FileHelper.hasCSVFormat(ledgerBalanceFile) && FileHelper.hasXMLFormat(swiftFile)) {
            try {
                List<LedgerTransaction> ledgerTransactions = fileService.ledgerCSVparseToPOJO(ledgerTransactionFile);
                List<LedgerBalance> ledgerBalances = fileService.balanceCSVparseToPOJO(ledgerBalanceFile);
                SwiftMessage swiftMessage = fileService.swiftXMLparseToPOJO(swiftFile);

                // Save SWIFT message
                modelService.saveSwiftMessage(swiftMessage);

                // Start reconciliation process
                String integrityCheckMessage = reconciliationService.fullIntegrityCheck(ledgerTransactions, swiftMessage);
                log.info("{}", integrityCheckMessage);

                String proofingMessage = reconciliationService.proofing(ledgerBalances, swiftMessage);
                log.info("{}", proofingMessage);

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
