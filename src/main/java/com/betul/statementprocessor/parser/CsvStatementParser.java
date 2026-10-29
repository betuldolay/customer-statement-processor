package com.betul.statementprocessor.parser;

import com.betul.statementprocessor.model.StatementRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.List;

@Component
public class CsvStatementParser implements StatementParser {

    // The delivered CSV files are Windows-1252 encoded (e.g. "Theuß")
    static final Charset CSV_CHARSET = Charset.forName("windows-1252");

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()                 // read column names from the first line
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .get();

    @Override
    public List<StatementRecord> parse(InputStream input) {
        try (Reader reader = new InputStreamReader(input, CSV_CHARSET);
             CSVParser parser = FORMAT.parse(reader)) {
            return parser.stream().map(this::toRecord).toList();
        } catch (IOException e) {
            throw new StatementParseException("Could not read CSV file", e);
        }
    }

    private StatementRecord toRecord(CSVRecord row) {
        return new StatementRecord(
                Long.parseLong(row.get("Reference")),
                row.get("Account Number"),
                row.get("Description"),
                new BigDecimal(row.get("Start Balance")),
                new BigDecimal(row.get("Mutation")),
                new BigDecimal(row.get("End Balance"))
        );
    }
}
