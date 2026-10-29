package com.betul.statementprocessor.parser;

import com.betul.statementprocessor.model.StatementRecord;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XmlStatementParserTest {

    private final XmlStatementParser parser = new XmlStatementParser();

    @Test
    void parsesAllRecordsFromSampleFile() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/records.xml")) {
            List<StatementRecord> records = parser.parse(input);

            assertThat(records).hasSize(10);
        }
    }

    @Test
    void parsesFieldsCorrectly() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <records>
                  <record reference="177364">
                    <accountNumber>NL56RABO0149876948</accountNumber>
                    <description>Flowers for Jan Theuß</description>
                    <startBalance>23.19</startBalance>
                    <mutation>+28.49</mutation>
                    <endBalance>51.68</endBalance>
                  </record>
                </records>
                """;

        StatementRecord record = parser.parse(toStream(xml)).getFirst();

        assertThat(record.reference()).isEqualTo(177364L);
        assertThat(record.accountNumber()).isEqualTo("NL56RABO0149876948");
        assertThat(record.description()).isEqualTo("Flowers for Jan Theuß");
        assertThat(record.startBalance()).isEqualByComparingTo("23.19");
        assertThat(record.mutation()).isEqualByComparingTo("28.49");
        assertThat(record.endBalance()).isEqualByComparingTo("51.68");
    }

    @Test
    void rejectsXmlWithDoctypeToPreventXxe() {
        String maliciousXml = """
                <?xml version="1.0"?>
                <!DOCTYPE records [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <records><record reference="1"><description>&xxe;</description></record></records>
                """;

        assertThatThrownBy(() -> parser.parse(toStream(maliciousXml)))
                .isInstanceOf(StatementParseException.class);
    }

    @Test
    void failsClearlyWhenFieldIsMissing() {
        String xml = """
                <records><record reference="1"><description>No balances</description></record></records>
                """;

        assertThatThrownBy(() -> parser.parse(toStream(xml)))
                .isInstanceOf(StatementParseException.class)
                .hasMessageContaining("accountNumber");
    }

    private InputStream toStream(String xml) {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void returnsEmptyListWhenFileHasNoRecords() {
        assertThat(parser.parse(toStream("<records/>"))).isEmpty();
    }

    @Test
    void trimsWhitespaceAroundValues() {
        String xml = """
                <records>
                  <record reference="1">
                    <accountNumber>  NL56RABO0149876948  </accountNumber>
                    <description>
                        Flowers
                    </description>
                    <startBalance> 10.00 </startBalance>
                    <mutation> +5.00 </mutation>
                    <endBalance> 15.00 </endBalance>
                  </record>
                </records>
                """;

        StatementRecord record = parser.parse(toStream(xml)).getFirst();

        assertThat(record.accountNumber()).isEqualTo("NL56RABO0149876948");
        assertThat(record.description()).isEqualTo("Flowers");
        assertThat(record.mutation()).isEqualByComparingTo("5.00");
    }

    @Test
    void ignoresUnknownElements() {
        String xml = """
                <records>
                  <record reference="1">
                    <accountNumber>NL56RABO0149876948</accountNumber>
                    <description>Flowers</description>
                    <currency>EUR</currency>
                    <startBalance>10.00</startBalance>
                    <mutation>+5.00</mutation>
                    <endBalance>15.00</endBalance>
                  </record>
                </records>
                """;

        assertThat(parser.parse(toStream(xml))).hasSize(1);
    }

    @Test
    void doesNotExpandInternalEntities() {
        String xml = """
                <?xml version="1.0"?>
                <!DOCTYPE records [<!ENTITY injected "INJECTED">]>
                <records><record reference="1"><description>&injected;</description></record></records>
                """;

        assertThatThrownBy(() -> parser.parse(toStream(xml)))
                .isInstanceOf(StatementParseException.class);
    }

    @Test
    void failsClearlyWhenReferenceIsMissing() {
        String xml = """
                <records>
                  <record>
                    <accountNumber>NL56RABO0149876948</accountNumber>
                    <description>No reference</description>
                    <startBalance>10.00</startBalance>
                    <mutation>+5.00</mutation>
                    <endBalance>15.00</endBalance>
                  </record>
                </records>
                """;

        assertThatThrownBy(() -> parser.parse(toStream(xml)))
                .isInstanceOf(StatementParseException.class)
                .hasMessageContaining("reference");
    }

}