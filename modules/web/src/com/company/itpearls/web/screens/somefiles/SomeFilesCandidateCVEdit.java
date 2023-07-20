package com.company.itpearls.web.screens.somefiles;

import com.company.itpearls.entity.CandidateCV;
import com.company.itpearls.entity.FileType;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.components.SuggestionPickerField;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.company.itpearls.web.screens.somefiles.SomeFilesEdit;

import javax.inject.Inject;

@UiController("itpearls_SomeFilesCandidateCV.edit")
@UiDescriptor("some-files-candidatecv-edit.xml")
public class SomeFilesCandidateCVEdit extends SomeFilesEdit {
    @Inject
    private TextField<String> fileDescriptionField;
    @Inject
    private LookupPickerField<FileType> fileTypeField;
//    @Inject
//    private SuggestionPickerField<CandidateCV> candidateCVSuggestPickerField;

/*    @Subscribe("fileTypeField")
    public void onFileTypeFieldValueChange(HasValue.ValueChangeEvent<FileType> event) {
        if (fileDescriptionField.getValue() == null) {
            fileDescriptionField.setValue(fileTypeField.getValue().getNameFileType());
        } else {
            if (candidateCVSuggestPickerField.getValue() != null) {
                if (fileDescriptionField.getValue().equals(candidateCVSuggestPickerField.getValue().getCandidate().getFullName())) {
                    fileDescriptionField.setValue(fileTypeField.getValue().getNameFileType() + " / " +
                            fileDescriptionField.getValue());
                }
            }
        }
    }

    @Subscribe("candidateCVSuggestPickerField")
    public void onCandidateCVSuggestPickerFieldValueChange(HasValue.ValueChangeEvent<CandidateCV> event) {
        if (fileDescriptionField.getValue() == null) {
            fileDescriptionField.setValue(candidateCVSuggestPickerField.getValue().getCandidate().getFullName());
        } else {
            if (fileDescriptionField.getValue() != null) {
                if (fileDescriptionField.getValue().equals(fileTypeField.getValue().getNameFileType())) {
                    fileDescriptionField.setValue(fileDescriptionField.getValue() + " / " +
                            candidateCVSuggestPickerField.getValue().getCandidate().getFullName());
                }
            }
        }
    }  */
}