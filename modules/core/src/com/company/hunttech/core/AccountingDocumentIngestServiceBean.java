package com.company.hunttech.core;

import com.company.hunttech.config.HunttechAccountingBotConfig;
import com.company.hunttech.entity.AccountingAutomationSettings;
import com.company.hunttech.entity.AccountingDocument;
import com.company.hunttech.entity.AccountingDocumentEvent;
import com.company.hunttech.entity.AccountingDocumentEventType;
import com.company.hunttech.entity.AccountingDocumentStatus;
import com.company.hunttech.entity.AccountingDocumentType;
import com.company.hunttech.entity.AccountingFlowType;
import com.company.hunttech.entity.Currency;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.security.app.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service(AccountingDocumentIngestService.NAME)
public class AccountingDocumentIngestServiceBean implements AccountingDocumentIngestService {
    private static final Logger log = LoggerFactory.getLogger(AccountingDocumentIngestServiceBean.class);

    private static final String ENV_ALLOWED_USER_ID = "ACCOUNTING_BOT_ALLOWED_TELEGRAM_USER_ID";
    private static final String ENV_YANDEX_ROOT = "ACCOUNTING_BOT_YANDEX_DISK_ROOT";
    private static final String ENV_INCOMING_SCANS_PATH = "ACCOUNTING_BOT_INCOMING_SCANS_PATH";
    private static final String DEFAULT_YANDEX_ROOT =
            "Yandex.Disk-alan@hunttech.ru.localized/ХантТек";
    private static final String DEFAULT_INCOMING_SCANS_PATH = "Сканы/Входящие";

    @Inject
    private Metadata metadata;
    @Inject
    private DataManager dataManager;
    @Inject
    private Configuration configuration;
    @Inject
    private Authentication authentication;

    @Override
    public AccountingDocumentIngestResult ingestTelegramFile(String sourceFilePath,
                                                             String originalFileName,
                                                             String mimeType,
                                                             Long fileSize,
                                                             String telegramChatId,
                                                             Long telegramMessageId,
                                                              String telegramFileId,
                                                              String telegramUserId,
                                                              String caption) {
        // Telegram long polling работает вне UI-сеанса, поэтому запись в БД выполняется под системной аутентификацией.
        authentication.begin();
        try {
            return ingestTelegramFileAuthenticated(sourceFilePath, originalFileName, mimeType, fileSize,
                    telegramChatId, telegramMessageId, telegramFileId, telegramUserId, caption);
        } finally {
            authentication.end();
        }
    }

    private AccountingDocumentIngestResult ingestTelegramFileAuthenticated(String sourceFilePath,
                                                                           String originalFileName,
                                                                           String mimeType,
                                                                           Long fileSize,
                                                                           String telegramChatId,
                                                                           Long telegramMessageId,
                                                                           String telegramFileId,
                                                                           String telegramUserId,
                                                                           String caption) {
        if (!isTelegramUserAllowed(telegramUserId)) {
            return AccountingDocumentIngestResult.failure("Документ не принят: пользователь Telegram не разрешен.");
        }
        if (!AccountingDocumentIngestSupport.isSupportedTelegramFile(originalFileName, mimeType)) {
            return AccountingDocumentIngestResult.failure("Документ не принят: поддерживаются только фото и PDF.");
        }
        if (!isConfigured(sourceFilePath)) {
            return AccountingDocumentIngestResult.failure("Документ не принят: Telegram не передал локальный файл.");
        }

        Path sourcePath = Paths.get(sourceFilePath);
        if (!Files.isRegularFile(sourcePath)) {
            return AccountingDocumentIngestResult.failure("Документ не принят: локальный файл Telegram не найден.");
        }

        try {
            String fileHash = calculateSha256(sourcePath);
            if (hasDuplicate(fileHash)) {
                return AccountingDocumentIngestResult.duplicate(
                        "Документ уже есть в реестре HRM HuntTech, повторная обработка остановлена.");
            }

            LocalDateTime receivedAt = LocalDateTime.now();
            Path incomingDayFolder = resolveIncomingDayFolder(receivedAt.toLocalDate());
            Files.createDirectories(incomingDayFolder);

            String storedFileName = AccountingDocumentIngestSupport.buildStoredFileName(
                    originalFileName, telegramChatId, telegramMessageId, receivedAt);
            Path storedPath = AccountingDocumentIngestSupport.ensureUniquePath(incomingDayFolder.resolve(storedFileName));
            // Этап 2 только копирует новый Telegram-файл во входящие; существующий архив Yandex.Disk не меняется.
            Files.copy(sourcePath, storedPath);

            AccountingFlowType flowType = AccountingDocumentIngestSupport.resolveFlowType(originalFileName, caption);
            AccountingDocumentType documentType = AccountingDocumentIngestSupport.resolveDocumentType(
                    originalFileName, caption, flowType);

            AccountingDocument document = metadata.create(AccountingDocument.class);
            document.setFlowType(flowType);
            document.setDocumentType(documentType);
            document.setStatus(AccountingDocumentStatus.NEW);
            document.setReceivedAt(toDate(receivedAt));
            document.setCurrency(loadRubCurrency());
            document.setOriginalFileName(originalFileName);
            document.setFinalFileName(storedPath.getFileName().toString());
            document.setYandexDiskOriginalPath(storedPath.toString());
            document.setFileHash(fileHash);
            document.setFileSize(resolveFileSize(fileSize, storedPath));
            document.setMimeType(mimeType);
            document.setTelegramChatId(telegramChatId);
            document.setTelegramMessageId(telegramMessageId);
            document.setTelegramFileId(telegramFileId);
            document.setConfirmedByTelegramUserId(telegramUserId);

            AccountingDocument savedDocument = dataManager.commit(document);
            saveReceivedEvent(savedDocument, telegramUserId, storedPath);

            String answer = "Документ принят в реестр HRM HuntTech.\n"
                    + "Поток: " + flowType + "\n"
                    + "Файл: " + storedPath.getFileName();
            return AccountingDocumentIngestResult.success(savedDocument.getId(), answer, storedPath.toString());
        } catch (IOException e) {
            log.warn("Unable to ingest Telegram accounting document: {}", e.getMessage(), e);
            return AccountingDocumentIngestResult.failure("Документ не принят: ошибка сохранения файла.");
        } catch (NoSuchAlgorithmException e) {
            log.warn("Unable to calculate accounting document hash: {}", e.getMessage(), e);
            return AccountingDocumentIngestResult.failure("Документ не принят: ошибка расчета hash.");
        }
    }

    private void saveReceivedEvent(AccountingDocument document, String telegramUserId, Path storedPath) {
        AccountingDocumentEvent event = metadata.create(AccountingDocumentEvent.class);
        event.setDocument(document);
        event.setEventAt(new Date());
        event.setEventType(AccountingDocumentEventType.RECEIVED);
        event.setNewStatus(AccountingDocumentStatus.NEW);
        event.setSource("telegram");
        event.setTelegramUserId(telegramUserId);
        event.setMessage("Файл получен из Telegram и сохранен во входящую папку: " + storedPath);
        dataManager.commit(event);
    }

    private boolean hasDuplicate(String fileHash) {
        Long count = dataManager.loadValue(
                        "select count(e) from hunttech_AccountingDocument e where e.fileHash = :fileHash",
                        Long.class)
                .parameter("fileHash", fileHash)
                .one();
        return count != null && count > 0;
    }

    private Currency loadRubCurrency() {
        List<Currency> currencies = dataManager.load(Currency.class)
                .query("select e from hunttech_Currency e where e.currencyShortName = :code")
                .parameter("code", "RUB")
                .view("currency-view")
                .list();
        return currencies.isEmpty() ? null : currencies.get(0);
    }

    private Path resolveIncomingDayFolder(LocalDate date) {
        AccountingAutomationSettings settings = loadActiveSettings();
        String root = firstConfigured(
                settings == null ? null : settings.getYandexDiskRootPath(),
                configuration.getConfig(HunttechAccountingBotConfig.class).getYandexDiskRootPath(),
                System.getenv(ENV_YANDEX_ROOT),
                Paths.get(System.getProperty("user.home"), DEFAULT_YANDEX_ROOT).toString()
        );
        String incomingPath = firstConfigured(
                settings == null ? null : settings.getIncomingScansPath(),
                configuration.getConfig(HunttechAccountingBotConfig.class).getIncomingScansPath(),
                System.getenv(ENV_INCOMING_SCANS_PATH),
                DEFAULT_INCOMING_SCANS_PATH
        );
        return Paths.get(root).resolve(incomingPath).resolve(date.toString());
    }

    private AccountingAutomationSettings loadActiveSettings() {
        List<AccountingAutomationSettings> settings = dataManager.load(AccountingAutomationSettings.class)
                .query("select e from hunttech_AccountingAutomationSettings e where e.active = true")
                .view("accountingAutomationSettings-view")
                .maxResults(1)
                .list();
        return settings.isEmpty() ? null : settings.get(0);
    }

    private boolean isTelegramUserAllowed(String telegramUserId) {
        String allowedTelegramUserId = firstConfigured(
                loadActiveSettingsTelegramUserId(),
                configuration.getConfig(HunttechAccountingBotConfig.class).getAllowedTelegramUserId(),
                System.getenv(ENV_ALLOWED_USER_ID)
        );
        if (!isConfigured(allowedTelegramUserId)) {
            log.warn("Accounting bot allowed Telegram user id is not configured; incoming file is accepted");
            return true;
        }
        return allowedTelegramUserId.trim().equals(telegramUserId == null ? "" : telegramUserId.trim());
    }

    private String loadActiveSettingsTelegramUserId() {
        AccountingAutomationSettings settings = loadActiveSettings();
        return settings == null ? null : settings.getConfirmationTelegramUserId();
    }

    private Long resolveFileSize(Long telegramFileSize, Path storedPath) throws IOException {
        return telegramFileSize != null && telegramFileSize > 0 ? telegramFileSize : Files.size(storedPath);
    }

    private String calculateSha256(Path sourcePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream inputStream = Files.newInputStream(sourcePath);
             DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
            byte[] buffer = new byte[8192];
            while (digestInputStream.read(buffer) >= 0) {
                // DigestInputStream обновляет hash при чтении.
            }
        }
        return toHex(digest.digest());
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private String firstConfigured(String... values) {
        for (String value : values) {
            if (isConfigured(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean isConfigured(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
