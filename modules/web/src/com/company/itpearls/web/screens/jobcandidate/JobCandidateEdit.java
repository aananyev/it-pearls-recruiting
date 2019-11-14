package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.entity.City;
import com.company.itpearls.entity.IteractionList;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
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
    private CollectionLoader<IteractionList> iteractionListsDl;
    @Inject
    private CollectionContainer<City> citiesDc;

    @Subscribe("jobCityCandidateField")
    public void onJobCityCandidateFieldValueChange(HasValue.ValueChangeEvent<City> event) {
        if(!getEditedEntity().getCityOfResidence().equals(null)
            && PersistenceHelper.isNew(getEditedEntity())) {
                getEditedEntity().setPositionCountry(getEditedEntity()
                    .getCityOfResidence()
                    .getCityRegion()
                    .getRegionCountry());
        }
        
    }

    // загрузить таблицу взаимодействий
    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
       if(!PersistenceHelper.isNew(getEditedEntity())) {
           if(!getEditedEntity().getFullName().equals(null)) {
                iteractionListsDl.setParameter("candidate", getEditedEntity().getId());

                iteractionListsDl.load();
           }
       }
    }

    @Subscribe
    public void onBeforeClose1(BeforeCloseEvent event) {
        setFullNameCandidate();
    }

    
    @Subscribe(id = "jobCandidateDc", target = Target.DATA_CONTAINER)
    private void onJobCandidateDcItemChange(InstanceContainer.ItemChangeEvent<JobCandidate> event) {
        setFullNameCandidate();
    }

    @Subscribe
    public void onBeforeClose(BeforeCloseEvent event) {
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
}
