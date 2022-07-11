package com.example.BEcodetoconnect.service;

import com.example.BEcodetoconnect.helper.FileHelper;
import com.example.BEcodetoconnect.model.LedgerTransaction;
import com.example.BEcodetoconnect.repository.LedgerTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class LedgerTransactionService {
    public static String CSV_TYPE = "text/csv";
    public static String XML_TYPE = "application/xml";
    @Autowired
    LedgerTransactionRepository ledgerTransactionRepository;

    public List<LedgerTransaction> parseToPOJO (MultipartFile file) {
        List<LedgerTransaction> ledgerTransactions = null;
        try {
            if (file.getContentType() == CSV_TYPE) {
                ledgerTransactions = FileHelper.csvToLedgerTransactions(file.getInputStream());
            } else if (file.getContentType() == XML_TYPE) {
                ledgerTransactions = FileHelper.xmlToLedgerTransactions(file.getInputStream());
            }
            return ledgerTransactions;
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void save(MultipartFile file) {
        try {
            List<LedgerTransaction> ledgerTransactions = FileHelper.xmlToLedgerTransactions(file.getInputStream());
            ledgerTransactionRepository.saveAll(ledgerTransactions);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store csv data: " + e.getMessage());
        }
    }

    public void save2(MultipartFile file) {
        try {
            List<LedgerTransaction> ledgerTransactions = FileHelper.csvToLedgerTransactions(file.getInputStream());
            ledgerTransactionRepository.saveAll(ledgerTransactions);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store csv data: " + e.getMessage());
        }
    }

    public void test(MultipartFile file) {
        try {
            FileHelper.xmlToLedgerTransactions(file.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<LedgerTransaction> getAllTransactionsByTransactionReference(String transactionReference) {
        return ledgerTransactionRepository.findByTransactionReference(transactionReference);
    }
}
