package com.company.itpearls.web.screens.iteractionlist;

import com.company.itpearls.entity.*;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.model.InstanceLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.core.global.Metadata;

import javax.inject.Inject;
import javax.swing.text.html.parser.Entity;
import java.math.BigDecimal;
import java.util.Date;

@UiController("itpearls_IteractionList.edit")
@UiDescriptor("iteraction-list-edit.xml")
@EditedEntityContainer("iteractionListDc")
@LoadDataBeforeShow
public class IteractionListEdit extends StandardEditor<IteractionList> {
    @Inject
    private CollectionContainer<JobCandidate> candidatesDc;
    @Inject
    private DataManager iteractionListEditDataManager;
    @Inject
    private UserSession userSession;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private Dialogs dialogs;
    @Inject
    private Notifications notifications;
    @Inject
    private Screens screens;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private InstanceLoader<IteractionList> iteractionListDl;
    @Inject
    private InstanceContainer<IteractionList> iteractionListDc;
    @Inject
    private Metadata metadata;

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
        if(getEditedEntity().getVacancy().getProjectName() != null)
            getEditedEntity().setProject(getEditedEntity().getVacancy().getProjectName());
    }

    @Subscribe("projectField")
    public void onProjectFieldValueChange(HasValue.ValueChangeEvent<Project> event) {
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
    public void onAfterClose(AfterCloseEvent event) {
        dialogs.createOptionDialog()
                .withCaption("Внимание")
                .withMessage("Создать новую запись?")
                .withActions(
                        new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY)
                        .withHandler(e -> {
                            createNewField(this.getEditedEntity());
                        }),
                        new DialogAction(DialogAction.Type.NO)
                ).show();
    }

    // создать новый экран
    private void createNewField(IteractionList entity) {
        JobCandidate setJobCandidate = getEditedEntity().getCandidate();
        OpenPosition vacansy = entity.getVacancy();
        Project project = entity.getProject();
        String communicationMethod = entity.getCommunicationMethod();
        Iteraction itercation = entity.getIteractionType();
        CompanyDepartament departament = entity.getCompanyDepartment();

        IteractionList newItercation = metadata.create(IteractionList.class);

        screenBuilders.editor(IteractionList.class, this)
                .editEntity(newItercation)
                .withInitializer( iteractionList -> {
                    newItercation.setCandidate(setJobCandidate);
                    newItercation.setVacancy(vacansy);
                    newItercation.setProject(project);
                    newItercation.setCommunicationMethod(communicationMethod);
//                    iteractionList.setIteractionType(itercation);
                    newItercation.setCompanyDepartment(departament);

                    iteractionListEditDataManager.commit(newItercation);
                } )
                .withScreenClass(IteractionListEdit.class)
                .build()
                .show();
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

//    @Inject
//    private CollectionLoader<JobCandidate> personPositionLc;

// После изменения имени кандидата надо заполнить поле специализация кандидата
    protected void setCandidateSpecialisation() {
//       String personPos = iteractionListEditDataManager.loadValues("select b.position_ru_name " +
//                "from itpearls_job_candidate a " +
//                "inner join itpearls_position b on a.person_position_id = b.id " +
//                "where a.full_name='Королев Сергей'").one().toString();
//        getEditedEntity().setComment(personPos); */
//            getEditedEntity().setCurrentJobPosition(getEditedEntity().getCandidate().getPersonPosition());
    }
}
