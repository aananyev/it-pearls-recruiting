package com.company.itpearls.web.screens.jobcandidate.jobcandidatepartners;

import com.company.itpearls.core.PartnerPersonService;
import com.company.itpearls.core.StdImage;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.Partners;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.company.itpearls.web.screens.jobcandidate.JobCandidateEdit;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
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
}