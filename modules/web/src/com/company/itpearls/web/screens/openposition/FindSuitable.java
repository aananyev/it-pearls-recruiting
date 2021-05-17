package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.core.PdfParserService;
import com.company.itpearls.entity.*;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import java.util.ArrayList;
import java.util.List;

@UiController("itpearls_FindSuitable")
@UiDescriptor("find-suitable.xml")
public class FindSuitable extends StandardLookup<OpenPosition> {
    @Inject
    private CollectionLoader<OpenPosition> openPositionDl;

    private JobCandidate jobCandidate = null;
    protected Integer numberCounter = 0;
    @Inject
    private DataGrid<OpenPosition> suitableCheckDataGrid;
    @Inject
    private DataManager dataManager;
    @Inject
    private PdfParserService pdfParserService;
    @Inject
    private Dialogs dialogs;
    @Inject
    private HBoxLayout suitableCheckHBox;
    @Inject
    private Label<String> candidateNameLabel;
    @Inject
    private Label<String> positionNameLabel;
    @Inject
    private VBoxLayout suitableCheckVBox;
    @Inject
    private LookupPickerField<Position> jobPositionLookupPickerField;

    @Subscribe
    public void onInit(InitEvent event) {

        DataGrid.ButtonRenderer<OpenPosition> suitableCheckDataGridRelevanceRenderer = suitableCheckDataGrid.createRenderer(DataGrid.ButtonRenderer.class);
        suitableCheckDataGridRelevanceRenderer.setRendererClickListener(clickableTextRendererClickEvent -> {
        });
        suitableCheckDataGrid.getColumn("relevance").setRenderer(suitableCheckDataGridRelevanceRenderer);
    }

    @Install(to = "suitableCheckDataGrid.relevance", subject = "columnGenerator")
    private String suitableCheckDataGridRelevanceColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        List<CandidateCV> candidateCVs = dataManager.load(CandidateCV.class)
                .query("select e from itpearls_CandidateCV e where e.candidate = :candidate")
                .parameter("candidate", jobCandidate)
                .list();

        List<SkillTree> skillTrees = new ArrayList<>();

        if (candidateCVs != null) {
            for (CandidateCV candidateCV : candidateCVs) {
                List<SkillTree> st = new ArrayList<>();

                if (candidateCV.getTextCV() != null) {
                    st = pdfParserService.parseSkillTree(candidateCV.getTextCV());
                }

                for (SkillTree cv : st) {
                    for (SkillTree cv1 : skillTrees) {
                        if (cv1.equals(cv)) {
                            st.remove(cv);
                        }
                    }
                }

                if (st != null) {
                    skillTrees.addAll(st);
                }
            }
        }

        List<SkillTree> skillTreesJD = new ArrayList<>();

        if (event.getItem().getComment() != null) {
            skillTreesJD = pdfParserService.parseSkillTree(event.getItem().getComment());
        }

        Integer counter = 0;

        for (SkillTree skillTree : skillTreesJD) {
            for (SkillTree st : skillTrees) {
                if (skillTree.equals(st))
                    counter++;
            }
        }

        Integer percent = counter * 100 / skillTrees.size();

        return percent.toString() + "%";
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (jobCandidate != null) {
            if (jobCandidate.getPersonPosition() != null) {
                openPositionDl.setParameter("positionType", this.jobCandidate.getPersonPosition());
            } else {
                openPositionDl.removeParameter("positionType");
            }
        } else {
            openPositionDl.removeParameter("positionType");
        }

        openPositionDl.load();

        candidateNameLabel.setValue(jobCandidate.getFullName());
        positionNameLabel.setValue(jobCandidate.getPersonPosition().getPositionEnName()
                + " / "
                + jobCandidate.getPersonPosition().getPositionRuName());

        jobPositionLookupPickerField.setValue(jobCandidate.getPersonPosition());
    }

    @Install(to = "suitableCheckDataGrid.number", subject = "columnGenerator")
    private String suitableCheckDataGridNumberColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        return (++numberCounter).toString();
    }

    public void setJobCandidate(JobCandidate jobCandidate) {
        this.jobCandidate = jobCandidate;
    }

    public JobCandidate getJobCandidate() {
        return this.jobCandidate;
    }

/*    public DataGrid getSuitableCheckDataGrid() {
        return suitableCheckDataGrid;
    } */

    public HBoxLayout getSuitableCheckHBox() {
        return suitableCheckHBox;
    }

    public VBoxLayout getSuitableCheckVBox() {
        return suitableCheckVBox;
    }

    public void rescanSuitable() {
        openPositionDl.setParameter("positionType", jobPositionLookupPickerField.getValue());
        numberCounter = 0;
        openPositionDl.load();
    }
}