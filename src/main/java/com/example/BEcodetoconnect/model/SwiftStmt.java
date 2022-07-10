package com.example.BEcodetoconnect.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="Stmt")
@XmlAccessorType(XmlAccessType.FIELD)
public class SwiftStmt {
    private String id;
    private String account;
}
