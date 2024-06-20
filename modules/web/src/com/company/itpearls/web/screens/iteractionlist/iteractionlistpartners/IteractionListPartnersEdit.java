package com.company.itpearls.web.screens.iteractionlist.iteractionlistpartners;

import com.company.itpearls.core.PartnerPersonService;
import com.company.itpearls.core.StdImage;
import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.entity.JobCandidatePartners;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.Partners;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.QueryUtils;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.company.itpearls.web.screens.iteractionlist.IteractionListEdit;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

@UiController("itpearls_IteractionListPartners.edit")
@UiDescriptor("iteraction-list-partners-edit.xml")
public class IteractionListPartnersEdit extends IteractionListEdit {
    @Inject
    private CollectionLoader<OpenPosition> openPositionsDl;
    @Inject
    private UserSession userSession;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private LookupPickerField partnersLookupPickerField;
    @Inject
    private PartnerPersonService partnerPersonService;
    @Inject
    private SuggestionPickerField<JobCandidate> candidateField;
    @Inject
    private DataManager dataManager;
    @Inject
    private CheckBox onlyMySubscribeCheckBox;

    @Subscribe
    public void onBeforeShow1(BeforeShowEvent event) {
        setOpenPositionDl();
//        partnersLookupPickerField.setOptionImageProvider(this::setPartnersLogo);
        setPartners();
    }

    protected void candidateFieldSearchExecutor() {
        candidateField.setSearchExecutor((searchString, searchParams) -> {
            searchString = QueryUtils.escapeForLike(searchString);

            List<JobCandidatePartners> jobCandidatePartners = dataManager.load(JobCandidatePartners.class)
                    .query("e.partners = :partner and " +
                            "lower(e.fullName) like lower(:searchString)")
                    .parameter("partner", partnerPersonService.getMyPartner())
                    .parameter("searchString", "%" + searchString + "%")
                    .view("jobCandidatePartners-view")
                    .cacheable(true)
                    .list();

            return jobCandidatePartners;
        });
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

    private void setPartners() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            partnersLookupPickerField.setValue(partnerPersonService.getMyPartner());
        }
    }

    private void setOpenPositionDl() {
        openPositionsDl.setParameter("login", userSession.getUser().getLogin());
        openPositionsDl.setParameter("currentDate", new Date());
        onlyMySubscribeCheckBox.setValue(false);
        openPositionsDl.load();
    }
}