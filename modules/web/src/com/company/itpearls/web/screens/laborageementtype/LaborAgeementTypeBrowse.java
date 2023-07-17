package com.company.itpearls.web.screens.laborageementtype;

import com.company.itpearls.web.screens.laboragreement.AgreementType;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.LaborAgeementType;

import javax.inject.Inject;

@UiController("itpearls_LaborAgeementType.browse")
@UiDescriptor("labor-ageement-type-browse.xml")
@LookupComponent("laborAgeementTypesTable")
@LoadDataBeforeShow
public class LaborAgeementTypeBrowse extends StandardLookup<LaborAgeementType> {
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private UiComponents uiComponents;

    public Component employeeOrcompanyGenerator(LaborAgeementType entity) {
        Label companyOrEmployeeLabel = uiComponents.create(Label.class);
        String msgEmployeeOrCompany;

        if (entity.getEmployeeOrcompany() != null) {
            if (entity.getEmployeeOrcompany() == AgreementType.COMPANY) {
                msgEmployeeOrCompany = messageBundle.getMessage(AgreementType.MSG_COMPANY);
            } else {
                msgEmployeeOrCompany = messageBundle.getMessage(AgreementType.MSG_EMPLOYEE);
            }
        } else {
            msgEmployeeOrCompany = messageBundle.getMessage("Undefined");
        }

        companyOrEmployeeLabel.setValue(msgEmployeeOrCompany);
        return companyOrEmployeeLabel;
    }
}