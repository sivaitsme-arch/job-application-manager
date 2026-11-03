package com.siva.jobmanager.service;

import com.siva.jobmanager.repository.JobApplicationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ExcelProcessingServiceTest {

    @Test
    void testImportSampleExcel() throws Exception {
        JobApplicationRepository repository = Mockito.mock(JobApplicationRepository.class);
        when(repository.existsByCompanyNameIgnoreCaseAndJobRoleIgnoreCaseAndRecruiterEmailIgnoreCase(any(), any(), any()))
                .thenReturn(false);

        ExcelProcessingService service = new ExcelProcessingService(repository);
        Path sampleExcel = Paths.get("sample-excel-template.xlsx");
        assertTrue(Files.exists(sampleExcel), "sample-excel-template.xlsx should exist");

        ExcelProcessingService.ImportResult result = service.importFile(sampleExcel);
        assertEquals(1, result.imported());
        assertEquals(0, result.duplicates());
        assertEquals(0, result.invalid());
    }

    @Test
    void testImportSampleCsv() throws Exception {
        JobApplicationRepository repository = Mockito.mock(JobApplicationRepository.class);
        when(repository.existsByCompanyNameIgnoreCaseAndJobRoleIgnoreCaseAndRecruiterEmailIgnoreCase(any(), any(), any()))
                .thenReturn(false);

        ExcelProcessingService service = new ExcelProcessingService(repository);
        Path sampleCsv = Paths.get("sample-excel-template.csv");
        assertTrue(Files.exists(sampleCsv), "sample-excel-template.csv should exist");

        ExcelProcessingService.ImportResult result = service.importFile(sampleCsv);
        assertEquals(1, result.imported());
        assertEquals(0, result.duplicates());
        assertEquals(0, result.invalid());
    }
}
