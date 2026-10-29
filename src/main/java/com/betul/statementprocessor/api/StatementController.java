package com.betul.statementprocessor.api;

import com.betul.statementprocessor.service.StatementProcessingService;
import com.betul.statementprocessor.validation.ValidationReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api/statements")
@Tag(name = "Statements", description = "Validate customer statement files")
public class StatementController {

    private final StatementProcessingService processingService;

    public StatementController(StatementProcessingService processingService) {
        this.processingService = processingService;
    }

    @Operation(
            summary = "Validate a statement file",
            description = "Upload a .csv or .xml file. Returns every record with a duplicate "
                    + "reference or an incorrect end balance.")
    @ApiResponse(responseCode = "200", description = "Validation report")
    @ApiResponse(responseCode = "400", description = "Unsupported or malformed file",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(value = "/validate",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ValidationReport validate(
            @Parameter(description = "Statement file (.csv or .xml)")
            @RequestParam("file") MultipartFile file) throws IOException {
        try (InputStream input = file.getInputStream()) {
            return processingService.process(file.getOriginalFilename(), input);
        }
    }
}