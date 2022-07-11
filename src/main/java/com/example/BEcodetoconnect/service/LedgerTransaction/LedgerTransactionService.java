package com.example.BEcodetoconnect.service.LedgerTransaction;

import com.example.BEcodetoconnect.helper.FileHelper;
import com.example.BEcodetoconnect.model.LedgerTransaction;
import com.example.BEcodetoconnect.model.SwiftEntry;
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

    public List<Object> parseToPOJO (MultipartFile file) {
        List<Object> records = null;
        try {
            if (CSV_TYPE.equals(file.getContentType())) {
                records = FileHelper.csvToLedgerTransactions(file.getInputStream());
            } else if (XML_TYPE.equals(file.getContentType())) {
                records = FileHelper.xmlToLedgerTransactions(file.getInputStream());
            }
            return records;
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void save(MultipartFile file) {

    }

    public void save2(MultipartFile file) {
//        try {
//            List<LedgerTransaction> ledgerTransactions = FileHelper.csvToLedgerTransactions(file.getInputStream());
//            ledgerTransactionRepository.saveAll(ledgerTransactions);
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to store csv data: " + e.getMessage());
//        }
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
