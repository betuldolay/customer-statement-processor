package com.betul.statementprocessor.service;

import com.betul.statementprocessor.model.StatementRecord;
import com.betul.statementprocessor.parser.StatementParseException;
import com.betul.statementprocessor.parser.StatementParser;
import com.betul.statementprocessor.validation.StatementValidator;
import com.betul.statementprocessor.validation.ValidationReport;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
public class StatementProcessingService {

    private final List<StatementParser> parsers;
    private final StatementValidator validator;

    public StatementProcessingService(List<StatementParser> parsers, StatementValidator validator) {
        this.parsers = parsers;
        this.validator = validator;
    }

    public ValidationReport process(String fileName, InputStream content) {
        String name = fileName == null ? "" : fileName;

        StatementParser parser = parsers.stream()
                .filter(p -> p.supports(name))
                .findFirst()
                .orElseThrow(() -> new UnsupportedFileTypeException(name));

        List<StatementRecord> records = parse(parser, name, content);
        return new ValidationReport(records.size(), validator.validate(records));
    }

    private List<StatementRecord> parse(StatementParser parser, String fileName, InputStream content) {
        try {
            return parser.parse(content);
        } catch (IllegalArgumentException e) {
            // e.g. a non-numeric balance or a missing CSV column
            throw new StatementParseException("Invalid record data in '" + fileName + "'", e);
        }
    }
}