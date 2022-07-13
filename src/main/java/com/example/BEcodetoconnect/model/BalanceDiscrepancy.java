package com.example.BEcodetoconnect.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@AllArgsConstructor
@Document("balanceDiscrepancies")
public class BalanceDiscrepancy {
    // Ledger balance identifiers
    private String account;
    private Date date;

    // Swift message identifiers
    private String bicfiFrom;
    private String bicfiTo;
    private String msgId;
    private Date creDtTm;
    private String stmtId;
    private Integer pgNb;
    private String lastPgInd;
    private String lglSeqNb;
    private String acctId;
    private String ccy;

    // Discrepancies
    private Double ledgerOpeningBalance;
    private Double swiftOpeningBalance;
    private Double ledgerClosingBalance;
    private Double swiftClosingBalance;
}
