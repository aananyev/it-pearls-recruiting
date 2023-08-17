package com.company.itpearls.web.screens.employee;

import com.company.itpearls.entity.EmployeeWorkStatus;
import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.web.StandartRegistrationForWork;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Employee;
import com.haulmont.cuba.gui.screen.LookupComponent;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
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
    private DataManager dataManager;
    @Inject
    private CollectionLoader<Employee> employeesDl;
    @Inject
    private Metadata metadata;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private GroupTable<Employee> employeesTable;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        setRadioButtonGroup();
    }

    private void setRadioButtonGroup() {
        Map<String, EmployeeWorkStatus> workStatusMap = new LinkedHashMap<>();
        EmployeeWorkStatus allWorkStatus = metadata.create(EmployeeWorkStatus.class);
        allWorkStatus.setWorkStatusName("All");

        workStatusMap.put(messageBundle.getMessage("mainmsgWorkStatusAll"),
                allWorkStatus);

        List<EmployeeWorkStatus> employeeWorkStatuses = dataManager.load(EmployeeWorkStatus.class)
                .list();

        if (employeeWorkStatuses.size() > 0) {
            for (EmployeeWorkStatus employeeWorkStatus : employeeWorkStatuses) {
                workStatusMap.put(employeeWorkStatus.getWorkStatusName(), employeeWorkStatus);
            }
        }

        selectTypeOfWorksRadioButton.setOptionsMap(workStatusMap);

        selectTypeOfWorksRadioButton.addValueChangeListener(e -> {
            if (!((EmployeeWorkStatus) selectTypeOfWorksRadioButton.getValue()).equals(allWorkStatus)) {
                employeesDl.setParameter("workStatus", (EmployeeWorkStatus) selectTypeOfWorksRadioButton.getValue());
            } else {
                employeesDl.removeParameter("workStatus");
            }

            employeesDl.load();
        });

        /* Map<String, Integer> recruitingOrOutstaffingMap = new LinkedHashMap<>();
        recruitingOrOutstaffingMap.put(messageBundle.getMessage(StandartRegistrationForWork.ALL_MSG),
                StandartRegistrationForWork.ALL);
        recruitingOrOutstaffingMap.put(messageBundle.getMessage(StandartRegistrationForWork.RECRUITING_MSG),
                StandartRegistrationForWork.RECRUITING);
        recruitingOrOutstaffingMap.put(messageBundle.getMessage(StandartRegistrationForWork.OUTSTAFING_MSG),
                StandartRegistrationForWork.OUTSTAFING);

        recrutingOrAutstaffingRadioButtonGroup.setOptionsMap(recruitingOrOutstaffingMap); */
    }

    @Subscribe
    public void onInit(InitEvent event) {
        employeesTable.addGeneratedColumn("fileImageFace", employee -> {
            HBoxLayout hBox = uiComponents.create(HBoxLayout.class);
            Image image = uiComponents.create(Image.NAME);

            if (employee.getJobCandidate().getFileImageFace() != null) {
                image.setValueSource(new ContainerValueSource<>(employeesTable.getInstanceContainer(employee),
                        "jobCandidate.fileImageFace"));

                hBox.add(image);
                image.setWidth("20px");
                image.setStyleName("circle-20px");

                image.setScaleMode(Image.ScaleMode.CONTAIN);
                image.setAlignment(Component.Alignment.MIDDLE_CENTER);

                hBox.setWidthFull();
                hBox.setHeightFull();
                hBox.add(image);
                return hBox;

            }

            return null;
        });
    }
}
