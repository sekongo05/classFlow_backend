package com.mosek.classFlow.business;

import com.mosek.classFlow.exception.BusinessRuleException;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Lecteur mutualisé pour les fichiers tabulaires CSV et Excel (.xlsx).
 */
@Slf4j
final class TabularFileReader {

    private TabularFileReader() {
    }

    static List<List<String>> readRows(MultipartFile file, String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        try {
            if (lower.endsWith(".csv")) {
                byte[] bytes = file.getBytes();
                String sample = new String(bytes, 0, Math.min(bytes.length, 1024), StandardCharsets.UTF_8);
                char sep = sample.chars().filter(c -> c == ';').count() >= sample.chars().filter(c -> c == ',').count() ? ';' : ',';
                try (var br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
                     CSVReader reader = new CSVReaderBuilder(br)
                             .withCSVParser(new CSVParserBuilder().withSeparator(sep).build())
                             .build()) {
                    List<List<String>> rows = new ArrayList<>();
                    String[] line;
                    while ((line = reader.readNext()) != null) {
                        rows.add(Arrays.stream(line).map(String::trim).toList());
                    }
                    return rows;
                }
            }
            if (lower.endsWith(".xlsx")) {
                List<List<String>> rows = new ArrayList<>();
                DataFormatter fmt = new DataFormatter();
                try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
                    Sheet sheet = wb.getSheetAt(0);
                    for (Row row : sheet) {
                        List<String> values = new ArrayList<>();
                        for (int c = 0; c < row.getLastCellNum(); c++) {
                            var cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                            values.add(cell == null ? "" : fmt.formatCellValue(cell).trim());
                        }
                        rows.add(values);
                    }
                }
                return rows;
            }
            throw new BusinessRuleException("UNSUPPORTED_FORMAT", "Formats acceptés : Excel (.xlsx) ou CSV (.csv).");
        } catch (BusinessRuleException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Tabular file parse error ({}): {}", fileName, ex.getMessage(), ex);
            throw new BusinessRuleException("FILE_READ_ERROR", "Impossible de lire le fichier. Vérifiez son format.");
        }
    }

    static String cell(List<String> row, int idx) {
        return idx < row.size() && row.get(idx) != null ? row.get(idx).trim() : "";
    }
}
