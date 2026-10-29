package com.betul.statementprocessor.api;

import com.betul.statementprocessor.service.StatementProcessingService;
import com.betul.statementprocessor.validation.ValidationReport;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api/statements")
public class StatementController {

    private final StatementProcessingService processingService;

    public StatementController(StatementProcessingService processingService) {
        this.processingService = processingService;
    }

    @PostMapping(value = "/validate",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ValidationReport validate(@RequestParam("file") MultipartFile file) throws IOException {
        try (InputStream input = file.getInputStream()) {
            return processingService.process(file.getOriginalFilename(), input);
        }
    }
}