package com.betul.statementprocessor.parser;

import com.betul.statementprocessor.model.StatementRecord;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.dataformat.xml.XmlFactory;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import javax.xml.stream.XMLInputFactory;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Component
public class XmlStatementParser implements StatementParser {

    private final XmlMapper xmlMapper = createSecureMapper();

    @Override
    public boolean supports(String fileName) {
        return fileName.toLowerCase(Locale.ROOT).endsWith(".xml");
    }

    @Override
    public List<StatementRecord> parse(InputStream input) {
        try {
            XmlRecords document = xmlMapper.readValue(input, XmlRecords.class);
            if (document == null || document.records() == null) {
                return List.of();
            }
            return document.records().stream()
                    .map(XmlStatementParser::toRecord)
                    .toList();
        } catch (JacksonException e) {
            throw new StatementParseException("Could not read XML file", e);
        }
    }

    private static StatementRecord toRecord(XmlRecord xml) {
        return new StatementRecord(
                Long.parseLong(required(xml.reference(), "reference")),
                required(xml.accountNumber(), "accountNumber"),
                required(xml.description(), "description"),
                new BigDecimal(required(xml.startBalance(), "startBalance")),
                new BigDecimal(required(xml.mutation(), "mutation")),
                new BigDecimal(required(xml.endBalance(), "endBalance"))
        );
    }

    private static String required(String value, String fieldName) {
        if (value == null) {
            throw new StatementParseException("Missing <" + fieldName + "> in record");
        }
        return value.trim();
    }

    // Protects against XXE: no DTDs, no external entities
    private static XmlMapper createSecureMapper() {
        XMLInputFactory inputFactory = XMLInputFactory.newFactory();
        inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return XmlMapper.builder(XmlFactory.builder().xmlInputFactory(inputFactory).build()).build();
    }

    // XML shape of the file, kept private to this parser
    record XmlRecords(
            @JacksonXmlElementWrapper(useWrapping = false)
            @JacksonXmlProperty(localName = "record")
            List<XmlRecord> records) {
    }

    record XmlRecord(
            @JacksonXmlProperty(isAttribute = true) String reference,
            String accountNumber,
            String description,
            String startBalance,
            String mutation,
            String endBalance) {
    }
}