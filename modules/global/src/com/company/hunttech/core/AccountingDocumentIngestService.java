package com.company.hunttech.core;

public interface AccountingDocumentIngestService {
    String NAME = "hunttech_AccountingDocumentIngestService";

    AccountingDocumentIngestResult ingestTelegramFile(String sourceFilePath,
                                                      String originalFileName,
                                                      String mimeType,
                                                      Long fileSize,
                                                      String telegramChatId,
                                                      Long telegramMessageId,
                                                      String telegramFileId,
                                                      String telegramUserId,
                                                      String caption);
}
