package com.example.BEcodetoconnect.helper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
public class FileHelper {

    public static String CSV_TYPE = "text/csv";
    public static String XML_TYPE = "application/xml";
    static String[] HEADERs = { "Account", "ValueDate", "Currency", "CreditDebit", "Amount", "TransactionReference" };

    public static boolean hasCSVFormat(MultipartFile file) {
        if (!CSV_TYPE.equals(file.getContentType())) {
            return false;
        }
        return true;
    }

    public static boolean hasXMLFormat(MultipartFile file) {
        if (!XML_TYPE.equals(file.getContentType())) {
            return false;
        }
        return true;
    }

}

