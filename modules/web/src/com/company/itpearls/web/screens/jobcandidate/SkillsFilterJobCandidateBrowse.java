package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.core.PdfParserService;
import com.company.itpearls.entity.*;
import com.company.itpearls.web.StandartPrioritySkills;
import com.company.itpearls.web.screens.candidatecv.CandidateCVSimpleBrowse;
import com.company.itpearls.web.screens.fragments.OnlyTextFromFile;
import com.company.itpearls.web.screens.fragments.Onlytext;
import com.company.itpearls.web.screens.iteractionlist.IteractionListSimpleBrowse;
import com.company.itpearls.web.screens.openposition.OpenPositionEdit;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundTaskHandler;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.global.UserSession;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UiController("itpearls_SkillsFilterJobCandidate.browse")
@UiDescriptor("skills-filter-job-candidate-browse.xml")
@LookupComponent("jobCandidatesTable")
@LoadDataBeforeShow
public class SkillsFilterJobCandidateBrowse extends StandardLookup<JobCandidate> {
    private static final String QUERY_SKILL_TREE_GROUP
            = "select e " +
            "from itpearls_SkillTree e " +
            "where e.skillTree is null and " +
            "e in (select f.skillTree from itpearls_SkillTree f where f.skillTree = e) " +
            "order by e.skillName";
    private static final String QUERY_SKILL_TREE_ITEM
            = "select e from itpearls_SkillTree e where e.skillTree = :skillGroup";
    private static final String QUERY_GET_PERSONEL_RESERVE = "select e from itpearls_PersonelReserve e " +
            "where e.jobCandidate = :jobCandidate " +
            "and " +
            "(e.endDate > :currDate or e.endDate is null)";
    private static final String QUERY_GET_FROM_SUBSCRIBERS = "select e from itpearls_RecrutiesTasks e " +
            "where e.reacrutier = :reacrutier " +
            "and e.openPosition = :openPosition " +
            "and @dateAfter(e.endDate, now)";
    private static final String QUERY_GET_ALL_PERSONEL_RESERVE = "select e from itpearls_PersonelReserve e " +
            "where (e.endDate > :currDate or e.endDate is null)";

    private List<SkillTree> skillTreeGroup = new ArrayList<>();
    private HashMap<LinkButton, LinkButton> skillsPairAllToFilter = new HashMap<>();
    private HashMap<LinkButton, LinkButton> skillsPairFilterToAll = new HashMap<>();
    private HashMap<LinkButton, SkillTree> filter = new HashMap<>();
    private List<JobCandidate> selectedCandidates = new ArrayList<>();
    private HashMap<JobCandidate, CheckBox> personalReserveCheckBoxes = new HashMap<>();
//    private List<JobCandidate> removeFromTable = new ArrayList<>();

    private Boolean stopSearchProcess = false;
    private OpenPosition openPosition = null;

    @Inject
    private UiComponents uiComponents;
    @Inject
    private ScrollBoxLayout allSkillsScrollBox;
    @Inject
    private ScrollBoxLayout filterSkillsScrollBox;
    @Inject
    private DataManager dataManager;
    @Inject
    private UserSession userSession;
    @Inject
    private DataGrid<JobCandidate> jobCandidatesTable;
    @Inject
    private Screens screens;
    @Inject
    private Label<String> arrowLeft1;
    @Inject
    private Label<String> arrowLeft2;
    @Inject
    private Label<String> arrowLeft3;
    @Inject
    private Label<String> arrowRight1;
    @Inject
    private Label<String> arrowRight2;
    @Inject
    private Label<String> arrowRight3;
    @Inject
    private CollectionContainer<JobCandidate> jobCandidatesDc;
    @Inject
    private PdfParserService pdfParserService;
    @Inject
    private ProgressBar filterProgressbar;
    @Inject
    private BackgroundWorker backgroundWorker;
    @Inject
    private Label<String> progressLabel;
    @Inject
    private Notifications notifications;
    @Inject
    private Label<String> foundLabel;
    @Inject
    private Button buttonStop;
    @Inject
    private CollectionLoader<JobCandidate> jobCandidatesDl;
    private boolean stopScan = false;
    @Inject
    private LookupPickerField<Position> personPositionLookupPickerField;
    @Inject
    private LookupPickerField<City> cityLookupPickerField;
    @Inject
    private Button clearFilterButton;
    @Inject
    private HBoxLayout progressBarHBox;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private Button clearSearchResultButton;
    @Inject
    private Button startSearchProcessButton;
    @Inject
    private SuggestionPickerField findSkillsSuggestionPickerField;
    @Inject
    private Button stopAndCloseButton;
    @Inject
    private PopupButton loadFromOpenPositionPopupButton;
    @Inject
    private RadioButtonGroup filterMethodRadioButtonGroup;
    @Inject
    private Button openPositionViewButton;
    private OpenPosition selectedOpenPosition = null;
    @Inject
    private Label<String> counterSelectedCandidatesLabel;
    private Integer counter = 0;
    @Inject
    private Button deleteUnselectedCandidatesButton;
    @Inject
    private Button putPersonelReserveButton;
    @Inject
    private Dialogs dialogs;
    @Inject
    private Metadata metadata;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private DataContext dataContext;
    @Inject
    private Label<String> loadFromVacancyLabel;
    @Inject
    private Button basketUnselectedCandidatesButton;

    @Subscribe
    public void onInit(InitEvent event) {
        initFilterMethodRadioButtonGroup();
        setVisibleOfCandidates();
    }

    private void setVisibleOfCandidates() {
    }

    private void initFilterMethodRadioButtonGroup() {
        Map<String, Integer> filterMethodMap = new LinkedHashMap<>();

        filterMethodMap.put(messageBundle.getMessage("msgSearchAllFromMany"), 1);
        filterMethodMap.put(messageBundle.getMessage("msgSearchOneFromMany"), 0);

        filterMethodRadioButtonGroup.setOptionsMap(filterMethodMap);

        filterMethodRadioButtonGroup.setValue(1);
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        skillTreeGroup = getSkillTreeGroup();
        addSkillTreeGroupBoxes();
        candidateImageColumnRenderer();
        candidateLastInteractionColumnRenderer();
        initArrows();
    }

    private void initArrows() {
        int filterSize = getFilterSize();

        if (filterSize == 0) {
            arrowLeft1.setVisible(false);
            arrowLeft2.setVisible(false);
            arrowLeft3.setVisible(false);

            arrowRight1.setVisible(true);
            arrowRight2.setVisible(true);
            arrowRight3.setVisible(true);

            startSearchProcessButton.setEnabled(false);
        }

        if (filterSize > 0 && filterSize < skillsPairAllToFilter.size()) {
            arrowLeft1.setVisible(true);
            arrowLeft2.setVisible(true);
            arrowLeft3.setVisible(true);

            arrowRight1.setVisible(true);
            arrowRight2.setVisible(true);
            arrowRight3.setVisible(true);

            startSearchProcessButton.setEnabled(true);
        }

        if (filterSize == skillsPairAllToFilter.size()) {
            arrowLeft1.setVisible(true);
            arrowLeft2.setVisible(true);
            arrowLeft3.setVisible(true);

            arrowRight1.setVisible(false);
            arrowRight2.setVisible(false);
            arrowRight3.setVisible(false);

            startSearchProcessButton.setEnabled(true);
        }
    }

    private int getFilterSize() {
        int ret = 0;

        for (Map.Entry s : filter.entrySet()) {
            if (s.getValue() != null) {
                ret++;
            }
        }

        return ret;
    }

    private void candidateLastInteractionColumnRenderer() {
        DataGrid.ClickableTextRenderer<JobCandidate> jobCandidatesTableLastIteractionRenderer =
                jobCandidatesTable.createRenderer(DataGrid.ClickableTextRenderer.class);

        jobCandidatesTableLastIteractionRenderer.setRendererClickListener(clickableTextRendererClickEvent -> {
            IteractionListSimpleBrowse iteractionListSimpleBrowse = screens.create(IteractionListSimpleBrowse.class);
            iteractionListSimpleBrowse.setSelectedCandidate(clickableTextRendererClickEvent.getItem());
            screens.show(iteractionListSimpleBrowse);
        });

        jobCandidatesTable.getColumn("lastIteraction").setStyleProvider(e -> {
            IteractionList iteractionList = getLastIteraction(e);
            String retStr = "";

            if (iteractionList != null) {
                Calendar calendar = Calendar.getInstance();

                if (iteractionList.getDateIteraction() != null) {
                    calendar.setTime(iteractionList.getDateIteraction());
                }

                calendar.add(Calendar.MONTH, 1);

                Calendar calendar1 = Calendar.getInstance();

                if (calendar.after(calendar1)) {
                    if (iteractionList.getRecrutier() != null) {
                        if (!iteractionList.getRecrutier().equals(userSession.getUser())) {
                            retStr = "button_table_red";
                        } else {
                            retStr = "button_table_yellow";
                        }
                    } else {
                        retStr = "button_table_white";
                    }
                } else {
                    retStr = "button_table_green";
                }
            } else {
                retStr = "button_table_white";
            }

            return retStr;
        });

        jobCandidatesTable.getColumn("lastIteraction")
                .setRenderer(jobCandidatesTable.createRenderer(DataGrid.HtmlRenderer.class));

    }


    private IteractionList getLastIteraction(JobCandidate jobCandidate) {
        if (jobCandidate.getIteractionList() != null) {
            IteractionList maxIteraction = null;

            for (IteractionList iteractionList : jobCandidate.getIteractionList()) {
                if (maxIteraction == null)
                    maxIteraction = iteractionList;

                if (iteractionList.getNumberIteraction() != null) {
                    if (maxIteraction.getNumberIteraction().compareTo(iteractionList.getNumberIteraction()) < 0) {
                        maxIteraction = iteractionList;
                    }
                }
            }

            return maxIteraction;
        } else
            return null;
    }

    private GroupBoxLayout setSkillGroupBox(SkillTree skillTree) {
        GroupBoxLayout groupBoxLayout = uiComponents.create(GroupBoxLayout.class);

        groupBoxLayout.setStyleName("light");
        groupBoxLayout.setCollapsable(true);
        groupBoxLayout.setCaption(skillTree.getSkillName());
        groupBoxLayout.setDescription(skillTree.getComment() != null
                ? Jsoup.parse(skillTree.getComment()).text() : "");
        groupBoxLayout.setWidthFull();

        return groupBoxLayout;
    }

    private void addSkillTreeGroupBoxes() {
        for (SkillTree skillTree : skillTreeGroup) {
            // Левый
            GroupBoxLayout groupBoxLayoutLeft = setSkillGroupBox(skillTree);
            // Правый
            GroupBoxLayout groupBoxLayoutRight = setSkillGroupBox(skillTree);

            addSkillPairLabels(skillTree, groupBoxLayoutLeft, groupBoxLayoutRight);

            allSkillsScrollBox.add(groupBoxLayoutLeft);
            filterSkillsScrollBox.add(groupBoxLayoutRight);
        }
    }

    private FlowBoxLayout setFlowBoxLayout() {
        FlowBoxLayout flowBoxLayoutLeft = uiComponents.create(FlowBoxLayout.class);
        flowBoxLayoutLeft.setWidthAuto();
        flowBoxLayoutLeft.setSpacing(true);

        return flowBoxLayoutLeft;
    }

    private LinkButton setLinkButton(SkillTree st) {
        LinkButton label = uiComponents.create(LinkButton.class);
        label.setCaption(st.getSkillName());
        label.setStyleName(getStyleForSkillPriority(st));
        label.setVisible(true);

        return label;
    }

    private void addSkillPairLabels(SkillTree skillTreeGroup,
                                    GroupBoxLayout groupBoxLayoutLeft,
                                    GroupBoxLayout groupBoxLayoutRight) {
        List<SkillTree> skillTree = dataManager.load(SkillTree.class)
                .query(QUERY_SKILL_TREE_ITEM)
                .parameter("skillGroup", skillTreeGroup)
                .view("skillTree-view")
                .list();

        FlowBoxLayout flowBoxLayoutLeft = setFlowBoxLayout();
        FlowBoxLayout flowBoxLayoutRight = setFlowBoxLayout();

        if (skillTree.size() != 0) {
            for (SkillTree st : skillTree) {
                // левый
                LinkButton labelLeft = setLinkButton(st);
                LinkButton labelRight = setLinkButton(st);
                labelRight.setVisible(false);

                labelLeft.addClickListener(e -> {
                    reverseVisible(e);

                    SkillTree sTree = new SkillTree();
                    for (SkillTree s : skillTree) {
                        if (s.getSkillName().equals(e.getSource().getCaption())) {
                            sTree.setSkillName(s.getSkillName());
                            sTree.setComment(s.getComment());
                            sTree.setOpenPosition(s.getOpenPosition());
                            sTree.setSkillTree(s.getSkillTree());
                            sTree.setNotParsing(s.getNotParsing());
                            sTree.setPrioritySkill(s.getPrioritySkill());
                            sTree.setSpecialisation(s.getSpecialisation());
                            sTree.setFileImageLogo(s.getFileImageLogo());
                            sTree.setStyleHighlighting(s.getStyleHighlighting());
                            break;
                        }
                    }

                    filter.put(skillsPairAllToFilter.get(e.getSource()), sTree);
                    startSearchProcessButton.setEnabled(true);
                    initArrows();
                });

                labelRight.addClickListener(e -> {
                    reverseVisibleFilter(e);

                    SkillTree sTree = new SkillTree();
                    for (SkillTree s : skillTree) {
                        if (s.getSkillName().equals(e.getSource().getCaption())) {
                            filter.remove(e.getSource());
                            initArrows();
                        }
                    }
                });

                skillsPairAllToFilter.put(labelLeft, labelRight);
                skillsPairFilterToAll.put(labelRight, labelLeft);

                flowBoxLayoutLeft.add(labelLeft);
                flowBoxLayoutRight.add(labelRight);
            }

            groupBoxLayoutLeft.setVisible(true);
            groupBoxLayoutRight.setVisible(false);
        } else {
            groupBoxLayoutLeft.setVisible(false);
            groupBoxLayoutRight.setVisible(false);
        }

        groupBoxLayoutLeft.add(flowBoxLayoutLeft);
        groupBoxLayoutRight.add(flowBoxLayoutRight);
    }

    private void reverseVisibleFilter(Button.ClickEvent e) {
        e.getSource().setVisible(!e.getSource().isVisible());
        skillsPairFilterToAll.get(e.getSource()).setVisible(
                !skillsPairFilterToAll.get(e.getSource()).isVisible());

        skillsPairFilterToAll.get(e.getSource())
                .getParent()
                .getParent()
                .setVisible(skillsPairFilterToAll
                        .get(e.getSource())
                        .isVisible());

        if (!e.getSource().isVisible()) {
            Boolean flag = false;
            for (Component component : ((FlowBoxLayout) e.getSource().getParent()).getComponents()) {
                if (component.isVisible()) {
                    flag = true;

                    break;
                }
            }

            e.getSource()
                    .getParent()
                    .getParent()
                    .setVisible(flag);
        }
    }

    private void reverseVisible(Button.ClickEvent e) {
        e.getSource().setVisible(!e.getSource().isVisible());
        skillsPairAllToFilter.get(e.getSource()).setVisible(
                !skillsPairAllToFilter.get(e.getSource()).isVisible());
        skillsPairAllToFilter.get(e.getSource())
                .getParent()
                .getParent()
                .setVisible(skillsPairAllToFilter
                        .get(e.getSource())
                        .isVisible());

        if (!e.getSource().isVisible()) {
            Boolean flag = false;
            for (Component component : ((FlowBoxLayout) e.getSource().getParent()).getComponents()) {
                if (component.isVisible()) {
                    flag = true;
                    break;
                }
            }

            e.getSource()
                    .getParent()
                    .getParent()
                    .setVisible(flag);
        }
    }

    private void candidateImageColumnRenderer() {
        jobCandidatesTable.addGeneratedColumn("fileImageFace", entity -> {
            HBoxLayout hBox = uiComponents.create(HBoxLayout.class);
            Image image = uiComponents.create(Image.NAME);

            if (entity.getItem().getFileImageFace() != null) {
                try {
                    image.setValueSource(new ContainerValueSource<JobCandidate, FileDescriptor>(entity.getContainer(),
                            "fileImageFace"));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                image.setWidth("20px");
                image.setStyleName("circle-20px");
            } else {
                image.setStyleName("pic-center");
            }

            image.setScaleMode(Image.ScaleMode.CONTAIN);
            image.setAlignment(Component.Alignment.MIDDLE_CENTER);

            hBox.setWidthFull();
            hBox.setHeightFull();
            hBox.add(image);

            return hBox;
        });
    }

    private String getStyleForSkillPriority(SkillTree st) {
        String retStr;

        if (st != null) {
            if (st.getPrioritySkill() != null) {
                switch (st.getPrioritySkill()) {
                    case -1:
                        retStr = StandartPrioritySkills.NOT_USED_SKILLS_STYLE;
                        break;
                    case 0:
                        retStr = StandartPrioritySkills.DEFAULT_STYLE;
                        break;
                    case 1:
                        retStr = StandartPrioritySkills.SUBJECT_AREA_STYLE;
                        break;
                    case 2:
                        retStr = StandartPrioritySkills.FRAMEWORKS_STYLE;
                        break;
                    case 3:
                        retStr = StandartPrioritySkills.METHODOLORY_STYLE;
                        break;
                    case 4:
                        retStr = StandartPrioritySkills.PROGRAMMING_LANGUAGE_STYLE;
                        break;
                    default:
                        retStr = StandartPrioritySkills.NOT_USED_SKILLS_STYLE;
                        break;
                }
            } else {
                retStr = StandartPrioritySkills.NOT_USED_SKILLS_STYLE;
            }
        } else {
            retStr = StandartPrioritySkills.NOT_USED_SKILLS_STYLE;
        }

        return retStr;
    }

    private List<SkillTree> getSkillTreeGroup() {
        return dataManager.load(SkillTree.class)
                .query(QUERY_SKILL_TREE_GROUP)
                .view("skillTree-view")
                .list();
    }

    private int ITERATIONS = 0;
    BackgroundTaskHandler taskHandler;
    final long[] startParsingTime = {0};
    final long[] endParsingTime = {0};
    List<JobCandidate> jobCandidatesFilered = new ArrayList<>();

    public void startSearchProcessButtonInvoke() {
        searchProcessRun(((int) filterMethodRadioButtonGroup.getValue()));
    }

    private void searchProcessRun(int searchMethod) {
        ITERATIONS = jobCandidatesDc.getItems().size();
        final int[] count = {0};
        final int[] foundedCounter = {0};
        final long[] parsingAverageTime = {0};
        final int[] timeCounter = {0};

        jobCandidatesFilered.removeAll(jobCandidatesFilered);
        clearSearchResultButton.setEnabled(false);

        startParsingTime[0] = System.currentTimeMillis();
        jobCandidatesDl.removeParameter("jobCandidateFiltered");
        jobCandidatesDl.load();

        endScanBackgroundTaskListener();
        getWindow().setIcon(CubaIcon.CLOCK_O.source());
        stopAndCloseButton.setEnabled(true);

        for (JobCandidate jobCandidate : jobCandidatesDc.getMutableItems()) {
            if (!stopScan) {
                BackgroundTask<Integer, Void> task = new BackgroundTask<Integer, Void>(1000, this) {
                    long startTime;
                    long endTime;

                    @Override
                    public Void run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
                        if (!stopSearchProcess) {
                            startTime = System.currentTimeMillis();

                            taskLifeCycle.publish(count[0] + 1);
                            count[0] = ++count[0];

                            if (!stopSearchProcess) {
                                JobCandidate retJobCandidate = null;

                                switch (searchMethod) {
                                    case 0:
                                        retJobCandidate = scanJobCandidatesCV(jobCandidate, filter);
                                        break;
                                    case 1:
                                        retJobCandidate = scanJobCandidatesCVAllFromMany(jobCandidate, filter);
                                        break;
                                    default:
                                        break;
                                }

                                if (retJobCandidate != null) {
                                    jobCandidatesFilered.add(retJobCandidate);
                                    foundedCounter[0]++;
                                }

                                jobCandidatesDl.setParameter("jobCandidateFiltered", jobCandidatesFilered);
                                jobCandidatesDl.load();
                            }
                        }

                        return null;
                    }

                    @Override
                    public void progress(List<Integer> changes) {
                        if (!stopSearchProcess) {
                            endTime = System.currentTimeMillis();
                            if (timeCounter[0] == 0) {
                                parsingAverageTime[0] = endTime - startTime;
                                timeCounter[0]++;
                            } else {
                                parsingAverageTime[0] = (parsingAverageTime[0] * (timeCounter[0] + 1) + endTime - startTime) /
                                        (++timeCounter[0] + 1);
                            }

                            if (count[0] != 0) {
                                double percent = ((double) count[0] + 1) / (double) ITERATIONS;
                                double seconds = (System.currentTimeMillis() - startParsingTime[0]) / ((double) count[0] + 1)
                                        * (((double) ITERATIONS) - ((double) count[0] + 1)) / 1000;

                                int numberOfDays = (int) (seconds / 86400);
                                int numberOfHours = (int) ((seconds % 86400) / 3600);
                                int numberOfMinutes = (int) (((seconds % 86400) % 3600) / 60);
                                int numberOfSeconds = (int) (((seconds % 86400) % 3600) % 60);

                                filterProgressbar.setValue(percent);

                                int progressPercent = (int) (percent * 100);
                                filterProgressbar.setDescription("Прогресс: " + progressPercent + "%");
                                progressLabel.setValue((count[0]) + " из " + ITERATIONS);
                                progressLabel.setDescription("Осталось до конца операции: "
                                        + (numberOfHours != 0 ? numberOfHours + " ч. " : "")
                                        + (numberOfMinutes != 0 ? numberOfMinutes + " м. " : "")
                                        + (numberOfSeconds != 0 ? numberOfSeconds + " с." : ""));

                                foundLabel.setValue("найдено: " + (foundedCounter[0]));
                            }
                        }
                    }

                    @Override
                    public void canceled() {
                        stopScan = true;
                        notifications.create(Notifications.NotificationType.WARNING)
                                .withType(Notifications.NotificationType.WARNING)
                                .withDescription(messageBundle.getMessage("msgInterruptScanningCV"))
                                .withCaption(messageBundle.getMessage("msgWarning"))
                                .show();

                        getWindow().setIcon(CubaIcon.FILTER.source());
                        stopAndCloseButton.setEnabled(false);
                    }
                };

                taskHandler = backgroundWorker.handle(task);
                taskHandler.execute();

                if (taskHandler.isCancelled()) {
                    break;
                }
            } else {
                endParsingTime[0] = System.currentTimeMillis();
                long parsingTimeSec = (endParsingTime[0] - startParsingTime[0]) / 1000;

                notifications.create(Notifications.NotificationType.HUMANIZED)
                        .withType(Notifications.NotificationType.HUMANIZED)
                        .withPosition(Notifications.Position.BOTTOM_RIGHT)
                        .withDescription("Процесс сканирования резюме завершен за "
                                + parsingTimeSec
                                + " секунд. Найдено "
                                + jobCandidatesFilered.size() + " кандидатов с указанными скиллами.")
                        .withCaption(messageBundle.getMessage("msgWarning"))
                        .show();

                getWindow().setIcon(CubaIcon.FILTER.source());
                stopAndCloseButton.setEnabled(false);

                jobCandidatesDl.setParameter("jobCandidateFiltered", jobCandidatesFilered);
                jobCandidatesDl.load();

                break;
            }
        }
    }

    @Install(to = "jobCandidatesTable.selectCandidateColumn", subject = "columnGenerator")
    private Component jobCandidatesTableSelectCandidateColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        CheckBox retCheckBox = uiComponents.create(CheckBox.class);

        retCheckBox.setDescription(messageBundle.getMessage("msgSelectCandidate"));
        retCheckBox.setAlignment(Component.Alignment.MIDDLE_CENTER);

        retCheckBox.addValueChangeListener(e -> {
            countSelectedCandidates(event.getItem(), e.getValue());
        });

        retCheckBox.setValue(false);

        personalReserveCheckBoxes.put(event.getItem(), retCheckBox);

        return retCheckBox;
    }

    private void countSelectedCandidates(JobCandidate jobCandidate, Boolean checkBoxValue) {
        if (checkBoxValue != null) {
            counter = counter + (checkBoxValue ? 1 : -1);
        } else {
            counter--;
        }

        if (counter > 0) {
            deleteUnselectedCandidatesButton.setEnabled(true);
            putPersonelReserveButton.setEnabled(true);
            basketUnselectedCandidatesButton.setEnabled(true);

            selectedCandidates.add(jobCandidate);
        } else {
            deleteUnselectedCandidatesButton.setEnabled(false);
            putPersonelReserveButton.setEnabled(false);
            basketUnselectedCandidatesButton.setEnabled(false);
            selectedCandidates.remove(jobCandidate);
        }

        counterSelectedCandidatesLabel.setValue(
                messageBundle.getMessage("msgCounterCandidates") + counter);
    }


    @Install(to = "jobCandidatesTable.viewCandidateButton", subject = "columnGenerator")
    private Component jobCandidatesTableViewCandidateButtonColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        VBoxLayout retVBox = uiComponents.create(VBoxLayout.class);
        retVBox.setWidthFull();
        retVBox.setHeightFull();
        retVBox.setAlignment(Component.Alignment.MIDDLE_CENTER);
        retVBox.setSpacing(true);

        PopupButton retButton = uiComponents.create(PopupButton.class);
        retButton.setCaption(messageBundle.getMessage("msgActions"));
        retButton.setAlignment(Component.Alignment.MIDDLE_CENTER);

        retButton.addAction(new BaseAction("openCandidateCardAction")
                .withCaption(messageBundle.getMessage("msgOpenCard"))
                .withHandler(actionPerformedEvent ->
                        screenBuilders.editor(JobCandidate.class, this)
                                .withScreenClass(JobCandidateEdit.class)
                                .editEntity(event.getItem())
                                .build()
                                .show()));
        retButton.addAction(new BaseAction("openCVList")
                .withCaption(messageBundle.getMessage("msgOpenCVList"))
                .withHandler(actionPerformedEvent -> {
                    CandidateCVSimpleBrowse candidateCVSimpleBrowse = screens.create(CandidateCVSimpleBrowse.class);
                    candidateCVSimpleBrowse.setSelectedCandidate(event.getItem());
                    candidateCVSimpleBrowse.setJobCandidate(event.getItem());
                    screens.show(candidateCVSimpleBrowse);
                }));
        retButton.addAction(new BaseAction("addPersonalReserve")
                .withCaption(messageBundle.getMessage("msgAddPersonalReserve"))
                .withHandler(actionPerformedEvent -> {
                    addPersonaLReserveMonth(event.getItem());
                }));

        retVBox.add(retButton);

        return retVBox;
    }

    @Subscribe("filterProgressbar")
    public void onFilterProgressbarValueChange(HasValue.ValueChangeEvent<Double> event) {
        if (event.getValue() == 1) {
            endParsingTime[0] = System.currentTimeMillis();
            long parsingTimeSec = (endParsingTime[0] - startParsingTime[0]) / 1000;

            notifications.create(Notifications.NotificationType.WARNING)
                    .withType(Notifications.NotificationType.WARNING)
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .withDescription("Процесс сканирования резюме завершен за "
                            + parsingTimeSec
                            + " сек. Найдено "
                            + jobCandidatesDc.getItems().size() + " кандидатов с указанными скиллами.")
                    .withCaption("ВНИМАНИЕ")
                    .show();

            getWindow().setIcon(CubaIcon.FILTER.source());
            stopAndCloseButton.setEnabled(false);
            clearSearchResultButton.setEnabled(true);

            jobCandidatesDl.setParameter("jobCandidateFiltered", jobCandidatesFilered);
            jobCandidatesDl.load();
        }

        setEnabledButtons(event.getValue());
    }

    private void setEnabledButtons(Double value) {
        if (value == 0 || value == 1) {
            cityLookupPickerField.setEnabled(true);
            personPositionLookupPickerField.setEnabled(true);
            findSkillsSuggestionPickerField.setEnabled(true);
            clearFilterButton.setEnabled(true);
            startSearchProcessButton.setEnabled(true);
            progressBarHBox.setVisible(false);
            filterProgressbar.setVisible(false);
            loadFromOpenPositionPopupButton.setEnabled(true);
            filterMethodRadioButtonGroup.setEnabled(true);
        } else {
            cityLookupPickerField.setEnabled(false);
            personPositionLookupPickerField.setEnabled(false);
            findSkillsSuggestionPickerField.setEnabled(false);
            clearFilterButton.setEnabled(false);
            startSearchProcessButton.setEnabled(false);
            progressBarHBox.setVisible(true);
            filterProgressbar.setVisible(true);
            loadFromOpenPositionPopupButton.setEnabled(false);
            filterMethodRadioButtonGroup.setEnabled(false);
        }
    }

    private void endScanBackgroundTaskListener() {
    }

    @Subscribe("personPositionLookupPickerField")
    public void onPersonPositionLookupPickerFieldValueChange(HasValue.ValueChangeEvent<Position> event) {
        if (event.getValue() != null) {
            jobCandidatesDl.setParameter("personPosition", event.getValue());
        } else {
            jobCandidatesDl.removeParameter("personPosition");
        }

        jobCandidatesDl.load();
    }

    private JobCandidate scanJobCandidatesCVAllFromMany(JobCandidate jobCandidate,
                                                        HashMap<LinkButton, SkillTree> filter) {
        Boolean flag = false;

        for (CandidateCV candidateCV : jobCandidate.getCandidateCv()) {
            List<SkillTree> skillTreesFromCV = pdfParserService.parseSkillTree(candidateCV.getTextCV());
            for (Map.Entry entry : filter.entrySet()) {
                for (SkillTree skill : skillTreesFromCV) {
                    if (!((SkillTree) entry.getValue()).getSkillName().equals(skill.getSkillName())) {
                        flag = true;
                        break;
                    }

                    if (flag)
                        break;
                }

                if (flag)
                    break;
            }

            if (flag)
                break;
        }

        if (flag) {
            return null;
        } else {
            return jobCandidate;
        }
    }

    private JobCandidate scanJobCandidatesCV(JobCandidate jobCandidate,
                                             HashMap<LinkButton, SkillTree> filter) {
        Boolean flag = false;


        for (CandidateCV candidateCV : jobCandidate.getCandidateCv()) {
            List<SkillTree> skillTreesFromCV = pdfParserService.parseSkillTree(candidateCV.getTextCV());
            for (Map.Entry entry : filter.entrySet()) {
                for (SkillTree skill : skillTreesFromCV) {
                    if (((SkillTree) entry.getValue()).getSkillName().equals(skill.getSkillName())) {
                        flag = true;

                        break;
                    }

                    if (flag)
                        break;
                }

                if (flag)
                    break;
            }

            if (flag)
                break;
        }

        if (flag) {
            return jobCandidate;
        } else {
            return null;
        }
    }

    @Subscribe("cityLookupPickerField")
    public void onCityLookupPickerFieldValueChange(HasValue.ValueChangeEvent<City> event) {
        if (event.getValue() != null) {
            if (event.getValue() != null) {
                jobCandidatesDl.setParameter("city", event.getValue());
            } else {
            }

            jobCandidatesDl.load();
        }

    }

    public void stopSearchProcessButtonInvoke() {
        taskHandler.cancel();
        stopScan = true;
        buttonStop.setVisible(false);
    }

    public void clearFilterButtonInvoke() {
        personPositionLookupPickerField.setValue(null);
        cityLookupPickerField.setValue(null);

        clearAllSkillsFromFilter();

        jobCandidatesDl.removeParameter("personPosition");
        jobCandidatesDl.removeParameter("city");
        jobCandidatesDl.removeParameter("jobCandidateFiltered");
        jobCandidatesDl.load();
    }

    private void clearAllSkillsFromFilter() {
        for (Map.Entry s : skillsPairFilterToAll.entrySet()) {
            ((LinkButton) s.getKey()).setVisible(false);
            ((LinkButton) s.getValue()).setVisible(true);
        }

        startSearchProcessButton.setEnabled(false);
        filter.clear();
    }

    @Install(to = "jobCandidatesTable.lastIteraction", subject = "columnGenerator")
    private String jobCandidatesTableLastIteractionColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        IteractionList iteractionList = getLastIteraction(event.getItem());

        String date = null;

        try {
            date = new SimpleDateFormat("dd-MM-yyyy").format(iteractionList.getDateIteraction());
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        String retStr = "";
        Boolean checkBlockCandidate = event.getItem().getBlockCandidate() == null ? false : event.getItem().getBlockCandidate();

        if (checkBlockCandidate != null) {
            if (checkBlockCandidate != true) {
                if (iteractionList != null) {
                    Calendar calendar = Calendar.getInstance();

                    if (iteractionList.getDateIteraction() != null) {
                        calendar.setTime(iteractionList.getDateIteraction());
                    } else {
                        calendar.setTime(event.getItem().getCreateTs());
                    }

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
            } else {
                retStr = "button_table_black";
            }
        } else {
            retStr = "button_table_green";
        }

        return
                "<div class=\"" +
                        retStr
                        + "\">" +
                        (date != null ? date : "нет")
                        + "</div>"
                ;
    }

    public void clearSearchResultButtonInvoke() {
        jobCandidatesDl.removeParameter("jobCandidateFiltered");
//        jobCandidatesDl.load();

        jobCandidatesDl.setParameter("personPosition",
                personPositionLookupPickerField.getValue());
        jobCandidatesDl.load();

        clearSearchResultButton.setEnabled(false);
        startSearchProcessButton.setEnabled(true);
    }

    @Subscribe("findSkillsSuggestionPickerField")
    public void onFindSkillsSuggestionPickerFieldValueChange(HasValue.ValueChangeEvent event) {
        if (findSkillsSuggestionPickerField.getValue() != null) {
            for (Map.Entry s : skillsPairAllToFilter.entrySet()) {
                if (((LinkButton) s.getKey()).getCaption()
                        .equals(((SkillTree) event.getValue()).getSkillName())) {
                    ((LinkButton) s.getKey()).setVisible(false);
                    ((LinkButton) s.getValue()).getParent().getParent().setVisible(true);
                    ((LinkButton) s.getValue()).setVisible(true);

                    filter.put((LinkButton) s.getValue(), (SkillTree) event.getValue());
                    startSearchProcessButton.setEnabled(true);

                    break;
                }
            }

            findSkillsSuggestionPickerField.setValue(null);
        }
    }

    public void stopAndCloseButtonInvoke() {
        stopSearchProcess = true;
        stopAndCloseButton.setEnabled(false);

        notifications.create(Notifications.NotificationType.WARNING)
                .withType(Notifications.NotificationType.WARNING)
                .withDescription(messageBundle.getMessage("msgInterruptScanningCV"))
                .withCaption(messageBundle.getMessage("msgWarning"))
                .show();

        getWindow().setIcon(CubaIcon.FILTER.source());
        stopAndCloseButton.setEnabled(false);
        filterMethodRadioButtonGroup.setEnabled(true);
        clearSearchResultButton.setEnabled(true);
        startSearchProcessButton.setEnabled(true);
        findSkillsSuggestionPickerField.setEnabled(true);
        loadFromOpenPositionPopupButton.setEnabled(true);
        clearFilterButton.setEnabled(true);
        personPositionLookupPickerField.setEnabled(true);
        cityLookupPickerField.setEnabled(true);
        progressBarHBox.setVisible(false);
    }

    @Subscribe("loadFromOpenPositionPopupButton.loadFromClipboard")
    public void onLoadFromOpenPositionPopupButtonLoadFromClipboard(Action.ActionPerformedEvent event) {
        Screen screen = screenBuilders.screen(this)
                .withScreenClass(Onlytext.class)
                .withOpenMode(OpenMode.DIALOG)
                .build();

        screen.addAfterCloseListener(afterCloseEvent -> {
            if (!((Onlytext) screen).getCancel()) {
                List<SkillTree> allSkills = pdfParserService.parseSkillTree(((Onlytext) screen).getResultText());

                for (SkillTree st : allSkills) {
                    for (Map.Entry entry : skillsPairFilterToAll.entrySet()) {
                        if (st.getSkillName().equals(((LinkButton) entry.getKey()).getCaption())) {
                            ((LinkButton) entry.getKey()).setVisible(true);
                            ((LinkButton) entry.getKey()).getParent().getParent().setVisible(true);
                            ((LinkButton) entry.getValue()).setVisible(false);

                            filter.put((LinkButton) entry.getKey(), st);
                        }
                    }
                }


                loadFromVacancyLabel.setVisible(false);
                openPositionViewButton.setVisible(false);
                setEnabledButtons((double) (filter.size() > 0 ? 1 : 0));
                selectedOpenPosition = null;
            }
        });

        screen.show();
    }

    @Subscribe("loadFromOpenPositionPopupButton.loadFromFile")
    public void onLoadFromOpenPositionPopupButtonLoadFromFile(Action.ActionPerformedEvent event) {
        Screen screen = screenBuilders.screen(this)
                .withScreenClass(OnlyTextFromFile.class)
                .withOpenMode(OpenMode.DIALOG)
                .build();

        screen.addAfterCloseListener(afterCloseEvent -> {
            if (!((OnlyTextFromFile) screen).getCancel()) {
                List<SkillTree> allSkills =
                        pdfParserService.parseSkillTree(((OnlyTextFromFile) screen).getResultText());

                for (SkillTree st : allSkills) {
                    for (Map.Entry entry : skillsPairFilterToAll.entrySet()) {
                        if (st.getSkillName().equals(((LinkButton) entry.getKey()).getCaption())) {
                            ((LinkButton) entry.getKey()).setVisible(true);
                            ((LinkButton) entry.getKey()).getParent().getParent().setVisible(true);
                            ((LinkButton) entry.getValue()).setVisible(false);

                            filter.put((LinkButton) entry.getKey(), st);
                        }
                    }
                }

                loadFromVacancyLabel.setVisible(false);
                openPositionViewButton.setVisible(false);
                setEnabledButtons((double) (filter.size() > 0 ? 1 : 0));
                selectedOpenPosition = null;
            }
        });

        screen.show();
    }

    private void scanOpenPositionField(Collection<OpenPosition> openPosition) {
        for (OpenPosition op : openPosition) {
            List<SkillTree> skillTreesRu = pdfParserService.parseSkillTree(op.getComment());
            List<SkillTree> skillTreesEn = pdfParserService.parseSkillTree(op.getCommentEn());
            Set<SkillTree> allSkill = Stream.concat(skillTreesRu.stream(), skillTreesEn.stream())
                    .collect(Collectors.toSet());

            List<SkillTree> allSkills = new ArrayList<>(allSkill);

            for (SkillTree st : allSkills) {
                for (Map.Entry entry : skillsPairFilterToAll.entrySet()) {
                    if (st.getSkillName().equals(((LinkButton) entry.getKey()).getCaption())) {
                        ((LinkButton) entry.getKey()).setVisible(true);
                        ((LinkButton) entry.getKey()).getParent().getParent().setVisible(true);
                        ((LinkButton) entry.getValue()).setVisible(false);

                        filter.put((LinkButton) entry.getKey(), st);
                    }
                }
            }

            loadFromOpenPositionPopupButton.setDescription(loadFromOpenPositionPopupButton.getDescription()
                    + ": "
                    + op.getVacansyName());

            loadFromVacancyLabel.setValue(op.getVacansyName());
            loadFromVacancyLabel.setVisible(true);
            openPositionViewButton.setDescription(op.getVacansyName());
            openPositionViewButton.setVisible(true);
            selectedOpenPosition = op;
        }

        setEnabledButtons((double) (filter.size() > 0 ? 1 : 0));
    }

    @Subscribe("loadFromOpenPositionPopupButton.loadFromOpenPosition")
    public void onLoadFromOpenPositionPopupButtonLoadFromOpenPosition(Action.ActionPerformedEvent event) {
        screenBuilders.lookup(OpenPosition.class, this)
                .withScreenId("itpearls_OpenPosition.browse")
                .withLaunchMode(OpenMode.DIALOG)
                .withSelectHandler(openPosition -> {
                    scanOpenPositionField(openPosition);
                })
                .build()
                .show();
    }

    public void expandAllSkillsButtonInvoke() {
        for (Component component : allSkillsScrollBox.getComponents()) {
            try {
                ((GroupBoxLayout) component).setExpanded(true);
            } catch (ClassCastException e) {

            }
        }
    }

    public void compressAllSkillsButtonInvoke() {
        for (Component component : allSkillsScrollBox.getComponents()) {
            try {
                ((GroupBoxLayout) component).setExpanded(false);
            } catch (ClassCastException e) {

            }
        }
    }

    public void compressFilterSkillsButtonInvoke() {
        for (Component component : filterSkillsScrollBox.getComponents()) {
            try {
                ((GroupBoxLayout) component).setExpanded(false);
            } catch (ClassCastException e) {

            }
        }
    }

    public void expandFilterSkillsButtonInvoke() {
        for (Component component : filterSkillsScrollBox.getComponents()) {
            try {
                ((GroupBoxLayout) component).setExpanded(true);
            } catch (ClassCastException e) {

            }
        }
    }

    public void openPositionViewButtonInvoke() {
        if (selectedOpenPosition != null) {
            screenBuilders.editor(OpenPosition.class, this)
                    .withScreenClass(OpenPositionEdit.class)
                    .editEntity(selectedOpenPosition)
                    .build()
                    .show();
        } else {
            openPositionViewButton.setVisible(false);
            notifications.create(Notifications.NotificationType.WARNING)
                    .withType(Notifications.NotificationType.WARNING)
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withDescription(messageBundle.getMessage("msgNotSelectedOpenPosition"))
                    .withHideDelayMs(5000)
                    .show();
        }
    }

    public void deleteUnselectedCandidatesButtonInvoke() {
        jobCandidatesDl.setParameter("jobCandidateFiltered", selectedCandidates);
        jobCandidatesDl.load();
    }

    public void putPersonelReserveButtonInvoke() {
        dialogs.createOptionDialog(Dialogs.MessageType.CONFIRMATION)
                .withMessage(messageBundle.getMessage("msgPutToPersonelReserve")
                        + "\n\nВыбрано: "
                        + selectedCandidates.size()
                        + " "
                        + messageBundle.getMessage("msgCandidatesPoint"))
                .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY)
                                .withHandler(e -> putCandidatesToPersonelReserve()),
                        new DialogAction(DialogAction.Type.NO, Action.Status.NORMAL))
                .withType(Dialogs.MessageType.CONFIRMATION)
                .show();
    }

    private void putCandidatesToPersonelReserve() {

        for (JobCandidate jobCandidate : selectedCandidates) {
            PersonelReserve personelReserveCheck = null;

            List<RecrutiesTasks> recrutiesTasks = dataManager.load(RecrutiesTasks.class)
                    .query(QUERY_GET_FROM_SUBSCRIBERS)
                    .parameter("reacrutier", userSession.getUser())
                    .parameter("openPosition", selectedOpenPosition)
                    .view("recrutiesTasks-view")
                    .list();
            if (recrutiesTasks.size() > 0 || loadFromVacancyLabel.getValue() == null) {
                try {
                    personelReserveCheck = dataManager.load(PersonelReserve.class)
                            .query(QUERY_GET_PERSONEL_RESERVE)
                            .view("personelReserve-view")
                            .parameter("jobCandidate", jobCandidate)
                            .parameter("currDate", new Date())
                            .one();
                } catch (IllegalStateException e) {
                    personelReserveCheck = null;
                }

                if (personelReserveCheck == null) {
                    addPersonaLReserveMonth(jobCandidate);
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy");

                    notifications.create(Notifications.NotificationType.WARNING)
                            .withCaption("ВНИМАНИЕ")
                            .withDescription(messageBundle.getMessage("msgCanNotAddToPersonalReserve")
                                    + ": " + jobCandidate.getFullName()
                                    + "\n" + messageBundle.getMessage("msgRecruterOwner")
                                    + " " + personelReserveCheck.getRecruter().getName()
                                    + "\n" + messageBundle.getMessage("msgEndDateReserve")
                                    + " " + (personelReserveCheck.getEndDate() != null
                                    ? sdf.format(personelReserveCheck.getEndDate()) : messageBundle.getMessage("msgUnlimited")))
                            .withType(Notifications.NotificationType.ERROR)
                            .show();
                }
            } else {
                notifications.create(Notifications.NotificationType.ERROR)
                        .withType(Notifications.NotificationType.ERROR)
                        .withDescription(messageBundle.getMessage("msgCanNotAddToPersonalReserve")
                                + ": " + jobCandidate.getFullName()
                                + "\nДобавить кандидата возможно только на те вакансии на которые Вы подписаны." +
                                "\nОформите подписку на вакании в пункте меню \"Подписки\".")
                        .withCaption(messageBundle.getMessage("msgError"))
                        .show();
            }
        }

        selectedCandidates.clear();
        jobCandidatesDl.load();

        counterSelectedCandidatesLabel.setValue(
                messageBundle.getMessage("msgCounterCandidates") + counter);
    }

    private void removeAllCheckBoxesPersonalReserve() {
        for (Map.Entry<JobCandidate, CheckBox> entry : personalReserveCheckBoxes.entrySet()) {
            entry.getValue().setValue(false);
        }
    }

    private void addPersonaLReserveMonth(JobCandidate jobCandidate) {
        PersonelReserve personelReserve = metadata.create(PersonelReserve.class);

        personelReserve.setDate(new Date());
        personelReserve.setJobCandidate(jobCandidate);
        personelReserve.setRecruter(userSessionSource.getUserSession().getUser());
        personelReserve.setInProcess(true);

        int noOfDays = 30;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_YEAR, noOfDays);
        Date endDate = calendar.getTime();
        personelReserve.setEndDate(endDate);

        if (personPositionLookupPickerField.getValue() != null) {
            personelReserve.setPersonPosition(personPositionLookupPickerField.getValue());
        }

        if (selectedOpenPosition != null) {
            personelReserve.setOpenPosition(selectedOpenPosition);
        }

        dataManager.commit(personelReserve);
        counter--;
    }

    public void basketUnselectedCandidatesButtonInvoke() {
    }
}