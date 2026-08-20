package com.company.hunttech.web.screens.openposition;

import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.service.SmartOpenPositionIngestResult;
import com.company.hunttech.service.SmartOpenPositionIngestService;
import com.company.hunttech.service.SmartOpenPositionParsedData;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;
import com.haulmont.cuba.security.global.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.List;

@UiController("hunttech_SmartOpenPositionUploadScreen")
@UiDescriptor("smart-open-position-upload-screen.xml")
public class SmartOpenPositionUploadScreen extends Screen {
    private static final Logger log = LoggerFactory.getLogger(SmartOpenPositionUploadScreen.class);
    private static final DecimalFormat SALARY_FORMAT = new DecimalFormat("#,###");

    @Inject
    private FileUploadField uploadField;
    @Inject
    private FileUploadingAPI fileUploadingAPI;
    @Inject
    private ProgressBar progressBar;
    @Inject
    private Label<String> statusLabel;

    @Inject
    private VBoxLayout duplicateBox;
    @Inject
    private Label<String> duplicateInfoLabel;

    @Inject
    private VBoxLayout previewCard;
    @Inject
    private Label<String> previewVacancyName;
    @Inject
    private Label<String> previewProject;
    @Inject
    private Label<String> previewPositionType;
    @Inject
    private Label<String> previewGrade;
    @Inject
    private Label<String> previewCity;
    @Inject
    private Label<String> previewRemote;
    @Inject
    private Label<String> previewSalary;
    @Inject
    private Label<String> previewExperience;
    @Inject
    private Label<String> previewSkills;

    @Inject
    private VBoxLayout missingFieldsBox;
    @Inject
    private Label<String> missingFieldsLabel;

    @Inject
    private Button saveNewPositionBtn;
    @Inject
    private Button cancelBtn;

    @Inject
    private RichTextArea vacancyRichTextArea;
    @Inject
    private Button analyzeTextBtn;
    @Inject
    private Button clearTextBtn;

    @Inject
    private com.haulmont.cuba.gui.components.TextField<String> urlField;
    @Inject
    private Button loadFromUrlBtn;
    @Inject
    private Button clearUrlBtn;

    @Inject
    private SmartOpenPositionIngestService smartOpenPositionIngestService;
    @Inject
    private BackgroundWorker backgroundWorker;
    @Inject
    private Notifications notifications;
    @Inject
    private UserSession userSession;
    @Inject
    private com.haulmont.cuba.core.global.DataManager dataManager;

    private SmartOpenPositionParsedData currentParsedData;
    private OpenPosition existingDuplicatePosition;
    private OpenPosition createdPosition;

    public OpenPosition getCreatedPosition() {
        return createdPosition;
    }

    @Subscribe
    public void onInit(InitEvent event) {
        setupUploadListeners();
        setupButtonListeners();
    }

    private void setupUploadListeners() {
        uploadField.addFileUploadSucceedListener(e -> {
            File file = fileUploadingAPI.getFile(uploadField.getFileId());
            if (file != null) {
                statusLabel.setValue("Файл загружен (" + e.getFileName() + "). Распознавание требований вакансии...");
                progressBar.setVisible(true);
                runAsyncFileAnalysis(file);
            }
        });

        uploadField.addFileUploadErrorListener(e -> {
            statusLabel.setValue("Ошибка при загрузке файла: " + e.getCause().getMessage());
            progressBar.setVisible(false);
        });
    }

    private void setupButtonListeners() {
        analyzeTextBtn.addClickListener(e -> {
            String text = vacancyRichTextArea.getValue();
            if (text == null || text.trim().isEmpty()) {
                notifications.create(Notifications.NotificationType.WARNING)
                        .withCaption("Пустой текст")
                        .withDescription("Вставьте текст описания вакансии для распознавания")
                        .show();
                return;
            }
            statusLabel.setValue("AI-анализ текста вакансии...");
            progressBar.setVisible(true);
            runAsyncTextAnalysis(text);
        });

        clearTextBtn.addClickListener(e -> {
            vacancyRichTextArea.clear();
            resetPreview();
            statusLabel.setValue("Поле текста очищено");
        });

        if (clearUrlBtn != null) {
            clearUrlBtn.addClickListener(e -> {
                if (urlField != null) urlField.setValue("");
                statusLabel.setValue("Поле ссылки очищено");
            });
        }

        if (loadFromUrlBtn != null) {
            loadFromUrlBtn.addClickListener(e -> {
                String url = urlField != null ? urlField.getValue() : null;
                if (url == null || url.trim().isEmpty()) {
                    notifications.create(Notifications.NotificationType.WARNING)
                            .withCaption("Укажите ссылку")
                            .withDescription("Пожалуйста, введите интернет-ссылку на вакансию")
                            .show();
                    return;
                }
                statusLabel.setValue("Загрузка страницы по ссылке и AI-анализ...");
                progressBar.setVisible(true);
                runAsyncUrlAnalysis(url.trim());
            });
        }

        saveNewPositionBtn.addClickListener(e -> onSaveNewPositionClick());
        cancelBtn.addClickListener(e -> close(StandardOutcome.CLOSE));
    }

    private void runAsyncUrlAnalysis(String urlString) {
        BackgroundTask<Integer, SmartOpenPositionParsedData> task = new BackgroundTask<Integer, SmartOpenPositionParsedData>(120, this) {
            @Override
            public SmartOpenPositionParsedData run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
                String rawText = fetchTextFromUrl(urlString);
                if (rawText == null || rawText.trim().isEmpty()) {
                    throw new IllegalStateException("Не удалось извлечь текст по указанной ссылке: " + urlString);
                }
                return smartOpenPositionIngestService.parseVacancyText(rawText);
            }

            @Override
            public void done(SmartOpenPositionParsedData result) {
                progressBar.setVisible(false);
                displayAnalysisResult(result);
            }

            @Override
            public boolean handleException(Exception ex) {
                progressBar.setVisible(false);
                statusLabel.setValue("Ошибка загрузки по ссылке: " + ex.getMessage());
                log.error("Ошибка при загрузке вакансии по URL", ex);
                notifications.create(Notifications.NotificationType.ERROR)
                        .withCaption("Ошибка загрузки по ссылке")
                        .withDescription(ex.getMessage())
                        .show();
                return true;
            }
        };

        backgroundWorker.handle(task).execute();
    }

    private String fetchTextFromUrl(String urlString) throws Exception {
        if (urlString == null || urlString.trim().isEmpty()) return null;
        String cleanUrl = urlString.trim();
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://" + cleanUrl;
        }

        org.jsoup.nodes.Document doc = org.jsoup.Jsoup.connect(cleanUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                .referrer("https://www.google.com")
                .timeout(20000)
                .followRedirects(true)
                .get();

        doc.select("script, style, noscript, svg, nav, footer, header, .cookie-banner, .advertisement").remove();

        String title = doc.title();
        String mainContent = "";
        org.jsoup.nodes.Element contentEl = doc.selectFirst("[data-qa='vacancy-description'], [data-qa='resume-block-container'], main, article, .vacancy-section, .job-description, .content, #content, body");
        if (contentEl != null) {
            mainContent = contentEl.text();
        } else if (doc.body() != null) {
            mainContent = doc.body().text();
        } else {
            mainContent = doc.text();
        }

        StringBuilder result = new StringBuilder();
        if (title != null && !title.isEmpty()) {
            result.append(title).append("\n\n");
        }
        result.append(mainContent);
        return result.toString();
    }

    private void runAsyncFileAnalysis(File file) {
        byte[] fileBytes = null;
        FileDescriptor committedFd = null;
        try {
            try (InputStream is = new FileInputStream(file)) {
                fileBytes = is.readAllBytes();
            }
            FileDescriptor fd = uploadField.getFileDescriptor();
            if (fd != null) {
                fileUploadingAPI.putFileIntoStorage(uploadField.getFileId(), fd);
                committedFd = dataManager.commit(fd);
            }
        } catch (Exception ex) {
            log.error("Ошибка чтения или сохранения файла", ex);
        }

        final byte[] bytes = fileBytes;
        final FileDescriptor fd = committedFd;

        BackgroundTask<Integer, SmartOpenPositionParsedData> task = new BackgroundTask<Integer, SmartOpenPositionParsedData>(120, this) {
            @Override
            public SmartOpenPositionParsedData run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
                String rawText = smartOpenPositionIngestService.extractTextFromFile(fd, bytes);
                return smartOpenPositionIngestService.parseVacancyText(rawText);
            }

            @Override
            public void done(SmartOpenPositionParsedData result) {
                progressBar.setVisible(false);
                displayAnalysisResult(result);
            }

            @Override
            public boolean handleException(Exception ex) {
                progressBar.setVisible(false);
                statusLabel.setValue("Ошибка анализа: " + ex.getMessage());
                log.error("Ошибка при AI-распознавании файла вакансии", ex);
                return true;
            }
        };

        backgroundWorker.handle(task).execute();
    }

    private void runAsyncTextAnalysis(String rawText) {
        BackgroundTask<Integer, SmartOpenPositionParsedData> task = new BackgroundTask<Integer, SmartOpenPositionParsedData>(120, this) {
            @Override
            public SmartOpenPositionParsedData run(TaskLifeCycle<Integer> taskLifeCycle) {
                return smartOpenPositionIngestService.parseVacancyText(rawText);
            }

            @Override
            public void done(SmartOpenPositionParsedData result) {
                progressBar.setVisible(false);
                displayAnalysisResult(result);
            }

            @Override
            public boolean handleException(Exception ex) {
                progressBar.setVisible(false);
                statusLabel.setValue("Ошибка анализа: " + ex.getMessage());
                log.error("Ошибка при AI-распознавании текста вакансии", ex);
                return true;
            }
        };

        backgroundWorker.handle(task).execute();
    }

    private void displayAnalysisResult(SmartOpenPositionParsedData data) {
        this.currentParsedData = data;
        if (data == null) {
            statusLabel.setValue("Не удалось извлечь данные");
            return;
        }

        statusLabel.setValue("✓ Данные вакансии успешно распознаны");

        // 1. Заполнение карточки превью
        previewVacancyName.setValue(data.getVacansyName() != null ? data.getVacansyName() : "-");
        previewProject.setValue((data.getProjectName() != null ? data.getProjectName() : "Основной проект") +
                (data.getCompanyName() != null ? " (" + data.getCompanyName() + ")" : ""));
        previewPositionType.setValue(data.getPositionTypeName() != null ? data.getPositionTypeName() : "-");
        previewGrade.setValue(data.getGradeName() != null ? data.getGradeName() : "Не указан");
        previewCity.setValue(data.getCityName() != null ? data.getCityName() : "Не указан");

        String remoteStr = "Удаленно";
        if (data.getRemoteWork() != null) {
            if (data.getRemoteWork() == 0) remoteStr = "В офисе";
            else if (data.getRemoteWork() == 2) remoteStr = "Гибридный";
        }
        previewRemote.setValue(remoteStr);

        if (data.getSalaryMin() != null || data.getSalaryMax() != null) {
            StringBuilder sb = new StringBuilder();
            if (data.getSalaryMin() != null) sb.append(SALARY_FORMAT.format(data.getSalaryMin())).append(" ₽");
            if (data.getSalaryMax() != null) {
                if (sb.length() > 0) sb.append(" — ");
                sb.append(SALARY_FORMAT.format(data.getSalaryMax())).append(" ₽");
            }
            previewSalary.setValue(sb.toString());
        } else {
            previewSalary.setValue("По договоренности");
        }

        previewExperience.setValue(data.getWorkExperience() != null ? data.getWorkExperience() + " лет" : "3 года");

        // Навыки
        if (data.getRequiredSkills() != null && !data.getRequiredSkills().isEmpty()) {
            StringBuilder sb = new StringBuilder("<div style='display: flex; flex-wrap: wrap; gap: 4px;'>");
            for (String s : data.getRequiredSkills()) {
                sb.append("<span style='background: #e0f2fe; color: #0369a1; padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: 500;'>")
                        .append(s)
                        .append("</span>");
            }
            sb.append("</div>");
            previewSkills.setValue(sb.toString());
        } else {
            previewSkills.setValue("<span style='color: #9ca3af;'>Навыки не определены</span>");
        }

        previewCard.setVisible(true);

        // 2. Проверка дубликатов
        existingDuplicatePosition = smartOpenPositionIngestService.findDuplicate(data);
        if (existingDuplicatePosition != null) {
            duplicateInfoLabel.setValue("В базе уже существует открытая вакансия с похожим наименованием: <b>" +
                    existingDuplicatePosition.getVacansyName() + "</b> (ID: " + (existingDuplicatePosition.getVacansyID() != null ? existingDuplicatePosition.getVacansyID() : "") + ")");
            duplicateBox.setVisible(true);
        } else {
            duplicateBox.setVisible(false);
        }

        // 3. Недостающие поля
        if (data.getMissingFields() != null && !data.getMissingFields().isEmpty()) {
            StringBuilder sb = new StringBuilder("<b>Обратите внимание:</b><ul>");
            for (String mf : data.getMissingFields()) {
                sb.append("<li>").append(mf).append("</li>");
            }
            sb.append("</ul>");
            missingFieldsLabel.setValue(sb.toString());
            missingFieldsBox.setVisible(true);
        } else {
            missingFieldsBox.setVisible(false);
        }

        saveNewPositionBtn.setVisible(true);
    }

    private void resetPreview() {
        previewCard.setVisible(false);
        duplicateBox.setVisible(false);
        missingFieldsBox.setVisible(false);
        saveNewPositionBtn.setVisible(false);
        currentParsedData = null;
        existingDuplicatePosition = null;
    }

    private void onSaveNewPositionClick() {
        if (currentParsedData == null) return;
        ExtUser currentUser = (ExtUser) userSession.getCurrentOrSubstitutedUser();
        SmartOpenPositionIngestResult result = smartOpenPositionIngestService.createOpenPosition(currentParsedData, currentUser);

        if (result.isSuccess()) {
            this.createdPosition = result.getOpenPosition();
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption("Вакансия открыта")
                    .withDescription(result.getMessage())
                    .show();
            close(StandardOutcome.COMMIT);
        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка")
                    .withDescription(result.getMessage())
                    .show();
        }
    }
}
