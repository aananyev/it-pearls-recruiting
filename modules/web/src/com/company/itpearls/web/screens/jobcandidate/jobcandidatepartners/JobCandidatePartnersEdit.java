package com.company.itpearls.web.screens.jobcandidate.jobcandidatepartners;

import com.company.itpearls.core.InteractionListService;
import com.company.itpearls.core.OpenPositionService;
import com.company.itpearls.core.PartnerPersonService;
import com.company.itpearls.core.StdImage;
import com.company.itpearls.entity.*;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.CollectionPropertyContainer;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.company.itpearls.web.screens.jobcandidate.JobCandidateEdit;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Date;

@UiController("itpearls_JobCandidatePartners.edit")
@UiDescriptor("job-candidate-partners-edit.xml")
public class JobCandidatePartnersEdit extends JobCandidateEdit {
    @Inject
    private UiComponents uiComponents;
    @Inject
    private LookupPickerField<Partners> partnersLookupPickerField;
    @Inject
    private PartnerPersonService partnerPersonService;
    @Inject
    private UserSession userSession;
    @Inject
    private CollectionLoader<OpenPosition> openPositionDl;
    @Inject
    private Button blockCandidateButton;
    @Inject
    private Metadata metadata;
    @Inject
    private InteractionListService interactionListService;
    @Inject
    private DataManager dataManager;
    @Inject
    private Notifications notifications;
    @Inject
    private OpenPositionService openPositionService;
    @Inject
    private CollectionPropertyContainer<IteractionList> jobCandidateIteractionDc;
    @Inject
    private DataContext dataContext;

    @Subscribe
    public void onAfterShow2(AfterShowEvent event) {
        setOpenPositionDl();
    }

    private void setOpenPositionDl() {
        openPositionDl.setParameter("login", userSession.getUser().getLogin());
        openPositionDl.setParameter("currentDate", new Date());
        openPositionDl.load();
    }

    private Resource setPartnersLogo(Partners partners) {
        Image retImage = uiComponents.create(Image.class);
        retImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        retImage.setWidth("30px");

        if (partners.getFileCompanyLogo() != null) {
            return retImage.createResource(FileDescriptorResource.class)
                    .setFileDescriptor(partners
                            .getFileCompanyLogo());
        } else {
            return retImage.createResource(ThemeResource.class).setPath(StdImage.NO_COMPANY);
        }
    }

    @Subscribe
    public void onBeforeShow1(BeforeShowEvent event) {
        blockCandidateButton.setVisible(false);
        partnersLookupPickerField.setOptionImageProvider(this::setPartnersLogo);
        setPartners();
    }

    private void setPartners() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            partnersLookupPickerField.setValue(partnerPersonService.getMyPartner());
        }
    }

    protected void addIteractionOfNewCandidate() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            IteractionListPartners iteractionList = metadata.create(IteractionListPartners.class);

            iteractionList.setCandidate(getEditedEntity());
            iteractionList.setPartner(partnerPersonService.getMyPartner());
            iteractionList.setRating(4);
            iteractionList.setDateIteraction(new Date());
            iteractionList.setNumberIteraction(interactionListService.getCountInteraction().add(BigDecimal.ONE));
            iteractionList.setRecrutier((ExtUser) userSession.getUser());
            iteractionList.setRecrutierName(userSession.getUser().getName());

            Iteraction iteraction = null;
            OpenPosition openPosition = null;

            try {
                iteraction = dataManager.load(Iteraction.class)
                        .query("select e from itpearls_Iteraction e where e.iterationName like :iteractionName")
                        .view("iteraction-view")
                        .parameter("iteractionName", "Новый контакт")
                        .cacheable(true)
                        .one();
            } catch (Exception e) {
                notifications.create(Notifications.NotificationType.ERROR)
                        .withCaption("SQL ERROR")
                        .withDescription("Нет взаимодействия \"Новый контакт\"")
                        .show();
            }

            try {
                openPosition = openPositionService.getOpenPositionDefault();
            } catch (Exception e) {
                notifications.create(Notifications.NotificationType.ERROR)
                        .withCaption("SQL ERROR")
                        .withDescription("Нет вакансии \"по умолчанию\" Default")
                        .show();
            }

            if (iteraction != null && openPosition != null) {
                iteractionList.setIteractionType(iteraction);
                iteractionList.setVacancy(openPosition);

                jobCandidateIteractionDc.getMutableItems().add(dataContext.merge(iteractionList));
            }
        }
    }
}