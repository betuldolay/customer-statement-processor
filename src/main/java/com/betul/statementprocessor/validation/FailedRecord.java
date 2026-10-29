package com.betul.statementprocessor.validation;

import java.util.List;

public record FailedRecord(long reference, String description, List<FailureReason> reasons) {
}
