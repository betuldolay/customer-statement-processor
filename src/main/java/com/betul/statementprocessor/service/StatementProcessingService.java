package com.betul.statementprocessor.service;

import com.betul.statementprocessor.model.StatementRecord;
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

        List<StatementRecord> records = parser.parse(content);
        return new ValidationReport(records.size(), validator.validate(records));
    }
}