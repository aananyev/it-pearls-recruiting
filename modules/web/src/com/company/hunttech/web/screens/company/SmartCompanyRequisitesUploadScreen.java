package com.company.hunttech.web.screens.company;

import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.Person;
import com.company.hunttech.service.CompanyRequisitesIngestService;
import com.company.hunttech.service.CompanyRequisitesParsedData;
import com.company.hunttech.service.CompanySearchAiService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@UiController("hunttech_SmartCompanyRequisitesUploadScreen")
@UiDescriptor("smart-company-requisites-upload-screen.xml")
public class SmartCompanyRequisitesUploadScreen extends Screen {
    private static final Logger log = LoggerFactory.getLogger(SmartCompanyRequisitesUploadScreen.class);

    @Inject
    private CompanyRequisitesIngestService companyRequisitesIngestService;
    @Inject
    private CompanySearchAiService companySearchAiService;
    @Inject
    private FileUploadingAPI fileUploadingAPI;
    @Inject
    private DataManager dataManager;
    @Inject
    private Notifications notifications;
    @Inject
    private BackgroundWorker backgroundWorker;
    @Inject
    private UiComponents uiComponents;

    // Вкладка 1: Поиск в интернете
    @Inject
    private TextField<String> searchCompanyNameField;
    @Inject
    private TextField<String> searchInnField;
    @Inject
    private Button searchWebBtn;
    @Inject
    private Button clearSearchBtn;

    // Вкладка 2: Файл
    @Inject
    private FileUploadField fileUpload;
    @Inject
    private Label<String> selectedFileNameLabel;

    // Вкладка 3: Текст
    @Inject
    private RichTextArea rawRichTextArea;

    // Вкладка 4: URL
    @Inject
    private TextField<String> urlField;

    // Прогресс
    @Inject
    private ProgressBar progressBar;
    @Inject
    private Label<String> statusLabel;

    // Секция кандидатов
    @Inject
    private VBoxLayout candidatesCard;
    @Inject
    private VBoxLayout candidatesListBox;

    // Дубликаты
    @Inject
    private VBoxLayout duplicateBox;
    @Inject
    private Label<String> duplicateInfoLabel;

    // Предпросмотр
    @Inject
    private VBoxLayout previewCard;
    @Inject
    private Label<String> previewName;
    @Inject
    private Label<String> previewLegalName;
    @Inject
    private Label<String> previewInnkpp;
    @Inject
    private Label<String> previewOgrn;
    @Inject
    private Label<String> previewOkpoOkved;
    @Inject
    private Label<String> previewOwnership;
    @Inject
    private Label<String> previewGeo;
    @Inject
    private Label<String> previewStreetAddress;
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
    private Label<String> previewDescription;
    @Inject
    private Label<String> previewWorkingConditions;

    @Inject
    private Button applyBtn;

    private List<CompanyRequisitesParsedData> foundCandidates = new ArrayList<>();
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
                    statusLabel.setValue("Файл загружен: " + fileUpload.getFileName());
                } catch (Exception ex) {
                    notifications.create(Notifications.NotificationType.ERROR)
                            .withCaption("Ошибка загрузки файла")
                            .withDescription(ex.getMessage())
                            .show();
                }
            }
        });
    }

    /**
     * Предзаполнение поисковых параметров из вызывающей формы (CompanyEdit).
     */
    public void setInitialSearchParams(String companyName, String inn) {
        if (companyName != null && !companyName.trim().isEmpty()) {
            searchCompanyNameField.setValue(companyName.trim());
        }
        if (inn != null && !inn.trim().isEmpty()) {
            searchInnField.setValue(inn.trim());
        }
    }

    @Subscribe("searchWebBtn")
    public void onSearchWebBtnClick(Button.ClickEvent event) {
        String companyName = searchCompanyNameField.getValue();
        String inn = searchInnField.getValue();

        if ((companyName == null || companyName.trim().isEmpty()) && (inn == null || inn.trim().isEmpty())) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Введите наименование или ИНН компании")
                    .show();
            return;
        }

        setLoadingState(true, "Поиск информации об организации в интернете через AI...");
        candidatesCard.setVisible(false);
        previewCard.setVisible(false);
        applyBtn.setEnabled(false);

        BackgroundTask<Integer, List<CompanyRequisitesParsedData>> searchTask =
                new BackgroundTask<Integer, List<CompanyRequisitesParsedData>>(60, this) {
                    @Override
                    public List<CompanyRequisitesParsedData> run(TaskLifeCycle<Integer> taskLifeCycle) {
                        return companySearchAiService.searchCompanyInWeb(companyName, inn);
                    }

                    @Override
                    public void done(List<CompanyRequisitesParsedData> result) {
                        setLoadingState(false, "Поиск завершен");
                        foundCandidates = result != null ? result : new ArrayList<>();
                        if (!foundCandidates.isEmpty()) {
                            renderCandidatesList(foundCandidates);
                            selectCandidate(foundCandidates.get(0));
                            statusLabel.setValue("✓ Найдено вариантов организаций: " + foundCandidates.size());
                        } else {
                            statusLabel.setValue("Организации по запросу не найдены");
                            notifications.create(Notifications.NotificationType.HUMANIZED)
                                    .withCaption("По запросу ничего не найдено")
                                    .show();
                        }
                    }

                    @Override
                    public boolean handleException(Exception ex) {
                        setLoadingState(false, "Ошибка поиска");
                        log.error("Ошибка при AI-поиске компании", ex);
                        notifications.create(Notifications.NotificationType.ERROR)
                                .withCaption("Ошибка поиска в интернете")
                                .withDescription(ex.getMessage())
                                .show();
                        return true;
                    }
                };

        backgroundWorker.handle(searchTask).execute();
    }

    @Subscribe("clearSearchBtn")
    public void onClearSearchBtnClick(Button.ClickEvent event) {
        searchCompanyNameField.clear();
        searchInnField.clear();
        statusLabel.setValue("Поля поиска очищены");
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
            setLoadingState(true, "Чтение файла и отправка в AI-модуль распознавания...");
            File file = fileUploadingAPI.getFile(fileUpload.getFileId());
            if (file != null && uploadedFileDescriptor != null) {
                fileUploadingAPI.putFileIntoStorage(fileUpload.getFileId(), uploadedFileDescriptor);
                dataManager.commit(uploadedFileDescriptor);
                String text = companyRequisitesIngestService.extractTextFromFile(uploadedFileDescriptor);
                processTextAndShowPreview(text);
            } else {
                setLoadingState(false, "Файл не выбран или не был загружен");
                notifications.create(Notifications.NotificationType.WARNING)
                        .withCaption("Файл не найден или не был загружен")
                        .show();
            }
        } catch (Exception e) {
            setLoadingState(false, "Ошибка распознавания");
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка сохранения файла в хранилище")
                    .withDescription(e.getMessage())
                    .show();
        }
    }

    @Subscribe("parseTextBtn")
    public void onParseTextBtnClick(Button.ClickEvent event) {
        String rawHtmlOrText = rawRichTextArea.getValue();
        if (rawHtmlOrText == null || rawHtmlOrText.trim().isEmpty()) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Вставьте текст реквизитов")
                    .show();
            return;
        }
        String cleanText = Jsoup.parse(rawHtmlOrText).text();
        if (cleanText.isEmpty()) {
            cleanText = rawHtmlOrText;
        }
        setLoadingState(true, "AI-распознавание введенного текста реквизитов...");
        processTextAndShowPreview(cleanText);
    }

    @Subscribe("clearTextBtn")
    public void onClearTextBtnClick(Button.ClickEvent event) {
        rawRichTextArea.clear();
        statusLabel.setValue("Текстовое поле очищено");
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
        setLoadingState(true, "Загрузка веб-страницы по ссылке и AI-парсинг...");
        try {
            String text = companyRequisitesIngestService.extractTextFromUrl(url);
            if (text.isEmpty()) {
                setLoadingState(false, "Не удалось извлечь текст");
                notifications.create(Notifications.NotificationType.WARNING)
                        .withCaption("Не удалось извлечь текст по указанной ссылке")
                        .show();
                return;
            }
            processTextAndShowPreview(text);
        } catch (Exception e) {
            setLoadingState(false, "Ошибка загрузки страницы");
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка при загрузке URL")
                    .withDescription(e.getMessage())
                    .show();
        }
    }

    @Subscribe("clearUrlBtn")
    public void onClearUrlBtnClick(Button.ClickEvent event) {
        urlField.clear();
        statusLabel.setValue("Поле ссылки очищено");
    }

    private void setLoadingState(boolean loading, String message) {
        progressBar.setVisible(loading);
        statusLabel.setValue(message);
    }

    private void processTextAndShowPreview(String text) {
        try {
            parsedData = companySearchAiService.parseCompanyData(text);
            setLoadingState(false, "Обработка завершена");

            if (parsedData != null && hasMeaningfulData(parsedData)) {
                foundCandidates = new ArrayList<>();
                foundCandidates.add(parsedData);
                candidatesCard.setVisible(false);
                selectCandidate(parsedData);
                statusLabel.setValue("✓ Реквизиты организации успешно распознаны");
            } else {
                if (parsedData != null) {
                    selectCandidate(parsedData);
                }
                applyBtn.setEnabled(false);
                statusLabel.setValue("Не удалось распознать реквизиты из предоставленного источника");
            }
        } catch (Exception e) {
            setLoadingState(false, "Ошибка при обработке реквизитов");
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка AI-распознавания")
                    .withDescription(e.getMessage())
                    .show();
        }
    }

    private void renderCandidatesList(List<CompanyRequisitesParsedData> candidates) {
        candidatesListBox.removeAll();
        if (candidates == null || candidates.isEmpty() || candidates.size() == 1) {
            candidatesCard.setVisible(false);
            return;
        }

        candidatesCard.setVisible(true);
        for (int i = 0; i < candidates.size(); i++) {
            CompanyRequisitesParsedData candidate = candidates.get(i);
            boolean isSelected = candidate == parsedData;

            HBoxLayout cardBox = uiComponents.create(HBoxLayout.class);
            cardBox.setWidth("100%");
            cardBox.setSpacing(true);
            cardBox.setAlignment(Component.Alignment.MIDDLE_LEFT);
            cardBox.setStyleName(isSelected ? "edit-card candidate-filter-bar" : "edit-card");

            VBoxLayout infoBox = uiComponents.create(VBoxLayout.class);
            infoBox.setWidth("100%");
            infoBox.setSpacing(false);

            String title = candidate.getCompanyName() != null ? candidate.getCompanyName() : "Организация #" + (i + 1);
            if (candidate.getLegalEntityName() != null && !candidate.getLegalEntityName().equals(candidate.getCompanyName())) {
                title += " (" + candidate.getLegalEntityName() + ")";
            }

            Label<String> titleLabel = uiComponents.create(Label.TYPE_STRING);
            titleLabel.setHtmlEnabled(true);
            titleLabel.setValue("<span style='font-size: 13.5px; font-weight: 700; color: #1e3a8a;'>🏢 " + escapeHtml(title) + "</span>");
            infoBox.add(titleLabel);

            StringBuilder sub = new StringBuilder();
            if (candidate.getInn() != null) sub.append("ИНН: ").append(candidate.getInn()).append("  ");
            if (candidate.getOgrn() != null) sub.append("ОГРН: ").append(candidate.getOgrn()).append("  ");
            if (candidate.getCity() != null) sub.append("📍 ").append(candidate.getCity()).append("  ");
            if (candidate.getDirectorFullName() != null && !candidate.getDirectorFullName().isEmpty()) {
                sub.append("👤 ").append(candidate.getDirectorFullName());
            }

            Label<String> subLabel = uiComponents.create(Label.TYPE_STRING);
            subLabel.setHtmlEnabled(true);
            subLabel.setValue("<span style='font-size: 11.5px; color: #475569;'>" + escapeHtml(sub.toString()) + "</span>");
            infoBox.add(subLabel);

            if (candidate.getCompanyDescription() != null && !candidate.getCompanyDescription().isEmpty()) {
                Label<String> descLabel = uiComponents.create(Label.TYPE_STRING);
                descLabel.setHtmlEnabled(true);
                String descSnippet = candidate.getCompanyDescription();
                if (descSnippet.length() > 140) descSnippet = descSnippet.substring(0, 140) + "...";
                descLabel.setValue("<span style='font-size: 11px; color: #64748b;'>ℹ️ " + escapeHtml(descSnippet) + "</span>");
                infoBox.add(descLabel);
            }

            Button selectBtn = uiComponents.create(Button.class);
            selectBtn.setCaption(isSelected ? "✓ Выбрано" : "Выбрать");
            selectBtn.setIcon(isSelected ? "font-icon:CHECK" : "font-icon:ARROW_RIGHT");
            selectBtn.setStyleName(isSelected ? "primary candidate-btn" : "secondary candidate-btn");
            selectBtn.addClickListener(e -> {
                selectCandidate(candidate);
                renderCandidatesList(foundCandidates);
            });

            cardBox.add(infoBox);
            cardBox.expand(infoBox);
            cardBox.add(selectBtn);

            candidatesListBox.add(cardBox);
        }
    }

    private void selectCandidate(CompanyRequisitesParsedData candidate) {
        this.parsedData = candidate;
        updatePreview(candidate);
        checkDuplicates(candidate);
        previewCard.setVisible(true);
        applyBtn.setEnabled(hasMeaningfulData(candidate));
    }

    private void checkDuplicates(CompanyRequisitesParsedData data) {
        if (data == null || data.getInn() == null || data.getInn().trim().isEmpty()) {
            duplicateBox.setVisible(false);
            return;
        }

        String inn = data.getInn().trim();
        List<Company> existing = dataManager.load(Company.class)
                .query("select c from hunttech_Company c where c.inn = :inn")
                .parameter("inn", inn)
                .view("company-browse-view")
                .list();

        if (!existing.isEmpty()) {
            Company comp = existing.get(0);
            String compName = comp.getComanyName() != null ? comp.getComanyName() : "Без названия";
            String compShort = comp.getCompanyShortName() != null ? " (" + comp.getCompanyShortName() + ")" : "";
            duplicateInfoLabel.setValue("Организация <b>" + escapeHtml(compName + compShort) + "</b> с ИНН <b>" +
                    escapeHtml(inn) + "</b> уже присутствует в базе. При нажатии «Применить к карточке компании» данные карточки будут обновлены.");
            duplicateBox.setVisible(true);
        } else {
            duplicateBox.setVisible(false);
        }
    }

    private boolean hasMeaningfulData(CompanyRequisitesParsedData data) {
        if (data == null) return false;
        return isNotBlank(data.getInn()) ||
                isNotBlank(data.getCompanyName()) ||
                isNotBlank(data.getLegalEntityName()) ||
                isNotBlank(data.getOgrn()) ||
                isNotBlank(data.getBik()) ||
                isNotBlank(data.getLegalAddress()) ||
                isNotBlank(data.getDirectorFullName()) ||
                isNotBlank(data.getCompanyDescription());
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
        String name = data.getCompanyName() != null ? data.getCompanyName() : "";
        if (data.getCompanyShortName() != null && !data.getCompanyShortName().isEmpty()
                && !data.getCompanyShortName().equals(name)) {
            name += " (" + data.getCompanyShortName() + ")";
        }
        previewName.setValue(!name.isEmpty() ? name : "—");

        previewLegalName.setValue(data.getLegalEntityName() != null ? data.getLegalEntityName() : "—");

        String innkpp = (data.getInn() != null ? "ИНН: " + data.getInn() : "") +
                (data.getKpp() != null && !data.getKpp().isEmpty() ? " / КПП: " + data.getKpp() : "");
        previewInnkpp.setValue(!innkpp.isEmpty() ? innkpp : "—");

        previewOgrn.setValue(data.getOgrn() != null ? data.getOgrn() : "—");

        String okpoOkved = (data.getOkpo() != null ? "ОКПО: " + data.getOkpo() : "") +
                (data.getOkved() != null && !data.getOkved().isEmpty() ? " / ОКВЭД: " + data.getOkved() : "");
        previewOkpoOkved.setValue(!okpoOkved.isEmpty() ? okpoOkved : "—");

        previewOwnership.setValue(data.getOwnership() != null ? data.getOwnership() : "—");

        StringBuilder geo = new StringBuilder();
        if (isNotBlank(data.getCountry())) geo.append(data.getCountry());
        if (isNotBlank(data.getRegion())) {
            if (geo.length() > 0) geo.append(" / ");
            geo.append(data.getRegion());
        }
        if (isNotBlank(data.getCity())) {
            if (geo.length() > 0) geo.append(" / ");
            geo.append(data.getCity());
        }
        previewGeo.setValue(geo.length() > 0 ? geo.toString() : "—");

        previewStreetAddress.setValue(data.getStreetAddress() != null ? data.getStreetAddress() : "—");
        previewLegalAddress.setValue(data.getLegalAddress() != null ? data.getLegalAddress() : "—");
        previewActualAddress.setValue(data.getActualAddress() != null ? data.getActualAddress() : "—");

        String bankBik = (data.getBankName() != null ? data.getBankName() : "") +
                (data.getBik() != null && !data.getBik().isEmpty() ? " (БИК: " + data.getBik() + ")" : "");
        previewBankBik.setValue(!bankBik.isEmpty() ? bankBik : "—");

        String accounts = (data.getSettlementAccount() != null ? "Р/с: " + data.getSettlementAccount() : "") +
                (data.getCorrespondentAccount() != null && !data.getCorrespondentAccount().isEmpty() ? " / К/с: " + data.getCorrespondentAccount() : "");
        previewAccounts.setValue(!accounts.isEmpty() ? accounts : "—");

        String contacts = (data.getPhone() != null ? "📞 " + data.getPhone() : "") +
                (data.getEmail() != null ? " ✉ " + data.getEmail() : "") +
                (data.getWebsite() != null ? " 🌐 " + data.getWebsite() : "");
        previewContacts.setValue(!contacts.isEmpty() ? contacts : "—");

        String directorName = data.getDirectorFullName();
        if (!directorName.isEmpty()) {
            String safeDirectorName = escapeHtml(directorName);
            String lastName = data.getDirectorLastName() != null ? data.getDirectorLastName().trim().toLowerCase() : "";
            String firstName = data.getDirectorFirstName() != null ? data.getDirectorFirstName().trim().toLowerCase() : "";
            List<Person> matches = dataManager.load(Person.class)
                    .query("select p from hunttech_Person p where lower(p.secondName) = :lastName and lower(p.firstName) = :firstName")
                    .parameter("lastName", lastName)
                    .parameter("firstName", firstName)
                    .view("person-picker-view")
                    .list();

            if (matches != null && !matches.isEmpty()) {
                previewDirector.setValue("👤 <b>" + safeDirectorName + "</b> <span style='background: #dcfce7; color: #15803d; padding: 2px 6px; border-radius: 4px; font-size: 11px; font-weight: 600;'>Найден в базе</span>");
            } else {
                previewDirector.setValue("👤 <b>" + safeDirectorName + "</b> <span style='background: #eff6ff; color: #2563eb; padding: 2px 6px; border-radius: 4px; font-size: 11px; font-weight: 600;'>Будет создан новый</span>");
            }
        } else {
            previewDirector.setValue("—");
        }

        previewDescription.setValue(isNotBlank(data.getCompanyDescription()) ? data.getCompanyDescription() : "—");
        previewWorkingConditions.setValue(isNotBlank(data.getWorkingConditions()) ? data.getWorkingConditions() : "—");
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
