package com.company.hunttech.web.screens.company;

import com.company.hunttech.entity.Person;
import com.company.hunttech.service.CompanyRequisitesIngestService;
import com.company.hunttech.service.CompanyRequisitesParsedData;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.util.List;

@UiController("hunttech_SmartCompanyRequisitesUploadScreen")
@UiDescriptor("smart-company-requisites-upload-screen.xml")
public class SmartCompanyRequisitesUploadScreen extends Screen {

    @Inject
    private CompanyRequisitesIngestService companyRequisitesIngestService;
    @Inject
    private FileUploadingAPI fileUploadingAPI;
    @Inject
    private DataManager dataManager;
    @Inject
    private Notifications notifications;

    @Inject
    private FileUploadField fileUpload;
    @Inject
    private Label<String> selectedFileNameLabel;
    @Inject
    private TextArea<String> rawTextArea;
    @Inject
    private TextField<String> urlField;

    @Inject
    private GroupBoxLayout previewBox;
    @Inject
    private Label<String> previewName;
    @Inject
    private Label<String> previewInnkpp;
    @Inject
    private Label<String> previewOgrn;
    @Inject
    private Label<String> previewOkpoOkved;
    @Inject
    private Label<String> previewLegalAddress;
    @Inject
    private Label<String> previewActualAddress;
    @Inject
    private Label<String> previewBankBik;
    @Inject
    private Label<String> previewAccounts;
    @Inject
    private Label<String> previewContacts;
    @Inject
    private Label<String> previewDirector;
    @Inject
    private Label<String> statusLabel;
    @Inject
    private Button applyBtn;

    private CompanyRequisitesParsedData parsedData;
    private FileDescriptor uploadedFileDescriptor;

    @Subscribe
    public void onInit(InitEvent event) {
        fileUpload.addFileUploadSucceedListener(e -> {
            File file = fileUploadingAPI.getFile(fileUpload.getFileId());
            if (file != null) {
                try {
                    uploadedFileDescriptor = fileUploadingAPI.getFileDescriptor(fileUpload.getFileId(), fileUpload.getFileName());
                    selectedFileNameLabel.setValue(fileUpload.getFileName() + " (" + (file.length() / 1024) + " КБ)");
                } catch (Exception ex) {
                    notifications.create(Notifications.NotificationType.ERROR)
                            .withCaption("Ошибка загрузки файла")
                            .withDescription(ex.getMessage())
                            .show();
                }
            }
        });
    }

    @Subscribe("parseFileBtn")
    public void onParseFileBtnClick(Button.ClickEvent event) {
        if (fileUpload.getFileId() == null) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Выберите файл для распознавания")
                    .show();
            return;
        }

        try {
            File file = fileUploadingAPI.getFile(fileUpload.getFileId());
            if (file != null && uploadedFileDescriptor != null) {
                fileUploadingAPI.putFileIntoStorage(fileUpload.getFileId(), uploadedFileDescriptor);
                dataManager.commit(uploadedFileDescriptor);
                String text = companyRequisitesIngestService.extractTextFromFile(uploadedFileDescriptor);
                processTextAndShowPreview(text);
            }
        } catch (Exception e) {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка сохранения файла в хранилище")
                    .withDescription(e.getMessage())
                    .show();
        }
    }

    @Subscribe("parseTextBtn")
    public void onParseTextBtnClick(Button.ClickEvent event) {
        String text = rawTextArea.getValue();
        if (text == null || text.trim().isEmpty()) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Вставьте текст реквизитов")
                    .show();
            return;
        }
        processTextAndShowPreview(text);
    }

    @Subscribe("parseUrlBtn")
    public void onParseUrlBtnClick(Button.ClickEvent event) {
        String url = urlField.getValue();
        if (url == null || url.trim().isEmpty()) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Введите URL")
                    .show();
            return;
        }
        String text = companyRequisitesIngestService.extractTextFromUrl(url);
        if (text.isEmpty()) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Не удалось извлечь текст по указанной ссылке")
                    .show();
            return;
        }
        processTextAndShowPreview(text);
    }

    private void processTextAndShowPreview(String text) {
        statusLabel.setValue("Распознавание реквизитов...");
        parsedData = companyRequisitesIngestService.parseRequisites(text);
        if (parsedData != null && hasMeaningfulData(parsedData)) {
            updatePreview(parsedData);
            previewBox.setVisible(true);
            applyBtn.setEnabled(true);
            statusLabel.setValue("✓ Реквизиты успешно распознаны");
        } else {
            if (parsedData != null) {
                updatePreview(parsedData);
                previewBox.setVisible(true);
            }
            applyBtn.setEnabled(false);
            statusLabel.setValue("Не удалось распознать реквизиты из предоставленного текста");
        }
    }

    private boolean hasMeaningfulData(CompanyRequisitesParsedData data) {
        if (data == null) return false;
        return isNotBlank(data.getInn()) ||
                isNotBlank(data.getCompanyName()) ||
                isNotBlank(data.getOgrn()) ||
                isNotBlank(data.getBik()) ||
                isNotBlank(data.getLegalAddress()) ||
                isNotBlank(data.getDirectorFullName());
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private void updatePreview(CompanyRequisitesParsedData data) {
        String name = (data.getCompanyName() != null ? data.getCompanyName() : "") +
                (data.getCompanyShortName() != null ? " (" + data.getCompanyShortName() + ")" : "");
        previewName.setValue(!name.isEmpty() ? name : "—");

        String innkpp = (data.getInn() != null ? "ИНН: " + data.getInn() : "") +
                (data.getKpp() != null ? " / КПП: " + data.getKpp() : "");
        previewInnkpp.setValue(!innkpp.isEmpty() ? innkpp : "—");

        previewOgrn.setValue(data.getOgrn() != null ? data.getOgrn() : "—");

        String okpoOkved = (data.getOkpo() != null ? "ОКПО: " + data.getOkpo() : "") +
                (data.getOkved() != null ? " / ОКВЭД: " + data.getOkved() : "");
        previewOkpoOkved.setValue(!okpoOkved.isEmpty() ? okpoOkved : "—");

        previewLegalAddress.setValue(data.getLegalAddress() != null ? data.getLegalAddress() : "—");
        previewActualAddress.setValue(data.getActualAddress() != null ? data.getActualAddress() : "—");

        String bankBik = (data.getBankName() != null ? data.getBankName() : "") +
                (data.getBik() != null ? " (БИК: " + data.getBik() + ")" : "");
        previewBankBik.setValue(!bankBik.isEmpty() ? bankBik : "—");

        String accounts = (data.getSettlementAccount() != null ? "Р/с: " + data.getSettlementAccount() : "") +
                (data.getCorrespondentAccount() != null ? " / К/с: " + data.getCorrespondentAccount() : "");
        previewAccounts.setValue(!accounts.isEmpty() ? accounts : "—");

        String contacts = (data.getPhone() != null ? "📞 " + data.getPhone() : "") +
                (data.getEmail() != null ? " ✉ " + data.getEmail() : "") +
                (data.getWebsite() != null ? " 🌐 " + data.getWebsite() : "");
        previewContacts.setValue(!contacts.isEmpty() ? contacts : "—");

        String directorName = data.getDirectorFullName();
        if (!directorName.isEmpty()) {
            String safeDirectorName = escapeHtml(directorName);
            // Проверяем наличие в базе
            String lastName = data.getDirectorLastName() != null ? data.getDirectorLastName().trim().toLowerCase() : "";
            String firstName = data.getDirectorFirstName() != null ? data.getDirectorFirstName().trim().toLowerCase() : "";
            List<Person> matches = dataManager.load(Person.class)
                    .query("select p from hunttech_Person p where lower(p.secondName) = :lastName and lower(p.firstName) = :firstName")
                    .parameter("lastName", lastName)
                    .parameter("firstName", firstName)
                    .view("person-picker-view")
                    .list();

            if (matches != null && !matches.isEmpty()) {
                previewDirector.setValue("👤 <b>" + safeDirectorName + "</b> <span style='background: #dcfce7; color: #15803d; padding: 1px 6px; border-radius: 4px; font-size: 10px; font-weight: 600;'>Найден в базе</span>");
            } else {
                previewDirector.setValue("👤 <b>" + safeDirectorName + "</b> <span style='background: #eff6ff; color: #2563eb; padding: 1px 6px; border-radius: 4px; font-size: 10px; font-weight: 600;'>Будет создан новый</span>");
            }
        } else {
            previewDirector.setValue("—");
        }
    }

    @Subscribe("applyBtn")
    public void onApplyBtnClick(Button.ClickEvent event) {
        if (parsedData != null) {
            close(StandardOutcome.COMMIT);
        }
    }

    @Subscribe("cancelBtn")
    public void onCancelBtnClick(Button.ClickEvent event) {
        close(StandardOutcome.CLOSE);
    }

    public CompanyRequisitesParsedData getParsedData() {
        return parsedData;
    }
}
