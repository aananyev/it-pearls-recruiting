package com.company.itpearls.web.screens.laborageementtype;

import com.company.itpearls.web.screens.laboragreement.AgreementType;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.RadioButtonGroup;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.LaborAgeementType;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

@UiController("itpearls_LaborAgeementType.edit")
@UiDescriptor("labor-ageement-type-edit.xml")
@EditedEntityContainer("laborAgeementTypeDc")
@LoadDataBeforeShow
public class LaborAgeementTypeEdit extends StandardEditor<LaborAgeementType> {
    @Inject
    private RadioButtonGroup employeeOrCustomerRadioButtonGroup;
    @Inject
    private MessageBundle messageBundle;

    private void setEmployeeOrCustomerCheckBox() {
        Map<String, Integer> employeeOrCustomerMap = new LinkedHashMap<>();
        
        employeeOrCustomerMap.put(messageBundle.getMessage(AgreementType.MSG_EMPLOYEE),
                AgreementType.EPLOYEE);
        employeeOrCustomerMap.put(messageBundle.getMessage(AgreementType.MSG_COMPANY),
                AgreementType.COMPANY);

        employeeOrCustomerRadioButtonGroup.setOptionsMap(employeeOrCustomerMap);
    }

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        setEmployeeOrCustomerCheckBox();
    }
}