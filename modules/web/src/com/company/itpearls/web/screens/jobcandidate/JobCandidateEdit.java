package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.entity.CandidateCV;
import com.company.itpearls.entity.City;
import com.company.itpearls.entity.IteractionList;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.JobCandidate;

import javax.inject.Inject;
import javax.inject.Named;

@UiController("itpearls_JobCandidate.edit")
@UiDescriptor("job-candidate-edit.xml")
@EditedEntityContainer("jobCandidateDc")
@LoadDataBeforeShow
public class JobCandidateEdit extends StandardEditor<JobCandidate> {
    @Inject
    private CollectionLoader<IteractionList> iteractionListsDl;
    @Inject
    private Label<String> labelCV;
    @Named("tabIteraction")
    private VBoxLayout tabIteraction;
    @Named("tabResume")
    private VBoxLayout tabResume;
    @Inject
    private Table<IteractionList> jobCandidateIteractionListTable;
    @Inject
    private Table<CandidateCV> jobCandidateCandidateCvTable;

    @Subscribe("firstNameField")
    public void onFirstNameFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        setLabelFullName( setFullName( getEditedEntity().getFirstName(), null, null) );
    }

    @Subscribe("middleNameField")
    public void onMiddleNameFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        setLabelFullName( setFullName( null, getEditedEntity().getMiddleName(), null ) );
    }

    @Subscribe("secondNameField")
    public void onSecondNameFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        setLabelFullName( setFullName( null, null, getEditedEntity().getSecondName() ) );
    }

    private String setFullName(String firstName, String middleName, String secondName) {
        String fullName = "", localFirstName = "", localMiddleName = "", localSecondName = "";

        if( getEditedEntity().getFirstName() != null )
            localFirstName = getEditedEntity().getFirstName();
        else
            if( firstName != null )
                localFirstName = firstName;

        if( getEditedEntity().getSecondName() != null )
            localSecondName = getEditedEntity().getSecondName();
        else
            if( secondName != null )
                localSecondName = secondName;

        if( getEditedEntity().getMiddleName() != null )
            localMiddleName = getEditedEntity().getMiddleName();
        else
            if( middleName != null )
                localMiddleName = middleName;

        fullName = localSecondName + " " + localFirstName + " " + localMiddleName;

       return fullName;
    }

    private void setLabelFullName(String fullName ) {
        getEditedEntity().setFullName( fullName );
    }

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

                if( getEditedEntity().getFullName() == null )
                    getEditedEntity().setFullName("");
           }

           // заблокировать вкладки с резюме и итеракицями
           tabIteraction.setVisible( true );
           tabResume.setVisible( true );
       } else {
           // заблокировать вкладки с резюме и итеракицями
           tabIteraction.setVisible( false );
           tabResume.setVisible( false );
       }

       // если есть резюме, то поставить галку
        if( !PersistenceHelper.isNew( getEditedEntity() ) ) {
            if (getEditedEntity().getCandidateCv().isEmpty()) {
                labelCV.setValue("| Резюме: НЕТ");
            } else {
                labelCV.setValue("|dataManager Резюме: ДА");
            }
        }
        // кто последникй рекрутер
        // последний контакт с кандидатом
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
