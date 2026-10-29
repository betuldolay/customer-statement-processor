# Customer Statement Processor

A Spring Boot REST service that validates monthly customer statement records
delivered as CSV or XML files.

## Validation rules

1. All transaction references must be unique.
2. The end balance must equal `start balance + mutation`.

The report lists the reference, description and failure reason(s) of each failed record.

## Tech stack

- Java 25
- Spring Boot (Web)
- Apache Commons CSV
- JUnit 5, AssertJ, MockMvc

## Running the application

No Maven installation needed, the Maven Wrapper is included:

```bash
./mvnw spring-boot:run
```

Run the tests:

```bash
./mvnw test
```

## API

### `POST /api/statements/validate`

Upload a `.csv` or `.xml` file as `multipart/form-data` with the field name `file`.

```bash
curl -F "file=@src/test/resources/records.xml" http://localhost:8080/api/statements/validate
```

**Response `200 OK`**

```json
{
  "totalRecords": 10,
  "failedRecords": [
    {
      "reference": 122671,
      "description": "Flowers from Peter Dekker",
      "reasons": ["INCORRECT_END_BALANCE"]
    }
  ]
}
```

**Response `400 Bad Request`** for unsupported file types or malformed files,
in the standard [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) Problem Details format.

## Design decisions

- **`BigDecimal` for money.** Floating-point types cannot represent amounts like `0.1` exactly,
  which would make correct records fail the balance check.
- **`compareTo` instead of `equals`.** `BigDecimal.equals` also compares scale,
  so `9.8` and `9.80` would be considered different.
- **All records sharing a duplicate reference are reported**, not only the second occurrence:
  from the file alone it is impossible to know which one is the genuine transaction.
- **A record failing both rules is reported once**, with both reasons.
- **One `StatementParser` interface, one implementation per format.** The service picks the
  parser by file extension. Supporting a new format means adding one class (Open/Closed principle).
- **CSV encoding.** The delivered CSV files are Windows-1252 encoded (e.g. `Theuß`),
  so the CSV parser reads them explicitly as such. XML files declare their own encoding.
- **XXE protection.** The XML parser rejects `DOCTYPE` declarations, preventing
  XML External Entity attacks on uploaded files.
- **Sample files live in `src/test/resources`**, so test data is not packaged into the application.

## Possible improvements

- Stream very large files instead of loading all records into memory
- Validate the IBAN format of account numbers
- Detect the file type by content as well as by extension
- Add OpenAPI/Swagger documentation