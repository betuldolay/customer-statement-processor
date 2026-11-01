package com.betul.statementprocessor.parser;

import com.betul.statementprocessor.model.StatementRecord;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void failsWithParseExceptionOnMalformedCsv() {
        String csv = """
                Reference,Account Number,Description,Start Balance,Mutation,End Balance
                1,NL56RABO0149876948,"Unclosed quote,10.00,+5.00,15.00
                """;
        InputStream input = new ByteArrayInputStream(csv.getBytes(CsvStatementParser.CSV_CHARSET));

        assertThatThrownBy(() -> parser.parse(input))
                .isInstanceOf(StatementParseException.class)
                .hasMessageContaining("Could not read CSV file");
    }

    @Test
    void returnsEmptyListWhenFileHasOnlyHeader() {
        String csv = "Reference,Account Number,Description,Start Balance,Mutation,End Balance\n";

        assertThat(parser.parse(toStream(csv))).isEmpty();
    }

    @Test
    void returnsEmptyListWhenFileIsEmpty() {
        assertThat(parser.parse(toStream(""))).isEmpty();
    }

    @Test
    void trimsWhitespaceAroundValues() {
        String csv = """
                Reference,Account Number,Description,Start Balance,Mutation,End Balance
                 1 , NL56RABO0149876948 , Flowers , 10.00 , +5.00 , 15.00
                """;

        StatementRecord record = parser.parse(toStream(csv)).getFirst();

        assertThat(record.reference()).isEqualTo(1L);
        assertThat(record.accountNumber()).isEqualTo("NL56RABO0149876948");
        assertThat(record.description()).isEqualTo("Flowers");
        assertThat(record.mutation()).isEqualByComparingTo("5.00");
    }

    @Test
    void readsQuotedDescriptionContainingComma() {
        String csv = """
                Reference,Account Number,Description,Start Balance,Mutation,End Balance
                1,NL56RABO0149876948,"Flowers, candy and cake",10.00,+5.00,15.00
                """;

        assertThat(parser.parse(toStream(csv)).getFirst().description())
                .isEqualTo("Flowers, candy and cake");
    }

    @Test
    void failsWhenColumnIsMissing() {
        String csv = """
                Reference,Account Number,Description,Start Balance,Mutation
                1,NL56RABO0149876948,Flowers,10.00,+5.00
                """;

        assertThatThrownBy(() -> parser.parse(toStream(csv)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End Balance");
    }

    @Test
    void failsWhenBalanceIsNotANumber() {
        String csv = """
                Reference,Account Number,Description,Start Balance,Mutation,End Balance
                1,NL56RABO0149876948,Flowers,abc,+5.00,15.00
                """;

        assertThatThrownBy(() -> parser.parse(toStream(csv)))
                .isInstanceOf(NumberFormatException.class);
    }

    private InputStream toStream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(CsvStatementParser.CSV_CHARSET));
    }
}