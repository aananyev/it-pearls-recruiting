package com.company.itpearls.web.screens.hrmasters.suggestjobcandidates;

import com.company.itpearls.core.PdfParserService;
import com.company.itpearls.entity.*;
import com.company.itpearls.web.screens.candidatecv.CandidateCVEdit;
import com.company.itpearls.web.screens.iteractionlist.iteractionlistbrowse.IteractionListSimpleBrowse;
import com.company.itpearls.web.screens.skilltree.SkillTreeBrowseCheck;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.data.GroupInfo;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;
import jdk.internal.jline.internal.Nullable;
import org.jsoup.Jsoup;
import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;

@UiController("itpearls_SuggestJobCandidate")
@UiDescriptor("SuggestJobCandidate.xml")
public class Suggestjobcandidate extends Screen {
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
    private Label<String> vacancyNameLabel;
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
    private Screens screens;
    @Inject
    private Button viewIteractionListButton;
    @Inject
    private GroupTable<CandidateCV> suitableCheckDataGrid;

    private OpenPosition openPosition = null;
    @Inject
    private RichTextArea candidateCVRichTextArea;
    @Inject
    private UserSession userSession;

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

    @Subscribe
    public void onInit(InitEvent event) {
        setSuggestTableStyleProvider();
    }

    private void setSuggestTableStyleProvider() {
        suitableCheckDataGrid.setStyleProvider(new GroupTable.GroupStyleProvider<CandidateCV>() {
            @SuppressWarnings("unchecked")
            @Override
            public String getStyleName(GroupInfo info) {
                Object property = info.getProperty();
                JobCandidate jobCandidate = (JobCandidate) info.getPropertyValue(info.getProperty());
                return setLastInteractionRowStyleName(jobCandidate);
            }

            @Override
            public String getStyleName(CandidateCV candidateCV, @Nullable String property) {
                return setLastInteractionRowStyleName(candidateCV.getCandidate());
            }
        });
    }

    @Install(to = "suitableCheckDataGrid.blackRectangle", subject = "columnGenerator")
    private Component suitableCheckDataGridBlackRectangleColumnGenerator(CandidateCV candidateCV) {
        return setBatteryIndicator(candidateCV);
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

        suitableCheckDataGrid.addSelectionListener(e -> {
            if (e.getSelected() == null) {
                viewCandidateButton.setEnabled(false);
                viewCandidateCVButton.setEnabled(false);
                viewCandidateCheckSkillsButton.setEnabled(false);
                viewIteractionListButton.setEnabled(false);
            } else {
                viewCandidateCVButton.setEnabled(true);
                viewCandidateButton.setEnabled(true);
                viewCandidateCheckSkillsButton.setEnabled(true);
                viewIteractionListButton.setEnabled(true);
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

    @Install(to = "suitableCheckDataGrid.lastIteraction", subject = "columnGenerator")
    private Component suitableCheckDataGridLastIteractionColumnGenerator(CandidateCV candidateCV) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        Label retLabel = uiComponents.create(Label.class);

        retLabel.setValue(sdf.format(getLastIteraction(candidateCV.getCandidate()).getDateIteraction()));
        retLabel.setDescription(getLatInteractionDesciption(candidateCV));
        retLabel.setStyleName(setLastInteractionStyleName(candidateCV));

        return retLabel;
    }

    @Subscribe("suitableCheckDataGrid")
    public void onSuitableCheckDataGridSelection(Table.SelectionEvent<CandidateCV> event) {
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
                iteractionLists = jobCandidate.getIteractionList();
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

    private String setLastInteractionStyleName(CandidateCV candidateCV) {
        return setLastInteractionStyleName(candidateCV.getCandidate());
    }


    private String setLastInteractionRowStyleName(CandidateCV candidateCV) {
        return setLastInteractionRowStyleName(candidateCV.getCandidate());
    }

    private String setLastInteractionRowStyleName(JobCandidate jobCandidate) {
        IteractionList iteractionList = getLastIteraction(jobCandidate);

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
                    retStr = "row_table_red";
                } else {
                    retStr = "row_table_yellow";
                }
            } else {
                retStr = "row_table_green";
            }
        } else {
            retStr = "row_table_gray";
        }

        return retStr;
    }

    private String setLastInteractionStyleName(JobCandidate jobCandidate) {
        IteractionList iteractionList = getLastIteraction(jobCandidate);

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

        return retStr;
    }

    public void viewCandidateButton() {
        screenBuilders.editor(JobCandidate.class, this)
                .editEntity(suitableCheckDataGrid.getSingleSelected().getCandidate())
                .build()
                .show();
    }

    private Label setBatteryIndicator(CandidateCV item) {
        Label labelBattery = uiComponents.create(Label.NAME);

        String percentStr = getRelevancePercent(item);
        labelBattery.setDescription(percentStr);

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
            labelBattery.setStyleName("rating_battery_teal_3");
        } else if (percent < 85) {
            labelBattery.setStyleName("rating_battery_green_4");
        } else {
            labelBattery.setStyleName("rating_battery_blue_5");
        }

        return labelBattery;
    }

    private String getLatInteractionDesciption(CandidateCV candidateCV) {
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

        List<SkillTree> skillTreesFromJD = pdfParserService.parseSkillTree(openPosition.getComment());

        SkillTreeBrowseCheck s = screenBuilders.screen(this)
                .withScreenClass(SkillTreeBrowseCheck.class)
                .build();

        s.setCandidateCVSkills(skillTrees);
        s.setOpenPositionSkills(skillTreesFromJD);
        s.setTitle("Кандидат: " + suitableCheckDataGrid.getSingleSelected().getToVacancy().getVacansyName());
        s.setTitleToVacancy("Вакансия: " + openPosition.getVacansyName());

        s.show();
    }

    public void closeButton() {
        closeWithDefaultAction();
    }

    public void viewIteractionListButton() {
        IteractionListSimpleBrowse iteractionListSimpleBrowse = screens.create(IteractionListSimpleBrowse.class);
        iteractionListSimpleBrowse.setSelectedCandidate(suitableCheckDataGrid.getSingleSelected().getCandidate());
        screens.show(iteractionListSimpleBrowse);

    }

    public void stopSearchProcessButtonInvoke() {
    }

    public void putPersonelReserveButtonInvoke() {
    }

    public void basketUnselectedCandidatesButtonInvoke() {
    }

    public void deleteUnselectedCandidatesButtonInvoke() {
    }

    public void clearSearchResultButtonInvoke() {
    }

    public void stopAndCloseButtonInvoke() {
    }

    public void startSearchProcessButtonInvoke() {
    }
}