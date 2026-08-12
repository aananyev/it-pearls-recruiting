package com.company.hunttech.web.screens.project;

import com.company.hunttech.UiNotificationEvent;
import com.company.hunttech.ai.ProjectDescriptionTextExtractor;
import com.company.hunttech.entity.CompanyDepartament;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.Person;
import com.company.hunttech.entity.Project;
import com.company.hunttech.service.ProjectAiService;
import com.haulmont.cuba.core.app.FileStorageService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Events;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.ViewBuilder;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundTaskHandler;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.hunttech.hrm.web.components.WebOvaFallbackImage;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.screen.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@UiController("hunttech_Project.edit")
@UiDescriptor("project-edit.xml")
@EditedEntityContainer("projectDc")
@LoadDataBeforeShow
public class ProjectEdit extends StandardEditor<Project> {
    private static final Logger log = LoggerFactory.getLogger(ProjectEdit.class);
    private static final long PROJECT_DESCRIPTION_UPLOAD_LIMIT = 10L * 1024L * 1024L;

    @Inject
    private WebOvaFallbackImage projectLogoFileImage;
    @Inject
    private FileUploadField projectLogoFileUpload;

    // OvaFallbackImage сам читает projectLogo из projectDc и показывает fallback
    // (icons/no-company.png) при отсутствии файла; загрузка/очистка через upload
    // (fileStoragePutMode=IMMEDIATE, property=projectLogo) обновляет контейнер —
    // ручное переключение видимости/источника не требуется.
    @Subscribe
    public void onBeforeShow1(BeforeShowEvent event) {
        // логотип отображается компонентом автоматически
    }


    @Inject
    private DateField<Date> startProjectDateField;
    @Inject
    private CheckBox checkBoxProjectIsClosed;
    @Inject
    private TextField<String> projectNameField;
    @Inject
    private DateField<Date> endProjectDateField;
    @Inject
    private LookupPickerField<CompanyDepartament> projectDepartmentField;
    @Inject
    private LookupPickerField<Person> projectOwnerField;
    @Inject
    private Dialogs dialogs;
    @Inject
    private DataManager dataManager;

    private Project beforeEdit = null;

    List<OpenPosition> openPositions = new ArrayList<>();
    @Inject
    private CollectionLoader<OpenPosition> projectOpenPositionsDl;
    @Inject
    private DataContext dataContext;
    @Inject
    private Events events;
    @Inject
    private Messages messages;
    @Inject
    private TextField<String> generalChatTextField;
    @Inject
    private TextField<String> chatForCVTextField;
    @Inject
    private Link generalChatLink;
    @Inject
    private Link chatForCVLink;
    @Inject
    private TabSheet projectTab;
    @Inject
    private GroupBoxLayout projectDescriptionCard;
    @Inject
    private RichTextArea projectDescriptionRichTextArea;
    @Inject
    private FileLoader fileLoader;
    @Inject
    private FileStorageService fileStorageService;
    @Inject
    private ProjectAiService projectAiService;
    @Inject
    private BackgroundWorker backgroundWorker;
    @Inject
    private Notifications notifications;
    @Inject
    private UiComponents uiComponents;

    private FileUploadField projectDescriptionUpload;
    private Label<String> projectDescriptionAiStatus;

    // Presentation-контракт sidebar: пункты label-навигации «Разделы» = вкладки TabSheet
    // правой части экрана (по явному указанию владельца; навигация видна на всех
    // вкладках, правило контракта §3.6 не применяется). Активное состояние управляется
    // presentation-only методами ниже.
    @Inject
    private Label projectSidebarTitle;
    @Inject
    private Button projectEditorNavMain;
    @Inject
    private Button projectEditorNavDescription;
    @Inject
    private Button projectEditorNavVacancy;
    @Inject
    private Button projectEditorNavTemplate;

    /** Соответствие «имя вкладки TabSheet → пункт label-навигации». */
    private static final Map<String, String> TAB_TO_NAV_BUTTON =
            Collections.unmodifiableMap(new HashMap<String, String>() {{
                put("tabProject", "projectEditorNavMain");
                put("tabProjectDescription", "projectEditorNavDescription");
                put("tabVacansy", "projectEditorNavVacancy");
                put("tabTemplateLetter", "projectEditorNavTemplate");
            }});

    private boolean projectDescriptionLoaded;
    private boolean templateLetterLoaded;
    private boolean openPositionLoaded;
    private boolean openPositionLoaderReady;

    @Subscribe
    public void onInit(InitEvent event) {
        // Блокирует автозагрузку вакансий до установки обязательного параметра проекта.
        projectOpenPositionsDl.addPreLoadListener(loadEvent -> {
            if (!openPositionLoaderReady) {
                loadEvent.preventLoad();
            }
        });
        initProjectDescriptionUpload();
    }

    /**
     * Добавляет upload в существующую карточку описания без перестройки XML-контракта
     * ProjectEdit. Файл — только транспорт: после извлечения текста он удаляется.
     */
    private void initProjectDescriptionUpload() {
        HBoxLayout uploadRow = uiComponents.create(HBoxLayout.class);
        uploadRow.setId("projectDescriptionUploadRow");
        uploadRow.setWidthFull();
        uploadRow.setSpacing(true);
        uploadRow.setAlignment(Component.Alignment.MIDDLE_LEFT);

        projectDescriptionUpload = uiComponents.create(FileUploadField.class);
        projectDescriptionUpload.setId("projectDescriptionUpload");
        projectDescriptionUpload.setMode(FileUploadField.FileStoragePutMode.IMMEDIATE);
        projectDescriptionUpload.setUploadButtonCaption(
                messages.getMessage(getClass(), "msgProjectDescriptionUpload"));
        projectDescriptionUpload.setAccept(".pdf,.docx,.txt");
        projectDescriptionUpload.setPermittedExtensions(new LinkedHashSet<>(
                Arrays.asList(".pdf", ".docx", ".txt")));
        projectDescriptionUpload.setFileSizeLimit(PROJECT_DESCRIPTION_UPLOAD_LIMIT);
        projectDescriptionUpload.setShowFileName(true);
        projectDescriptionUpload.setShowClearButton(false);
        projectDescriptionUpload.setWidth("220px");
        projectDescriptionUpload.setHeight("36px");
        projectDescriptionUpload.addFileUploadSucceedListener(
                event -> onProjectDescriptionUploadSucceeded());
        projectDescriptionUpload.addFileUploadErrorListener(
                event -> showProjectDescriptionUploadError());

        projectDescriptionAiStatus = uiComponents.create(Label.TYPE_STRING);
        projectDescriptionAiStatus.setId("projectDescriptionAiStatus");
        projectDescriptionAiStatus.setWidthFull();
        projectDescriptionAiStatus.setValue(
                messages.getMessage(getClass(), "msgProjectDescriptionUploadHint"));
        projectDescriptionAiStatus.setStyleName("edit-toolbar-description");

        uploadRow.add(projectDescriptionUpload);
        uploadRow.add(projectDescriptionAiStatus);
        uploadRow.expand(projectDescriptionAiStatus);
        projectDescriptionCard.add(uploadRow, 0);
        projectDescriptionCard.expand(projectDescriptionRichTextArea);
    }

    @Subscribe("projectTab")
    public void onProjectTabSelectedTabChange(TabSheet.SelectedTabChangeEvent event) {
        if (event.getSelectedTab() == null || PersistenceHelper.isNew(getEditedEntity())) {
            return;
        }
        String tabName = event.getSelectedTab().getName();
        if ("tabProjectDescription".equals(tabName) && !projectDescriptionLoaded) {
            loadProjectDescription();
            projectDescriptionLoaded = true;
        }
        if ("tabTemplateLetter".equals(tabName) && !templateLetterLoaded) {
            loadTemplateLetter();
            templateLetterLoaded = true;
        }
        if ("tabVacansy".equals(tabName) && !openPositionLoaded) {
            loadOpenPositions();
            openPositionLoaded = true;
        }
    }

    private void loadProjectDescription() {
        Project reloaded = dataManager.reload(getEditedEntity(), ViewBuilder.of(Project.class)
                .add("projectDescription")
                .build());
        getEditedEntity().setProjectDescription(reloaded.getProjectDescription());
    }

    private void loadTemplateLetter() {
        Project reloaded = dataManager.reload(getEditedEntity(), ViewBuilder.of(Project.class)
                .add("templateLetter")
                .build());
        getEditedEntity().setTemplateLetter(reloaded.getTemplateLetter());
    }

    private void loadOpenPositions() {
        // Сначала задаёт параметр JPQL, затем разрешает единственную отложенную загрузку вкладки.
        projectOpenPositionsDl.setParameter("project", getEditedEntity());
        openPositionLoaderReady = true;
        projectOpenPositionsDl.load();
    }

    /**
     * После upload сначала сохраняем безопасный raw fallback в projectDescription,
     * затем запускаем AI в BackgroundWorker. Если AI недоступен, пользователь не
     * теряет извлечённый текст и может продолжить редактирование вручную.
     */
    private void onProjectDescriptionUploadSucceeded() {
        FileDescriptor uploaded = getUploadedProjectDescriptionDescriptor();
        if (uploaded == null) {
            showProjectDescriptionUploadError();
            return;
        }

        String sourceFileName = uploaded.getName();
        String sourceText;
        try (InputStream stream = fileLoader.openStream(uploaded)) {
            sourceText = ProjectDescriptionTextExtractor.extract(stream, resolveExtension(uploaded));
        } catch (Exception e) {
            log.warn("Не удалось извлечь текст описания проекта из файла {}: {}",
                    sourceFileName, e.getClass().getSimpleName());
            showProjectDescriptionUploadError();
            return;
        } finally {
            cleanupUploadedProjectDescription(uploaded);
            projectDescriptionUpload.setValue(null);
        }

        projectDescriptionRichTextArea.setValue(toSafeRichText(sourceText));
        projectDescriptionLoaded = true;
        runProjectDescriptionAi(sourceText, sourceFileName);
    }

    private FileDescriptor getUploadedProjectDescriptionDescriptor() {
        FileDescriptor descriptor = projectDescriptionUpload.getFileDescriptor();
        if (descriptor == null) {
            Object value = projectDescriptionUpload.getValue();
            if (value instanceof FileDescriptor) {
                descriptor = (FileDescriptor) value;
            }
        }
        return descriptor;
    }

    private String resolveExtension(FileDescriptor descriptor) {
        if (descriptor.getExtension() != null && !descriptor.getExtension().trim().isEmpty()) {
            return descriptor.getExtension().trim().toLowerCase(Locale.ROOT);
        }
        String name = descriptor.getName();
        int dot = name == null ? -1 : name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private void runProjectDescriptionAi(String sourceText, String sourceFileName) {
        /*
         * В background уходит только middleware-вызов: screen не выбирает provider,
         * model или credential и не содержит prompt. Вся маршрутизация принадлежит
         * PROJECT_DESCRIPTION_GENERATE в AI Control Plane.
         */
        String projectName = getEditedEntity().getProjectName();
        projectDescriptionUpload.setEnabled(false);
        projectDescriptionAiStatus.setValue(
                messages.getMessage(getClass(), "msgProjectDescriptionAiProcessing"));
        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption(messages.getMessage(getClass(), "msgProjectDescriptionAiStarted"))
                .show();

        BackgroundTask<Integer, String> task = new BackgroundTask<Integer, String>(120, this) {
            @Override
            public String run(TaskLifeCycle<Integer> taskLifeCycle) {
                return projectAiService.processUploadedDescription(
                        projectName, sourceFileName, sourceText);
            }

            @Override
            public void done(String processedText) {
                projectDescriptionUpload.setEnabled(true);
                projectDescriptionRichTextArea.setValue(toSafeRichText(processedText));
                projectDescriptionAiStatus.setValue(
                        messages.getMessage(ProjectEdit.class, "msgProjectDescriptionAiDone"));
                notifications.create(Notifications.NotificationType.TRAY)
                        .withCaption(messages.getMessage(ProjectEdit.class, "msgProjectDescriptionAiDone"))
                        .show();
            }

            @Override
            public boolean handleException(Exception exception) {
                // Сообщение внешнего provider не показываем: raw text остаётся в форме,
                // а UI сообщает только контролируемый результат fallback.
                log.warn("AI-обработка описания проекта не выполнена: {}",
                        exception.getClass().getSimpleName());
                projectDescriptionUpload.setEnabled(true);
                projectDescriptionAiStatus.setValue(
                        messages.getMessage(ProjectEdit.class, "msgProjectDescriptionAiFallback"));
                notifications.create(Notifications.NotificationType.WARNING)
                        .withCaption(messages.getMessage(ProjectEdit.class, "msgProjectDescriptionAiFailed"))
                        .show();
                return true;
            }
        };

        BackgroundTaskHandler taskHandler = backgroundWorker.handle(task);
        taskHandler.execute();
    }

    private void cleanupUploadedProjectDescription(FileDescriptor descriptor) {
        // Upload-файл — только транспорт для извлечения текста; в Project он не хранится.
        try {
            fileStorageService.removeFile(descriptor);
        } catch (Exception e) {
            log.warn("Не удалось удалить временный файл описания проекта {} из FileStorage: {}",
                    descriptor.getId(), e.getClass().getSimpleName());
        }
        try {
            dataManager.remove(descriptor);
        } catch (RuntimeException e) {
            // IMMEDIATE FileDescriptor мог уже отсутствовать после очистки FileStorage.
            log.debug("Временный FileDescriptor {} уже отсутствует: {}",
                    descriptor.getId(), e.getClass().getSimpleName());
        }
    }

    private void showProjectDescriptionUploadError() {
        if (projectDescriptionAiStatus != null) {
            projectDescriptionAiStatus.setValue(
                    messages.getMessage(getClass(), "msgProjectDescriptionAiFallback"));
        }
        notifications.create(Notifications.NotificationType.WARNING)
                .withCaption(messages.getMessage(getClass(), "msgProjectDescriptionUploadFailed"))
                .show();
    }

    /** RichTextArea получает только escaped HTML: LLM/файл не могут внедрить markup/script. */
    private String toSafeRichText(String value) {
        String text = value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n');
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("\n", "<br/>");
    }

    @Subscribe("checkBoxProjectIsClosed")
    public void onCheckBoxProjectIsClosedValueChange1(HasValue.ValueChangeEvent<Boolean> event) {
        String opeedPositionList = "";

        for (OpenPosition a : openPositions) {
            opeedPositionList = a.getVacansyName() + "<br>" + opeedPositionList;
        }

        if (!opeedPositionList.equals("")) {
            if (checkBoxProjectIsClosed.getValue()) {
                dialogs.createOptionDialog()
                        .withCaption("ВНИМАНИЕ")
                        .withType(Dialogs.MessageType.WARNING)
                        .withContentMode(ContentMode.HTML)
                        .withMessage("<b>Закрыть вакансии на этом проекте?</b><br>Открыты позиции: <br><i>" +
                                opeedPositionList + "</i>")
                        .withActions(
                                new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                                    closeAllVacansies();
                                }),
                                new DialogAction((DialogAction.Type.NO)
                                ))
                        .show();
            }
        }

        setEndDateProject();
    }

    private void setEndDateProject() {
        Date date = new Date();

        if (checkBoxProjectIsClosed.getValue())
            endProjectDateField.setValue(date);
        else
            endProjectDateField.setValue(null);
    }

    private void closeAllVacansies() {
        for (OpenPosition a : openPositions) {
            a.setOpenClose(true);

            CommitContext commitContext = new CommitContext(a);
            dataManager.commit(commitContext);

            events.publish(new UiNotificationEvent(this, "Закрыта вакансия: " +
                    a.getVacansyName()));
        }
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        beforeEdit = getEditedEntity();

        if (PersistenceHelper.isNew(getEditedEntity())) {
            Date date = new Date();

            startProjectDateField.setValue(date);
        }

        setStartDateOfProject();
        getOpenedPosition();
        setButtonsForChats();
    }

    @Subscribe("generalChatTextField")
    public void onGeneralChatTextFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        String general = generalChatTextField.getValue();
        String chat = chatForCVTextField.getValue();

        if (generalChatTextField.getValue() == null) {
            generalChatLink.setEnabled(false);
            generalChatLink.setUrl("");
        } else {
            generalChatLink.setEnabled(true);
            generalChatLink.setUrl(generalChatTextField.getValue());

        }
    }

    @Subscribe("chatForCVTextField")
    public void onChatForCVTextFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        String general = generalChatTextField.getValue();
        String chat = chatForCVTextField.getValue();

        if (chatForCVTextField.getValue() == null) {
            chatForCVLink.setEnabled(false);
            chatForCVLink.setUrl("");

        } else {
            chatForCVLink.setEnabled(true);
            chatForCVLink.setUrl(chatForCVTextField.getValue());
        }
    }

    private void setButtonsForChats() {
        if (generalChatTextField.getValue() == null) {
            generalChatLink.setEnabled(false);
            generalChatLink.setUrl("");
        } else {
            generalChatLink.setEnabled(true);
            generalChatLink.setUrl(generalChatTextField.getValue());
        }

        if (chatForCVTextField.getValue() == null) {
            chatForCVLink.setEnabled(false);
            chatForCVLink.setUrl("");
        } else {
            chatForCVLink.setEnabled(true);
            chatForCVLink.setUrl(chatForCVTextField.getValue());
        }
    }

    private void setStartDateOfProject() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            startProjectDateField.setValue(new Date());
        }
    }

    private void getOpenedPosition() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            openPositions = new ArrayList<>();
            return;
        }
        String positionsQuery = "select e from hunttech_OpenPosition e " +
                "where e.projectName = :projectName and " +
                "e.openClose = false";
        openPositions = dataManager.load(OpenPosition.class)
                .query(positionsQuery)
                .parameter("projectName", getEditedEntity())
                .list();
    }

    @Subscribe("checkBoxProjectIsClosed")
    public void onCheckBoxProjectIsClosedValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (checkBoxProjectIsClosed.getValue()) {
            projectNameField.setEditable(false);
            startProjectDateField.setEditable(false);
            endProjectDateField.setEditable(false);
            projectDepartmentField.setEditable(false);
            projectOwnerField.setEditable(false);
        } else {
            projectNameField.setEditable(true);
            startProjectDateField.setEditable(true);
            endProjectDateField.setEditable(true);
            projectDepartmentField.setEditable(true);
            projectOwnerField.setEditable(true);
        }
    }

    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        sendGlobalEventsMessage(event);
    }

    private void sendGlobalEventsMessage(BeforeCommitChangesEvent event) {
        if(getEditedEntity().getProjectIsClosed() == null) {
            getEditedEntity().setProjectIsClosed(false);
        }

        if(PersistenceHelper.isNew(getEditedEntity())) {
            if(getEditedEntity().getProjectIsClosed()) {
                sendCloseProjectMessage();
            } else {
                sendOpenProjectMessage();
            }
        } else {
            if(getEditedEntity().getProjectIsClosed()) {
                if(!beforeEdit.getProjectIsClosed().equals(getEditedEntity().getProjectIsClosed())) {
                    sendCloseProjectMessage();
                }
            } else {
                if(!beforeEdit.getProjectIsClosed().equals(getEditedEntity().getProjectIsClosed())) {
                    sendOpenProjectMessage();
                }
            }
        }
    }


    private void sendCloseProjectMessage() {
        events.publish(new UiNotificationEvent(this, "Закрыт проект: " +
                getEditedEntity().getProjectName()));
    }

    private void sendOpenProjectMessage() {
        events.publish(new UiNotificationEvent(this, "Открыт новый проект: " +
                getEditedEntity().getProjectName()));
    }

    public void gotoChatForCV() {
    }

    public void gotoGeneralChat() {
    }

    // ===== Presentation-only: sidebar-навигация «Разделы» (контракт Edit-форм) =====

    @Subscribe
    public void onBeforeShowSidebar(BeforeShowEvent event) {
        // Динамический title sidebar: наименование проекта, иначе общий заголовок формы.
        if (getEditedEntity().getProjectName() != null) {
            projectSidebarTitle.setValue(getEditedEntity().getProjectName());
        } else {
            projectSidebarTitle.setValue(messages.getMessage(getClass(), "browseCaption"));
        }
    }

    @Subscribe("projectEditorNavMain")
    public void onProjectEditorNavMainClick(Button.ClickEvent event) {
        setNavigationActive(projectEditorNavMain);
        projectTab.setSelectedTab("tabProject");
    }

    @Subscribe("projectEditorNavDescription")
    public void onProjectEditorNavDescriptionClick(Button.ClickEvent event) {
        setNavigationActive(projectEditorNavDescription);
        projectTab.setSelectedTab("tabProjectDescription");
    }

    @Subscribe("projectEditorNavVacancy")
    public void onProjectEditorNavVacancyClick(Button.ClickEvent event) {
        setNavigationActive(projectEditorNavVacancy);
        projectTab.setSelectedTab("tabVacansy");
    }

    @Subscribe("projectEditorNavTemplate")
    public void onProjectEditorNavTemplateClick(Button.ClickEvent event) {
        setNavigationActive(projectEditorNavTemplate);
        projectTab.setSelectedTab("tabTemplateLetter");
    }

    @Subscribe("projectTab")
    public void onProjectTabSelectedTabChangeNav(TabSheet.SelectedTabChangeEvent event) {
        // Отдельный обработчик смены вкладки: синхронизирует активный пункт
        // sidebar-навигации; бизнес-логика ленивой загрузки — в основном методе.
        updateActiveNavigation(event.getSelectedTab());
    }

    private void updateActiveNavigation(TabSheet.Tab selectedTab) {
        if (selectedTab == null) {
            return;
        }
        String navButtonId = TAB_TO_NAV_BUTTON.get(selectedTab.getName());
        if (navButtonId == null) {
            return;
        }
        switch (navButtonId) {
            case "projectEditorNavMain":
                setNavigationActive(projectEditorNavMain);
                break;
            case "projectEditorNavDescription":
                setNavigationActive(projectEditorNavDescription);
                break;
            case "projectEditorNavVacancy":
                setNavigationActive(projectEditorNavVacancy);
                break;
            case "projectEditorNavTemplate":
                setNavigationActive(projectEditorNavTemplate);
                break;
            default:
                break;
        }
    }

    private void resetNavigationActiveStyles() {
        projectEditorNavMain.removeStyleName("label-nav-item-active");
        projectEditorNavDescription.removeStyleName("label-nav-item-active");
        projectEditorNavVacancy.removeStyleName("label-nav-item-active");
        projectEditorNavTemplate.removeStyleName("label-nav-item-active");
    }

    private void setNavigationActive(Button activeButton) {
        resetNavigationActiveStyles();
        activeButton.addStyleName("label-nav-item-active");
    }
}
