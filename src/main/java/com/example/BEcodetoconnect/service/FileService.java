package com.example.BEcodetoconnect.service;

import com.example.BEcodetoconnect.model.LedgerTransaction;
import com.example.BEcodetoconnect.model.SwiftEntry;
import com.example.BEcodetoconnect.utils.DateFormatter;
import com.example.BEcodetoconnect.utils.XMLFieldRetriever;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class FileService {
    @Autowired
    DateFormatter dateFormatter;

    @Autowired
    XMLFieldRetriever xmlFieldRetriever;

    public static String CSV_TYPE = "text/csv";
    public static String XML_TYPE = "application/xml";

    public List<Object> parseToPOJO (MultipartFile file) {
        List<Object> records = null;
        try {
            if (CSV_TYPE.equals(file.getContentType())) {
                records = csvToLedgerTransactions(file.getInputStream());
            } else if (XML_TYPE.equals(file.getContentType())) {
                records = xmlToSwiftMessages(file.getInputStream());
            }
            return records;
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<Object> csvToLedgerTransactions(InputStream is) {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(new BOMInputStream(is), "UTF-8"));
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            // Get CSV Records
            Iterable<CSVRecord> csvRecords = csvParser.getRecords();
            // Form and return a list of LedgerTransaction objects
            return StreamSupport.stream(csvRecords.spliterator(), false)
                    .map(csvRecord -> new LedgerTransaction(
                            csvRecord.get("Account"),
                            dateFormatter.parseDate(csvRecord.get("ValueDate"), CSV_TYPE),
                            csvRecord.get("Currency"),
                            csvRecord.get("CreditDebit"),
                            Long.parseLong(csvRecord.get("Amount")),
                            csvRecord.get("TransactionReference")
                    )).collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<Object> xmlToSwiftMessages(InputStream is) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(is);

            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();

            String account = ((NodeList) xPath.compile("//Acct/Id/Othr/Id").evaluate(document, XPathConstants.NODESET)).item(0).getTextContent();
            NodeList nodeList = (NodeList) xPath.compile("//Ntry").evaluate(document, XPathConstants.NODESET);

            Stream<Node> nodeStream = IntStream.range(0, nodeList.getLength()).mapToObj(nodeList::item);
            return StreamSupport.stream(nodeStream.spliterator(), false)
                    .map(node -> {
                        String endToEndId = xmlFieldRetriever.getXMLField(node, "EndToEndId", nodeList);
                        String valueDate = xmlFieldRetriever.getXMLField(node, "DtTm", nodeList);
                        String amount = xmlFieldRetriever.getXMLField(node, "Amt", nodeList);
                        String currency = xmlFieldRetriever.getXMLAttribute(node, "Amt", nodeList);
                        String creditDebit = xmlFieldRetriever.getXMLField(node, "CdtDbtInd", nodeList);
                        String pmtInfId = xmlFieldRetriever.getXMLField(node, "PmtInfId", nodeList);
                        String nbOfTxs = xmlFieldRetriever.getXMLField(node, "NbOfTxs", nodeList);
                        String ttlAmt = xmlFieldRetriever.getXMLField(node, "TtlAmt", nodeList);
                        String transactionReference = xmlFieldRetriever.getXMLField(node, "EndToEndId", nodeList);
                        String uetr = xmlFieldRetriever.getXMLField(node, "UETR", nodeList);

                        int nbOfTxs_Int = !nbOfTxs.equals("") ? Integer.parseInt(nbOfTxs) : 1;

                        List<LedgerTransaction> ledgerTransactionsList = Stream.generate(() -> new LedgerTransaction(
                                account,
                                dateFormatter.parseDate(valueDate, XML_TYPE),
                                currency,
                                creditDebit.equals("CDIT") ? "Credit" : creditDebit.equals("DBIT") ? "Debit" : "",
                                Long.parseLong(amount),
                                transactionReference
                        )).limit(nbOfTxs_Int).toList();

                        Stack<LedgerTransaction> ledgerTransactionsStack = new Stack<>();
                        ledgerTransactionsStack.addAll(ledgerTransactionsList);

                        return new SwiftEntry(
                                ledgerTransactionsStack,
                                endToEndId,
                                uetr,
                                pmtInfId,
                                nbOfTxs_Int,
                                !ttlAmt.equals("") ? Long.parseLong(ttlAmt) : Long.parseLong(amount)
                        );
                    }).collect(Collectors.toList());
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    public void save(MultipartFile file) {
//        try {
//            List<LedgerTransaction> ledgerTransactions = FileHelper.csvToLedgerTransactions(file.getInputStream());
//            ledgerTransactionRepository.saveAll(ledgerTransactions);
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to store csv data: " + e.getMessage());
//        }
    }

}
