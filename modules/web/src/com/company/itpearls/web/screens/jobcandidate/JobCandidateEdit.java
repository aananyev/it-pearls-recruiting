package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.entity.*;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;

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
    @Inject
    private CollectionLoader<SocialNetworkURLs> socialNetworkURLsesDl;
    @Inject
    private CollectionContainer<JobHistory> jobHistoriesDc;
    @Inject
    private CollectionLoader<JobHistory> jobHistoriesDl;
    @Inject
    private DateField<Date> birdhDateField;
    @Inject
    private LookupPickerField<Company> currentCompanyField;
    @Inject
    private TextField<String> emailField;
    @Inject
    private TextField<String> firstNameField;
    @Inject
    private LookupPickerField<Specialisation> jobCandidateSpecialisationField;
    @Inject
    private TextField<String> middleNameField;
    @Inject
    private TextField<String> secondNameField;
    @Inject
    private LookupPickerField<City> jobCityCandidateField;
    @Inject
    private LookupPickerField<Position> personPositionField;
    @Inject
    private TextField<String> phoneField;
    @Inject
    private LookupPickerField<Country> positionCountryField;
    @Inject
    private TextField<String> skypeNameField;
    @Inject
    private TextField<String> telegramNameField;
    @Inject
    private TextField<String> whatsupNameField;
    @Inject
    private TextField<String> wiberNameField;
    @Inject
    private Label<String> labelQualityPercent;

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

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        setPercentLabel();
    }

    private void setPercentLabel() {
        // вычислить процент заполнения карточки кандидата
        Integer qualityPercent = setQualityPercent() * 100 / 15;

        if( !PersistenceHelper.isNew( getEditedEntity() ) ) {
            labelQualityPercent.setValue("| Процент заполнения карточки: " + qualityPercent.toString()
                    + "%");
        }
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

                socialNetworkURLsesDl.setParameter( "candidate", getEditedEntity().getId() );
                socialNetworkURLsesDl.load();

                jobHistoriesDl.setParameter( "candidate", getEditedEntity().getFullName() );
                jobHistoriesDl.load();

                if( getEditedEntity().getFullName() == null )
                    getEditedEntity().setFullName("");
           } else {
               iteractionListsDl.removeParameter( "candidate" );
               socialNetworkURLsesDl.removeParameter( "candidate" );
               jobHistoriesDl.removeParameter( "candidate" );
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
                labelCV.setValue("| Резюме: ДА");
            }
        }

        // обнулить статус для вновь создаваемного кандидата
        if( PersistenceHelper.isNew( getEditedEntity() ) ) {
            getEditedEntity().setStatus( 0 );
        }
        // кто последникй рекрутер
        // последний контакт с кандидатом
    }

    @Subscribe(target = Target.DATA_CONTEXT)
    public void onChange(DataContext.ChangeEvent event) {
       setPercentLabel();
    }


    private Integer setQualityPercent() {
        Integer qPercent = 0;

        if( birdhDateField.getValue() != null )
            qPercent = ++qPercent;

        if( currentCompanyField.getValue() != null )
            qPercent = ++qPercent;

        if( emailField.getValue() != null )
            qPercent = ++qPercent;

        if( firstNameField.getValue() != null )
            qPercent = ++qPercent;

        if( middleNameField.getValue() != null )
            qPercent = ++qPercent;

        if( secondNameField.getValue() != null )
            qPercent = ++qPercent;

        if( jobCandidateSpecialisationField != null )
            qPercent = ++qPercent;

        if( jobCityCandidateField.getValue() != null )
            qPercent = ++qPercent;

        if( personPositionField.getValue() != null )
            qPercent = ++qPercent;

        if( phoneField.getValue() != null )
            qPercent = ++qPercent;

        if( positionCountryField.getValue() != null )
            qPercent = ++qPercent;

        if( skypeNameField.getValue() != null )
            qPercent = ++qPercent;

        if( telegramNameField.getValue() != null )
            qPercent = ++qPercent;

        if( whatsupNameField.getValue() != null )
            qPercent = ++qPercent;

        if( wiberNameField.getValue() != null )
            qPercent = ++qPercent;

        return qPercent;

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
