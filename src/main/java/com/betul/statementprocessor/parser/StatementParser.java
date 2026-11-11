package com.betul.statementprocessor.parser;

import com.betul.statementprocessor.model.StatementRecord;

import java.io.InputStream;
import java.util.List;

public interface StatementParser {
    boolean supports(String fileName);

    /**
     * Reads all records from the input.
     *
     * @throws StatementParseException if the file cannot be read or contains invalid data
     */
    List<StatementRecord> parse(InputStream input);
}
