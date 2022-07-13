package com.example.BEcodetoconnect.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@Document("swiftMessages")
public class SwiftMessage {
    private String bicfiFrom;
    private String bicfiTo;
    private String msgId;
    private Date creDtTm;
    private String stmtId;
    private Integer pgNb;
    private String lastPgInd;
    private String lglSeqNb;
    private String acctId;
    private String ccy; //header statement currency

    private List<Balance> balances;
    private List<SwiftEntry> swiftEntries;
}
