package com.example.BEcodetoconnect.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@AllArgsConstructor
@Document("swiftEntries")
public class SwiftEntry {

    private LedgerTransaction ledgerTransaction;
    private String uetr;
    private String pmtInfId;
    private Integer nbOfTxs;
    private Long ttlAmt;

}
