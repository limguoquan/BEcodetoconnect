package com.example.BEcodetoconnect.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@AllArgsConstructor
@Document("ledgerTransactions")
public class LedgerTransaction {

    private String account;
    private String valueDate;
    private String currency;
    private String creditDebit;
    private Long amount;
    private String transactionReference;

}
