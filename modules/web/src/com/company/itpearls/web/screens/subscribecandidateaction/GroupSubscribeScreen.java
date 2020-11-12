package com.company.itpearls.web.screens.subscribecandidateaction;

import com.company.itpearls.entity.Company;
import com.company.itpearls.entity.CompanyDepartament;
import com.company.itpearls.entity.Position;
import com.company.itpearls.entity.Project;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.components.RadioButtonGroup;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

@UiController("itpearls_GroupSubscribeScreen")
@UiDescriptor("group-subscribe-screen.xml")
public class GroupSubscribeScreen extends Screen {
    @Inject
    private RadioButtonGroup typeSubscribeRadioButton;
    @Inject
    private LookupPickerField<Company> companyField;
    @Inject
    private LookupPickerField<CompanyDepartament> departamentField;
    @Inject
    private LookupPickerField<Project> projectField;
    @Inject
    private LookupPickerField<Position> typePositionField;

    @Subscribe
    public void onInit(InitEvent event) {

        Map<String, Integer> subscribeType = new LinkedHashMap<>();

        subscribeType.put("по типу позиции", 0);
        subscribeType.put("по компании", 1);
        subscribeType.put("по департаменту", 2);
        subscribeType.put("по проекту", 3);

        typeSubscribeRadioButton.setOptionsMap(subscribeType);
    }

    @Subscribe("typeSubscribeRadioButton")
    public void onTypeSubscribeRadioButtonValueChange(HasValue.ValueChangeEvent event) {
        switch ((int) event.getValue()) {
            case 0:
                companyField.setVisible(false);
                departamentField.setVisible(false);
                projectField.setVisible(false);
                typePositionField.setVisible(true);
                break;
            case 1:
                companyField.setVisible(true);
                departamentField.setVisible(false);
                projectField.setVisible(false);
                typePositionField.setVisible(false);
                break;
            case 2:
                companyField.setVisible(false);
                departamentField.setVisible(true);
                projectField.setVisible(false);
                typePositionField.setVisible(false);
                break;
            case 3:
                companyField.setVisible(false);
                departamentField.setVisible(false);
                projectField.setVisible(true);
                typePositionField.setVisible(false);
                break;
        }
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        typeSubscribeRadioButton.setValue(0);

    }
}