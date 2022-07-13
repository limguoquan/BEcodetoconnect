package com.example.BEcodetoconnect.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;
import java.util.Stack;

@Data
@AllArgsConstructor
public class SwiftEntry {

    private String ccy;
    private Long amt;
    private String cdtDbtInd;
    private String cd;
    private Date bookgDt;
    private Date valDt;

    // Swift transaction details
    private String endToEndId;
    private String uetr;
    private String ccyTrans;
    private Long amtTrans;
    private String cdtDbtIndTrans;

    // Swift batch details
    private String msgId;
    private String pmtInfId;
    private Integer nbOfTxs;
    private Long ttlAmt;
    private String cdtDbtIndBtch;

//    private String endToEndId;
//    private String uetr;
//    private String pmtInfId;
//    private Integer nbOfTxs;
//    private Long ttlAmt;

}
