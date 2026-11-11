package com.betul.statementprocessor.api;

import com.betul.statementprocessor.parser.StatementParseException;
import com.betul.statementprocessor.service.StatementProcessingService;
import com.betul.statementprocessor.service.UnsupportedFileTypeException;
import com.betul.statementprocessor.validation.FailedRecord;
import com.betul.statementprocessor.validation.ValidationReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.betul.statementprocessor.validation.FailureReason.DUPLICATE_REFERENCE;
import static com.betul.statementprocessor.validation.FailureReason.INCORRECT_END_BALANCE;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatementController.class)
class StatementControllerTest {

    private static final String VALIDATE_URL = "/api/statements/validate";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatementProcessingService service;

    private final MockMultipartFile csvFile =
            new MockMultipartFile("file", "records.csv", "text/csv", "some content".getBytes());

    @Test
    void returnsValidationReportAsJson() throws Exception {
        when(service.process(eq("records.csv"), any())).thenReturn(new ValidationReport(10, List.of(
                new FailedRecord(112806, "Clothes for Willem Dekker", List.of(DUPLICATE_REFERENCE)),
                new FailedRecord(122671, "Flowers from Peter Dekker",
                        List.of(DUPLICATE_REFERENCE, INCORRECT_END_BALANCE)))));

        mockMvc.perform(multipart(VALIDATE_URL).file(csvFile))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalRecords").value(10))
                .andExpect(jsonPath("$.failedRecords.length()").value(2))
                .andExpect(jsonPath("$.failedRecords[0].reference").value(112806))
                .andExpect(jsonPath("$.failedRecords[0].description").value("Clothes for Willem Dekker"))
                .andExpect(jsonPath("$.failedRecords[1].reasons[0]").value("DUPLICATE_REFERENCE"))
                .andExpect(jsonPath("$.failedRecords[1].reasons[1]").value("INCORRECT_END_BALANCE"));
    }

    @Test
    void returnsEmptyListWhenNoRecordFails() throws Exception {
        when(service.process(any(), any())).thenReturn(new ValidationReport(10, List.of()));

        mockMvc.perform(multipart(VALIDATE_URL).file(csvFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(10))
                .andExpect(jsonPath("$.failedRecords").isEmpty());
    }

    @Test
    void returnsBadRequestForUnsupportedFileType() throws Exception {
        when(service.process(any(), any())).thenThrow(new UnsupportedFileTypeException("records.txt"));

        mockMvc.perform(multipart(VALIDATE_URL).file(csvFile))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(containsString("Only .csv and .xml")));
    }

    @Test
    void returnsBadRequestWhenParsingFails() throws Exception {
        when(service.process(any(), any()))
                .thenThrow(new StatementParseException("Invalid data in record 2: not a number"));

        mockMvc.perform(multipart(VALIDATE_URL).file(csvFile))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Invalid data in record 2: not a number"));
    }

    @Test
    void rejectsRequestWithoutFile() throws Exception {
        mockMvc.perform(multipart(VALIDATE_URL))
                .andExpect(status().isBadRequest());

        verify(service, never()).process(any(), any());
    }
}
