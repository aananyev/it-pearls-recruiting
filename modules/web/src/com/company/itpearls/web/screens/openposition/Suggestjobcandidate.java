package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.core.PdfParserService;
import com.company.itpearls.entity.*;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@UiController("itpearls_SuggestJobCandidate")
@UiDescriptor("SuggestJobCandidate.xml")
public class Suggestjobcandidate extends Screen {
    @Inject
    private DataGrid<CandidateCV> suitableCheckDataGrid;
    @Inject
    private DataManager dataManager;
    @Inject
    private PdfParserService pdfParserService;

    private OpenPosition openPosition = null;
    @Inject
    private CollectionLoader<CandidateCV> candidateCVDl;
    @Inject
    private LookupPickerField<Position> jobPositionLookupPickerField;
    @Inject
    private CheckBox useLocationCheckBox;
    @Inject
    private RichTextArea candidateCVRichTextArea;
    @Inject
    private Label<String> vacancyNameLabel;
    @Inject
    private UserSession userSession;

    @Subscribe
    public void onInit(InitEvent event) {
        DataGrid.ButtonRenderer<OpenPosition> suitableCheckDataGridRelevanceRenderer = suitableCheckDataGrid
                .createRenderer(DataGrid.ButtonRenderer.class);

        suitableCheckDataGridRelevanceRenderer.setRendererClickListener(clickableTextRendererClickEvent -> {
        });

        suitableCheckDataGrid.getColumn("relevance").setRenderer(suitableCheckDataGridRelevanceRenderer);
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (openPosition != null) {
            if (openPosition.getPositionType() != null) {
                candidateCVDl.setParameter("resumePosition", this.openPosition.getPositionType());
            } else {
                candidateCVDl.removeParameter("resumePosition");
            }
        } else {
            candidateCVDl.removeParameter("resumePosition");
        }

        candidateCVDl.load();

        jobPositionLookupPickerField.setValue(this.openPosition.getPositionType());
        useLocationCheckBox.setValue(false);
        vacancyNameLabel.setValue(openPosition.getVacansyName());

        setCityPosition();
    }

    private void setCityPosition() {
        if (useLocationCheckBox.getValue()) {
            candidateCVDl.setParameter("cityOfResidence", openPosition.getCityPosition());
        } else {
            candidateCVDl.removeParameter("cityOfResidence");
        }
        candidateCVDl.load();
    }

    @Subscribe("useLocationCheckBox")
    public void onUseLocationCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        setCityPosition();
    }

    @Install(to = "suitableCheckDataGrid.relevance", subject = "columnGenerator")
    private String suitableCheckDataGridRelevanceColumnGenerator(DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        List<SkillTree> skillsFromCV = pdfParserService.parseSkillTree(event.getItem().getTextCV());
        List<SkillTree> skillsFromJD = pdfParserService.parseSkillTree(openPosition.getComment());

        Integer counter = 0;

        Set<SkillTree> setSt = new HashSet<>(skillsFromJD);
        skillsFromJD.clear();
        skillsFromJD.addAll(setSt);

        Set<SkillTree> setStCV = new HashSet<>(skillsFromCV);
        skillsFromCV.clear();
        skillsFromCV.addAll(setStCV);

        for (SkillTree skillTree : skillsFromJD) {
            for (SkillTree st : skillsFromCV) {
                if (skillTree.equals(st))
                    counter++;
            }
        }

        String percent = (skillsFromJD.size() != 0 ? String.valueOf(counter * 100 / skillsFromJD.size()) : "...") + "%";


        return percent;

    }

    public List<SkillTree> rescanResume(CandidateCV candidateCV) {
        if (candidateCV.getTextCV() != null) {
            String inputText = Jsoup.parse(candidateCV.getTextCV()).text();
            List<SkillTree> skillTrees = pdfParserService.parseSkillTree(inputText);

            candidateCV.setSkillTree(skillTrees);

            return skillTrees;
        } else {
            return null;
        }
    }

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
    }

    public void rescanSuitable() {
        candidateCVDl.setParameter("resumePosition", jobPositionLookupPickerField.getValue());
        candidateCVDl.load();
    }

    @Subscribe("suitableCheckDataGrid")
    public void onSuitableCheckDataGridSelection(DataGrid.SelectionEvent<CandidateCV> event) {
        if (suitableCheckDataGrid.getSingleSelected() != null) {
            candidateCVRichTextArea.setValue(suitableCheckDataGrid
                    .getSingleSelected()
                    .getTextCV());
        }
    }


    private IteractionList getLastIteraction(JobCandidate jobCandidate) {
        if (jobCandidate.getIteractionList() != null) {
            IteractionList maxIteraction = null;

            List<IteractionList> iteractionLists = dataManager.load(IteractionList.class)
                    .query("select e from itpearls_IteractionList e where e.candidate = :candidate")
                    .view("iteractionList-view")
                    .parameter("candidate", jobCandidate)
                    .list();

            for (IteractionList iteractionList : iteractionLists) {
                if (maxIteraction == null)
                    maxIteraction = iteractionList;

                if (maxIteraction.getNumberIteraction().compareTo(iteractionList.getNumberIteraction()) < 0) {
                    maxIteraction = iteractionList;
                }
            }

            return maxIteraction;
        } else
            return null;
    }

    @Install(to = "suitableCheckDataGrid.lastIteraction", subject = "columnGenerator")
    private String suitableCheckDataGridLastIteractionColumnGenerator(DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        IteractionList iteractionList = getLastIteraction(event.getItem().getCandidate());

        String date = null;

        try {
            date = new SimpleDateFormat("dd-MM-yyyy").format(iteractionList.getDateIteraction());
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        String retStr = "";

        if (iteractionList != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(iteractionList.getDateIteraction());
            calendar.add(Calendar.MONTH, 1);

            Calendar calendar1 = Calendar.getInstance();

            if (calendar.after(calendar1)) {
                if (!iteractionList.getRecrutier().equals(userSession.getUser())) {
                    retStr = "button_table_red";
                } else {
                    retStr = "button_table_yellow";
                }
            } else {
                retStr = "button_table_green";
            }
        } else {
            retStr = "button_table_white";
        }

        return
                "<div class=\"" +
                        retStr
                        + "\">" +
                        date
                        + "</div>"
                ;
    }

    @Install(to = "suitableCheckDataGrid.toVacancy", subject = "descriptionProvider")
    private String suitableCheckDataGridToVacancyDescriptionProvider(CandidateCV candidateCV) {
        if (candidateCV.getToVacancy() != null) {
            return candidateCV.getToVacancy().getVacansyName();
        } else {
            return null;
        }
    }
}