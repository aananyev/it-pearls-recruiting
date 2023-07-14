package com.company.itpearls.web.screens.laborageementtype;

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
            if (entity.getEmployeeOrcompany()) {
                msgEmployeeOrCompany = messageBundle.getMessage("msgAgreementCompany");
            } else {
                msgEmployeeOrCompany = messageBundle.getMessage("msgAgreementEmployee");
            }
        } else {
            msgEmployeeOrCompany = messageBundle.getMessage("Undefined");
        }

        companyOrEmployeeLabel.setValue(msgEmployeeOrCompany);
        return companyOrEmployeeLabel;
    }
}