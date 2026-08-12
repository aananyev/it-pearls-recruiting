package com.company.hunttech.web.screens.project;

import com.company.hunttech.UiNotificationEvent;
import com.company.hunttech.entity.CompanyDepartament;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.Person;
import com.company.hunttech.entity.Project;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Events;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.ViewBuilder;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UiController("hunttech_Project.edit")
@UiDescriptor("project-edit.xml")
@EditedEntityContainer("projectDc")
@LoadDataBeforeShow
public class ProjectEdit extends StandardEditor<Project> {
    @Inject
    private Image projectDefaultLogoFileImage;
    @Inject
    private Image projectLogoFileImage;
    @Inject
    private FileUploadField projectLogoFileUpload;

    @Subscribe("projectLogoFileUpload")
    public void onProjectLogoFileUploadBeforeValueClear(FileUploadField.BeforeValueClearEvent event) {
        setProjectPicImage();

    }

    @Subscribe
    public void onBeforeShow1(BeforeShowEvent event) {
        setProjectPicImage();
    }


    private void setProjectPicImage() {
        if (getEditedEntity().getProjectLogo() == null) {
            projectDefaultLogoFileImage.setVisible(true);
            projectLogoFileImage.setVisible(false);
        } else {
            projectDefaultLogoFileImage.setVisible(false);
            projectLogoFileImage.setVisible(true);
        }
    }

    @Subscribe("projectLogoFileUpload")
    public void onProjectLogoFileUploadFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {
        try {
            projectLogoFileImage.setVisible(true);
            projectDefaultLogoFileImage.setVisible(false);

            FileDescriptorResource fileDescriptorResource =
                    projectLogoFileImage.createResource(FileDescriptorResource.class)
                            .setFileDescriptor(
                                    projectLogoFileUpload.getFileDescriptor());

            projectLogoFileImage.setSource(fileDescriptorResource);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
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