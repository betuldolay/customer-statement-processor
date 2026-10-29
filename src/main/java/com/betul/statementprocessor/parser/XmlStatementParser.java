package com.betul.statementprocessor.parser;

import com.betul.statementprocessor.model.StatementRecord;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class XmlStatementParser implements StatementParser {

    @Override
    public boolean supports(String fileName) {
        return fileName.toLowerCase(Locale.ROOT).endsWith(".xml");
    }

    @Override
    public List<StatementRecord> parse(InputStream input) {
        try {
            Document document = createSecureBuilder().parse(input);
            NodeList nodes = document.getElementsByTagName("record");

            List<StatementRecord> records = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                records.add(toRecord((Element) nodes.item(i)));
            }
            return List.copyOf(records);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new StatementParseException("Could not read XML file", e);
        }
    }

    // Protects against XXE (XML External Entity) attacks
    private DocumentBuilder createSecureBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new DefaultHandler()); // throw errors, don't print them
        return builder;
    }

    private StatementRecord toRecord(Element item) {
        return new StatementRecord(
                Long.parseLong(item.getAttribute("reference")),
                text(item, "accountNumber"),
                text(item, "description"),
                new BigDecimal(text(item, "startBalance")),
                new BigDecimal(text(item, "mutation")),
                new BigDecimal(text(item, "endBalance"))
        );
    }

    private String text(Element parent, String tagName) {
        Node node = parent.getElementsByTagName(tagName).item(0);
        if (node == null) {
            throw new StatementParseException("Missing <" + tagName + "> in record");
        }
        return node.getTextContent().trim();
    }
}
