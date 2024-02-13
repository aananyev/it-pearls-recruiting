package com.company.itpearls.web.screens.fragments.candidatecv;

import com.company.itpearls.entity.CandidateCV;
import com.company.itpearls.entity.CandidateCVWorkPlaces;
import com.company.itpearls.entity.Company;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.BulkEditors;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;

@UiController("itpearls_WorkPlacesFragment")
@UiDescriptor("work-places-fragment.xml")
public class WorkPlacesFragment extends ScreenFragment {
    @Inject
    private DateField<Date> endDateField;
    private GroupBoxLayout newWorkPlaceGroupBox;
    @Inject
    private DateField<Date> startDateField;
    @Inject
    private CheckBox workToThisDayCheckBox;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private LookupPickerField<Company> companyLookupPickerField;

    private Boolean deletedWorkPlace = false;
    @Inject
    private TextField<String> workPlaceCommentTextField;
    @Inject
    private RichTextArea functionalityAtWorkRichTextArea;
    @Inject
    private RichTextArea achievementsRichTextArea;
    @Inject
    private RichTextArea personalRoleRichTextArea;
    private CandidateCVWorkPlaces candidateCVWorkPlaces;
    @Inject
    private SuggestionPickerField<Company> companySuggestPickerField;
    @Inject
    private Metadata metadata;
    private CandidateCV candidateCV;

    public Boolean getDeletedWorkPlace() {
        return deletedWorkPlace;
    }

    public Company getCompany() {
        return companyLookupPickerField.getValue();
    }

    public void setDeletedWorkPlace(Boolean deletedWorkPlace) {
        this.deletedWorkPlace = deletedWorkPlace;
    }

    public void setNewWorkPlaceGroupBox(GroupBoxLayout newWorkPlaceGroupBox) {
        this.newWorkPlaceGroupBox = newWorkPlaceGroupBox;
    }

    @Subscribe("endDateField")
    public void onEndDateFieldValueChange(HasValue.ValueChangeEvent<Date> event) {
        companyLookupPickerFieldGroupBoxSetHeader(
                companySuggestPickerField,
                newWorkPlaceGroupBox,
                startDateField,
                endDateField,
                workToThisDayCheckBox);

        candidateCVWorkPlaces.setEndDate(event.getValue());
    }

    @Subscribe("workToThisDayCheckBox")
    public void onWorkToThisDayCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        companyLookupPickerFieldGroupBoxSetHeader(companySuggestPickerField,
                newWorkPlaceGroupBox,
                startDateField,
                endDateField,
                workToThisDayCheckBox);

        if (event.getValue() != null) {
            if (event.getValue()) {
                endDateField.setEditable(false);
            } else {
                endDateField.setEditable(true);
            }
        } else {
            endDateField.setEditable(true);
        }

        candidateCVWorkPlaces.setWorkToThisDay(event.getValue());
    }

    @Subscribe("companySuggestPickerField")
    public void onCompanySuggestPickerFieldValueChange(HasValue.ValueChangeEvent<Company> event) {
        companyLookupPickerFieldGroupBoxSetHeader(companySuggestPickerField,
                newWorkPlaceGroupBox,
                startDateField,
                endDateField,
                workToThisDayCheckBox);

        candidateCVWorkPlaces.setWorkPlace(event.getValue());
    }

    @Subscribe("startDateField")
    public void onStartDateFieldValueChange(HasValue.ValueChangeEvent<Date> event) {
        companyLookupPickerFieldGroupBoxSetHeader(companySuggestPickerField,
                newWorkPlaceGroupBox,
                startDateField,
                endDateField,
                workToThisDayCheckBox);

        candidateCVWorkPlaces.setStartDate(event.getValue());
    }

    private void companyLookupPickerFieldGroupBoxSetHeader(SuggestionPickerField companyLookupPickerField,
                                                           GroupBoxLayout newWorkPlaceGroupBox,
                                                           DateField startDateField,
                                                           DateField endDateField,
                                                           CheckBox workToThisDayCheckBox) {
        StringBuilder sb = new StringBuilder();
        if (companyLookupPickerField.getValue() != null && startDateField.getValue() != null &&
                (endDateField.getValue() != null || workToThisDayCheckBox.getValue() == true)) {
            if (((Company) companyLookupPickerField.getValue()).getComanyName() != null) {
                sb.append(((Company) companyLookupPickerField.getValue()).getComanyName());
            }

            if (((Company) companyLookupPickerField.getValue()).getCompanyShortName() != null) {
                sb.append(" / ");
                sb.append(((Company) companyLookupPickerField.getValue()).getCompanyShortName());
            }

//            sb.append(((Company)companyLookupPickerField.getValue()).getCompanyShortName());
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.YYYY");

            if ((startDateField.getValue() != null)
                    && (endDateField.getValue() != null)
                    && (workToThisDayCheckBox.getValue() != null)) {

                sb.append(" - c ")
                        .append(sdf.format(startDateField.getValue()))
                        .append(" по ")
                        .append(workToThisDayCheckBox.getValue() == true
                                ? messageBundle.getMessage("msgCurrentTime")
                                : sdf.format(endDateField.getValue()))
                        .toString();
            }

            newWorkPlaceGroupBox.setCaption(sb.toString());
        }
    }

    public String getWorkPlaceComment() {
        return workPlaceCommentTextField.getValue();
    }

    public String getFunctionalityAtWork() {
        return functionalityAtWorkRichTextArea.getValue();
    }

    public String getAchievements() {
        return achievementsRichTextArea.getValue();
    }

    public Boolean getWorkToThisDay() {
        return workToThisDayCheckBox.getValue();
    }

    public Date getEndDate() {
        return endDateField.getValue();
    }

    public Date getStartDate() {
        return startDateField.getValue();
    }

    public String getPersonalRole() {
        return personalRoleRichTextArea.getValue();
    }

    public void setNewWorkPlace(CandidateCVWorkPlaces candidateCVWorkPlaces) {
        if (candidateCVWorkPlaces != null) {
            companyLookupPickerField.setValue(candidateCVWorkPlaces.getWorkPlace());
            startDateField.setValue(candidateCVWorkPlaces.getStartDate());
            endDateField.setValue(candidateCVWorkPlaces.getEndDate());
            workToThisDayCheckBox.setValue(candidateCVWorkPlaces.getWorkToThisDay());
            workPlaceCommentTextField.setValue(candidateCVWorkPlaces.getWorkPlaceComment());
            functionalityAtWorkRichTextArea.setValue(candidateCVWorkPlaces.getFunctionalityAtWork());
            personalRoleRichTextArea.setValue(candidateCVWorkPlaces.getPersonalRole());
            achievementsRichTextArea.setValue(candidateCVWorkPlaces.getAchievements());

            this.candidateCVWorkPlaces = candidateCVWorkPlaces;
        } else {
            createCandidateCVWorkPlaces();
        }
    }

    public void setCandidateCV(CandidateCV candidateCV) {
        this.candidateCV = candidateCV;
        candidateCVWorkPlaces.setCandidateCV(this.candidateCV);
    }

    @Subscribe("workPlaceCommentTextField")
    public void onWorkPlaceCommentTextFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        this.candidateCVWorkPlaces.setWorkPlaceComment(event.getValue());
    }

    @Subscribe("functionalityAtWorkRichTextArea")
    public void onFunctionalityAtWorkRichTextAreaValueChange(HasValue.ValueChangeEvent<String> event) {
        this.candidateCVWorkPlaces.setFunctionalityAtWork(event.getValue());
    }

    @Subscribe("achievementsRichTextArea")
    public void onAchievementsRichTextAreaValueChange(HasValue.ValueChangeEvent<String> event) {
        this.candidateCVWorkPlaces.setAchievements(event.getValue());
    }

    @Subscribe("personalRoleRichTextArea")
    public void onPersonalRoleRichTextAreaValueChange(HasValue.ValueChangeEvent<String> event) {
       this.candidateCVWorkPlaces.setPersonalRole(event.getValue());
    }

    public CandidateCVWorkPlaces getCandidateCVWorkPlaces() {
        return this.candidateCVWorkPlaces;
    }

    public void createCandidateCVWorkPlaces() {
        this.candidateCVWorkPlaces = metadata.create(CandidateCVWorkPlaces.class);
    }
}