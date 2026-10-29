package com.betul.statementprocessor.parser;

import com.betul.statementprocessor.model.StatementRecord;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvStatementParserTest {

    private final CsvStatementParser parser = new CsvStatementParser();

    @Test
    void parsesAllRecordsFromSampleFile() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/records.csv")) {
            List<StatementRecord> records = parser.parse(input);

            assertThat(records).hasSize(10);
        }
    }

    @Test
    void parsesFieldsAndSpecialCharactersCorrectly() {
        String csv = """
                Reference,Account Number,Description,Start Balance,Mutation,End Balance
                177364,NL56RABO0149876948,Flowers for Jan Theuß,23.19,+28.49,51.68
                """;
        InputStream input = new ByteArrayInputStream(csv.getBytes(CsvStatementParser.CSV_CHARSET));

        StatementRecord record = parser.parse(input).getFirst();

        assertThat(record.reference()).isEqualTo(177364L);
        assertThat(record.accountNumber()).isEqualTo("NL56RABO0149876948");
        assertThat(record.description()).isEqualTo("Flowers for Jan Theuß");
        assertThat(record.startBalance()).isEqualByComparingTo("23.19");
        assertThat(record.mutation()).isEqualByComparingTo("28.49");
        assertThat(record.endBalance()).isEqualByComparingTo("51.68");
    }
}