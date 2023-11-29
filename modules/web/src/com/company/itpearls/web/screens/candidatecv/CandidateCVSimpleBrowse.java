package com.company.itpearls.web.screens.candidatecv;

import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.CandidateCV;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.Date;

@UiController("itpearls_CandidateCVSimple.browse")
@UiDescriptor("candidate-cv-simple-browse.xml")
@LookupComponent("candidateCVsTable")
@LoadDataBeforeShow
public class CandidateCVSimpleBrowse extends StandardLookup<CandidateCV> {

    @Inject
    private CollectionLoader<CandidateCV> candidateCVsDl;
    @Inject
    private Button copyCVButton;
    @Inject
    private DataGrid<CandidateCV> candidateCVsTable;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private UserSession userSession;
    @Inject
    private Label<String> recrutierLabel;
    @Inject
    private Label<String> vacancyNameLabel;
    @Inject
    private Label<String> candidateLabel;

    JobCandidate jobCandidate = null;
    @Inject
    private Label<String> candidatePositionEnLabel;
    @Inject
    private Label<String> candidatePositionLabel;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private Screens screens;
    @Inject
    private Button viewOriginalCVButton;

    public void setSelectedCandidate(JobCandidate entity) {
        candidateCVsDl.setParameter("candidate", entity);
        candidateCVsDl.load();
    }

    public void setJobCandidate(JobCandidate jobCandidate) {
        this.jobCandidate = jobCandidate;
    }

    public JobCandidate getJobCandidate() {
        return jobCandidate;
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        setButtonActions();
        setCandidateLabel(event);
    }

    private void setCandidateLabel(BeforeShowEvent event) {
        if (jobCandidate != null) {
            candidateLabel.setValue(jobCandidate.getFullName());
            candidatePositionEnLabel.setValue(jobCandidate.getPersonPosition().getPositionEnName());
            candidatePositionLabel.setValue(jobCandidate.getPersonPosition().getPositionRuName());
        }
    }

    private void setButtonActions() {
        copyCVButton.setEnabled(false);

        candidateCVsTable.addSelectionListener(e -> {
            if(e.getSelected() == null) {
                copyCVButton.setEnabled(false);
            } else {
                copyCVButton.setEnabled(true);

                if(candidateCVsTable.getSingleSelected().getOwner() != null) {
                    recrutierLabel.setValue(candidateCVsTable
                            .getSingleSelected()
                            .getOwner()
                            .getName());

                    recrutierLabel.setVisible(true);
                }

                if(candidateCVsTable.getSingleSelected().getToVacancy() != null) {
                    vacancyNameLabel.setValue(candidateCVsTable
                            .getSingleSelected()
                            .getToVacancy()
                            .getVacansyName());

                    vacancyNameLabel.setVisible(true);
                }
            }
        });
    }

    public void copyCandidateCVButton() {
        screenBuilders.editor(CandidateCV.class, this)
                .newEntity()
                .withScreenClass(CandidateCVEdit.class)
                .withInitializer(e -> {
                    e.setCandidate(candidateCVsTable.getSingleSelected().getCandidate());
                    e.setOwner((ExtUser) userSession.getUser());
                    e.setResumePosition(candidateCVsTable.getSingleSelected().getResumePosition());
                    e.setTextCV(candidateCVsTable.getSingleSelected().getTextCV());
                    e.setLetter(candidateCVsTable.getSingleSelected().getLetter());
                    e.setLinkOriginalCv(candidateCVsTable.getSingleSelected().getLinkOriginalCv());
                    e.setDatePost(new Date());
                })
                .withAfterCloseListener(ee -> {
                    candidateCVsDl.load();
                    candidateCVsTable.repaint();
                })
                .build()
                .show();

        candidateCVsDl.load();
    }

    @Install(to = "candidateCVsTable.candidateOriginalCVColumn", subject = "columnGenerator")
    private Component candidateCVsTableCandidateOriginalCVColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        Link link = uiComponents.create(Link.NAME);

        if (event.getItem().getLinkOriginalCv() != null) {
            String url = event.getItem().getLinkOriginalCv();

            link.setUrl(url);
            link.setCaption("Оригинальное CV");
            link.setTarget("_blank");
            link.setWidthAuto();
            link.setVisible(true);
        } else {
            link.setVisible(false);
        }

        return link;
    }

    @Install(to = "candidateCVsTable.candidateITPearlsCVColumn", subject = "columnGenerator")
    private Component candidateCVsTableCandidateITPearlsCVColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        Link link = uiComponents.create(Link.NAME);

        if (event.getItem().getLinkItPearlsCV() != null) {
            String url = event.getItem().getLinkItPearlsCV();

            link.setUrl(url);
            link.setCaption("CV IT Pearls");
            link.setTarget("_blank");
            link.setWidthAuto();
            link.setVisible(true);
        } else {
            link.setVisible(false);
        }

        return link;
    }

    @Subscribe("candidateCVsTable")
    public void onCandidateCVsTableSelection(DataGrid.SelectionEvent<CandidateCV> event) {
        if(candidateCVsTable.getSingleSelected() == null) {
            viewOriginalCVButton.setEnabled(false);
        } else {
            viewOriginalCVButton.setEnabled(true);
        }

    }



    public void viewOriginalCVButton() {
        ViewerTextScreen viewerTextScreen = screens.create(ViewerTextScreen.class);
        viewerTextScreen.setTextToArea(candidateCVsTable.getSingleSelected().getTextCV());
        viewerTextScreen.show();
    }
}