package com.betul.statementprocessor.validation;

import com.betul.statementprocessor.model.StatementRecord;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.betul.statementprocessor.validation.FailureReason.DUPLICATE_REFERENCE;
import static com.betul.statementprocessor.validation.FailureReason.INCORRECT_END_BALANCE;

@Component
public class StatementValidator {

    public List<FailedRecord> validate(List<StatementRecord> records) {
        Set<Long> duplicateReferences = findDuplicateReferences(records);

        List<FailedRecord> failedRecords = new ArrayList<>();
        for (StatementRecord record : records) {
            List<FailureReason> reasons = new ArrayList<>();
            if (duplicateReferences.contains(record.reference())) {
                reasons.add(DUPLICATE_REFERENCE);
            }
            if (!hasCorrectEndBalance(record)) {
                reasons.add(INCORRECT_END_BALANCE);
            }
            if (!reasons.isEmpty()) {
                failedRecords.add(new FailedRecord(record.reference(), record.description(), List.copyOf(reasons)));
            }
        }
        return List.copyOf(failedRecords);
    }

    private Set<Long> findDuplicateReferences(List<StatementRecord> records) {
        return records.stream()
                .collect(Collectors.groupingBy(StatementRecord::reference, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private boolean hasCorrectEndBalance(StatementRecord record) {
        return record.startBalance()
                .add(record.mutation())
                .compareTo(record.endBalance()) == 0;
    }
}