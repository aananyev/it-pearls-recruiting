package com.company.itpearls.web.screens.employee;

import com.company.itpearls.web.StandartRegistrationForWork;
import com.haulmont.cuba.gui.components.RadioButtonGroup;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Employee;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

@UiController("itpearls_Employee.browse")
@UiDescriptor("employee-browse.xml")
@LookupComponent("employeesTable")
@LoadDataBeforeShow
public class EmployeeBrowse extends StandardLookup<Employee> {

    @Inject
    private RadioButtonGroup selectTypeOfWorksRadioButton;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private RadioButtonGroup recrutingOrAutstaffingRadioButtonGroup;

    @Subscribe
    public void onInit(InitEvent event) {
        setRadioButtonGroup();
    }

    private void setRadioButtonGroup() {
        Map<String, Integer> workStatusMap = new LinkedHashMap<>();

        workStatusMap.put("В проекте", 0);
        workStatusMap.put("На бенче", 1);
        workStatusMap.put("Ранее работал на проектах фуллтайм", 2);
        workStatusMap.put("Ранее работал на проектах парттайм", 3);

        selectTypeOfWorksRadioButton.setOptionsMap(workStatusMap);

        Map<String, Integer> recruitingOrOutstaffingMap = new LinkedHashMap<>();
        recruitingOrOutstaffingMap.put(messageBundle.getMessage(StandartRegistrationForWork.ALL_MSG),
                StandartRegistrationForWork.ALL);
        recruitingOrOutstaffingMap.put(messageBundle.getMessage(StandartRegistrationForWork.RECRUITING_MSG),
                StandartRegistrationForWork.RECRUITING);
        recruitingOrOutstaffingMap.put(messageBundle.getMessage(StandartRegistrationForWork.OUTSTAFING_MSG),
                StandartRegistrationForWork.OUTSTAFING);

        recrutingOrAutstaffingRadioButtonGroup.setOptionsMap(recruitingOrOutstaffingMap);
    }
}
