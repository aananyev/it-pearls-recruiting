package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.service.SmartCvIngestResult;
import com.company.hunttech.service.SmartCvIngestService;
import com.company.hunttech.service.SmartCvParsedData;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.FileUploadField;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.ProgressBar;
import com.haulmont.cuba.gui.components.RichTextArea;
import com.haulmont.cuba.gui.components.UploadField;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.StandardCloseAction;
import com.haulmont.cuba.gui.screen.StandardOutcome;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;
import com.haulmont.cuba.security.global.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@UiController("hunttech_SmartCvUploadScreen")
@UiDescriptor("smart-cv-upload-screen.xml")
public class SmartCvUploadScreen extends Screen {
    private static final Logger log = LoggerFactory.getLogger(SmartCvUploadScreen.class);

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
    private Label<String> previewFullName;
    @Inject
    private Label<String> previewPosition;
    @Inject
    private Label<String> previewCity;
    @Inject
    private Label<String> previewCompany;
    @Inject
    private Label<String> previewPhone;
    @Inject
    private Label<String> previewEmail;
    @Inject
    private Label<String> previewTelegram;
    @Inject
    private Label<String> previewSalary;
    @Inject
    private Label<String> previewSkills;
    @Inject
    private VBoxLayout summaryBox;
    @Inject
    private Label<String> previewSummary;

    @Inject
    private VBoxLayout missingFieldsBox;
    @Inject
    private Label<String> missingFieldsLabel;

    @Inject
    private Button attachDuplicateBtn;
    @Inject
    private Button createNewAnywayBtn;
    @Inject
    private Button saveNewCandidateBtn;
    @Inject
    private Button cancelBtn;

    @Inject
    private RichTextArea cvRichTextArea;
    @Inject
    private Button analyzeTextBtn;
    @Inject
    private Button clearTextBtn;

    @Inject
    private SmartCvIngestService smartCvIngestService;
    @Inject
    private BackgroundWorker backgroundWorker;
    @Inject
    private Notifications notifications;
    @Inject
    private UserSession userSession;
    @Inject
    private DataManager dataManager;

    private FileDescriptor currentFileDescriptor;
    private byte[] currentFileBytes;
    private SmartCvParsedData currentParsedData;
    private JobCandidate currentDuplicateCandidate;
    private JobCandidate createdCandidate;

    public JobCandidate getCreatedCandidate() {
        return createdCandidate;
    }

    @Subscribe("clearTextBtn")
    public void onClearTextBtnClick(Button.ClickEvent event) {
        cvRichTextArea.setValue("");
    }

    @Subscribe("analyzeTextBtn")
    public void onAnalyzeTextBtnClick(Button.ClickEvent event) {
        String text = cvRichTextArea.getValue();
        if (text == null || text.trim().isEmpty()) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Предупреждение")
                    .withDescription("Пожалуйста, введите или вставьте текст резюме")
                    .show();
            return;
        }
        currentFileDescriptor = null;
        currentFileBytes = null;
        startAnalysis(text);
    }

    @Subscribe("uploadField")
    public void onUploadFieldFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {
        File file = fileUploadingAPI.getFile(uploadField.getFileId());
        if (file == null) {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка")
                    .withDescription("Не удалось получить временный файл")
                    .show();
            return;
        }

        try {
            // Читаем байты из временного файла ДО того, как putFileIntoStorage переместит его
            try (InputStream is = new FileInputStream(file)) {
                currentFileBytes = is.readAllBytes();
            }

            currentFileDescriptor = uploadField.getFileDescriptor();
            fileUploadingAPI.putFileIntoStorage(uploadField.getFileId(), currentFileDescriptor);
            currentFileDescriptor = dataManager.commit(currentFileDescriptor);
        } catch (Exception e) {
            log.error("Ошибка сохранения файла в FileStorage", e);
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка")
                    .withDescription("Не удалось сохранить файл в хранилище: " + e.getMessage())
                    .show();
            return;
        }

        startAnalysis(null);
    }

    private void startAnalysis(String directRawText) {
        progressBar.setVisible(true);
        statusLabel.setValue("Извлечение текста и AI-анализ резюме...");
        previewCard.setVisible(false);
        duplicateBox.setVisible(false);
        missingFieldsBox.setVisible(false);
        saveNewCandidateBtn.setVisible(false);
        attachDuplicateBtn.setVisible(false);
        createNewAnywayBtn.setVisible(false);

        final FileDescriptor fd = currentFileDescriptor;
        final byte[] bytes = currentFileBytes;
        final String textInput = directRawText;

        BackgroundTask<Integer, AnalysisOutcome> task =
                new BackgroundTask<Integer, AnalysisOutcome>(300, this) {
                    @Override
                    public AnalysisOutcome run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
                        String rawText;
                        if (textInput != null && !textInput.trim().isEmpty()) {
                            rawText = textInput;
                        } else {
                            rawText = smartCvIngestService.extractTextFromFile(fd, bytes);
                        }
                        SmartCvParsedData parsed = smartCvIngestService.parseCvText(rawText);
                        JobCandidate duplicate = smartCvIngestService.findDuplicate(parsed);
                        return new AnalysisOutcome(parsed, duplicate);
                    }

                    @Override
                    public void done(AnalysisOutcome outcome) {
                        progressBar.setVisible(false);
                        statusLabel.setValue("AI-анализ завершен успешно!");
                        currentParsedData = outcome.parsedData;
                        currentDuplicateCandidate = outcome.duplicate;
                        displayParsedResult();
                    }

                    @Override
                    public boolean handleException(Exception ex) {
                        progressBar.setVisible(false);
                        statusLabel.setValue("Ошибка при AI-анализе");
                        notifications.create(Notifications.NotificationType.ERROR)
                                .withCaption("Ошибка анализа")
                                .withDescription(ex.getMessage())
                                .show();
                        return true;
                    }
                };

        backgroundWorker.handle(task).execute();
    }

    private void displayParsedResult() {
        if (currentParsedData == null) return;

        previewFullName.setValue(currentParsedData.getFullName().isEmpty() ? "Не указано" : currentParsedData.getFullName());
        previewPosition.setValue(currentParsedData.getPosition() != null ? currentParsedData.getPosition() : "-");
        previewCity.setValue(currentParsedData.getCity() != null ? currentParsedData.getCity() : "-");
        previewCompany.setValue(currentParsedData.getCurrentCompany() != null ? currentParsedData.getCurrentCompany() : "-");
        previewPhone.setValue(currentParsedData.getPhone() != null ? currentParsedData.getPhone() : "-");
        previewEmail.setValue(currentParsedData.getEmail() != null ? currentParsedData.getEmail() : "-");
        previewTelegram.setValue(currentParsedData.getTelegram() != null ? "@" + currentParsedData.getTelegram() : "-");
        previewSalary.setValue(currentParsedData.getSalary() != null ? currentParsedData.getSalary() : "-");

        if (currentParsedData.getSkills() != null && !currentParsedData.getSkills().isEmpty()) {
            StringBuilder sb = new StringBuilder("<div style='display: flex; flex-wrap: wrap; gap: 4px;'>");
            String[] palette = new String[]{"#38bdf8", "#4ade80", "#c084fc", "#fb923c", "#2dd4bf", "#f472b6", "#facc15", "#60a5fa"};
            for (String sk : currentParsedData.getSkills()) {
                String col = palette[Math.floorMod(sk.hashCode(), palette.length)];
                sb.append(String.format("<span style='background: %s18; color: %s; border: 1px solid %s44; padding: 2px 7px; border-radius: 10px; font-size: 11px; font-weight: 600;'>%s</span>",
                        col, col, col, escapeHtml(sk)));
            }
            sb.append("</div>");
            previewSkills.setValue(sb.toString());
        } else {
            previewSkills.setValue("<span style='color: #94a3b8; font-size: 11px;'>Навыки не определены</span>");
        }

        if (currentParsedData.getSummary() != null && !currentParsedData.getSummary().isEmpty()) {
            summaryBox.setVisible(true);
            previewSummary.setValue(currentParsedData.getSummary());
        } else {
            summaryBox.setVisible(false);
        }

        previewCard.setVisible(true);

        // Проверка дубликатов
        if (currentDuplicateCandidate != null) {
            duplicateBox.setVisible(true);
            String dupName = escapeHtml(currentDuplicateCandidate.getFullName() != null ? currentDuplicateCandidate.getFullName() : "Без имени");
            String dupPhone = escapeHtml(currentDuplicateCandidate.getPhone() != null ? currentDuplicateCandidate.getPhone() : "-");
            String dupEmail = escapeHtml(currentDuplicateCandidate.getEmail() != null ? currentDuplicateCandidate.getEmail() : "-");
            String dupRecruiter = escapeHtml(currentDuplicateCandidate.getCreatedBy() != null ? currentDuplicateCandidate.getCreatedBy() : "-");

            duplicateInfoLabel.setValue(String.format(
                    "В базе уже есть кандидат с похожими данными: <b>%s</b> (Тел: %s, Email: %s). Создан пользователем: <b>%s</b>.<br/>" +
                    "Вы можете добавить это резюме как новую версию существующего кандидата или создать новую запись.",
                    dupName, dupPhone, dupEmail, dupRecruiter));

            attachDuplicateBtn.setVisible(true);
            createNewAnywayBtn.setVisible(true);
            saveNewCandidateBtn.setVisible(false);
        } else {
            duplicateBox.setVisible(false);
            attachDuplicateBtn.setVisible(false);
            createNewAnywayBtn.setVisible(false);
            saveNewCandidateBtn.setVisible(true);
        }

        // Проверка недостающих полей
        List<String> missing = validateMissingFields(currentParsedData);
        if (!missing.isEmpty()) {
            missingFieldsBox.setVisible(true);
            List<String> escapedMissing = new ArrayList<>();
            for (String m : missing) {
                escapedMissing.add(escapeHtml(m));
            }
            missingFieldsLabel.setValue(String.format(
                    "<span style='color: #1e40af; font-size: 12px;'>ℹ️ <b>Внимание:</b> В резюме не найдены некоторые рекомендуемые поля: <b>%s</b>. " +
                    "После сохранения вы сможете заполнить их в карточке кандидата.</span>",
                    String.join(", ", escapedMissing)));
        } else {
            missingFieldsBox.setVisible(false);
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private List<String> validateMissingFields(SmartCvParsedData d) {
        List<String> list = new ArrayList<>();
        if (d.getFirstName() == null || d.getFirstName().isEmpty()) list.add("Имя");
        if (d.getLastName() == null || d.getLastName().isEmpty()) list.add("Фамилия");
        if (d.getPosition() == null || d.getPosition().isEmpty()) list.add("Должность");
        if (d.getCity() == null || d.getCity().isEmpty()) list.add("Город");
        if ((d.getPhone() == null || d.getPhone().isEmpty()) && (d.getEmail() == null || d.getEmail().isEmpty()) && (d.getTelegram() == null || d.getTelegram().isEmpty())) {
            list.add("Контакты");
        }
        return list;
    }

    @Subscribe("saveNewCandidateBtn")
    public void onSaveNewCandidateBtnClick(Button.ClickEvent event) {
        if (currentParsedData == null) return;
        ExtUser currentUser = (ExtUser) userSession.getUser();
        SmartCvIngestResult res = smartCvIngestService.createNewCandidate(currentParsedData, currentFileDescriptor, null, currentUser);
        if (res.getStatus() == SmartCvIngestResult.Status.SUCCESS) {
            createdCandidate = res.getCandidate();
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption("Кандидат успешно создан")
                    .withDescription(createdCandidate.getFullName())
                    .show();
            close(StandardOutcome.COMMIT);
        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка")
                    .withDescription(res.getMessage())
                    .show();
        }
    }

    @Subscribe("attachDuplicateBtn")
    public void onAttachDuplicateBtnClick(Button.ClickEvent event) {
        if (currentParsedData == null || currentDuplicateCandidate == null) return;
        ExtUser currentUser = (ExtUser) userSession.getUser();
        SmartCvIngestResult res = smartCvIngestService.attachCvToExistingCandidate(
                currentDuplicateCandidate.getId(), currentParsedData, currentFileDescriptor, null, currentUser);
        if (res.getStatus() == SmartCvIngestResult.Status.SUCCESS) {
            createdCandidate = res.getCandidate();
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption("Резюме прикреплено к кандидату")
                    .withDescription(createdCandidate.getFullName())
                    .show();
            close(StandardOutcome.COMMIT);
        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка")
                    .withDescription(res.getMessage())
                    .show();
        }
    }

    @Subscribe("createNewAnywayBtn")
    public void onCreateNewAnywayBtnClick(Button.ClickEvent event) {
        onSaveNewCandidateBtnClick(event);
    }

    @Subscribe("cancelBtn")
    public void onCancelBtnClick(Button.ClickEvent event) {
        close(StandardOutcome.CLOSE);
    }

    private static class AnalysisOutcome {
        final SmartCvParsedData parsedData;
        final JobCandidate duplicate;

        AnalysisOutcome(SmartCvParsedData parsedData, JobCandidate duplicate) {
            this.parsedData = parsedData;
            this.duplicate = duplicate;
        }
    }
}
