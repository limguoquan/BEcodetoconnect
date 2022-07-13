package com.example.BEcodetoconnect.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@AllArgsConstructor
@Document("ledgerBalances")
public class LedgerBalance {
    private String account;
    private String currency;
    private Double balance;
    private Date AsOfDateTS;
}
