package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.core.PdfParserService;
import com.company.itpearls.entity.*;
import com.company.itpearls.web.screens.candidatecv.CandidateCVEdit;
import com.company.itpearls.web.screens.jobcandidate.JobCandidateEdit;
import com.company.itpearls.web.screens.skilltree.SkillTreeBrowseCheck;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;

@UiController("itpearls_SuggestJobCandidate")
@UiDescriptor("SuggestJobCandidate.xml")
public class Suggestjobcandidate extends Screen {
    @Inject
    private DataGrid<CandidateCV> suitableCheckDataGrid;
    @Inject
    private DataManager dataManager;
    @Inject
    private PdfParserService pdfParserService;
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
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private Button viewCandidateButton;
    @Inject
    private Button viewCandidateCVButton;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private Button viewCandidateCheckSkillsButton;
    @Inject
    private CollectionContainer<Position> personPositionDc;

    private OpenPosition openPosition = null;
    @Inject
    private CollectionContainer<CandidateCV> candidateCVDc;

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
        setButtoncActions();
        setJobPositionLookupPickerField();
    }

    private void setJobPositionLookupPickerField() {
        try {
            Map<String, Position> positionsList = new LinkedHashMap<>();
            List<Position> positionList = dataManager.load(Position.class).list();

            for(Position position : positionList) {
                positionsList.put(position.getPositionRuName(), position);
            }

            jobPositionLookupPickerField.setOptionsMap(positionsList);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void setButtoncActions() {

        viewCandidateButton.setEnabled(false);
        viewCandidateCVButton.setEnabled(false);
        viewCandidateCheckSkillsButton.setEnabled(false);

        suitableCheckDataGrid.addSelectionListener(e -> {
            if (e.getSelected() == null) {
                viewCandidateButton.setEnabled(false);
                viewCandidateCVButton.setEnabled(false);
                viewCandidateCheckSkillsButton.setEnabled(false);
            } else {
                viewCandidateCVButton.setEnabled(true);
                viewCandidateButton.setEnabled(true);
                viewCandidateCheckSkillsButton.setEnabled(true);
            }
        });
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

    private String getRelevancePercent(CandidateCV candidateCV) {
        List<SkillTree> skillsFromCV = new ArrayList<>();
        List<SkillTree> skillsFromJD = new ArrayList<>();

        if (candidateCV.getTextCV() != null) {
            skillsFromCV = pdfParserService.parseSkillTree(candidateCV.getTextCV());
        }

        if (openPosition.getComment() != null) {
            skillsFromJD = pdfParserService.parseSkillTree(openPosition.getComment());
        }

        Integer counter = 0;

        if (skillsFromJD.size() != 0) {
            Set<SkillTree> setSt = new HashSet<>(skillsFromJD);
            skillsFromJD.clear();
            skillsFromJD.addAll(setSt);
        }

        if (skillsFromCV.size() != 0) {
            Set<SkillTree> setStCV = new HashSet<>(skillsFromCV);
            skillsFromCV.clear();
            skillsFromCV.addAll(setStCV);
        }

        if (skillsFromJD.size() != 0) {
            for (SkillTree skillTree : skillsFromJD) {
                if (skillsFromCV.size() != 0) {
                    for (SkillTree st : skillsFromCV) {
                        if (skillTree.equals(st))
                            counter++;
                    }
                }
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

            List<IteractionList> iteractionLists = new ArrayList<>();

            try {
                iteractionLists = dataManager.load(IteractionList.class)
                        .query("select e from itpearls_IteractionList e where e.candidate = :candidate")
                        .view("iteractionList-view")
                        .parameter("candidate", jobCandidate)
                        .list();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (iteractionLists.size() != 0) {
                for (IteractionList iteractionList : iteractionLists) {
                    if (maxIteraction == null)
                        maxIteraction = iteractionList;

                    if (maxIteraction.getNumberIteraction().compareTo(iteractionList.getNumberIteraction()) < 0) {
                        maxIteraction = iteractionList;
                    }
                }
            }

            return maxIteraction;
        } else
            return null;
    }

    @Subscribe("useLocationCheckBox")
    public void onUseLocationCheckBoxValueChange1(HasValue.ValueChangeEvent<Boolean> event) {
        if (event.getValue())
            candidateCVDl.setParameter("cityOfResidence", openPosition.getCityPosition());
        else
            candidateCVDl.removeParameter("cityOfResidence");

        candidateCVDl.load();
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

    public void viewCandidateButton() {
        screenBuilders.editor(JobCandidate.class, this)
                .withScreenClass(JobCandidateEdit.class)
                .editEntity(dataManager.load(JobCandidate.class)
                        .query("select e from itpearls_JobCandidate e where e = :candidate")
                        .view("jobCandidate-view")
                        .parameter("candidate", suitableCheckDataGrid.getSingleSelected().getCandidate())
                        .one()
                )
                .build()
                .show();
    }

    @Install(to = "suitableCheckDataGrid.blackRectangle", subject = "descriptionProvider")
    private String suitableCheckDataGridBlackRectangleDescriptionProvider(CandidateCV candidateCV) {
        return getRelevancePercent(candidateCV);
    }

    @Install(to = "suitableCheckDataGrid.blackRectangle", subject = "columnGenerator")
    private Component suitableCheckDataGridBlackRectangleColumnGenerator(DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        Label labelBattery = uiComponents.create(Label.NAME);

        String percentStr = getRelevancePercent(event.getItem());

        int percent = 0;

        try {
            percent = Integer.parseInt(percentStr.substring(0, percentStr.length() - 1));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        if (percent < 5) {
            labelBattery.setStyleName("rating_battery_grey_0");
        } else if (percent < 20) {
            labelBattery.setStyleName("rating_battery_red_1");
        } else if (percent < 45) {
            labelBattery.setStyleName("rating_battery_orange_2");
        } else if (percent < 60) {
            labelBattery.setStyleName("rating_battery_yellow_3");
        } else if (percent < 85) {
            labelBattery.setStyleName("rating_battery_green_4");
        } else {
            labelBattery.setStyleName("rating_battery_blue_5");
        }


        return labelBattery;
    }

    @Install(to = "suitableCheckDataGrid.lastIteraction", subject = "descriptionProvider")
    private String suitableCheckDataGridLastIteractionDescriptionProvider(CandidateCV candidateCV) {
        IteractionList iteractionList = getLastIteraction(candidateCV.getCandidate());
        String recrutierName = "";

        if (iteractionList != null) {
            if (iteractionList.getRecrutier() != null) {
                if (iteractionList.getRecrutier().getName() != null) {
                    recrutierName = iteractionList.getRecrutier().getName();
                }
            }
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");

        return iteractionList != null ?
                simpleDateFormat.format(iteractionList.getDateIteraction())
                        + "\n"
                        + iteractionList.getIteractionType().getIterationName()
                        + "\n"
                        + recrutierName : "";
    }

    public void viewCandidateCVButton() {
        screenBuilders.editor(CandidateCV.class, this)
                .withScreenClass(CandidateCVEdit.class)
                .editEntity(suitableCheckDataGrid.getSingleSelected())
                .build();
    }

    public void viewCandidateCheckSkillsButton() {
        List<SkillTree> skillTrees = pdfParserService.parseSkillTree(suitableCheckDataGrid
                .getSingleSelected()
                .getTextCV());
        List<SkillTree> skillTreesFromJD = pdfParserService.parseSkillTree(suitableCheckDataGrid
                .getSingleSelected()
                .getToVacancy()
                .getComment());

        SkillTreeBrowseCheck s = screenBuilders.screen(this)
                .withScreenClass(SkillTreeBrowseCheck.class)
                .build();

        s.setCandidateCVSkills(skillTrees);
        s.setOpenPositionSkills(skillTreesFromJD);
        s.setTitle(suitableCheckDataGrid.getSingleSelected().getToVacancy().getVacansyName());

        s.show();
    }
}