package com.example.BEcodetoconnect.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class Balance {
    private String cd;
    private String ccy;
    private Double amt;
    private String cdtDbtInd;
    private Date dt;
}
