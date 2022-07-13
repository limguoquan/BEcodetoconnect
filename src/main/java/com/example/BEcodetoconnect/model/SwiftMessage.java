package com.example.BEcodetoconnect.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
public class SwiftMessage {
    private String bicfiFrom;
    private String bicfiTo;
    private String msgId;
    private Date creDtTm;
    private String acctId;
    private String ccy; //header statement currency

    private Long openingBalance;
    private String openingBalanceCcy;
    private String openingCreditDebit;
    private Long closingBalance;
    private String closingBalanceCcy;
    private String closingCreditDebit;

    private List<SwiftEntry> swiftEntries;
}
