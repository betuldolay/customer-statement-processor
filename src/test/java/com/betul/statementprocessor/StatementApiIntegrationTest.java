package com.betul.statementprocessor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class StatementApiIntegrationTest {

    private static final String VALIDATE_URL = "/api/statements/validate";

    @Autowired
    private RestTestClient client;

    @Test
    void validatesSampleXmlFile() {
        upload(new ClassPathResource("records.xml"))
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalRecords").isEqualTo(10)
                .jsonPath("$.failedRecords.length()").isEqualTo(2)
                .jsonPath("$.failedRecords[0].reference").isEqualTo(122671)
                .jsonPath("$.failedRecords[0].reasons[0]").isEqualTo("INCORRECT_END_BALANCE");
    }

    @Test
    void validatesSampleCsvFile() {
        upload(new ClassPathResource("records.csv"))
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalRecords").isEqualTo(10)
                .jsonPath("$.failedRecords.length()").isEqualTo(3)
                .jsonPath("$.failedRecords[0].reasons[0]").isEqualTo("DUPLICATE_REFERENCE");
    }

    @Test
    void returnsProblemDetailForInvalidData() {
        String csv = "Reference,Account Number,Description,Start Balance,Mutation,End Balance\n"
                + "1,NL56RABO0149876948,Flowers,abc,+5.00,15.00\n";

        upload(namedFile("invalid.csv", csv.getBytes(StandardCharsets.UTF_8)))
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").value(String.class,
                        detail -> assertThat(detail).contains("Invalid data in record 1"));
    }

    @Test
    void rejectsUnsupportedFileType() {
        upload(namedFile("records.txt", "hello".getBytes(StandardCharsets.UTF_8)))
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").value(String.class,
                        detail -> assertThat(detail).contains("Only .csv and .xml"));
    }

    @Test
    void rejectsFileLargerThanUploadLimit() {
        byte[] elevenMegabytes = new byte[11 * 1024 * 1024];

        upload(namedFile("large.csv", elevenMegabytes))
                .expectStatus().isEqualTo(413);
    }

    @Test
    void servesOpenApiDocumentation() {
        client.get().uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paths['" + VALIDATE_URL + "']").exists();
    }

    private RestTestClient.ResponseSpec upload(Resource file) {
        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("file", file);

        return client.post().uri(VALIDATE_URL)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(multipart)
                .exchange();
    }

    private Resource namedFile(String fileName, byte[] content) {
        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
    }
}
