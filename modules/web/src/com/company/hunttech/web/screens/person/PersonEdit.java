package com.company.hunttech.web.screens.person;

import com.company.hunttech.entity.City;
import com.company.hunttech.entity.Person;
import com.company.hunttech.service.TelegramIntegrationService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.FileDescriptorResource;
import com.haulmont.cuba.gui.components.FileUploadField;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.screen.*;
import com.hunttech.hrm.gui.components.OvaFallbackImage;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;

@UiController("hunttech_Person.edit")
@UiDescriptor("person-edit.xml")
@EditedEntityContainer("personDc")
@LoadDataBeforeShow
public class PersonEdit extends StandardEditor<Person> {

    @Inject
    private FileUploadField fileImageFaceUpload;
    @Inject
    private OvaFallbackImage personPic;
    @Inject
    private TextField<String> firstNameField;
    @Inject
    private TextField<String> emailField;
    @Inject
    private TextField<String> telegramNameField;
    @Inject
    private Button loadTelegramPhotoButton;
    @Inject
    private LookupPickerField<City> positionCityField;
    @Inject
    private Button personMainNav;
    @Inject
    private Button personContactsNav;
    @Inject
    private Button personLocationNav;
    @Inject
    private TelegramIntegrationService telegramIntegrationService;
    @Inject
    private Notifications notifications;
    @Inject
    private Messages messages;
    @Inject
    private DataContext dataContext;

    @Subscribe("fileImageFaceUpload")
    public void onFileImageFaceUploadFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {
        try {
            personPic.setSource(personPic.createResource(FileDescriptorResource.class)
                    .setFileDescriptor(fileImageFaceUpload.getFileDescriptor()));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        // Если фото не задано — показать fallback-аватар OvaFallbackImage
        // (как эталон SkillTreeEdit/JobCandidateEdit: applyFallback при отсутствии файла).
        if (getEditedEntity().getFileImageFace() == null) {
            personPic.applyFallback();
        }
        updateLoadTelegramButtonState();
        telegramNameField.addValueChangeListener(e -> updateLoadTelegramButtonState());
    }

    @Subscribe("loadTelegramPhotoButton")
    public void onLoadTelegramPhotoButtonClick(Button.ClickEvent event) {
        String telegramAccount = telegramNameField.getValue();
        if (StringUtils.isBlank(telegramAccount)) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption(messages.getMessage(getClass(), "msgTelegramNameRequired"))
                    .show();
            return;
        }

        try {
            FileDescriptor fd = telegramIntegrationService.saveUserProfilePhotoToFileStorage(telegramAccount, null);
            if (fd != null) {
                FileDescriptor mergedFd = dataContext.merge(fd);
                getEditedEntity().setFileImageFace(mergedFd);
                personPic.setSource(personPic.createResource(FileDescriptorResource.class)
                        .setFileDescriptor(mergedFd));
                notifications.create(Notifications.NotificationType.TRAY)
                        .withCaption(messages.getMessage(getClass(), "msgTelegramPhotoSuccess"))
                        .show();
            } else {
                notifications.create(Notifications.NotificationType.WARNING)
                        .withCaption(messages.getMessage(getClass(), "msgTelegramPhotoNotFound"))
                        .show();
            }
        } catch (Exception e) {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption(String.format(messages.getMessage(getClass(), "msgTelegramPhotoError"), e.getMessage()))
                    .show();
        }
    }

    private void updateLoadTelegramButtonState() {
        if (loadTelegramPhotoButton != null && telegramNameField != null) {
            loadTelegramPhotoButton.setEnabled(StringUtils.isNotBlank(telegramNameField.getValue()));
        }
    }

    /**
     * Презентационная навигация: переводит фокус к имени
     * и подсвечивает активный пункт sidebar. Entity, loaders и lifecycle не затрагиваются.
     */
    public void focusMainSection() {
        firstNameField.focus();
        setActiveNavigation(personMainNav, personContactsNav, personLocationNav);
    }

    /**
     * Презентационная навигация: переводит фокус к email
     * и подсвечивает активный пункт sidebar. Entity, loaders и lifecycle не затрагиваются.
     */
    public void focusContactsSection() {
        emailField.focus();
        setActiveNavigation(personContactsNav, personMainNav, personLocationNav);
    }

    /**
     * Презентационная навигация: переводит фокус к городу проживания
     * и подсвечивает активный пункт sidebar. Entity, loaders и lifecycle не затрагиваются.
     */
    public void focusLocationSection() {
        positionCityField.focus();
        setActiveNavigation(personLocationNav, personMainNav, personContactsNav);
    }

    private void setActiveNavigation(Button activeButton, Button... inactiveButtons) {
        activeButton.addStyleName("label-nav-item-active");
        for (Button inactiveButton : inactiveButtons) {
            inactiveButton.removeStyleName("label-nav-item-active");
        }
    }
}
