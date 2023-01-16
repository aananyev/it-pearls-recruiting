package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.core.PdfParserService;
import com.company.itpearls.entity.*;
import com.company.itpearls.web.StandartPrioritySkills;
import com.company.itpearls.web.screens.iteractionlist.IteractionListSimpleBrowse;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundTaskHandler;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.global.UserSession;
import org.jsoup.Jsoup;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

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

    private List<SkillTree> skillTreeGroup = new ArrayList<>();
    private HashMap<LinkButton, LinkButton> skillsPairAllToFilter = new HashMap<>();
    private HashMap<LinkButton, LinkButton> skillsPairFilterToAll = new HashMap<>();
    private HashMap<LinkButton, SkillTree> filter = new HashMap<>();

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
        }

        if (filterSize > 0 && filterSize < skillsPairAllToFilter.size()) {
            arrowLeft1.setVisible(true);
            arrowLeft2.setVisible(true);
            arrowLeft3.setVisible(true);

            arrowRight1.setVisible(true);
            arrowRight2.setVisible(true);
            arrowRight3.setVisible(true);
        }

        if (filterSize == skillsPairAllToFilter.size()) {
            arrowLeft1.setVisible(true);
            arrowLeft2.setVisible(true);
            arrowLeft3.setVisible(true);

            arrowRight1.setVisible(false);
            arrowRight2.setVisible(false);
            arrowRight3.setVisible(false);

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

    public void startSearchProcessButtonInvoke() {
        ITERATIONS = jobCandidatesDc.getItems().size();
        final int[] count = {0};
        final int[] foundedCounter = {0};
        List<JobCandidate> jobCandidatesFilered = new ArrayList<>();
        final long[] parsingAverageTime = {0};
        final int[] timeCounter = {0};

        for (JobCandidate jobCandidate : jobCandidatesDc.getMutableItems()) {

            BackgroundTask<Integer, Void> task = new BackgroundTask<Integer, Void>(1000, this) {
                @Override
                public Void run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
                    long startTime = System.currentTimeMillis();

                    if (!taskLifeCycle.isCancelled()) {
                        JobCandidate retJobCandidate = scanJobCandidatesCV(jobCandidate, filter);
                        taskLifeCycle.publish(count[0]);
                        count[0] = ++count[0];

                        if (retJobCandidate != null) {
                            jobCandidatesFilered.add(retJobCandidate);
                            foundedCounter[0]++;
                        }
                    }

                    long endTime = System.currentTimeMillis();
                    if (timeCounter[0] == 0) {
                        parsingAverageTime[0] = endTime - startTime;
                        timeCounter[0] ++;
                    } else {
                        parsingAverageTime[0] = ((parsingAverageTime[0] * timeCounter[0]) + endTime - startTime) /
                                ++ timeCounter[0];
                    }


                    return null;
                }

                @Override
                public void progress(List<Integer> changes) {
                    double percent = (double) count[0] / (double) ITERATIONS;
                    double seconds = parsingAverageTime[0] * jobCandidatesDc.getMutableItems().size() / 1000;

                    int numberOfDays = (int) (seconds / 86400);
                    int numberOfHours = (int) ((seconds % 86400) / 3600);
                    int numberOfMinutes = (int) (((seconds % 86400) % 3600) / 60);
                    int numberOfSeconds = (int) (((seconds % 86400) % 3600) % 60);

                    filterProgressbar.setValue(percent);
                    progressLabel.setValue(count[0] + " из " + ITERATIONS);
                    progressLabel.setDescription("Осталось до конца операции: "
                            + (numberOfHours != 0 ? numberOfHours + " ч.": "")
                            + (numberOfMinutes != 0 ? numberOfMinutes + " м.": "")
                            + (numberOfSeconds != 0 ? numberOfSeconds + " с." : ""));

                    foundLabel.setValue("найдено: " + foundedCounter[0]);
                }

                @Override
                public void canceled() {
                    notifications.create(Notifications.NotificationType.WARNING)
                            .withType(Notifications.NotificationType.WARNING)
                            .withDescription("Процесс сканирования резюме прерван")
                            .withCaption("ВНИМАНИЕ")
                            .show();
                }
            };

            taskHandler = backgroundWorker.handle(task);
            taskHandler.execute();

            if (taskHandler.isCancelled()) {
                break;
            }
        }
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


    private JobCandidate scanJobCandidatesCV(JobCandidate jobCandidate, HashMap<LinkButton, SkillTree> filter) {
        Boolean flag = false;

        for (CandidateCV candidateCV : jobCandidate.getCandidateCv()) {
            for (Map.Entry entry : filter.entrySet()) {
                List<SkillTree> skillTreesFromCV = pdfParserService.parseSkillTree(candidateCV.getTextCV());

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

    public void stopSearchProcessButtonInvoke() {
        taskHandler.cancel();
    }
}