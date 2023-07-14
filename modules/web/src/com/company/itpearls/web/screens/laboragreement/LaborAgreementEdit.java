package com.company.itpearls.web.screens.laboragreement;

import com.company.itpearls.entity.Company;
import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.LaborAgreement;

import javax.inject.Inject;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@UiController("itpearls_LaborAgreement.edit")
@UiDescriptor("labor-agreement-edit.xml")
@EditedEntityContainer("laborAgreementDc")
@LoadDataBeforeShow
public class LaborAgreementEdit extends StandardEditor<LaborAgreement> {
    @Inject
    private CheckBox parpetualAgreementCheckBox;
    @Inject
    private DateField<Date> laborAgreementEndDateField;
    @Inject
    private LookupPickerField<Company> customerCompanyLookupPickerField;
    @Inject
    private LookupPickerField<Company> legalEntityEmployeeLookupPickerField;
    @Inject
    private CollectionLoader<LaborAgreement> additionalAgreementDl;
    @Inject
    private SuggestionPickerField<JobCandidate> candidateField;

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if(parpetualAgreementCheckBox.getValue() != null) {
                laborAgreementEndDateField.setEnabled(!parpetualAgreementCheckBox.getValue());
        }

        setAdditionalLaborAgreementDataGrid();
    }

    private void setAdditionalLaborAgreementDataGrid() {
        additionalAgreementDl.setParameter("additionalLaborAgreement", getEditedEntity());
        additionalAgreementDl.load();
    }

    @Subscribe
    public void onInit(InitEvent event) {
        parpetualAgreementCheckBox.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                laborAgreementEndDateField.setEnabled(!parpetualAgreementCheckBox.getValue());
            }
        });

        // setEmployeeOrCustomerCheckBox();
    }

    /* private void setEmployeeOrCustomerCheckBox() {
        Map<String, Integer> employeeOrCustomerMap = new LinkedHashMap<>();
        employeeOrCustomerMap.put("Сотрудником", 1);
        employeeOrCustomerMap.put("Клиентом", 2);

        employeeOrCustomerRadioButtonGroup.setOptionsMap(employeeOrCustomerMap);
    }

    @Subscribe("employeeOrCustomerRadioButtonGroup")
    public void onEmployeeOrCustomerRadioButtonGroupValueChange(HasValue.ValueChangeEvent<Integer> event) {
        switch (event.getValue()) {
            case 1:
                // компания иили сотрудник
                candidateField.setVisible(true);
                customerCompanyLookupPickerField.setVisible(false);

                legalEntityEmployeeLookupPickerField.setVisible(true);
                break;
            case 2:
                // компания иили сотрудник
                candidateField.setVisible(false);
                customerCompanyLookupPickerField.setVisible(true);

                legalEntityEmployeeLookupPickerField.setVisible(false);

                break;
            default:
                break;
        }
    } */
}