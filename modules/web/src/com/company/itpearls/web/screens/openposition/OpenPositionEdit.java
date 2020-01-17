package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.entity.*;
import com.haulmont.cuba.core.app.EmailService;
import com.haulmont.cuba.core.global.EmailInfo;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;

@UiController("itpearls_OpenPosition.edit")
@UiDescriptor("open-position-edit.xml")
@EditedEntityContainer("openPositionDc")
@LoadDataBeforeShow
public class OpenPositionEdit extends StandardEditor<OpenPosition> {
    @Inject
    private LookupPickerField<City> cityOpenPositionField;
    @Inject
    private LookupPickerField<CompanyDepartament> companyDepartamentField;
    @Inject
    private LookupPickerField<Company> companyNameField;
    @Inject
    private TextField<Integer> numberPositionField;
    @Inject
    private LookupPickerField<Position> positionTypeField;
    @Inject
    private LookupPickerField<Project> projectNameField;
    @Inject
    private TextField<String> vacansyNameField;
    @Inject
    private CollectionLoader<JobCandidate> jobCandidatesDl;
    @Inject
    private CollectionLoader<RecrutiesTasks> recrutiesTasksesDl;
    @Inject
    private Dialogs dialogs;
    @Inject
    private EmailService emailService;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if(!PersistenceHelper.isNew(getEditedEntity())) {
            jobCandidatesDl.setParameter("candidatePersonPosition", getEditedEntity().getPositionType().getPositionRuName());
            recrutiesTasksesDl.setParameter( "openPosition", getEditedEntity().getVacansyName() );
        } else {
            jobCandidatesDl.removeParameter("candidatePersonPosition");
            recrutiesTasksesDl.removeParameter( "openPosition" );
        }

        jobCandidatesDl.load();
    }

    @Subscribe("companyDepartamentField")
    public void onCompanyDepartamentFieldValueChange(HasValue.ValueChangeEvent<CompanyDepartament> event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            if( getEditedEntity().getCompanyDepartament().getDepartamentRuName() != null )
                getEditedEntity().setCompanyName(getEditedEntity().getCompanyDepartament().getCompanyName());
        }
    }

    @Subscribe("companyNameField")
    public void onCompanyNameFieldValueChange(HasValue.ValueChangeEvent<Company> event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            if( getEditedEntity().getCompanyName().getCityOfCompany() != null )
                getEditedEntity().setCityPosition(getEditedEntity().getCompanyName().getCityOfCompany());
        }
    }

    @Subscribe("projectNameField")
    public void onProjectNameFieldValueChange(HasValue.ValueChangeEvent<Project> event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            if( getEditedEntity().getProjectName().getProjectDepartment() != null )
            getEditedEntity().setCompanyDepartament(getEditedEntity().getProjectName().getProjectDepartment());
        }
    }

    @Subscribe("openClosePositionCheckBox")
    public void onOpenClosePositionCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (getEditedEntity().getOpenClose()) {
            cityOpenPositionField.setEditable(false);
            companyDepartamentField.setEditable(false);
            companyNameField.setEditable(false);
            numberPositionField.setEditable(false);
            positionTypeField.setEditable(false);
            projectNameField.setEditable(false);
            vacansyNameField.setEditable(false);
        } else {
            cityOpenPositionField.setEditable(true);
            companyDepartamentField.setEditable(true);
            companyNameField.setEditable(true);
            numberPositionField.setEditable(true);
            positionTypeField.setEditable(true);
            projectNameField.setEditable(true);
            vacansyNameField.setEditable(true);
        }
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
       if(PersistenceHelper.isNew(getEditedEntity())) {
           getEditedEntity().setOpenClose(false);
       }
    }

    @Override
      protected void commit(Action.ActionPerformedEvent event) {
        // If a new entity was saved to the database, ask a user about sending an email
        if( PersistenceHelper.isNew( getEditedEntity() ) )
          dialogs.createOptionDialog()
                    .withCaption("Email")
                    .withMessage("Send the news item by email?")
                    .withContentMode( ContentMode.HTML )
                    .withActions(
                            new Action[] {
                                    new DialogAction(DialogAction.Type.YES) {
                                        @Override
                                        public void actionPerform(Component component) {
                                            sendByEmail();
                                        }
                                    },
                                    new DialogAction(DialogAction.Type.NO)
                            })
                    .show();

      super.commit( event );
    }

    @Subscribe
    public void onAfterClose(AfterCloseEvent event) {
        // разослать оповещение об изменении позиции
        sendMessage();
        
    }

    private void sendMessage() {
        OpenPosition openPosition = getEditedEntity();

        EmailInfo   emailInfo = new EmailInfo( "alan@itpearls.ru",
                openPosition.getVacansyName(),
                null, "com/company/itpearls/templates/news_item.txt",
                Collections.singletonMap( "openPosition", openPosition ));

        emailService.sendEmailAsync( emailInfo );
    }

    // Queues an email for sending asynchronously
    private void sendByEmail() {
        OpenPosition openPosttion = getEditedEntity();

        EmailInfo emailInfo = new EmailInfo(
                getMailList(),                                  // получатели
                getSubject(),                                   // тема сообщения
                null, // the "from" address will be taken from the "cuba.email.fromAddress" app property
                getNewOpenPositionText(),                       // текст сообщения
                Collections.singletonMap( "OpenPosition", openPosttion ) // template parameters
        );

        emailService.sendEmailAsync(emailInfo);
    }

    private String getNewOpenPositionText() {
        // "com/company/demo/templates/news_item.txt"
        return "com/itpearls/core/open_position.txt";
    }

    private String getSubject() {
        return "Test message";
    }

    private String getMailList() {
        return "a.ananjev@gmail.com";
    }




}