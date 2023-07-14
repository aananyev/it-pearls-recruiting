package com.company.itpearls.web.screens.laborageementtype;

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

    private void setEmployeeOrCustomerCheckBox() {
        Map<String, Boolean> employeeOrCustomerMap = new LinkedHashMap<>();
        employeeOrCustomerMap.put("Сотрудником", false);
        employeeOrCustomerMap.put("Клиентом", true);

        employeeOrCustomerRadioButtonGroup.setOptionsMap(employeeOrCustomerMap);
    }

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        setEmployeeOrCustomerCheckBox();
    }
}