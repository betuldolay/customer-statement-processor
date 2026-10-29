package com.betul.statementprocessor.validation;

import com.betul.statementprocessor.model.StatementRecord;
import com.betul.statementprocessor.parser.XmlStatementParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import static com.betul.statementprocessor.validation.FailureReason.DUPLICATE_REFERENCE;
import static com.betul.statementprocessor.validation.FailureReason.INCORRECT_END_BALANCE;
import static org.assertj.core.api.Assertions.assertThat;

class StatementValidatorTest {

    private final StatementValidator validator = new StatementValidator();

    @Test
    void returnsNoFailuresForValidRecords() {
        List<StatementRecord> records = List.of(
                record(1, "10.00", "+5.00", "15.00"),
                record(2, "20.00", "-5.50", "14.50")
        );

        assertThat(validator.validate(records)).isEmpty();
    }

    @Test
    void detectsIncorrectEndBalance() {
        List<StatementRecord> records = List.of(record(1, "10.00", "+5.00", "16.00"));

        assertThat(validator.validate(records))
                .containsExactly(new FailedRecord(1, "Record 1", List.of(INCORRECT_END_BALANCE)));
    }

    @Test
    void reportsAllRecordsWithDuplicateReference() {
        List<StatementRecord> records = List.of(
                record(1, "10.00", "+5.00", "15.00"),
                record(1, "20.00", "+5.00", "25.00"),
                record(2, "10.00", "+5.00", "15.00")
        );

        assertThat(validator.validate(records))
                .extracting(FailedRecord::reference)
                .containsExactly(1L, 1L);
    }

    @Test
    void reportsBothReasonsWhenRecordFailsTwice() {
        List<StatementRecord> records = List.of(
                record(1, "10.00", "+5.00", "15.00"),
                record(1, "10.00", "+5.00", "99.00")
        );

        assertThat(validator.validate(records).get(1).reasons())
                .containsExactly(DUPLICATE_REFERENCE, INCORRECT_END_BALANCE);
    }

    @Test
    void treatsDifferentScalesAsEqual() {
        List<StatementRecord> records = List.of(record(1, "10.1", "-0.3", "9.80"));

        assertThat(validator.validate(records)).isEmpty();
    }

    @Test
    void findsTheTwoBrokenRecordsInSampleXml() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/records.xml")) {
            List<StatementRecord> records = new XmlStatementParser().parse(input);

            assertThat(validator.validate(records))
                    .extracting(FailedRecord::reference)
                    .containsExactlyInAnyOrder  (122671L, 124889L);
        }
    }

    private StatementRecord record(long reference, String start, String mutation, String end) {
        return new StatementRecord(reference, "NL00TEST0000000000", "Record " + reference,
                new BigDecimal(start), new BigDecimal(mutation), new BigDecimal(end));
    }
}