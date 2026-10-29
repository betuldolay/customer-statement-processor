package com.betul.statementprocessor.api;

import com.betul.statementprocessor.parser.CsvStatementParser;
import com.betul.statementprocessor.parser.XmlStatementParser;
import com.betul.statementprocessor.service.StatementProcessingService;
import com.betul.statementprocessor.validation.StatementValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatementControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        StatementProcessingService service = new StatementProcessingService(
                List.of(new CsvStatementParser(), new XmlStatementParser()),
                new StatementValidator());

        mockMvc = MockMvcBuilders.standaloneSetup(new StatementController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void validatesXmlFile() throws Exception {
        mockMvc.perform(multipart("/api/statements/validate").file(sampleFile("records.xml")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(10))
                .andExpect(jsonPath("$.failedRecords.length()").value(2))
                .andExpect(jsonPath("$.failedRecords[0].reference").value(122671))
                .andExpect(jsonPath("$.failedRecords[0].reasons[0]").value("INCORRECT_END_BALANCE"));
    }

    @Test
    void validatesCsvFile() throws Exception {
        mockMvc.perform(multipart("/api/statements/validate").file(sampleFile("records.csv")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(10))
                .andExpect(jsonPath("$.failedRecords.length()").value(3));
    }

    @Test
    void rejectsUnsupportedFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "records.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/statements/validate").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Only .csv and .xml")));
    }

    @Test
    void rejectsMalformedXml() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "broken.xml", "application/xml", "<records><record".getBytes());

        mockMvc.perform(multipart("/api/statements/validate").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMalformedCsv() throws Exception {
        String csv = "Reference,Account Number,Description,Start Balance,Mutation,End Balance\n"
                + "1,NL56RABO0149876948,\"Unclosed quote,10.00,+5.00,15.00\n";
        MockMultipartFile file = new MockMultipartFile("file", "broken.csv", "text/csv", csv.getBytes());

        mockMvc.perform(multipart("/api/statements/validate").file(file))
                .andExpect(status().isBadRequest());
    }

    private MockMultipartFile sampleFile(String name) throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/" + name)) {
            return new MockMultipartFile("file", name, null, input.readAllBytes());
        }
    }
}