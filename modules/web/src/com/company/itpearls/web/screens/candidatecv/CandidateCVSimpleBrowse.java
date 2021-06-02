package com.company.itpearls.web.screens.candidatecv;

import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.CandidateCV;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.Date;

@UiController("itpearls_CandidateCVSimple.browse")
@UiDescriptor("candidate-cv-simple-browse.xml")
@LookupComponent("candidateCVsTable")
@LoadDataBeforeShow
public class CandidateCVSimpleBrowse extends StandardLookup<CandidateCV> {

    @Inject
    private CollectionContainer<CandidateCV> candidateCVsDc;
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

    public void setSelectedCandidate(JobCandidate entity) {
        candidateCVsDl.setParameter("candidate", entity);
        candidateCVsDl.load();
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        setButtonActions();
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
                    e.setOwner(userSession.getUser());
                    e.setResumePosition(candidateCVsTable.getSingleSelected().getResumePosition());
                    e.setTextCV(candidateCVsTable.getSingleSelected().getTextCV());
                    e.setLetter(candidateCVsTable.getSingleSelected().getLetter());
                    e.setLinkOriginalCv(candidateCVsTable.getSingleSelected().getLinkOriginalCv());
                    e.setDatePost(new Date());
                })
                .build()
                .show();

        candidateCVsDl.load();
    }
}