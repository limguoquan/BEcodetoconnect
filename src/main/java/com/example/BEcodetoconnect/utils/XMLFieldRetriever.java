package com.example.BEcodetoconnect.utils;

import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Service
public class XMLFieldRetriever {
    public String getXMLField(Node node, String elementName, NodeList nodeList) {
        Element element = (Element) node;
        if (elementExists(element, elementName)) {
            return element.getElementsByTagName(elementName).item(0).getTextContent();
        }
        return "";
    }

    public String getXMLAttribute(Node node, String elementName, NodeList nodeList) {
        Element element = (Element) node;
        if (elementExists(element, elementName)) {
            return element.getElementsByTagName(elementName).item(0).getAttributes().item(0).getTextContent();
        }
        return "";
    }

    public Boolean elementExists(Element element, String elementName) {
        NodeList nodeList = element.getElementsByTagName(elementName);
        if (nodeList.getLength() > 0) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }
}
