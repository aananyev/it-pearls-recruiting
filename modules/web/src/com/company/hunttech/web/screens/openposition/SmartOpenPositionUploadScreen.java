package com.company.hunttech.web.screens.openposition;

import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.service.SmartOpenPositionIngestResult;
import com.company.hunttech.service.SmartOpenPositionIngestService;
import com.company.hunttech.service.SmartOpenPositionParsedData;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.gui.Dialogs;
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
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
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
    @Inject
    private Dialogs dialogs;

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
                log.info("[SMART_VACANCY_OPENING_UI] Пользователь загрузил файл: '{}' (размер: {} байт)", e.getFileName(), file.length());
                statusLabel.setValue("Файл загружен (" + e.getFileName() + "). Распознавание требований вакансии...");
                progressBar.setVisible(true);
                runAsyncFileAnalysis(file);
            } else {
                log.warn("[SMART_VACANCY_OPENING_UI] Файл не найден в fileUploadingAPI для fileId: {}", uploadField.getFileId());
            }
        });

        uploadField.addFileUploadErrorListener(e -> {
            log.error("[SMART_VACANCY_OPENING_UI] Ошибка при загрузке файла через FileUploadField: {}", e.getCause().getMessage(), e.getCause());
            statusLabel.setValue("Ошибка при загрузке файла: " + e.getCause().getMessage());
            progressBar.setVisible(false);
        });
    }

    private void setupButtonListeners() {
        analyzeTextBtn.addClickListener(e -> {
            String text = vacancyRichTextArea.getValue();
            if (text == null || text.trim().isEmpty()) {
                log.warn("[SMART_VACANCY_OPENING_UI] Нажата кнопка анализа текста, но поле ввода пусто");
                notifications.create(Notifications.NotificationType.WARNING)
                        .withCaption("Пустой текст")
                        .withDescription("Вставьте текст описания вакансии для распознавания")
                        .show();
                return;
            }
            log.info("[SMART_VACANCY_OPENING_UI] Пользователь инициировал AI-анализ текста (длина: {} символов)", text.length());
            statusLabel.setValue("AI-анализ текста вакансии...");
            progressBar.setVisible(true);
            runAsyncTextAnalysis(text);
        });

        clearTextBtn.addClickListener(e -> {
            log.info("[SMART_VACANCY_OPENING_UI] Очистка текста вакансии и сброс превью");
            vacancyRichTextArea.clear();
            resetPreview();
            statusLabel.setValue("Поле текста очищено");
        });

        if (clearUrlBtn != null) {
            clearUrlBtn.addClickListener(e -> {
                log.info("[SMART_VACANCY_OPENING_UI] Очистка поля ссылки");
                if (urlField != null) urlField.setValue("");
                statusLabel.setValue("Поле ссылки очищено");
            });
        }

        if (loadFromUrlBtn != null) {
            loadFromUrlBtn.addClickListener(e -> {
                String url = urlField != null ? urlField.getValue() : null;
                if (url == null || url.trim().isEmpty()) {
                    log.warn("[SMART_VACANCY_OPENING_UI] Нажата кнопка загрузки по ссылке, но URL не введен");
                    notifications.create(Notifications.NotificationType.WARNING)
                            .withCaption("Укажите ссылку")
                            .withDescription("Пожалуйста, введите интернет-ссылку на вакансию")
                            .show();
                    return;
                }
                log.info("[SMART_VACANCY_OPENING_UI] Пользователь инициировал загрузку по ссылке: '{}'", url.trim());
                statusLabel.setValue("Загрузка страницы по ссылке и AI-анализ...");
                progressBar.setVisible(true);
                runAsyncUrlAnalysis(url.trim());
            });
        }

        saveNewPositionBtn.addClickListener(e -> onSaveNewPositionClick());
        cancelBtn.addClickListener(e -> {
            log.info("[SMART_VACANCY_OPENING_UI] Пользователь отменил создание вакансии и закрыл диалог");
            close(StandardOutcome.CLOSE);
        });
    }

    private void runAsyncUrlAnalysis(String urlString) {
        log.info("[SMART_VACANCY_OPENING_UI] Запуск фоновой задачи анализа URL: {}", urlString);
        BackgroundTask<Integer, SmartOpenPositionParsedData> task = new BackgroundTask<Integer, SmartOpenPositionParsedData>(120, this) {
            @Override
            public SmartOpenPositionParsedData run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
                String rawText = fetchTextFromUrl(urlString);
                if (rawText == null || rawText.trim().isEmpty()) {
                    throw new IllegalStateException("Не удалось извлечь текст по указанной ссылке: " + urlString);
                }
                log.info("[SMART_VACANCY_OPENING_UI] Текст по ссылке '{}' успешно получен (длина: {} символов). Запуск парсинга...", urlString, rawText.length());
                return smartOpenPositionIngestService.parseVacancyText(rawText);
            }

            @Override
            public void done(SmartOpenPositionParsedData result) {
                log.info("[SMART_VACANCY_OPENING_UI] Фоновый анализ URL успешно завершен. Результат: {}", result != null ? result.getVacansyName() : "null");
                progressBar.setVisible(false);
                displayAnalysisResult(result);
            }

            @Override
            public boolean handleException(Exception ex) {
                progressBar.setVisible(false);
                statusLabel.setValue("Ошибка загрузки по ссылке: " + ex.getMessage());
                log.error("[SMART_VACANCY_OPENING_UI] ✘ Исключение при загрузке/анализе вакансии по URL: " + urlString, ex);
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

        log.info("[SMART_VACANCY_OPENING_UI] HTTP GET запрос к странице вакансии: {}", cleanUrl);
        java.net.URL url = new java.net.URL(cleanUrl);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);

        int code = conn.getResponseCode();
        log.info("[SMART_VACANCY_OPENING_UI] HTTP статус ответа для {}: {}", cleanUrl, code);
        if (code >= 400) {
            throw new IllegalStateException("HTTP ошибка " + code + " при открытии страницы");
        }

        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            String html = sb.toString();
            // Простое извлечение видимого текста
            String text = html.replaceAll("(?is)<script.*?</script>", " ")
                    .replaceAll("(?is)<style.*?</style>", " ")
                    .replaceAll("<[^>]+>", " ")
                    .replaceAll("&nbsp;", " ")
                    .replaceAll("&quot;", "\"")
                    .replaceAll("&amp;", "&")
                    .replaceAll("\\s+", " ")
                    .trim();
            log.info("[SMART_VACANCY_OPENING_UI] Извлечен видимый текст со страницы (длина: {} символов)", text.length());
            return text;
        }
    }

    private void runAsyncFileAnalysis(File file) {
        log.info("[SMART_VACANCY_OPENING_UI] Запуск фоновой задачи анализа файла: '{}' (размер: {} байт)", file.getName(), file.length());
        FileDescriptor fd = uploadField.getValue();
        byte[] bytes;
        try (InputStream is = new FileInputStream(file)) {
            bytes = is.readAllBytes();
        } catch (Exception e) {
            log.error("[SMART_VACANCY_OPENING_UI] ✘ Не удалось прочитать локальный файл " + file.getAbsolutePath(), e);
            statusLabel.setValue("Ошибка чтения файла: " + e.getMessage());
            progressBar.setVisible(false);
            return;
        }

        BackgroundTask<Integer, SmartOpenPositionParsedData> task = new BackgroundTask<Integer, SmartOpenPositionParsedData>(120, this) {
            @Override
            public SmartOpenPositionParsedData run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
                String rawText = smartOpenPositionIngestService.extractTextFromFile(fd, bytes);
                log.info("[SMART_VACANCY_OPENING_UI] Текст из файла извлечен (длина: {}). Запуск парсинга...", rawText != null ? rawText.length() : 0);
                return smartOpenPositionIngestService.parseVacancyText(rawText);
            }

            @Override
            public void done(SmartOpenPositionParsedData result) {
                log.info("[SMART_VACANCY_OPENING_UI] Фоновый анализ файла завершен. Позиция: '{}'", result != null ? result.getVacansyName() : "null");
                progressBar.setVisible(false);
                displayAnalysisResult(result);
            }

            @Override
            public boolean handleException(Exception ex) {
                progressBar.setVisible(false);
                statusLabel.setValue("Ошибка анализа: " + ex.getMessage());
                log.error("[SMART_VACANCY_OPENING_UI] ✘ Ошибка при AI-распознавании файла вакансии", ex);
                return true;
            }
        };

        backgroundWorker.handle(task).execute();
    }

    private void runAsyncTextAnalysis(String rawText) {
        log.info("[SMART_VACANCY_OPENING_UI] Запуск фоновой задачи анализа текста вакансии...");
        BackgroundTask<Integer, SmartOpenPositionParsedData> task = new BackgroundTask<Integer, SmartOpenPositionParsedData>(120, this) {
            @Override
            public SmartOpenPositionParsedData run(TaskLifeCycle<Integer> taskLifeCycle) {
                return smartOpenPositionIngestService.parseVacancyText(rawText);
            }

            @Override
            public void done(SmartOpenPositionParsedData result) {
                log.info("[SMART_VACANCY_OPENING_UI] Фоновый анализ текста завершен. Позиция: '{}'", result != null ? result.getVacansyName() : "null");
                progressBar.setVisible(false);
                displayAnalysisResult(result);
            }

            @Override
            public boolean handleException(Exception ex) {
                progressBar.setVisible(false);
                statusLabel.setValue("Ошибка анализа: " + ex.getMessage());
                log.error("[SMART_VACANCY_OPENING_UI] ✘ Ошибка при AI-распознавании текста вакансии", ex);
                return true;
            }
        };

        backgroundWorker.handle(task).execute();
    }

    private void displayAnalysisResult(SmartOpenPositionParsedData data) {
        this.currentParsedData = data;
        if (data == null) {
            log.warn("[SMART_VACANCY_OPENING_UI] displayAnalysisResult вызван с data = null");
            statusLabel.setValue("Не удалось извлечь данные");
            return;
        }

        log.info("[SMART_VACANCY_OPENING_UI] Отображение карточки превью: vacansyName='{}', project='{}', company='{}', salary={}-{}, skills={}",
                data.getVacansyName(), data.getProjectName(), data.getCompanyName(), data.getSalaryMin(), data.getSalaryMax(), data.getRequiredSkills());

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
            log.info("[SMART_VACANCY_OPENING_UI] Отображено предупреждение о дубликате: ID={}, name='{}'",
                    existingDuplicatePosition.getVacansyID(), existingDuplicatePosition.getVacansyName());
            duplicateInfoLabel.setValue("В базе уже существует открытая вакансия с похожим наименованием: <b>" +
                    existingDuplicatePosition.getVacansyName() + "</b> (ID: " + (existingDuplicatePosition.getVacansyID() != null ? existingDuplicatePosition.getVacansyID() : "") + ")");
            duplicateBox.setVisible(true);
        } else {
            duplicateBox.setVisible(false);
        }

        // 3. Недостающие поля
        if (data.getMissingFields() != null && !data.getMissingFields().isEmpty()) {
            log.info("[SMART_VACANCY_OPENING_UI] Отображение недостающих полей: {}", data.getMissingFields());
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
        if (currentParsedData == null) {
            log.warn("[SMART_VACANCY_OPENING_UI] onSaveNewPositionClick вызван при currentParsedData == null");
            return;
        }

        // 1. Проверка на дубликат: если дубликат обнаружен, запрашиваем явное подтверждение
        if (existingDuplicatePosition != null) {
            log.warn("[SMART_VACANCY_OPENING_UI] Попытка создания вакансии при наличии существующего дубликата: ID={}, name='{}'",
                    existingDuplicatePosition.getVacansyID(), existingDuplicatePosition.getVacansyName());
            dialogs.createOptionDialog()
                    .withCaption("Обнаружен дубликат вакансии")
                    .withMessage("В базе уже существует открытая вакансия с похожим наименованием:\n«" +
                            existingDuplicatePosition.getVacansyName() + "» (ID: " +
                            (existingDuplicatePosition.getVacansyID() != null ? existingDuplicatePosition.getVacansyID() : "") +
                            ").\n\nВы действительно хотите создать новую вакансию-дубликат?")
                    .withActions(
                            new DialogAction(DialogAction.Type.YES).withHandler(e -> checkMissingFieldsAndProceed()),
                            new DialogAction(DialogAction.Type.NO, Action.Status.PRIMARY)
                    )
                    .show();
            return;
        }

        checkMissingFieldsAndProceed();
    }

    private void checkMissingFieldsAndProceed() {
        // 2. Проверка нехватки ключевых данных: если не указан проект, город или зарплатная вилка
        List<String> missing = new ArrayList<>();
        if (currentParsedData.getProjectName() == null || currentParsedData.getProjectName().trim().isEmpty()) {
            missing.add("Проект (вакансия будет привязана к проекту по умолчанию)");
        }
        if (currentParsedData.getCityName() == null || currentParsedData.getCityName().trim().isEmpty()) {
            missing.add("Город/локация не указаны");
        }
        if (currentParsedData.getSalaryMin() == null && currentParsedData.getSalaryMax() == null) {
            missing.add("Зарплатная вилка не указана");
        }

        if (!missing.isEmpty()) {
            log.info("[SMART_VACANCY_OPENING_UI] Обнаружена нехватка данных перед сохранением: {}", missing);
            StringBuilder sb = new StringBuilder("В описании вакансии не указаны следующие данные:\n");
            for (String item : missing) {
                sb.append("• ").append(item).append("\n");
            }
            sb.append("\nСоздать черновик вакансии с текущими данными?");

            dialogs.createOptionDialog()
                    .withCaption("Неполные данные вакансии")
                    .withMessage(sb.toString())
                    .withActions(
                            new DialogAction(DialogAction.Type.YES).withHandler(e -> doCreatePosition()),
                            new DialogAction(DialogAction.Type.NO)
                    )
                    .show();
            return;
        }

        doCreatePosition();
    }

    private void doCreatePosition() {
        ExtUser currentUser = (ExtUser) userSession.getCurrentOrSubstitutedUser();
        log.info("[SMART_VACANCY_OPENING_UI] Нажата кнопка 'Открыть позицию'. Текущий пользователь: '{}', Вакансия: '{}'",
                currentUser != null ? currentUser.getLogin() : "null", currentParsedData.getVacansyName());

        SmartOpenPositionIngestResult result = smartOpenPositionIngestService.createOpenPosition(currentParsedData, currentUser);

        if (result.isSuccess()) {
            this.createdPosition = result.getOpenPosition();
            log.info("[SMART_VACANCY_OPENING_UI] ✓ Вакансия успешно открыта! ID={}, Сообщение: '{}'",
                    createdPosition != null ? createdPosition.getId() : "null", result.getMessage());
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption("Черновик создан")
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
