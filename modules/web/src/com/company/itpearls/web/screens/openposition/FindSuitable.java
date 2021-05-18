package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.core.PdfParserService;
import com.company.itpearls.entity.*;
import com.company.itpearls.web.screens.skilltree.SkillTreeBrowseCheck;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@UiController("itpearls_FindSuitable")
@UiDescriptor("find-suitable.xml")
public class FindSuitable extends StandardLookup<OpenPosition> {
    @Inject
    private CollectionLoader<OpenPosition> openPositionDl;

    private JobCandidate jobCandidate = null;
    @Inject
    private DataGrid<OpenPosition> suitableCheckDataGrid;
    @Inject
    private DataManager dataManager;
    @Inject
    private PdfParserService pdfParserService;
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
    @Inject
    private RichTextArea commentCVRichTextArea;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private Notifications notifications;
    @Inject
    private ProgressBar progressBar;

    @Subscribe
    public void onInit(InitEvent event) {

        DataGrid.ButtonRenderer<OpenPosition> suitableCheckDataGridRelevanceRenderer = suitableCheckDataGrid
                .createRenderer(DataGrid.ButtonRenderer.class);

        suitableCheckDataGridRelevanceRenderer.setRendererClickListener(clickableTextRendererClickEvent -> {
            List<CandidateCV> candidateCVs = dataManager.load(CandidateCV.class)
                    .query("select e from itpearls_CandidateCV e where e.candidate = :candidate")
                    .parameter("candidate", jobCandidate)
                    .view("candidateCV-view")
                    .list();
            if(candidateCVs.size() == 1) {
                checkSkillFromJD(candidateCVs.get(0), clickableTextRendererClickEvent.getItem());
            }
        });

        suitableCheckDataGrid.getColumn("relevance").setRenderer(suitableCheckDataGridRelevanceRenderer);
    }

    public void checkSkillFromJD(CandidateCV candidateCV, OpenPosition item) {
        List<SkillTree> skillTrees = rescanResume(candidateCV);
//        String inputText = Jsoup.parse(candidateCV.getToVacancy().getComment()).text();
        String inputText = Jsoup.parse(item.getComment()).text();
        List<SkillTree> skillTreesFromJD = pdfParserService.parseSkillTree(inputText);

        if(candidateCV.getToVacancy() != null) {
            SkillTreeBrowseCheck s = screenBuilders.screen(this)
                    .withScreenClass(SkillTreeBrowseCheck.class)
                    .build();
            s.setCandidateCVSkills(skillTrees);
            s.setOpenPositionSkills(skillTreesFromJD);
            s.setTitle(item.getVacansyName());

            s.show();
        } else {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("ВНИМАНИЕ!")
                    .withDescription("Для проверки навыков кандидата по резюме " +
                            "\nнеобходимозаполнить поле \"Вакансия\".")
                    .show();
        }
    }

    public List<SkillTree> rescanResume(CandidateCV candidateCV) {
        if(candidateCV.getTextCV() != null) {
            String inputText = Jsoup.parse(candidateCV.getTextCV()).text();
            List<SkillTree> skillTrees = pdfParserService.parseSkillTree(inputText);

            candidateCV.setSkillTree(skillTrees);

            return skillTrees;
        } else {
            return null;
        }
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

                if(st != null) {
                    try {
                        for (SkillTree cv : st) {
                            for (SkillTree cv1 : skillTrees) {
                                if (cv1.equals(cv)) {
                                    st.remove(cv);
                                }
                            }
                        }
                    } catch (ConcurrentModificationException e) {
                        e.printStackTrace();
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
    public void onAfterShow(AfterShowEvent event) {
//    @Subscribe
//    public void onBeforeShow(BeforeShowEvent event) {
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

        progressBar.setVisible(false);
    }

    public void setJobCandidate(JobCandidate jobCandidate) {
        this.jobCandidate = jobCandidate;
    }

    public JobCandidate getJobCandidate() {
        return this.jobCandidate;
    }

    public HBoxLayout getSuitableCheckHBox() {
        return suitableCheckHBox;
    }

    public VBoxLayout getSuitableCheckVBox() {
        return suitableCheckVBox;
    }

    public void rescanSuitable() {
        openPositionDl.setParameter("positionType", jobPositionLookupPickerField.getValue());
        openPositionDl.load();
    }

    @Subscribe("suitableCheckDataGrid")
    public void onSuitableCheckDataGridSelection(DataGrid.SelectionEvent<OpenPosition> event) {
        commentCVRichTextArea.setValue(suitableCheckDataGrid.getSingleSelected().getComment());
    }

    @Install(to = "suitableCheckDataGrid.vacansyName", subject = "descriptionProvider")
    private String suitableCheckDataGridVacansyNameDescriptionProvider(OpenPosition openPosition) {
        return Jsoup.parse(openPosition.getVacansyName()).text();
    }

    @Install(to = "suitableCheckDataGrid.projectName", subject = "descriptionProvider")
    private String suitableCheckDataGridProjectNameDescriptionProvider(OpenPosition openPosition) {
        if(openPosition.getProjectName() != null) {
            if (openPosition.getProjectName().getProjectDescription() != null) {
                return Jsoup.parse(openPosition.getProjectName().getProjectDescription()).text();
            }
        }

        return null;
    }

    @Install(to = "suitableCheckDataGrid.priority", subject = "columnGenerator")
    private Icons.Icon suitableCheckDataGridPriorityColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {

        switch ((int) event.getItem().getPriority()) {
            case 0:
                return CubaIcon.ANGLE_DOUBLE_DOWN;
            case 1:
                return CubaIcon.ANGLE_DOWN;
            case 2:
                return CubaIcon.CIRCLE;
            case 3:
                return CubaIcon.ANGLE_UP;
            case 4:
                return CubaIcon.ANGLE_DOUBLE_UP;
        }

        return null;
    }

    @Install(to = "suitableCheckDataGrid.priority", subject = "styleProvider")
    private String suitableCheckDataGridPriorityStyleProvider(OpenPosition openPosition) {

        switch ((int) openPosition.getPriority()) {
            case 0:
                return "pic-center-large-grey";
            case 1:
                return "pic-center-large-blue";
            case 2:
                return "pic-center-large-green";
            case 3:
                return "pic-center-large-yellow";
            case 4:
                return "pic-center-large-red";
        }

        return null;
    }
}