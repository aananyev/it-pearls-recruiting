package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.entity.Country;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.JobCandidate;

import javax.inject.Inject;

@UiController("itpearls_JobCandidate.edit")
@UiDescriptor("job-candidate-edit.xml")
@EditedEntityContainer("jobCandidateDc")
@LoadDataBeforeShow
public class JobCandidateEdit extends StandardEditor<JobCandidate> {
    @Inject
    private InstanceContainer<JobCandidate> jobCandidateDc;
    @Inject
    private LookupPickerField<Country> positionCountryField;

    @Subscribe
    public void onAfterClose(AfterCloseEvent event) {
        setFullNameCandidate();
    }

    
    @Subscribe(id = "jobCandidateDc", target = Target.DATA_CONTAINER)
    private void onJobCandidateDcItemChange(InstanceContainer.ItemChangeEvent<JobCandidate> event) {
        setFullNameCandidate();
    }

    private void setFullNameCandidate(){
        String space = " ";

        if(getEditedEntity().getSecondName() != null &&
            getEditedEntity().getFirstName() != null ) {
                getEditedEntity().setFullName(
                    getEditedEntity().getSecondName() + space +
                            getEditedEntity().getFirstName());
        }

    }

//    @Subscribe("addButtonSN")
//    private void onAddButtonSNClick(Button.ClickEvent event) {
//    }
    
}
