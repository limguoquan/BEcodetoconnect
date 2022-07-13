package com.example.BEcodetoconnect.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

@Service
@Slf4j
public class XMLFieldRetriever {

    public String getXMLField(String elementPath, Document document) throws XPathExpressionException {
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();

        NodeList nodeList = (NodeList) xPath.compile(elementPath).evaluate(document, XPathConstants.NODESET);
        if (elementExists(nodeList)) {
            return nodeList.item(0).getTextContent();
        }
        return "";
    }

    public String getXMLField_2(Node node, String elementName, NodeList nodeList) {
        Element element = (Element) node;
        if (elementExists_2(element, elementName)) {
            return element.getElementsByTagName(elementName).item(0).getTextContent();
        }
        return "";
    }

    public String getXMLFieldWithParentNodeCheck(Node node, String elementName, String parentNodeName, NodeList nodeList) {
        Element element = (Element) node;
        if (elementExists_2(element, elementName)) {
            if (element.getNodeName().equals(elementName) && element.getParentNode().getNodeName().equals(parentNodeName)) {
                return element.getElementsByTagName(elementName).item(0).getTextContent();
            }
            return "";
        }
        return "";
    }

    public String getXMLAttribute(String elementPath, Document document) throws XPathExpressionException {
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();

        NodeList nodeList = (NodeList) xPath.compile(elementPath).evaluate(document, XPathConstants.NODESET);
        if (elementExists(nodeList)) {
            return nodeList.item(0).getAttributes().item(0).getTextContent();
        }
        return "";
    }

    public String getXMLAttribute_2(Node node, String elementName, NodeList nodeList) {
        Element element = (Element) node;
        if (elementExists_2(element, elementName)) {
            return element.getElementsByTagName(elementName).item(0).getAttributes().item(0).getTextContent();
        }
        return "";
    }

    public Boolean elementExists(NodeList nodeList) {
        if (nodeList.getLength() > 0) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    public Boolean elementExists_2(Element element, String elementName) {
        NodeList nodeList = element.getElementsByTagName(elementName);
        if (nodeList.getLength() > 0) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }
}
