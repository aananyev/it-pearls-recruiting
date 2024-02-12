package com.company.itpearls.web.screens.fragments.candidatecv;

import com.company.itpearls.entity.Company;
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

    public void setNewWorkPlaceGroupBox(GroupBoxLayout newWorkPlaceGroupBox) {
        this.newWorkPlaceGroupBox = newWorkPlaceGroupBox;
    }

    @Subscribe("endDateField")
    public void onEndDateFieldValueChange(HasValue.ValueChangeEvent<Date> event) {
        companyLookupPickerFieldGroupBoxSetHeader(
                companyLookupPickerField,
                newWorkPlaceGroupBox,
                startDateField,
                endDateField,
                workToThisDayCheckBox);
    }

    @Subscribe("workToThisDayCheckBox")
    public void onWorkToThisDayCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        companyLookupPickerFieldGroupBoxSetHeader(companyLookupPickerField,
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
    }

    @Subscribe("startDateField")
    public void onStartDateFieldValueChange(HasValue.ValueChangeEvent<Date> event) {
        companyLookupPickerFieldGroupBoxSetHeader(companyLookupPickerField,
                newWorkPlaceGroupBox,
                startDateField,
                endDateField,
                workToThisDayCheckBox);
    }
    private void companyLookupPickerFieldGroupBoxSetHeader(LookupPickerField companyLookupPickerField,
                                                           GroupBoxLayout newWorkPlaceGroupBox,
                                                           DateField startDateField,
                                                           DateField endDateField,
                                                           CheckBox workToThisDayCheckBox) {
        StringBuilder sb = new StringBuilder();
        if (companyLookupPickerField.getValue() != null) {
            sb.append(((Company)companyLookupPickerField.getValue()).getCompanyShortName());
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
}