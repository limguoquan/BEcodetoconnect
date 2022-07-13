package com.example.BEcodetoconnect.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Stack;

@Data
@AllArgsConstructor
public class SwiftEntry {

    private Stack<LedgerTransaction> ledgerTransactions;
    private String endToEndId;
    private String uetr;
    private String pmtInfId;
    private Integer nbOfTxs;
    private Long ttlAmt;

}
