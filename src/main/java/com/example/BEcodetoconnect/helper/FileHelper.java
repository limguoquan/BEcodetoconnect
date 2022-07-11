package com.example.BEcodetoconnect.helper;

import com.example.BEcodetoconnect.model.LedgerTransaction;
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

    public static List<LedgerTransaction> csvToLedgerTransactions(InputStream is) {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(new BOMInputStream(is), "UTF-8"));
             CSVParser csvParser = new CSVParser(fileReader,
                     CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());) {
            List<LedgerTransaction> ledgerTransactions = new ArrayList<LedgerTransaction>();
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
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<LedgerTransaction> xmlToLedgerTransactions(InputStream is) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(is);

            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();

            String account = ((NodeList) xPath.compile("//Acct/Id/Othr/Id").evaluate(document, XPathConstants.NODESET)).item(0).getTextContent();
            NodeList nodeList = (NodeList) xPath.compile("//Ntry").evaluate(document, XPathConstants.NODESET);

            List<LedgerTransaction> ledgerTransactions = new ArrayList<LedgerTransaction>();

            DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
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
                    int numberOfTransactions = 1;
                    Node numberOfTransactionsNode = element.getElementsByTagName("NbOfTxs").item(0);
                    if (numberOfTransactionsNode != null) {
                        numberOfTransactions = Integer.parseInt(numberOfTransactionsNode.getTextContent());
                    }
                    Long amount = Long.parseLong(element.getElementsByTagName("Amt").item(0).getTextContent()) * numberOfTransactions;

                    String transactionReference = "";
                    Node transactionReferenceNode = element.getElementsByTagName("EndToEndId").item(0);
                    if (transactionReferenceNode != null) {
                        transactionReference = transactionReferenceNode.getTextContent();
                    }

                    LedgerTransaction ledgerTransaction = new LedgerTransaction(
                            account,
                            valueDate,
                            currency,
                            creditDebit,
                            amount,
                            transactionReference
                    );
                    ledgerTransactions.add(ledgerTransaction);
                }
            }
            return ledgerTransactions;
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
