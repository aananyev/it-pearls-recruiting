package com.company.itpearls.web.screens.project;

import com.company.itpearls.UiNotificationEvent;
import com.company.itpearls.entity.CompanyDepartament;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.Person;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Events;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Project;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@UiController("itpearls_Project.edit")
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
    //    @Inject
//    private CollectionLoader<OpenPosition> projectOpenPositionsDl;
    @Inject
    private DataContext dataContext;
    @Inject
    private Events events;
    @Inject
    private TextField<String> generalChatTextField;
    @Inject
    private TextField<String> chatForCVTextField;
    @Inject
    private Link generalChatLink;
    @Inject
    private Link chatForCVLink;

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

        filterOpenPositionOnProject();
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

    private void filterOpenPositionOnProject() {
        if (!PersistenceHelper.isNew(getEditedEntity())) {
//            projectOpenPositionsDl.setParameter("project", getEditedEntity());
        } else {
//            projectOpenPositionsDl.removeParameter("project");
        }

//        projectOpenPositionsDl.load();
    }

    private void setStartDateOfProject() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            startProjectDateField.setValue(new Date());
        }
    }

    private void getOpenedPosition() {
        String positionsQuery = "select e from itpearls_OpenPosition e " +
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
}