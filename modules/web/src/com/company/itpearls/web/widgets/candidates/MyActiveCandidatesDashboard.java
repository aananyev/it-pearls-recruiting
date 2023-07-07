package com.company.itpearls.web.widgets.candidates;

import com.company.itpearls.entity.IteractionList;
import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.entity.OpenPosition;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@UiController("itpearls_MyActiveCandidatesDashboard")
@UiDescriptor("my-active-candidates-dashboard.xml")
@DashboardWidget(name = "My Active Candidateses")
public class MyActiveCandidatesDashboard extends ScreenFragment {
    @Inject
    private CollectionLoader<IteractionList> iteractionListsDl;
    @Inject
    private UserSession userSession;
    @Inject
    private CollectionContainer<IteractionList> iteractionListsDc;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private ScrollBoxLayout candidatesScrollBox;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private DataManager dataManager;

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        initInteractionListDataContainer();
        initCandidatesList();
    }

    private void initCandidatesList() {
        Set<JobCandidate> jobCandidateSet = new HashSet<>();

        for (IteractionList interactionList : iteractionListsDc.getItems()) {
            jobCandidateSet.add(interactionList.getCandidate());
        }

        for (JobCandidate jobCandidate : jobCandidateSet) {
            HBoxLayout hBoxLayout = uiComponents.create(HBoxLayout.class);
            hBoxLayout.setWidthAuto();
            hBoxLayout.setHeightAuto();
            hBoxLayout.setSpacing(true);

            LinkButton candidateLinkButton = uiComponents.create(LinkButton.class);
            candidateLinkButton.setAlignment(Component.Alignment.MIDDLE_LEFT);
            candidateLinkButton.setCaption(jobCandidate.getFullName()
                    + " / "
                    + jobCandidate.getPersonPosition().getPositionRuName());

            candidateLinkButton.addClickListener(e -> {
                screenBuilders.editor(JobCandidate.class, this)
                        .editEntity(jobCandidate)
                        .build()
                        .show();
            });

            Set<OpenPosition> caseClosedOpenPosition = getCaseClosedOpenPosition(jobCandidate);
//            Set<OpenPosition> processedOpenPosition = getProcessedOpenPosition(jobCandidate);
//            Set<OpenPosition> opportunityOpenPosition = getOpportunityOpenPosition(jobCandidate);

            hBoxLayout.add(candidateLinkButton);

            ScrollBoxLayout scrollBoxLayout = uiComponents.create(ScrollBoxLayout.class);
            scrollBoxLayout.setWidthAuto();
            scrollBoxLayout.setSpacing(true);
            scrollBoxLayout.setOrientation(HasOrientation.Orientation.HORIZONTAL);
            scrollBoxLayout.setScrollBarPolicy(ScrollBoxLayout.ScrollBarPolicy.HORIZONTAL);

            for (OpenPosition openPosition : caseClosedOpenPosition) {
                LinkButton retLinkButton = uiComponents.create(LinkButton.class);
                retLinkButton.setCaption(openPosition.getProjectName().getProjectName().substring(0, 20));
                retLinkButton.setDescription(openPosition.getVacansyName());
                retLinkButton.setWidthAuto();
                retLinkButton.setStyleName("text-block-gradient-red");
                retLinkButton.setAlignment(Component.Alignment.MIDDLE_LEFT);

                scrollBoxLayout.add(retLinkButton);
            }

            hBoxLayout.add(scrollBoxLayout);

            candidatesScrollBox.add(hBoxLayout);
        }
    }

    private Set<OpenPosition> getCaseClosedOpenPosition(JobCandidate jobCandidate) {
        String QUERY_CASE_CLOSED_OPEN_POSITION
                = "select e " +
                "from itpearls_IteractionList e " +
                "where e.iteractionType.signEndCase = true and e.candidate = :candidate";

        List<IteractionList> caseClosedInteraction = dataManager.load(IteractionList.class)
                .query(QUERY_CASE_CLOSED_OPEN_POSITION)
                .parameter("candidate", jobCandidate)
                .view("iteractionList-view")
                .list();

        Set<OpenPosition> retOpenPosition = new HashSet<>();

        for (IteractionList iteractionList : caseClosedInteraction) {
            retOpenPosition.add(iteractionList.getVacancy());
        }

        return retOpenPosition;
    }

    private void initInteractionListDataContainer() {
        iteractionListsDl.setParameter("daysBetween", 7); // 7 дней
        iteractionListsDl.setParameter("recrutier", userSession.getUser().getLogin());
        iteractionListsDl.removeParameter("personPosition");

        iteractionListsDl.load();
    }
}