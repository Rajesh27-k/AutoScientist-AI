package com.autoscientist.util;

import com.autoscientist.exception.BadRequestException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CsvValidator {

    public static class ValidationResult {
        private final List<String> headers;
        private final int rowCount;
        private final int columnCount;

        public ValidationResult(List<String> headers, int rowCount, int columnCount) {
            this.headers = headers;
            this.rowCount = rowCount;
            this.columnCount = columnCount;
        }

        public List<String> getHeaders() {
            return headers;
        }

        public int getRowCount() {
            return rowCount;
        }

        public int getColumnCount() {
            return columnCount;
        }
    }

    public static ValidationResult validateCsv(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(false)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            List<String> headers = new ArrayList<>(parser.getHeaderNames());

            if (headers.isEmpty()) {
                throw new BadRequestException("CSV validation failed: Missing header row or empty file.");
            }

            int columnCount = headers.size();
            int rowCount = 0;

            for (CSVRecord record : parser) {
                if (record.size() != columnCount) {
                    throw new BadRequestException(String.format(
                            "CSV validation failed: Inconsistent row length at row %d (expected %d columns, but found %d).",
                            rowCount + 1, columnCount, record.size()
                    ));
                }
                rowCount++;
            }

            if (rowCount == 0) {
                throw new BadRequestException("CSV validation failed: File contains headers but no data rows.");
            }

            return new ValidationResult(headers, rowCount, columnCount);

        } catch (IOException e) {
            throw new BadRequestException("Malformed CSV structure: " + e.getMessage());
        }
    }
}
