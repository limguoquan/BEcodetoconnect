package com.example.BEcodetoconnect.helper;

import com.example.BEcodetoconnect.model.LedgerTransaction;
import com.example.BEcodetoconnect.model.SwiftEntry;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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

    public static List<Object> csvToLedgerTransactions(InputStream is) {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(new BOMInputStream(is), "UTF-8"));
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {



            List<Object> ledgerTransactions = new ArrayList<>();
            Iterable<CSVRecord> csvRecords = csvParser.getRecords();
            DateFormat dateFormatter = new SimpleDateFormat("dd-MMM-yy");
            dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            for (CSVRecord csvRecord : csvRecords) {
                LedgerTransaction ledgerTransaction = new LedgerTransaction(
                        csvRecord.get("Account"),
                        dateFormatter.parse(csvRecord.get("ValueDate")),
                        csvRecord.get("Currency"),
                        csvRecord.get("CreditDebit"),
                        Long.parseLong(csvRecord.get("Amount")),
                        csvRecord.get("TransactionReference")
                    );
                ledgerTransactions.add(ledgerTransaction);
            }
            return ledgerTransactions;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static List<Object> xmlToLedgerTransactions(InputStream is) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(is);

            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();

            String account = ((NodeList) xPath.compile("//Acct/Id/Othr/Id").evaluate(document, XPathConstants.NODESET)).item(0).getTextContent();
            NodeList nodeList = (NodeList) xPath.compile("//Ntry").evaluate(document, XPathConstants.NODESET);
            DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
            List<Object> swiftEntries = new ArrayList<>();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    Date valueDate = dateFormatter.parse(element.getElementsByTagName("DtTm").item(0).getTextContent());
                    String currency = element.getElementsByTagName("Amt").item(0).getAttributes().item(0).getTextContent();
                    String creditDebit = element.getElementsByTagName("CdtDbtInd").item(0).getTextContent();
                    switch (creditDebit) {
                        case "CDIT":
                            creditDebit = "Credit";
                            break;
                        case "DBIT":
                            creditDebit = "Debit";
                            break;
                    }

                    String transactionReference = "";
                    String uetr = "";
                    String pmtInfId = "";
                    int numberOfTransactions = 1;
                    Long amount;
                    Long ttlAmt = 0L;

                    amount = Long.parseLong(element.getElementsByTagName("Amt").item(0).getTextContent());
                    Node refsNode = element.getElementsByTagName("Refs").item(0);
                    Node btchNode = element.getElementsByTagName("Btch").item(0);
                    if (btchNode != null) {
                        pmtInfId = element.getElementsByTagName("PmtInfId").item(0).getTextContent();
                        numberOfTransactions = Integer.parseInt(element.getElementsByTagName("NbOfTxs").item(0).getTextContent());
                        ttlAmt = Long.parseLong(element.getElementsByTagName("TtlAmt").item(0).getTextContent());
                    } else if (refsNode != null) {
                        transactionReference = element.getElementsByTagName("EndToEndId").item(0).getTextContent();
                        uetr = element.getElementsByTagName("UETR").item(0).getTextContent();
                    }

                    LedgerTransaction ledgerTransaction = new LedgerTransaction(
                            account,
                            valueDate,
                            currency,
                            creditDebit,
                            amount,
                            transactionReference
                    );

                    SwiftEntry swiftEntry = new SwiftEntry(
                            ledgerTransaction,
                            uetr,
                            pmtInfId,
                            numberOfTransactions,
                            ttlAmt
                    );

                    swiftEntries.add(swiftEntry);
                }
            }
            return swiftEntries;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }


    }
}
