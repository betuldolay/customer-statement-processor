package com.betul.statementprocessor.parser;

import com.betul.statementprocessor.model.StatementRecord;

import java.io.InputStream;
import java.util.List;

public interface StatementParser {
    List<StatementRecord> parse(InputStream input);
}
