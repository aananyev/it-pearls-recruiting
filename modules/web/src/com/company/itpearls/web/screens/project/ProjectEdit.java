package com.company.itpearls.web.screens.project;

import com.company.itpearls.entity.CompanyDepartament;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.Person;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Project;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@UiController("itpearls_Project.edit")
@UiDescriptor("project-edit.xml")
@EditedEntityContainer("projectDc")
@LoadDataBeforeShow
public class ProjectEdit extends StandardEditor<Project> {
    @Inject
    private DateField<Date> startProjectDateField;
    @Inject
    private CheckBox checkBoxProjectIsClosed;
    @Inject
    private TextField<String> projectNameField;
    @Inject
    private DateField<Date> endProjectDateField;
    @Inject
    private LookupPickerField<CompanyDepartament> projectDepartmentField;
    @Inject
    private LookupPickerField<Person> projectOwnerField;
    @Inject
    private Dialogs dialogs;
    @Inject
    private DataManager dataManager;

    List<OpenPosition> openPositions = new ArrayList<>();

    @Subscribe("checkBoxProjectIsClosed")
    public void onCheckBoxProjectIsClosedValueChange1(HasValue.ValueChangeEvent<Boolean> event) {
        String opeedPositionList = "";

        for (OpenPosition a : openPositions) {
            opeedPositionList = a.getVacansyName() + "<br>" + opeedPositionList;
        }

        if (!opeedPositionList.equals("")) {
            if (checkBoxProjectIsClosed.getValue()) {
                dialogs.createOptionDialog()
                        .withCaption("ВНИМАНИЕ")
                        .withType(Dialogs.MessageType.WARNING)
                        .withContentMode(ContentMode.HTML)
                        .withMessage("<b>Закрыть вакансии на этом проекте?</b><br>Открыты позиции: <br><i>" +
                                opeedPositionList + "</i>")
                        .withActions(
                                new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                                    closeAllVacansies();
                                }),
                                new DialogAction((DialogAction.Type.NO)
                                ))
                        .show();
            }
        }

        setEndDateProject();
    }

    private void setEndDateProject() {
        Date date = new Date();

        if (checkBoxProjectIsClosed.getValue())
            endProjectDateField.setValue(date);
        else
            endProjectDateField.setValue(null);
    }

    private void closeAllVacansies() {
        for (OpenPosition a : openPositions) {
            a.setOpenClose(true);

            CommitContext commitContext = new CommitContext(a);
            dataManager.commit(commitContext);
        }
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            Date date = new Date();

            startProjectDateField.setValue(date);
        }

        setStartDateOfProject();
        getOpenedPosition();
    }

    private void setStartDateOfProject() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            startProjectDateField.setValue(new Date());
        }
    }

    private void getOpenedPosition() {
        String positionsQuery = "select e from itpearls_OpenPosition e " +
                "where e.projectName = :projectName and " +
                "e.openClose = false";
        openPositions = dataManager.load(OpenPosition.class)
                .query(positionsQuery)
                .parameter("projectName", getEditedEntity())
                .list();
    }

    @Subscribe("checkBoxProjectIsClosed")
    public void onCheckBoxProjectIsClosedValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (checkBoxProjectIsClosed.getValue()) {
            projectNameField.setEditable(false);
            startProjectDateField.setEditable(false);
            endProjectDateField.setEditable(false);
            projectDepartmentField.setEditable(false);
            projectOwnerField.setEditable(false);
        } else {
            projectNameField.setEditable(true);
            startProjectDateField.setEditable(true);
            endProjectDateField.setEditable(true);
            projectDepartmentField.setEditable(true);
            projectOwnerField.setEditable(true);
        }

    }
}