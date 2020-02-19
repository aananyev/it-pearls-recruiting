package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.entity.*;
import com.haulmont.cuba.core.app.EmailService;
import com.haulmont.cuba.core.global.EmailInfo;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import org.jsoup.Jsoup;
import java.util.*;

import javax.inject.Inject;
import java.util.*;

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
    private EmailService emailService;
    @Inject
    private Notifications notifications;
    @Inject
    private LookupField priorityField;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (!PersistenceHelper.isNew(getEditedEntity())) {
            jobCandidatesDl.setParameter("candidatePersonPosition", getEditedEntity().getPositionType().getPositionRuName());
            recrutiesTasksesDl.setParameter("openPosition", getEditedEntity().getVacansyName());
        } else {
            jobCandidatesDl.removeParameter("candidatePersonPosition");
            recrutiesTasksesDl.removeParameter("openPosition");
        }

        jobCandidatesDl.load();
    }

    @Subscribe("companyDepartamentField")
    public void onCompanyDepartamentFieldValueChange(HasValue.ValueChangeEvent<CompanyDepartament> event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            if (getEditedEntity().getCompanyDepartament().getDepartamentRuName() != null)
                getEditedEntity().setCompanyName(getEditedEntity().getCompanyDepartament().getCompanyName());
        }
    }

    @Subscribe("companyNameField")
    public void onCompanyNameFieldValueChange(HasValue.ValueChangeEvent<Company> event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            if (getEditedEntity().getCompanyName().getCityOfCompany() != null)
                getEditedEntity().setCityPosition(getEditedEntity().getCompanyName().getCityOfCompany());
        }
    }

    @Subscribe("projectNameField")
    public void onProjectNameFieldValueChange(HasValue.ValueChangeEvent<Project> event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            if (getEditedEntity().getProjectName().getProjectDepartment() != null)
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
        if (PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setOpenClose(false);
        }
    }

    @Subscribe
    public void onAfterClose(AfterCloseEvent event) {
        sendMessage();
    }

    private void sendMessage() {
        // по почте
        OpenPosition openPosition = getEditedEntity();

        // нотификация
        if (PersistenceHelper.isNew(getEditedEntity())) {
            // пошлем по почте
            EmailInfo emailInfo = new EmailInfo("alan@itpearls.ru",
                    openPosition.getVacansyName(),
                    null, "com/company/itpearls/templates/create_new_pos.txt",
                    Collections.singletonMap("openPosition", openPosition));

            emailService.sendEmailAsync(emailInfo);
            // высплывающее сообщение
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption("Открыта новая позиции:" )
                    .withDescription( getEditedEntity().getVacansyName() )
                    .show();
        } else {
            String bodyMessage= Jsoup.parse( openPosition.getComment() ).text();

            EmailInfo emailInfo = new EmailInfo("alan@itpearls.ru",
                    openPosition.getVacansyName(),
                    null, "com/company/itpearls/templates/edit_open_pos.txt",
//                    Collections.singletonMap("openPosition", openPosition));
                    Collections.singletonMap("bodyMessage", bodyMessage ));

            emailService.sendEmailAsync(emailInfo);

            // всплывающее сообщение
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption("Изменение описания позиции:" )
                    .withDescription( getEditedEntity().getVacansyName() )
                    .show();
        }
    }

    @Subscribe
    public void onInit(InitEvent event) {
        Map<String, Integer> priorityMap = new LinkedHashMap<>();

        priorityMap.put("Paused", 0);
        priorityMap.put("Low", 1);
        priorityMap.put("Normal", 2);
        priorityMap.put("High", 3);
        priorityMap.put("Critical", 4);

        priorityField.setOptionsMap(priorityMap);

    }

    @Install(to = "priorityField", subject = "optionIconProvider")
    private String priorityFieldOptionIconProvider(Integer integer) {

        String icon = null;

        switch ( integer ) {
            case 0: //"Paused"
                icon = "icons/remove.png";
                break;
            case 1: //"Low"
                icon = "icons/traffic-lights_blue.png";
                break;
            case 2: //"Normal"
                icon = "icons/traffic-lights_green.png";
                break;
            case 3: //"High"
                icon = "icons/traffic-lights_yellow.png";
                break;
            case 4: //"Critical"
                icon = "icons/traffic-lights_red.png";
                break;
        }

        return icon;
    }
}
