package com.example.BEcodetoconnect.utils;

import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@Service
public class DateFormatter {
    public static String CSV_TYPE = "text/csv";
    public static String XML_TYPE = "application/xml";

    public Date parseDate(String date, String fileType) {
        DateFormat dateFormatter = null;
        if (CSV_TYPE.equals(fileType)) {
            dateFormatter = new SimpleDateFormat("dd-MMM-yy");
            dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        } else if (XML_TYPE.equals(fileType)) {
            dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
        }
        try {
            return dateFormatter.parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }



}
