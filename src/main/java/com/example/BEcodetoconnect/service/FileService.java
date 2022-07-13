package com.example.BEcodetoconnect.service;

import com.example.BEcodetoconnect.model.*;
import com.example.BEcodetoconnect.utils.DateFormatter;
import com.example.BEcodetoconnect.utils.NodeIterable;
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
import org.w3c.dom.Element;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;
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

    @Autowired
    NodeIterable nodeIterable;

    public static String CSV_TYPE = "text/csv";
    public static String XML_TYPE = "application/xml";

    public List<LedgerTransaction> ledgerCSVparseToPOJO (MultipartFile file) {
        try {
            return csvToLedgerTransactions(file.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<LedgerBalance> balanceCSVparseToPOJO (MultipartFile file) {
        try {
            return csvToLedgerBalance(file.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public SwiftMessage swiftXMLparseToPOJO (MultipartFile file) {
        try {
            return xmlToSwiftMessage(file.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<LedgerTransaction> csvToLedgerTransactions(InputStream is) {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(new BOMInputStream(is), "UTF-8"));
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            // Get CSV Records
            Iterable<CSVRecord> csvRecords = csvParser.getRecords();
            // Form and return a list of LedgerTransaction objects
            return StreamSupport.stream(csvRecords.spliterator(), false)
                    .map(csvRecord -> new LedgerTransaction(
                            csvRecord.get("Account"),
                            dateFormatter.parseDate(csvRecord.get("ValueDate"), "dd-MMM-yy"),
                            csvRecord.get("Currency"),
                            csvRecord.get("CreditDebit"),
                            Double.parseDouble(csvRecord.get("Amount")),
                            csvRecord.get("TransactionReference")
                    )).collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<LedgerBalance> csvToLedgerBalance(InputStream is) {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(new BOMInputStream(is), "UTF-8"));
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            // Get CSV Records
            Iterable<CSVRecord> csvRecords = csvParser.getRecords();
            // Form and return a list of LedgerTransaction objects
            return StreamSupport.stream(csvRecords.spliterator(), false)
                    .map(csvRecord -> new LedgerBalance(
                            csvRecord.get("Account"),
                            csvRecord.get("Currency"),
                            Double.parseDouble(csvRecord.get("Balance")),
                            dateFormatter.parseDate(csvRecord.get("AsOfDateTS"), "yyyy-MM-dd'T'HH:mm:ssX")
                    )).collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public SwiftMessage xmlToSwiftMessage(InputStream is) throws IOException {

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(is);

            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();

            NodeList nodeList = (NodeList) xPath.compile("//BkToCstmrStmt").evaluate(document, XPathConstants.NODESET);

            String bicfiFrom = xmlFieldRetriever.getXMLField("//Fr/FIId/FinInstnId/BICFI", document);
            String bicfiTo = xmlFieldRetriever.getXMLField("//To/FIId/FinInstnId/BICFI", document);
            String msgId = xmlFieldRetriever.getXMLField("//MsgId", document);
            String creDtTm = xmlFieldRetriever.getXMLField("//CreDtTm", document);
            String stmtId = xmlFieldRetriever.getXMLField("//Stmt/Id", document);
            String pgNb = xmlFieldRetriever.getXMLField("//Stmt/StmtPgntn/PgNb", document);
            String lastPgInd = xmlFieldRetriever.getXMLField("//Stmt/StmtPgntn/LastPgInd", document);
            String lglSeqNb = xmlFieldRetriever.getXMLField("//Stmt/LglSeqNb", document);
            String acctId = xmlFieldRetriever.getXMLField("//Acct/Id/Othr/Id", document);
            String ccy = xmlFieldRetriever.getXMLField( "//Acct/Ccy", document);

            NodeList nodeListBalance = (NodeList) xPath.compile("//Bal").evaluate(document, XPathConstants.NODESET);
            List<Balance> balances = xmlToBalances(nodeListBalance, document);

            NodeList nodeListSwiftEntry = (NodeList) xPath.compile("//Ntry").evaluate(document, XPathConstants.NODESET);
            List<SwiftEntry> swiftEntries =xmlToSwiftEntries(nodeListSwiftEntry, document);

            return new SwiftMessage(
                    bicfiFrom,
                    bicfiTo,
                    msgId,
                    dateFormatter.parseDate(creDtTm, "yyyy-MM-dd'T'HH:mm:ssX"),
                    stmtId,
                    Integer.parseInt(pgNb),
                    lastPgInd,
                    lglSeqNb,
                    acctId,
                    ccy,
                    balances,
                    swiftEntries
            );
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Balance> xmlToBalances(NodeList nodeList, Document document) {
        Stream<Node> nodeStream = IntStream.range(0, nodeList.getLength()).mapToObj(nodeList::item);
        return StreamSupport.stream(nodeStream.spliterator(), false)
                .map(node -> {
                        String cd = xmlFieldRetriever.getXMLField_2(node, "Cd", nodeList);
                        String ccy = xmlFieldRetriever.getXMLAttribute_2(node, "Amt", nodeList);
                        String amt = xmlFieldRetriever.getXMLField_2(node, "Amt", nodeList);
                        String cdtDbtInd = xmlFieldRetriever.getXMLField_2(node, "CdtDbtInd", nodeList);
                        String dt = xmlFieldRetriever.getXMLField_2(node, "Dt", nodeList).strip();

                    return new Balance(
                            cd,
                            ccy,
                            Double.parseDouble(amt),
                            cdtDbtInd,
                            dateFormatter.parseDate(dt, "yyyy-MM-dd")
                        );
                }).collect(Collectors.toList());

    }

    public List<SwiftEntry> xmlToSwiftEntries(NodeList nodeList, Document document) {
        Stream<Node> nodeStream = IntStream.range(0, nodeList.getLength()).mapToObj(nodeList::item);
        return StreamSupport.stream(nodeStream.spliterator(), false)
                .map(node -> {
                    String ccy = xmlFieldRetriever.getXMLAttribute_2(node, "Amt", nodeList);
                    String amt = xmlFieldRetriever.getXMLField_2(node, "Amt", nodeList);
                    String cdtDbtInd = xmlFieldRetriever.getXMLField_2(node, "CdtDbtInd", nodeList);
                    String cd = xmlFieldRetriever.getXMLField_2(node, "Cd", nodeList);
                    String bookgDt = xmlFieldRetriever.getXMLField_2(node, "DtTm", nodeList);
                    String endToEndId = xmlFieldRetriever.getXMLField_2(node, "EndToEndId", nodeList);
                    String valDt = xmlFieldRetriever.getXMLField_2(node, "Dt", nodeList).strip();

                    String uetr = xmlFieldRetriever.getXMLField_2(node, "UETR", nodeList);
                    String ccyTrans = xmlFieldRetriever.getXMLAttribute_2(node, "Amt", nodeList);
                    String amtTrans = xmlFieldRetriever.getXMLField_2(node, "Amt", nodeList);
                    String cdtDbtIndTrans = xmlFieldRetriever.getXMLField_2(node, "CdtDbtInd", nodeList);

                    String msgId = xmlFieldRetriever.getXMLField_2(node, "MsgId", nodeList);
                    String pmtInfId = xmlFieldRetriever.getXMLField_2(node, "PmtInfId", nodeList);
                    String nbOfTxs = xmlFieldRetriever.getXMLField_2(node, "NbOfTxs", nodeList);
                    String ttlAmt = xmlFieldRetriever.getXMLField_2(node, "TtlAmt", nodeList);
                    String cdtDbtIndBtch = xmlFieldRetriever.getXMLField_2(node, "CdtDbtInd", nodeList);

                    // EDGE CASE
                    // Assuming the transactions without TxDtls do not have uetr
                    // Hence the amount in transaction should be null also
                    if ("".equals(uetr)) {
                        amtTrans = "";
                    }

                    return new SwiftEntry(
                            ccy,
                            Double.parseDouble(amt),
                            cdtDbtInd,
                            cd,
                            dateFormatter.parseDate(bookgDt, "yyyy-MM-dd'T'HH:mm:ssX"),
                            dateFormatter.parseDate(valDt, "yyyy-MM-dd"),
                            endToEndId,
                            uetr,
                            ccyTrans,
                            "".equals(amtTrans) ?  null : Double.parseDouble(amtTrans),
                            cdtDbtIndTrans,
                            msgId,
                            pmtInfId,
                            "".equals(nbOfTxs) ? null : Integer.parseInt(nbOfTxs),
                            "".equals(ttlAmt) ? null : Double.parseDouble(ttlAmt),
                            cdtDbtIndBtch
                    );
                }).collect(Collectors.toList());
    }

}
