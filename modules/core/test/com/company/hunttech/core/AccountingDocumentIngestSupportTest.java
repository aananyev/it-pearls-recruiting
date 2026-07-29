package com.company.hunttech.core;

import com.company.hunttech.entity.AccountingDocumentType;
import com.company.hunttech.entity.AccountingFlowType;
import org.junit.Test;

import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AccountingDocumentIngestSupportTest {

    @Test
    public void acceptsTelegramPhotosAndPdfDocuments() {
        assertTrue(AccountingDocumentIngestSupport.isSupportedTelegramFile("scan.pdf", "application/pdf"));
        assertTrue(AccountingDocumentIngestSupport.isSupportedTelegramFile("photo.jpg", "image/jpeg"));
        assertTrue(AccountingDocumentIngestSupport.isSupportedTelegramFile("scan.heic", null));
    }

    @Test
    public void resolvesAdvanceReportFlowForReceipts() {
        assertEquals(AccountingFlowType.ADVANCE_REPORT,
                AccountingDocumentIngestSupport.resolveFlowType(
                        "2026-07-29 топливо 3200 ООО Ромашка.jpg", null));
        assertEquals(AccountingDocumentType.RECEIPT,
                AccountingDocumentIngestSupport.resolveDocumentType(
                        "receipt.pdf", "чек такси 900", AccountingFlowType.ADVANCE_REPORT));
    }

    @Test
    public void resolvesPrimaryDocumentTypeFromFileName() {
        assertEquals(AccountingFlowType.PRIMARY,
                AccountingDocumentIngestSupport.resolveFlowType("акт ХАННТЕК.pdf", null));
        assertEquals(AccountingDocumentType.ACT,
                AccountingDocumentIngestSupport.resolveDocumentType(
                        "акт ХАННТЕК.pdf", null, AccountingFlowType.PRIMARY));
        assertEquals(AccountingDocumentType.UPD,
                AccountingDocumentIngestSupport.resolveDocumentType(
                        "УПД клиент.pdf", null, AccountingFlowType.PRIMARY));
    }

    @Test
    public void buildsSafeStoredFileNameWithTelegramMetadata() {
        String fileName = AccountingDocumentIngestSupport.buildStoredFileName(
                "акт: клиент?.pdf",
                "12345",
                777L,
                LocalDateTime.of(2026, 7, 29, 10, 15, 30));

        assertEquals("20260729-101530-12345-777-акт_ клиент_.pdf", fileName);
    }

    @Test
    public void keepsNonExistingPathAsUniquePath() {
        assertEquals(Paths.get("/tmp/hrm-hunttech-doc.pdf"),
                AccountingDocumentIngestSupport.ensureUniquePath(Paths.get("/tmp/hrm-hunttech-doc.pdf")));
    }
}
