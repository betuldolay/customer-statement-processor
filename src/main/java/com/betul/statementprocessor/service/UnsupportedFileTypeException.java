package com.betul.statementprocessor.service;

public class UnsupportedFileTypeException extends RuntimeException {
    public UnsupportedFileTypeException(String fileName) {
        super("Unsupported file type: '" + fileName + "'. Only .csv and .xml files are supported.");
    }
}