package com.company.hunttech.web.screens.person;

import com.company.hunttech.entity.City;
import com.company.hunttech.entity.Person;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.FileDescriptorResource;
import com.haulmont.cuba.gui.components.FileUploadField;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.screen.*;
import com.hunttech.hrm.gui.components.OvaFallbackImage;

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
    private LookupPickerField<City> positionCityField;
    @Inject
    private Button personMainNav;
    @Inject
    private Button personContactsNav;
    @Inject
    private Button personLocationNav;

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
