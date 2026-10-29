package com.betul.statementprocessor.validation;

import java.util.List;

public record ValidationReport(int totalRecords, List<FailedRecord> failedRecords) {
}
