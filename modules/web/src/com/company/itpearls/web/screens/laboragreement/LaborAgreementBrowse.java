package com.company.itpearls.web.screens.laboragreement;

import com.haulmont.cuba.gui.components.RadioButtonGroup;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.LaborAgreement;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

@UiController("itpearls_LaborAgreement.browse")
@UiDescriptor("labor-agreement-browse.xml")
@LookupComponent("laborAgreementsTable")
@LoadDataBeforeShow
public class LaborAgreementBrowse extends StandardLookup<LaborAgreement> {
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private RadioButtonGroup employeeOrCompanyRadioButton;
    @Inject
    private CollectionLoader<LaborAgreement> laborAgreementsDl;

    @Subscribe
    public void onInit(InitEvent event) {
        setEnployeeOrCompanyRadioButton();
    }

    private void setEnployeeOrCompanyRadioButton() {
        Map<String, Integer> employeeOrCustomerMap = new LinkedHashMap<>();
        employeeOrCustomerMap.put(messageBundle.getMessage("msgAllAgreements"), 0);
        employeeOrCustomerMap.put(messageBundle.getMessage(AgreementType.MSG_EMPLOYEE),
                AgreementType.EPLOYEE);
        employeeOrCustomerMap.put(messageBundle.getMessage(AgreementType.MSG_COMPANY),
                AgreementType.COMPANY);

        employeeOrCompanyRadioButton.setOptionsMap(employeeOrCustomerMap);
        employeeOrCompanyRadioButton.addValueChangeListener(e -> {
            if (employeeOrCompanyRadioButton.getValue() == null) {
                laborAgreementsDl.removeParameter("laborAgreementType");
            } else {
                    laborAgreementsDl.setParameter("laborAgreementType",
                            employeeOrCompanyRadioButton.getValue());
            }

            laborAgreementsDl.load();
        });
    }
}