package com.company.itpearls.web.screens.candidatecv.candidatecvpartners;

import com.company.itpearls.core.PartnerPersonService;
import com.company.itpearls.entity.*;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.QueryUtils;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.components.SuggestionPickerField;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.company.itpearls.web.screens.candidatecv.CandidateCVEdit;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

@UiController("itpearls_CandidateCVPartners.edit")
@UiDescriptor("candidate-cv-partners-edit.xml")
public class CandidateCVPartnersEdit extends CandidateCVEdit {

    @Inject
    private LookupPickerField<Partners> partnersLookupPickerField;
    @Inject
    private PartnerPersonService partnerPersonService;
    @Inject
    private DataManager dataManager;
    @Inject
    private SuggestionPickerField<JobCandidate> candidateField;
    @Inject
    private CheckBox onlyMySubscribeCheckBox;
    @Inject
    private UserSession userSession;
    @Inject
    private CollectionLoader<OpenPosition> openPosDl;

    @Subscribe
    public void onAfterShow4(AfterShowEvent event) {
        onlyMySubscribeCheckBox.setVisible(false);
        onlyMySubscribeCheckBox.setValue(false);

        setPartners();
        candidateFieldSearchExecutor();
    }

    @Subscribe
    public void onBeforeShow1(BeforeShowEvent event) {
        setOpenPositionDl();
    }

    private void setPartners() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            partnersLookupPickerField.setValue(partnerPersonService.getMyPartner());
        }
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

    private void setOpenPositionDl() {
        openPosDl.setParameter("userlogin", userSession.getUser().getLogin());
        openPosDl.setParameter("currDate", new Date());
        openPosDl.load();
    }
}