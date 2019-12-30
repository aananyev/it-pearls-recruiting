package com.company.itpearls.web.screens.iteractionlist;

import com.company.itpearls.entity.*;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@UiController("itpearls_IteractionList.edit")
@UiDescriptor("iteraction-list-edit.xml")
@EditedEntityContainer("iteractionListDc")
@LoadDataBeforeShow
public class IteractionListEdit extends StandardEditor<IteractionList> {
    @Inject
    private DataManager iteractionListEditDataManager;
    @Inject
    private UserSession userSession;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private Button buttonCallAction;
    @Inject
    private LookupPickerField<Iteraction> iteractionTypeField;
    @Inject
    private InstanceContainer<IteractionList> iteractionListDc;
    @Inject
    private LookupPickerField<JobCandidate> candidateField;
    @Inject
    private CollectionContainer<JobCandidate> candidatesDc;
    @Inject
    private MetadataTools metadataTools;
    @Inject
    private CollectionLoader<JobCandidate> candidatesLc;
    @Inject
    private DataManager dataManager;

    @Subscribe(id = "iteractionListDc", target = Target.DATA_CONTAINER)
    private void onIteractionListDcItemChange(InstanceContainer.ItemChangeEvent<IteractionList> event) {
        if( PersistenceHelper.isNew(getEditedEntity()) ) {
            setIteractionNumber();
            setCurrentDate();
            setCurrentUserName(); // имя пользователя в нередактируемое поле
        }
    }

    @Subscribe("candidateField")
    public void onCandidateFieldValueChange(HasValue.ValueChangeEvent<JobCandidate> event) {
       setCandidateSpecialisation();
    }

    @Subscribe("vacancyFiels")
    public void onVacancyFielsValueChange(HasValue.ValueChangeEvent<OpenPosition> event) {
        if( getEditedEntity().getVacancy() != null )
            if(getEditedEntity().getVacancy().getProjectName() != null)
                getEditedEntity().setProject(getEditedEntity().getVacancy().getProjectName());
    }

    @Subscribe("projectField")
    public void onProjectFieldValueChange(HasValue.ValueChangeEvent<Project> event) {
        if( getEditedEntity().getVacancy() != null )
            if( getEditedEntity().getVacancy().getCompanyDepartament() != null )
                getEditedEntity().setCompanyDepartment(getEditedEntity().getVacancy().getCompanyDepartament());
    }


    private BigDecimal getCountIteraction() {
        BigDecimal maxValue = iteractionListEditDataManager.loadValue(
                "select max(a.numberIteraction) from itpearls_IteractionList a",
                            BigDecimal.class).one().add(BigDecimal.ONE);

        return maxValue;
    }

    protected void setIteractionNumber() {
        getEditedEntity().setNumberIteraction(getCountIteraction());
    }

    protected void setCurrentDate() {
        getEditedEntity().setDateIteraction(new Date());
    }

    // не могу получить имя пользователя и записать его в базу
    protected String getCurrentUser() {
        String currentUser =
                userSessionSource.getUserSession().getUser().getName();
        return currentUser;
    }

    @Subscribe
    public void onBeforeClose(AfterCloseEvent event) {
        // если кейс не нечат, то сначала надо начать
        // записать статус в карточку кандидата
        Integer i = Integer.parseInt( getEditedEntity().getIteractionType().getNumber());
        JobCandidate    candidate = loadJobCandidate( candidateField.getValue().getId() );
        candidate.setStatus( i );
        dataManager.commit( candidate );
    }

    private JobCandidate loadJobCandidate( UUID jobCandidateId ) {
        return dataManager.load( JobCandidate.class )
                .id( jobCandidateId )
                .view( "jobCandidate-view" )
                .one();
    }
    
    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        buttonCallAction.setVisible(false);
    }

    // создать новый экран
    private void createNewField(IteractionList entity) {
        // тут по идее надо создать новый экран и передать туда параметры
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if(PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setRecrutier(userSession.getUser());
            // спросить нужно ли копировать предыдущую запись?
        }
    }

    protected void setCurrentUserName() {
        String currentUser = getCurrentUser();
    }

// После изменения имени кандидата надо заполнить поле специализация кандидата
    protected void setCandidateSpecialisation() {
    }

    // изменение надписи на кнопке в зависимости от щначения поля ItercationType
    @Subscribe("iteractionTypeField")
    public void onIteractionTypeFieldValueChange(HasValue.ValueChangeEvent<Iteraction> event) {
        // надпись на кнопке
        if( iteractionTypeField.getValue().getCallButtonText() != null )
            buttonCallAction.setCaption(iteractionTypeField.getValue().getCallButtonText());
        // если установлен тип взаиподейтвия и нужно действие
        if( getEditedEntity().getIteractionType() != null )
            if( iteractionTypeField.getValue().getCallForm() != null )
                if(iteractionTypeField.getValue().getCallForm())
                    buttonCallAction.setVisible(true);
                else
                    buttonCallAction.setVisible(false);
    }

    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private Metadata metadata;

    public void callActionEntity() {
        String calledClass = getEditedEntity().getIteractionType().getCallClass();
        // еслп установлено разрешение в Iteraction показать кнопку и установить на ней надпсит
        if( getEditedEntity().getIteractionType() != null)
            if( getEditedEntity().getIteractionType().getCallForm() != null )
                if(!getEditedEntity().getIteractionType().getCallForm()) {
                }
                else {
                    screenBuilders.editor(metadata.getClassNN(calledClass).getJavaClass(), this)
                            .newEntity()
                            .withScreenId(calledClass + ".edit")
                            .withLaunchMode(OpenMode.NEW_TAB)
                            .build()
                            .show();
        }
    }

/*    @Subscribe("buttonAddNewIteraction")
    public void onButtonAddNewIteractionClick(Button.ClickEvent event) {
        screenBuilders.editor( Iteraction.class, this )
                .newEntity()
                .withScreenId( getEditedEntity().getIteractionType().getCallClass() )
                .withLaunchMode( OpenMode.NEW_TAB )
                .build()
                .show();
    } */

    public void addNewIteraction() {
        String classIL = "itpearls_" + getEditedEntity().getClass().getSimpleName() + ".edit";

        closeWithCommit();

        screenBuilders.editor( IteractionList.class, this )
                .newEntity()
                .withScreenId( classIL )
                .withLaunchMode( OpenMode.NEW_TAB )
                .withInitializer( e -> {
                    e.setCandidate(this.getEditedEntity().getCandidate());
                    e.setVacancy(this.getEditedEntity().getVacancy());
                    e.setCompanyDepartment(this.getEditedEntity().getCompanyDepartment());
                })
                .build()
                .show();
    }
}
