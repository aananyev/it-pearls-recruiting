package com.company.itpearls.web.screens.somefiles;

import com.company.itpearls.entity.CandidateCV;
import com.company.itpearls.entity.FileType;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.components.PickerField;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SomeFiles;

import javax.inject.Inject;

@UiController("itpearls_SomeFiles.edit")
@UiDescriptor("some-files-edit.xml")
@EditedEntityContainer("someFilesDc")
@LoadDataBeforeShow
public class SomeFilesEdit extends StandardEditor<SomeFiles> {
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private TextField<String> fileDescriptionField;
    @Inject
    private LookupPickerField<FileType> fileTypeField;
    @Inject
    private LookupPickerField<CandidateCV> candidateCVField;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if( PersistenceHelper.isNew( getEditedEntity() ) ) {
            getEditedEntity().setFileOwner( userSessionSource.getUserSession().getUser() );
        }
    }

    @Subscribe("fileTypeField")
    public void onFileTypeFieldValueChange(HasValue.ValueChangeEvent<FileType> event) {
        if(fileDescriptionField.getValue() == null) {
            fileDescriptionField.setValue(fileTypeField.getValue().getNameFileType());
        } else {
            if (candidateCVField.getValue() != null) {
                if (fileDescriptionField.getValue().equals(candidateCVField.getValue().getCandidate().getFullName())) {
                    fileDescriptionField.setValue(fileTypeField.getValue().getNameFileType() + " / " +
                             fileDescriptionField.getValue());
                }
            }
        }
    }

    @Subscribe("candidateCVField")
    public void onCandidateCVFieldValueChange(HasValue.ValueChangeEvent<CandidateCV> event) {
        if (fileDescriptionField.getValue() == null) {
            fileDescriptionField.setValue(candidateCVField.getValue().getCandidate().getFullName());
        } else {
            if (fileDescriptionField.getValue() != null) {
                if (fileDescriptionField.getValue().equals(fileTypeField.getValue().getNameFileType())) {
                    fileDescriptionField.setValue( fileDescriptionField.getValue() + " / " +
                             candidateCVField.getValue().getCandidate().getFullName());
                }
            }
        }
    }
}