package com.company.itpearls.web.screens.project;

import com.company.itpearls.entity.CompanyDepartament;
import com.company.itpearls.entity.Person;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Project;

import javax.inject.Inject;
import java.util.Date;

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

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
       if(PersistenceHelper.isNew( getEditedEntity() ) ) {
           Date date = new Date();

           startProjectDateField.setValue( date );
       }
    }

    @Subscribe("checkBoxProjectIsClosed")
    public void onCheckBoxProjectIsClosedValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if( checkBoxProjectIsClosed.getValue() ) {
            projectNameField.setEditable( false );
            startProjectDateField.setEditable( false );
            endProjectDateField.setEditable( false );
            projectDepartmentField.setEditable( false );
            projectOwnerField.setEditable( false );
        } else {
            projectNameField.setEditable( true );
            startProjectDateField.setEditable( true );
            endProjectDateField.setEditable( true );
            projectDepartmentField.setEditable( true );
            projectOwnerField.setEditable( true );
        }

    }
}