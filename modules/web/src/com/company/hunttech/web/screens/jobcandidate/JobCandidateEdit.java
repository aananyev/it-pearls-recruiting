package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.core.*;
import com.company.hunttech.entity.*;
import com.company.hunttech.service.GetRoleService;
import com.company.hunttech.web.StandartPrioritySkills;
import com.company.hunttech.web.StandartRoles;
import com.company.hunttech.web.screens.candidatecv.CandidateCVEdit;
import com.company.hunttech.web.screens.fragments.SkillLabelData;
import com.company.hunttech.web.screens.fragments.Skillsbar;
import com.company.hunttech.web.screens.jobcandidate.HistoryRowData;
import com.company.hunttech.web.screens.iteractionlist.IteractionListEdit;
import com.company.hunttech.web.screens.iteractionlist.iteractionlistbrowse.IteractionListSimpleBrowse;
import com.company.hunttech.web.screens.openposition.OpenPositionMasterBrowse;
import com.company.hunttech.web.screens.openposition.openpositionviews.QuickViewOpenPositionDescription;
import com.company.hunttech.web.screens.skilltree.SkillTreeBrowseCheck;
import com.company.hunttech.web.util.FileDescriptorImageHelper;
import com.hunttech.hrm.gui.components.OvaFallbackImage;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogActions;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogOutcome;
import com.haulmont.cuba.gui.app.core.inputdialog.InputDialog;
import com.haulmont.cuba.gui.app.core.inputdialog.InputParameter;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.components.data.ValueSource;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.*;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.MagicNames;
import org.jsoup.Jsoup;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@UiController("hunttech_JobCandidate.edit")
@UiDescriptor("job-candidate-edit.xml")
@EditedEntityContainer("jobCandidateDc")
@LoadDataBeforeShow
public class JobCandidateEdit extends StandardEditor<JobCandidate> {
    @Inject
    private DataManager dataManager;
    @Inject
    private Dialogs dialogs;
    @Inject
    private Label<String> labelCV;
    @Inject
    private Label<String> labelQualityPercent;
    @Inject
    private Label<String> candidateSkillsLabels;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private UserSession userSession;
    @Inject
    private Metadata metadata;
    @Inject
    private CollectionPropertyContainer<SocialNetworkURLs> jobCandidateSocialNetworksDc;
    @Inject
    private DataContext dataContext;
    //    @Inject
//    private CollectionLoader<SocialNetworkURLs> socialNetworkURLsesDl;
    @Inject
    private Label<String> personPositionTitle;
    @Inject
    private Label<String> emailTitle;
    @Inject
    private Label<String> phoneTitle;
    @Inject
    private Label<String> telegramTitle;
    @Inject
    private Label<String> skypeTitle;
    @Inject
    private Label<String> jobTitleTitle;

    /**
     * Single avatar component for both the stored candidate photo and the theme fallback.
     * The active value remains bound to {@code jobCandidateDc.fileImageFace}; the component
     * itself decides whether the bound file or {@code icons/no-programmer.jpeg} is rendered.
     */
    @Inject
    private OvaFallbackImage candidatePic;
    @Inject
    private FileUploadField fileImageFaceUpload;
    @Inject
    private LinkButton emailLinkButton;
    @Inject
    private LinkButton skypeLinkButton;
    @Inject
    private LinkButton telegrammLinkButton;
    @Inject
    private WebBrowserTools webBrowserTools;
    @Inject
    private PdfParserService pdfParserService;
    @Inject
    private CollectionPropertyContainer<CandidateCV> jobCandidateCandidateCvsDc;
    @Inject
    private Notifications notifications;
    @Inject
    private LinkButton telegrammGroupLinkButton;
    @Inject
    private Label<String> candidateRatingLabel;
    @Inject
    private StarsAndOtherService starsAndOtherService;
    @Inject
    private Screens screens;
    @Inject
    private ParseCVService parseCVService;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private BackgroundWorker backgroundWorker;
    @Inject
    private Logger log;
    @Inject
    private SuggestionField<String> firstNameField;
    @Inject
    private SuggestionField<String> secondNameField;
    @Inject
    private SuggestionField<String> middleNameField;
    @Inject
    private CssLayout positionsLabel;
    @Inject
    private SuggestionPickerField<Company> currentCompanyField;
    @Inject
    private LookupPickerField<Position> personPositionField;
    @Inject
    private LookupPickerField<City> jobCityCandidateField;
    @Inject
    private DateField<Date> birdhDateField;
    private RadioButtonGroup<Integer> priorityCommunicationMethodRadioButton;
    private TextField<String> mobilePhoneField;
    private DataGrid<IteractionList> jobCandidateIteractionListTable;
    private Button openPositionProjectDescriptionButton;
    private PopupButton frequentInteractionPopupButton;
    private DataGrid<CandidateCV> jobCandidateCandidateCvTable;
    private Button copyCVButton;
    private Button scanContactsFromCVButton;
    private Button checkSkillFromJD;
    private TextField<String> emailField;
    private TextField<String> phoneField;
    private TextField<String> skypeNameField;
    private TextField<String> telegramNameField;
    private TextField<String> whatsupNameField;
    private TextField<String> wiberNameField;
    private DataGrid<SocialNetworkURLs> socialNetworkTable;
    private static final String BLOCK_CANDIDATE_ON = "Запретить работу с кандидатом";
    private static final String BLOCK_CANDIDATE_OFF = "Разрешить работу с кандидатом";
    private static final String QUERY_GET_OTHER_SOCIAL_NETWORK = "select e from hunttech_SocialNetworkType e where e.socialNetwork = :other";
    private static final String QUERY_GET_CANDIDATE_CV = "select e from hunttech_CandidateCV e where e.candidate = :candidate";
    private static final String TELEGRAM_NAME_URL = "http://t.me/";
    private static final String QUERY_GET_LAST_ITERACTION = "select e from hunttech_IteractionList e where e.candidate = :candidate and e.numberIteraction = (select max(f.numberIteraction) from hunttech_IteractionList f where f.candidate = :candidate)";
    private static final String CREATE_COMPANY_ACTION_ID = "createCompany";

    /**
     * Entity properties represented by visible data-entry controls in the candidate card.
     * The list is intentionally independent from component creation so completion can be
     * calculated before CUBA builds the lazy tabs.
     */
    static final List<String> CARD_COMPLETION_PROPERTIES = Collections.unmodifiableList(Arrays.asList(
            "firstName", "middleName", "secondName", "birdhDate", "cityOfResidence",
            "personPosition", "currentCompany", "email", "phone", "mobilePhone",
            "telegramName", "whatsupName", "wiberName", "skypeName", "priorityContact"));

    List<Position> setPos = new ArrayList<>();
    // Данные вкладки «Позиции и вакансии» загружаются один раз при первом открытии.
    private Map<UUID, HistoryRowData> historyRowDataByVacancy = Collections.emptyMap();
    private boolean positionsTabLoading;
    private boolean positionsTabLoaded;
    private boolean skillsLoading;
    private boolean skillsLoaded;
    List<IteractionList> iteractionListFromCandidate = new ArrayList();
    IteractionList lastIteraction = null;
    private boolean lastIteractionLoaded;

    @Inject
    private Button blockCandidateButton;
    @Inject
    private Button candidateNavMain;
    @Inject
    private Button candidateNavPositions;
    @Inject
    private Button candidateNavIteraction;
    @Inject
    private Button candidateNavResume;
    @Inject
    private Button candidateNavContactInfo;
    @Inject
    private Button candidateNavComments;
    @Inject
    private Button candidateNavHistory;
//    @Inject
//    private Label<String> iteractionListLabelCandidate;
    @Inject
    private GetRoleService getRoleService;
    @Inject
    private CheckBox blockCandidateCheckBox;
    @Inject
    private CollectionPropertyContainer<IteractionList> jobCandidateIteractionDc;
    @Inject
    private InstanceLoader<JobCandidate> jobCandidateDl;
    @Inject
    private InstanceContainer<JobCandidate> jobCandidateDc;
    @Inject
    private InteractionService interactionService;
    @Inject
    private FileLoader fileLoader;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private Fragments fragments;
    @Inject
    private HBoxLayout skillBox;
    @Inject
    private KeyValueCollectionLoader lastProjectDl;
    @Inject
    private TabSheet tabSheetSocialNetworks;
    private boolean cvTabInitialized = false;
    private boolean interationTabInitialized = false;
    private boolean commentsTabInitialized = false;
    private boolean referenceLoadersInitialized = false;
    private boolean openPositionLoaderInitialized = false;
    private boolean candidateCvLoaded = false;
    private boolean interactionsLoaded = false;
    private boolean socialNetworksLoaded = false;
    private Button copyIteractionButton;
    private boolean candidateInitialized = false;
    private boolean tabContactInfoInitialized = false;
    private boolean positionsTabInitialized = false;
    private boolean initialInteractionAdded = false;
    private boolean companyEditorOpen = false;
    private Table lastProjectTable;
    @Inject
    private KeyValueCollectionContainer lastProjectDc;
    private boolean initSocialNetworkURLs = false;
    @Inject
    private CollectionLoader<OpenPosition> suggestOpenPositionDl;
    @Inject
    private CollectionLoader<OpenPosition> openPositionDl;
    @Inject
    private CollectionLoader<Company> currentCompaniesLc;
    @Inject
    private CollectionContainer<Company> currentCompaniesDc;
    @Inject
    private CollectionLoader<City> citiesDl;
    @Inject
    private CollectionLoader<Position> personPositionsLc;
    private Table<OpenPosition> suggestVacancyTable;
    @Inject
    private MessageBundle messageBundle;
    private Button sendCommentButton;
    private TextField<String> chatMessageTextField;
    private LookupPickerField<OpenPosition> vacancyPopupPickerField;
    private VBoxLayout jobCandidateCommentsContainer;
    @Inject
    private CollectionLoader<IteractionList> interactionCommentDl;
    @Inject
    private CollectionContainer<IteractionList> interactionCommentDc;
    private CollectionContainer<OpenPosition> suggestOpenPositionDc;
    @Inject
    private GridLayout dictionatysTavlesHBox;
    @Inject
    private GroupBoxLayout lastProjects;
    @Inject
    private Label<String> fullNameField;
    @Inject
    private Label<String> personPositionLabel;
    @Inject
    private Label<String> emailLabel;
    @Inject
    private Label<String> phoneLabel;
    @Inject
    private Label<String> mobilePhoneLabel;
    @Inject
    private Label<String> skypuLabel;
    @Inject
    private Label<String> telegramLabel;
    private Button addSocialNetworkListsButton;
    private LookupPickerField<OpenPosition> vacancyFilterLookupPickerField;
    @Inject
    private ResumeRecognitionService resumeRecognitionService;
    @Inject
    private OpenPositionService openPositionService;

    private Boolean ifCandidateIsExist() {
        setFullNameCandidate();
        // вдруг такой кандидат уже есть
        List<JobCandidate> candidates = dataManager.load(JobCandidate.class)
                .query("select e from hunttech_JobCandidate e where e.firstName like :firstName and e.secondName like :secondName")
                .cacheable(true)
                .parameter("firstName", firstNameField.getValue())
                .parameter("secondName", secondNameField.getValue())
                // Duplicate check needs only identity fields; avoid loading full candidate graph.
                .view("jobCandidate-view-search")
                .list();

        return candidates.size() == 0 ? false : true;
    }

    private void setSocialNetworkTable() {
/*       List<SocialNetworkType> socialNetworkType = dataManager.load(SocialNetworkType.class)
                .list();

        if (!PersistenceHelper.isNew(getEditedEntity())) {
            // основные социальные сети показать
            List<SocialNetworkURLs> socialNetwork = getEditedEntity().getSocialNetwork();

            // тут либо если не новая запись, то проверить на наличие других записей, либо если новая запись, то пофигу
            if (socialNetwork.size() < socialNetworkType.size()) {
                List<SocialNetworkType> type = dataManager
                        .load(SocialNetworkType.class)
                        .query("select e.socialNetworkURL " +
                                "from hunttech_SocialNetworkURLs e " +
                                "where e.jobCandidate = :candidate")
                        .cacheable(true)
                        .parameter("candidate", getEditedEntity())
                        .list();

                for (SocialNetworkType s : socialNetworkType) {
                    // Социальные сетиNetworkURLs socialNetworkURLs = dataManager.create(SocialNetworkURLs.class);
                    SocialNetworkURLs socialNetworkURLs = metadata.create(SocialNetworkURLs.class);

                    if (!type.contains(s)) {
                        socialNetworkURLs.setSocialNetworkURL(s);
                        socialNetworkURLs.setNetworkName(s.getSocialNetwork());
                        socialNetworkURLs.setJobCandidate(getEditedEntity());

                        dataManager.commit(socialNetworkURLs);
                    }
                }
            }
        } else {
            List<SocialNetworkURLs> sn = new ArrayList<SocialNetworkURLs>();

            for (SocialNetworkType s : socialNetworkType) {
                SocialNetworkURLs socialNetworkURLs = new SocialNetworkURLs();
                socialNetworkURLs.setSocialNetworkURL(s);
                socialNetworkURLs.setJobCandidate(getEditedEntity());
                socialNetworkURLs.setNetworkName(s.getSocialNetwork());

                jobCandidateSocialNetworksDc.getMutableItems().add(socialNetworkURLs);
            }

            DataContext dc = socialNetworkURLsesDl.getDataContext();
            dc.setParent(dataContext);
            dataContext.merge(sn);

            blockCandidateCheckBox.setValue(false);
        } */
    }

    private void setupSkillBox() {
        // Skills are now loaded asynchronously via startSkillsBackgroundLoading()
    }

    private String loadLastCvText(UUID candidateId) {
        if (candidateId == null) {
            return null;
        }
        return dataManager.loadValue(
                "select e.textCV from hunttech_CandidateCV e " +
                "where e.candidate.id = :candidateId " +
                "order by e.datePost desc",
                String.class)
                .parameter("candidateId", candidateId)
                .maxResults(1)
                .optional()
                .orElse(null);
    }

    private void startSkillsBackgroundLoading() {
        if (skillsLoading || skillsLoaded) {
            return;
        }
        if (PersistenceHelper.isNew(getEditedEntity())
                || getEditedEntity().getId() == null) {
            return;
        }

        UUID candidateId = getEditedEntity().getId();
        skillsLoading = true;

        BackgroundTask<Void, List<SkillLabelData>> task =
                new BackgroundTask<Void, List<SkillLabelData>>(
                        60, TimeUnit.SECONDS, this) {

                    @Override
                    public List<SkillLabelData> run(TaskLifeCycle<Void> taskLifeCycle) {
                        String cvText = loadLastCvText(candidateId);
                        if (cvText == null || cvText.isEmpty()) {
                            return Collections.emptyList();
                        }
                        // DataManager is safe to call from background thread
                        // via AppBeans (injected DataManager bound to middleware)
                        DataManager bgDataManager = AppBeans.get(DataManager.class);
                        PdfParserService bgPdf = AppBeans.get(PdfParserService.class);
                        ParseCVService bgParse = AppBeans.get(ParseCVService.class);

                        String plainText = Jsoup.parse(cvText).text();
                        List<SkillTree> skillTrees = bgPdf.parseSkillTree(plainText);
                        HashMap<SkillTree, Integer> skillCounter = new HashMap<>();
                        for (SkillTree skillTree : skillTrees) {
                            skillCounter.put(skillTree,
                                    bgParse.countMachesSkill(plainText, skillTree));
                        }

                        // Deduplicate
                        for (int i = 0; i < skillTrees.size(); i++) {
                            if (skillTrees.get(i).getNotParsing()) continue;
                            for (int j = i + 1; j < skillTrees.size(); j++) {
                                if (skillTrees.get(i).getSkillName()
                                        .equalsIgnoreCase(skillTrees.get(j).getSkillName())) {
                                    skillTrees.remove(j);
                                    break;
                                }
                            }
                        }

                        if (skillTrees.isEmpty()) {
                            return Collections.emptyList();
                        }

                        List<SkillLabelData> result = new ArrayList<>();
                        for (int i = StandartPrioritySkills.PROGRAMMING_LANGUAGE_INT;
                             i >= StandartPrioritySkills.DEFAULT_INT; i--) {
                            for (SkillTree st : skillTrees) {
                                if (!st.getNotParsing() && st.getPrioritySkill() != null
                                        && st.getPrioritySkill() == i) {
                                    Integer count = skillCounter.get(st);
                                    if (count != null && count > 0) {
                                        result.add(new SkillLabelData(
                                                st.getSkillName(), count,
                                                skillStyleForPriority(st.getPrioritySkill()),
                                                st.getComment(), count >= 2));
                                    }
                                }
                            }
                        }
                        return result;
                    }

                    @Override
                    public void done(List<SkillLabelData> result) {
                        skillsLoading = false;
                        skillsLoaded = true;
                        if (result == null || result.isEmpty()) {
                            return;
                        }
                        // UI thread — create fragment and render
                        Skillsbar skillBoxFragment = fragments.create(
                                JobCandidateEdit.this, Skillsbar.class);
                        if (skillBoxFragment.renderSkillLabels(result)) {
                            skillBox.add(skillBoxFragment.getFragment());
                        }
                    }

                    @Override
                    public boolean handleException(Exception exception) {
                        skillsLoading = false;
                        log.error("Unable to load candidate skills in background, " +
                                "candidateId={}", candidateId, exception);
                        return true;
                    }
                };

        backgroundWorker.handle(task).execute();
    }

    private static String skillStyleForPriority(Integer priority) {
        if (priority == null) return StandartPrioritySkills.DEFAULT_STYLE;
        switch (priority) {
            case -1: return StandartPrioritySkills.NOT_USED_SKILLS_STYLE;
            case 0:  return StandartPrioritySkills.DEFAULT_STYLE;
            case 1:  return StandartPrioritySkills.SUBJECT_AREA_STYLE;
            case 2:  return StandartPrioritySkills.FRAMEWORKS_STYLE;
            case 3:  return StandartPrioritySkills.METHODOLORY_STYLE;
            case 4:  return StandartPrioritySkills.PROGRAMMING_LANGUAGE_STYLE;
            default: return StandartPrioritySkills.NOT_USED_SKILLS_STYLE;
        }
    }

    private String getLastCVText() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            return null;
        }

        return dataManager.loadValue(
                "select e.textCV from hunttech_CandidateCV e where e.candidate = :candidate order by e.datePost desc",
                String.class)
                .parameter("candidate", getEditedEntity())
                .maxResults(1)
                .optional()
                .orElse(null);
    }

    private void setFrequentInteractionPopupButton() {
        if (frequentInteractionPopupButton != null) {
            int MAX_POPULAR_INTERACLION = 5;
            List<Iteraction> mostPopularInteraction = interactionService.getMostPolularIteraction(
                    userSessionSource.getUserSession().getUser(), MAX_POPULAR_INTERACLION);

            if (mostPopularInteraction.size() != 0) {
                int count = 1;
                for (Iteraction iteraction : mostPopularInteraction) {
                    frequentInteractionPopupButton.addAction(
                            new BaseAction(new StringBuilder()
                                    .append("setMostPopularInteractionPopupButton")
                                    .append("-")
                                    .append(count++)
                                    .toString())
                                    .withCaption(iteraction.getIterationName())
                                    .withHandler(actionPerformedEvent -> setMostPopularInteractionPopupButton(iteraction)));
                }
                frequentInteractionPopupButton.setEnabled(true);
            } else {
                frequentInteractionPopupButton.setEnabled(false);
            }
        }
    }

    public void setMostPopularInteractionPopupButton(Iteraction iteraction) {
        if (jobCandidateIteractionListTable != null) {
            if (jobCandidateIteractionListTable.getSingleSelected() != null) {
                Screen screen = screenBuilders.editor(jobCandidateIteractionListTable)
                        .newEntity()
                        .withInitializer(e -> {
                            e.setCandidate(getEditedEntity());
                            e.setIteractionType(iteraction);
                            e.setVacancy(jobCandidateIteractionListTable.getSingleSelected().getVacancy());
                        })
                        .build();

                screen.addAfterCloseListener(afterCloseEvent -> {
                    reloadInteractions();
                });
                screen.show();
            } else {
                Screen screen = screenBuilders.editor(jobCandidateIteractionListTable)
                        .newEntity()
                        .withInitializer(e -> {
                            e.setCandidate(getEditedEntity());
                            e.setIteractionType(iteraction);
                        })
                        .build();

                screen.addAfterCloseListener(afterCloseEvent -> {
                    reloadInteractions();
                });
                screen.show();
            }
        }
    }

    private void setLabelTitle() {
        String BEFORE = "";
        String AFTER = "&nbsp";

        jobTitleTitle.setValue(new StringBuilder().append(BEFORE).append(jobTitleTitle.getValue()).append(AFTER).toString());
        personPositionTitle.setValue(new StringBuilder().append(BEFORE).append(personPositionTitle.getRawValue()).append(AFTER).toString());
        emailTitle.setValue(new StringBuilder().append(BEFORE).append(emailTitle.getValue()).append(AFTER).toString());
        phoneTitle.setValue(new StringBuilder().append(BEFORE).append(phoneTitle.getValue()).append(AFTER).toString());
        telegramTitle.setValue(new StringBuilder().append(BEFORE).append(telegramTitle.getValue()).append(AFTER).toString());
        skypeTitle.setValue(new StringBuilder(BEFORE).append(skypeTitle.getValue()).append(AFTER).toString());
    }

    protected boolean isRequiredAddresField() {
        Boolean isEmptySN = false;

        if (tabContactInfoInitialized) {

            isEmptySN = ((emailField.getRawValue().equals("")) &&
                    (skypeNameField.getRawValue().equals("")) &&
                    (telegramNameField.getRawValue().equals("")) &&
                    (wiberNameField.getRawValue().equals("")) &&
                    (whatsupNameField.getRawValue().equals("")) &&
                    (mobilePhoneField.getRawValue().equals("")) &&
                    StringUtils.isBlank(getEditedEntity().getTelegramGroup()) &&
                    (phoneField.getRawValue().equals("")));
        }

        return isEmptySN;
    }

    private String setFullName(String firstName, String middleName, String secondName) {
        String fullName = "", localFirstName = "", localMiddleName = "", localSecondName = "";

        if (firstNameField.getValue() != null)
            localFirstName = firstNameField.getValue();
        else if (firstName != null)
            localFirstName = firstName;

        if (secondNameField.getValue() != null)
            localSecondName = secondNameField.getValue();
        else if (secondName != null)
            localSecondName = secondName;

        if (middleNameField.getValue() != null)
            localMiddleName = middleNameField.getValue();
        else if (middleName != null)
            localMiddleName = middleName;

        fullName = new StringBuilder().append(localSecondName)
                .append(" ")
                .append(localFirstName)
                .append(" ")
                .append(localMiddleName)
                .toString();

        return fullName;
    }

    private void setLabelFullName(String fullName) {
        getEditedEntity().setFullName(fullName);
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        // The sidebar is outside the lazy tabs, so populate it directly after screen data is ready.
        updateCandidateProfileLabels(getEditedEntity());
        setPercentLabel();

        Boolean b = getEditedEntity().getBlockCandidate() == null ?
                false : blockCandidateCheckBox.getValue();
        setBlockUnblockButton(b);

        // Запуск фоновой загрузки и анализа резюме для блока навыков (Skillsbar).
        // Операция вынесена из onBeforeShow, чтобы SQL-запрос полного textCV
        // и сопоставление навыков не блокировали открытие формы.
        // UI-компоненты Skillsbar создаются только в done() BackgroundTask.
        startSkillsBackgroundLoading();
    }

    @Subscribe
    public void onAfterClose(AfterCloseEvent event) {
        // чтоб после закрытия не возникало
        jobCandidateCandidateCvsDc.addCollectionChangeListener(e -> {
            if (jobCandidateCandidateCvTable != null) {
                jobCandidateCandidateCvTable.repaint();
            }
        });
    }

    private void setPercentLabel() {
        JobCandidate candidate = getEditedEntity();
        if (candidate != null) {
            // The calculation reads only the already loaded edited entity and never initializes lazy tabs.
            labelQualityPercent.setValue(calculateCardCompletionPercentage(candidate) + "%");
        }
    }

    protected void enableDisableContacts() {
        if (!tabContactInfoInitialized) {
            return;
        }

        Boolean flag = true;

        for (SocialNetworkURLs s : jobCandidateSocialNetworksDc.getItems()) {
            if (s.getNetworkURLS() != null) {
                if (!s.getNetworkURLS().equals("")) {
                    flag = false;
                    break;
                }
            }
        }

/*        skypeNameField.setRequired(true);
        phoneField.setRequired(true);
        mobilePhoneField.setRequired(true);
        emailField.setRequired(true);
        telegramNameField.setRequired(true);
        whatsupNameField.setRequired(true);
        wiberNameField.setRequired(true); */

        if (!isRequiredAddresField() || !flag) {
            skypeNameField.setRequired(false);
            phoneField.setRequired(false);
            mobilePhoneField.setRequired(false);
            emailField.setRequired(false);
            telegramNameField.setRequired(false);
            whatsupNameField.setRequired(false);
            wiberNameField.setRequired(false);
        }
    }

    @Subscribe
    public void onAfterShow1(AfterShowEvent event) {
    }

    // загрузить таблицу взаимодействий
    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        initTabCandidate();

        // Heavy comments data is loaded lazily from initTabComments() when the user opens the tab.
        // если есть резюме, то поставить галку
        if (!PersistenceHelper.isNew(getEditedEntity())) {
            boolean hasCv = hasCandidateCv();
            String locale = userSession.getLocale().getLanguage();
            boolean isRussian = "ru".equals(locale);
            String yes = isRussian ? "ДА" : "YES";
            String no = isRussian ? "НЕТ" : "NO";
            labelCV.setValue(hasCv ? yes : no);
        }

        // обнулить статус для вновь создаваемного кандидата
        if (PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setStatus(0);
        }

        setSaveRecordOfViewCandidate();

        setLabelTitle();
        setRatingLabel(getEditedEntity());

        setLinkButtonEmail();
        setLinkButtonTelegrem();
        setLinkButtonTelegremGroup();
        setLinkButtonSkype();

        setCandidatePicImage();
        checkTelegramName();
        initCandidateSkillsSidebar();

        lastIteraction = interactionService.getLastIteraction(getEditedEntity());
        lastIteractionLoaded = true;

        if (getRoleService.isUserRoles(userSession.getUser(), StandartRoles.MANAGER) ||
                getRoleService.isUserRoles(userSession.getUser(), StandartRoles.ADMINISTRATOR)) {
            blockCandidateButton.setVisible(true);
        } else {
            blockCandidateButton.setVisible(false);
        }

//        setLaborAgreement();
    }

    /**
     * Заполняет раздел «Основные навыки» в сайдбаре карточки кандидата цветными бейджами.
     */
    private void initCandidateSkillsSidebar() {
        if (candidateSkillsLabels == null) {
            return;
        }
        JobCandidate candidate = getEditedEntity();
        if (candidate == null || PersistenceHelper.isNew(candidate)) {
            candidateSkillsLabels.setValue("<span style='color: #7f8c8d; font-size: 11px;'>Навыки не определены</span>");
            return;
        }

        try {
            List<CandidateSkill> skills = dataManager.load(CandidateSkill.class)
                    .query("select e from hunttech_CandidateSkill e where e.candidate = :candidate order by e.priority, e.skill.skillName")
                    .parameter("candidate", candidate)
                    .view("candidateSkill-view")
                    .list();

            if (skills.isEmpty()) {
                candidateSkillsLabels.setValue("<span style='color: #7f8c8d; font-size: 11px;'>Навыки не определены</span>");
                return;
            }

            StringBuilder sb = new StringBuilder("<div style='display: flex; flex-wrap: wrap; gap: 4px; padding: 2px 0;'>");
            String[] palette = new String[]{
                    "#2b82c9", "#27ae60", "#8e44ad", "#d35400", "#16a085", "#2c3e50", "#e67e22", "#2980b9"
            };
            for (CandidateSkill cs : skills) {
                if (cs.getSkill() != null && cs.getSkill().getSkillName() != null) {
                    String skillName = cs.getSkill().getSkillName();
                    String color = palette[Math.abs(skillName.hashCode()) % palette.length];
                    String priorityIcon = cs.getPriority() == CandidateSkillPriority.MAIN ? "★ " : "";
                    sb.append(String.format(
                            "<span style='background: %s18; color: %s; border: 1px solid %s44; " +
                            "padding: 2px 7px; border-radius: 12px; font-size: 11px; font-weight: 600; " +
                            "white-space: nowrap; display: inline-block;'>%s%s</span>",
                            color, color, color, priorityIcon, skillName
                    ));
                }
            }
            sb.append("</div>");
            candidateSkillsLabels.setValue(sb.toString());
        } catch (Exception ex) {
            candidateSkillsLabels.setValue("<span style='color: #7f8c8d; font-size: 11px;'>Навыки не определены</span>");
        }
    }

    private Resource vacancyFilterLookupPickerFieldOptionImageProvider(OpenPosition object) {
        Image imageResource = uiComponents.create(Image.class);
        imageResource.setWidth("20px");
        imageResource.setHeight("20px");
        imageResource.setScaleMode(Image.ScaleMode.FILL);

        if (object.getProjectName().getProjectLogo() == null) {
            return imageResource.createResource(ThemeResource.class)
                    .setPath("icons/no-company.png");
        }
        return FileDescriptorImageHelper.createCompanyLogoResource(imageResource, fileLoader,
                object.getProjectName().getProjectLogo());
    }

    private void setIteractionListVacancyFilter() {
        if (!PersistenceHelper.isNew(getEditedEntity())) {
            Set<OpenPosition> openPositions = new HashSet<>();

            for (IteractionList iteractionList : getEditedEntity().getIteractionList()) {
                if (iteractionList.getVacancy() != null) {
                    openPositions.add(iteractionList.getVacancy());
                }
            }

            List<OpenPosition> op = new ArrayList<>(openPositions);
            vacancyFilterLookupPickerField.setOptionsList(op);

            vacancyFilterLookupPickerField.addValueChangeListener(e -> {
                if (vacancyFilterLookupPickerField.getValue() != null) {
                    jobCandidateIteractionDc.setDisconnectedItems(getEditedEntity().getIteractionList());

                    List<IteractionList> filtered = getEditedEntity()
                            .getIteractionList()
                            .stream()
                            .filter(iteractionList ->
                                    iteractionList.getVacancy() != null ?
                                        iteractionList.getVacancy().equals(vacancyFilterLookupPickerField.getValue()) : false
                            )
                            .collect(Collectors.toList());

                    jobCandidateIteractionDc.setDisconnectedItems(filtered);
                } else {
                    jobCandidateIteractionDc.setDisconnectedItems(getEditedEntity().getIteractionList());
                }

                jobCandidateIteractionListTable.repaint();
            });
        }
    }

    private void setAddSocialNetworkButtonEnable() {
        if (getEditedEntity().getSocialNetwork() != null) {
            if (getEditedEntity().getSocialNetwork().size() == 0) {
                addSocialNetworkListsButton.setEnabled(true);
            } else {
                addSocialNetworkListsButton.setEnabled(false);
            }
        } else {
            addSocialNetworkListsButton.setEnabled(true);
        }
    }

    private void setSaveRecordOfViewCandidate() {
        if (!PersistenceHelper.isNew(getEditedEntity())) {
            /* TODO надо сделать лог доступа к карточке. Кто создал, кто изменил, кто просмотртел */
        }
    }

    private void initInteractionCommentDl() {
        interactionCommentDl.setParameter("candidate", getEditedEntity());
        interactionCommentDl.setParameter("comment", null);
        interactionCommentDl.load();
        renderComments();
    }

    /**
     * Ensures that a missing file in storage cannot leave a broken avatar in the sidebar.
     * A valid FileDescriptor is rendered by the component's ValueSource; the controller only
     * forces the fallback for null descriptors and descriptors whose binary file is unavailable.
     */
    private void setCandidatePicImage() {
        FileDescriptor faceImage = getEditedEntity().getFileImageFace();
        if (!FileDescriptorImageHelper.fileExists(fileLoader, faceImage)) {
            candidatePic.applyFallback();
        }
    }

    /**
     * Shows the fallback immediately when the user clears the uploaded photo. The entity value
     * is still changed by the upload field's standard data binding and is saved with the editor.
     */
    @Subscribe("fileImageFaceUpload")
    public void onFileImageFaceUploadBeforeValueClear(FileUploadField.BeforeValueClearEvent event) {
        candidatePic.applyFallback();
    }

    private void setSuggestOpenPositionTable() {
        List<Position> positions = new ArrayList<>();

        suggestVacancyTable.addStyleName("borderless");
        suggestVacancyTable.addStyleName("no-horizontal-lines");
        suggestVacancyTable.addStyleName("no-vertical-lines");

        if (!PersistenceHelper.isNew(getEditedEntity())) {
            List<JobCandidatePositionLists> positionListsForSuggest = ensurePositionListLoaded();
            if (positionListsForSuggest != null) {
                for (JobCandidatePositionLists positionLists : positionListsForSuggest) {
                    positions.add(positionLists.getPositionList());
                }

                Position mainPosition = getEditedEntity().getPersonPosition();
                if (mainPosition != null) {
                    suggestOpenPositionDl.setParameter("positionType", mainPosition);
                } else {
                    suggestOpenPositionDl.removeParameter("positionType");
                }
                if (positions.size() > 0) {
                    suggestOpenPositionDl.setParameter("positionTypes", positions);
                } else {
                    suggestOpenPositionDl.removeParameter("positionTypes");
                }

            }
        } else {
            suggestOpenPositionDl.setMaxResults(1);
            lastProjects.setVisible(false);
            suggestVacancyTable.setVisible(false);
            lastProjectTable.setVisible(false);
            dictionatysTavlesHBox.setVisible(false);
        }

        suggestOpenPositionDl.load();
    }

    private void setLastProjectOfCandidate() {
        lastProjectTable.addStyleName("borderless");
        lastProjectTable.addStyleName("no-horizontal-lines");
        lastProjectTable.addStyleName("no-vertical-lines");
    }

    private void checkContactsCandidateListener() {
        jobCandidateCandidateCvsDc.addCollectionChangeListener(e -> {
            scanContactsFromCVs();
        });
    }

    @Subscribe
    public void onBeforeClose1(BeforeCloseEvent event) {
        // удалить листенер изменения, чтобы  не пугало сообщение о ненадйенности новых контактов в резюмехе
        jobCandidateCandidateCvsDc.addCollectionChangeListener(e -> {
            if (jobCandidateCandidateCvTable != null) {
                jobCandidateCandidateCvTable.repaint();
            }
        });
    }

/*    private void workStatusRadioButtonInit() {
        Map<String, Integer> workStatusMap = new LinkedHashMap<>();

        workStatusMap.put("Неопределен", 0);
        workStatusMap.put("Самозанятый", 1);
        workStatusMap.put("Индивидуальный предприниматель", 2);
        workStatusMap.put("Срочный трудовой договор", 3);
        workStatusMap.put("Договор ГПХ", 4);
        workStatusMap.put("В штат по ТК РФ", 5);

        workStatusRadioButton.setOptionsMap(workStatusMap);

        lastIteractionCount = 1;
    } */

    private void priorityCommenicationMethodRadioButtonInit() {
        Map<String, Integer> priorityMap = new LinkedHashMap<>();

        priorityMap.put("Email", 1);
        priorityMap.put("Phone", 2);
        priorityMap.put("Telegramm", 3);
        priorityMap.put("Skype", 4);
        priorityMap.put("Viber", 5);
        priorityMap.put("WhatsApp", 6);
        priorityMap.put("Social Network", 7);
        priorityMap.put("Other", 9);

        priorityCommunicationMethodRadioButton.setOptionsMap(priorityMap);
    }

    private void checkNotUsePosition() {
        if (personPositionField != null) {
            if (personPositionField.getValue() != null) {
                if (personPositionField.getValue().getPositionRuName().contains("не использовать")) {
                    personPositionField.setValue(null);
                }
            }
        }
    }

    @Subscribe
    public void onBeforeCommitChanges1(BeforeCommitChangesEvent event) {
        JobCandidate jobCandidate = checkDublicateCandidate();

        if (jobCandidate != null && PersistenceHelper.isNew(getEditedEntity())) {
            StringBuilder sb = new StringBuilder();

            sb.append("В базе уже присутствует кандидат ")
                    .append(firstNameField.getValue())
                    .append(" ")
                    .append(secondNameField.getValue())
                    .append("\n с заимаемой позицией ")
                    .append(personPositionField.getValue().getPositionRuName())
                    .append(" из города ")
                    .append(jobCityCandidateField.getValue().getCityRuName())
                    .append(".")
                    .append("\nПродолжить сохранение?");
            dialogs.createOptionDialog()
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withMessage(sb.toString())
                    .withActions(new DialogAction(DialogAction.Type.OK, Action.Status.PRIMARY).withHandler(e -> {
                        event.resume();
                        // вернуться и не закомитить
                    }), new DialogAction(DialogAction.Type.CANCEL).withHandler(f -> {
                        event.preventCommit();
                    }))
                    .show();

            event.preventCommit();
        }
    }


    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {

        replaceE_yo();
        setFullNameCandidate();

        checkTelegramName();
        trimTelegramName();

        addIteractionOfNewCandidate();
    }

    private void checkTelegramName() {
        String telegramName = telegramNameField != null
                ? telegramNameField.getValue()
                : getEditedEntity().getTelegramName();
        if (telegramName != null
                && telegramName.toLowerCase().startsWith(TELEGRAM_NAME_URL.toLowerCase())) {
            String normalizedTelegramName = telegramName.substring(TELEGRAM_NAME_URL.length());
            if (telegramNameField != null) {
                telegramNameField.setValue(normalizedTelegramName);
            } else {
                getEditedEntity().setTelegramName(normalizedTelegramName);
            }
        }
    }

    private void replaceE_yo() {
        if (secondNameField != null) {
            secondNameField.setValue(replaceE_E(secondNameField.getValue()));
        }

        if (firstNameField != null) {
            firstNameField.setValue(replaceE_E(firstNameField.getValue()));
        }

        if (middleNameField != null) {
            if (middleNameField.getValue() != null)
                middleNameField.setValue(replaceE_E(middleNameField.getValue()));
        }
    }


    private void addIteractionOfNewCandidate() {
        if (PersistenceHelper.isNew(getEditedEntity()) && !initialInteractionAdded) {
            // добавить сюда Iteraction "Новый кандиат"
            IteractionList iteractionList = metadata.create(IteractionList.class);

            iteractionList.setCandidate(getEditedEntity());
            iteractionList.setDateIteraction(new Date());
            iteractionList.setRecrutier((ExtUser) userSession.getUser());
            iteractionList.setRecrutierName(userSession.getUser().getName());
            iteractionList.setRating(4);

            BigDecimal numberIteraction;

            try {
                numberIteraction = dataManager
                        .loadValue("select max(e.numberIteraction) from hunttech_IteractionList e", BigDecimal.class)
                        .one();
            } catch (Exception e) {
                numberIteraction = BigDecimal.ONE;
            }

            iteractionList.setNumberIteraction(numberIteraction);

            Iteraction iteraction = null;
            OpenPosition openPosition = null;

            try {
                iteraction = dataManager.load(Iteraction.class)
                        .query("select e from hunttech_Iteraction e where e.iterationName like :iteractionName")
                        .view("iteraction-view")
                        .parameter("iteractionName", "Новый контакт")
                        .one();
            } catch (Exception e) {
                notifications.create(Notifications.NotificationType.ERROR)
                        .withCaption("Ошибка SQL")
                        .withDescription("Нет взаимодействия \"Новый контакт\"")
                        .show();
            }

            try {
                openPosition = openPositionService.getOpenPositionDefault();
/*                openPosition = dataManager.load(OpenPosition.class)
                        .query("select e from hunttech_OpenPosition e where e.vacansyName like :vacansyDefaultName")
                        .view("openPosition-view")
                        .parameter("vacansyDefaultName", "Default%")
                        .one(); */
            } catch (Exception e) {
                notifications.create(Notifications.NotificationType.ERROR)
                        .withCaption("Ошибка SQL")
                        .withDescription("Нет вакансии \"по умолчанию\" Default")
                        .show();
            }

            if (iteraction != null && openPosition != null) {
                iteractionList.setIteractionType(iteraction);
                iteractionList.setVacancy(openPosition);

                jobCandidateIteractionDc.getMutableItems().add(dataContext.merge(iteractionList));
                initialInteractionAdded = true;
            }
        }
    }

    private JobCandidate checkDublicateCandidate() {
        if (firstNameField != null &&
                secondNameField != null &&
                jobCityCandidateField != null &&
                personPositionField != null) {
            String queryStr = "select e from hunttech_JobCandidate e " +
                    "where e.firstName = :firstName and " +
                    "not e.id in " +
                    "(select f.id from hunttech_JobCandidate f where f.id = :uuid) and " +
                    "e.secondName = :secondName and " +
                    "e.cityOfResidence = :cityOfResidence and " +
                    "e.personPosition = :personPosition";
            List<JobCandidate> jobCandidates = dataManager.load(JobCandidate.class)
                    .query(queryStr)
                    .parameter("firstName", firstNameField.getValue())
                    .parameter("secondName", secondNameField.getValue())
                    .parameter("cityOfResidence", jobCityCandidateField.getValue())
                    .parameter("personPosition", personPositionField.getValue())
                    .parameter("uuid", getEditedEntity().getId())
                    .view("jobCandidate-view")
                    .list();

            if (jobCandidates.size() == 0) {
                return null;
            } else {
                return jobCandidates.get(0);
            }
        } else {
            return null;
        }
    }

    private void trimTelegramName() {
        String telegramName = telegramNameField != null
                ? telegramNameField.getValue()
                : getEditedEntity().getTelegramName();
        if (telegramName != null) {
            String trimmedTelegramName = telegramName.trim();
            String normalizedTelegramName = trimmedTelegramName.startsWith("@")
                    ? trimmedTelegramName.substring(1)
                    : trimmedTelegramName;
            if (telegramNameField != null) {
                telegramNameField.setValue(normalizedTelegramName);
            } else {
                getEditedEntity().setTelegramName(normalizedTelegramName);
            }
        }
    }

    String replaceE_E(String str) {
        return str.replace('ё', 'e');
    }

    @Subscribe(target = Target.DATA_CONTEXT)
    public void onChange(DataContext.ChangeEvent event) {
        setPercentLabel();
    }


    public Integer setQualityPercent() {
        return countFilledCardFields(getEditedEntity());
    }

    /**
     * Calculates a rounded 0..100 percentage using only the edited entity already held by
     * the screen DataContext. Fifteen property reads are synchronous by design: scheduling
     * a BackgroundTask would cost more and would introduce an unnecessary UI race.
     */
    static int calculateCardCompletionPercentage(JobCandidate candidate) {
        if (candidate == null || CARD_COMPLETION_PROPERTIES.isEmpty()) {
            return 0;
        }
        return (int) Math.round(countFilledCardFields(candidate) * 100.0
                / CARD_COMPLETION_PROPERTIES.size());
    }

    static int countFilledCardFields(JobCandidate candidate) {
        if (candidate == null) {
            return 0;
        }

        int filledFields = 0;
        for (String property : CARD_COMPLETION_PROPERTIES) {
            if (isFilledCardValue(readCandidatePropertySafely(candidate, property))) {
                filledFields++;
            }
        }
        return filledFields;
    }

    /**
     * Reads a property without forcing an unfetched detached association to load. The screen
     * view normally contains every completion property; the catch protects alternate invocation
     * contexts and stale detached instances from breaking form rendering.
     */
    private static Object readCandidatePropertySafely(JobCandidate candidate, String property) {
        try {
            return candidate.getValue(property);
        } catch (IllegalStateException e) {
            // Detached entities may contain an unfetched association; it counts as empty, not as a screen error.
            return null;
        }
    }

    /**
     * Treats null and whitespace-only text as empty; dates, references and option values are
     * filled when non-null.
     */
    private static boolean isFilledCardValue(Object value) {
        if (value instanceof CharSequence) {
            return StringUtils.isNotBlank((CharSequence) value);
        }
        return value != null;
    }

    /**
     * Handles replacement of the edited item in the data container. Profile labels are refreshed
     * here because they are not bound in XML and must not depend on whether {@code tabMain} was built.
     */
    @Subscribe(id = "jobCandidateDc", target = Target.DATA_CONTAINER)
    private void onJobCandidateDcItemChange(InstanceContainer.ItemChangeEvent<JobCandidate> event) {
        if (event.getItem() != null) {
            setFullNameCandidate();
        }
        updateCandidateProfileLabels(event.getItem());
    }

    /**
     * Keeps the always-visible sidebar synchronized with edits made through lazy-created fields.
     * Listening to the data container also covers programmatic changes that bypass field events.
     */
    @Subscribe(id = "jobCandidateDc", target = Target.DATA_CONTAINER)
    private void onJobCandidateDcItemPropertyChange(
            InstanceContainer.ItemPropertyChangeEvent<JobCandidate> event) {
        if ("firstName".equals(event.getProperty())
                || "secondName".equals(event.getProperty())) {
            String fullName = formatCandidateProfileName(
                    event.getItem().getSecondName(), event.getItem().getFirstName());
            // fullName is a denormalized entity attribute and must match the visible sidebar value.
            if (!Objects.equals(event.getItem().getFullName(), fullName)) {
                event.getItem().setFullName(fullName);
            }
            updateCandidateProfileLabels(event.getItem());
        } else if ("personPosition".equals(event.getProperty())) {
            updateCandidateProfileLabels(event.getItem());
        }
    }

    private void setFullNameCandidate() {
        String space = " ";

        if (getEditedEntity().getSecondName() != null &&
                getEditedEntity().getFirstName() != null) {
            getEditedEntity().setFullName(new StringBuilder()
                    .append(getEditedEntity().getSecondName())
                    .append(space)
                    .append(getEditedEntity().getFirstName())
                    .toString());
        }
    }

    public void onCardAuditInfoClick() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        JobCandidate candidate = getEditedEntity();
        StringBuilder sb = new StringBuilder("Автор карточки: ");
        if (candidate.getCreatedBy() != null) {
            sb.append(candidate.getCreatedBy());
        } else {
            sb.append("—");
        }
        if (candidate.getCreateTs() != null) {
            sb.append(" (").append(simpleDateFormat.format(candidate.getCreateTs())).append(")");
        }
        if (candidate.getUpdatedBy() != null) {
            sb.append("\nПоследние изменения: ").append(candidate.getUpdatedBy());
            if (candidate.getUpdateTs() != null) {
                sb.append(" (").append(simpleDateFormat.format(candidate.getUpdateTs())).append(")");
            }
        }
        notifications.create()
                .withCaption("Информация о карточке")
                .withDescription(sb.toString())
                .show();
    }

    public void onButtonSubscribeClick() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            dialogs.createOptionDialog()
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withMessage("Записать изменения?")
                    .withActions(
                            new DialogAction(DialogAction.Type.YES, DialogAction.Status.PRIMARY)
                                    .withHandler(e -> {
                                        commitChanges();

                                        screenBuilders.editor(SubscribeCandidateAction.class, this)
                                                .newEntity()
                                                .withInitializer(g -> {
                                                    g.setCandidate(getEditedEntity());
                                                    g.setSubscriber((ExtUser) userSession.getUser());
                                                    g.setStartDate(new Date());
                                                })
                                                .withOpenMode(OpenMode.DIALOG)
                                                .withParentDataContext(dataContext)
                                                .build()
                                                .show();
                                    }),
                            new DialogAction(DialogAction.Type.NO)
                                    .withHandler(f -> {
                                        closeWithDiscard();
                                    }))
                    .show();
        } else {
            screenBuilders.editor(SubscribeCandidateAction.class, this)
                    .newEntity()
                    .withOpenMode(OpenMode.DIALOG)
                    .withParentDataContext(dataContext)
                    .withInitializer(e -> {
                        e.setCandidate(getEditedEntity());
                        e.setSubscriber((ExtUser) userSession.getUser());
                        e.setStartDate(new Date());
                    })
                    .build()
                    .show();
        }
    }

    @Subscribe
    public void onInit(InitEvent event) {
        preventAutoLoadUntilReady(openPositionDl, () -> openPositionLoaderInitialized);
        preventAutoLoadUntilReady(citiesDl, () -> referenceLoadersInitialized);
        preventAutoLoadUntilReady(personPositionsLc, () -> referenceLoadersInitialized);
        // Запросы вкладки содержат параметры кандидата и позиции, поэтому
        // блокируем автоматическую загрузку до первого открытия вкладки.
        preventAutoLoadUntilReady(lastProjectDl, () -> positionsTabLoaded);
        preventAutoLoadUntilReady(suggestOpenPositionDl, () -> positionsTabLoaded);
        preventAutoLoadUntilReady(interactionCommentDl, () -> commentsTabInitialized);

        configureAvailableComponentRenderers();

        tabSheetSocialNetworks.addSelectedTabChangeListener(selectedTabChangeEvent -> {
            configureAvailableComponentRenderers();
            initTabResume();
            initTabInteractions();
            initTabCandidate();
            initTabContactInfo();
            initTabComments();
            initTabPositions();
            updateCandidateNavigationActiveState();
        });
        updateCandidateNavigationActiveState();

    }

    void configureAvailableComponentRenderers() {
        configureComponentRenderer("socialNetworkTable", "socialNetworkLogoColumn",
                this::socialNetworkTableSocialNetworkLogoColumnColumnGenerator);
        configureComponentRenderer("socialNetworkTable", "linkToWeb",
                this::socialNetworkTableLinkToWebColumnGenerator);
        configureComponentRenderer("jobCandidateIteractionListTable", "projectLogoColumn",
                this::jobCandidateIteractionListTableProjectLogoColumnColumnGenerator);
        configureComponentRenderer("jobCandidateCandidateCvTable", "projectLogoColumn",
                this::jobCandidateCandidateCvTableProjectLogoColumnColumnGenerator);
        configureComponentRenderer("jobCandidateCandidateCvTable", "candidateOriginalCVColumn",
                this::jobCandidateCandidateCvTableCandidateOriginalCvColumnGenerator);
        configureComponentRenderer("jobCandidateCandidateCvTable", "candidateHuntTechCVColumn",
                this::jobCandidateCandidateCvTableCandidateHuntTechCvColumnGenerator);
    }

    @SuppressWarnings("unchecked")
    <E extends Entity> void configureComponentRenderer(String dataGridId,
                                                        String columnId,
                                                        DataGrid.GenericColumnGenerator<E, Object> generator) {
        Component component = getWindow().getComponent(dataGridId);
        if (!(component instanceof DataGrid)) {
            return;
        }

        DataGrid<E> dataGrid = (DataGrid<E>) component;
        DataGrid.Column<E> column = dataGrid.getColumn(columnId);
        if (column != null && dataGrid.getColumnGenerator(columnId) == null) {
            column.setColumnGenerator(generator);
        }
    }

    private <E extends Entity> void preventAutoLoadUntilReady(CollectionLoader<E> loader,
                                                               java.util.function.BooleanSupplier ready) {
        loader.addPreLoadListener(e -> {
            if (!ready.getAsBoolean()) {
                e.preventLoad();
            }
        });
    }

    // Блокирует автоматическую загрузку KeyValue loader до установки обязательных параметров.
    private void preventAutoLoadUntilReady(KeyValueCollectionLoader loader,
                                            java.util.function.BooleanSupplier readySupplier) {
        loader.addPreLoadListener(loadEvent -> {
            if (!readySupplier.getAsBoolean()) {
                loadEvent.preventLoad();
            }
        });
    }

    private void ensureReferenceLoadersLoaded() {
        if (referenceLoadersInitialized) {
            return;
        }
        referenceLoadersInitialized = true;
        citiesDl.load();
        personPositionsLc.load();
    }

    private void ensureOpenPositionLoaded() {
        if (openPositionLoaderInitialized) {
            return;
        }
        openPositionLoaderInitialized = true;
        openPositionDl.load();
    }

    private boolean hasCandidateCv() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            return false;
        }
        List<CandidateCV> cvs = getEditedEntity().getCandidateCv();
        return cvs != null && !cvs.isEmpty();
    }

    private double loadAverageRating() {
        if (PersistenceHelper.isNew(getEditedEntity())
                || getEditedEntity().getId() == null) {
            return 0.0;
        }
        UUID candidateId = getEditedEntity().getId();
        Double avg = dataManager.loadValue(
                "select avg(e.rating + 1) from hunttech_IteractionList e where e.candidate.id = :candidateId and e.rating is not null",
                Double.class)
                .parameter("candidateId", candidateId)
                .one();
        return avg != null ? avg.doubleValue() : 0.0;
    }

    private List<CandidateCV> ensureCandidateCvLoaded() {
        if (candidateCvLoaded || PersistenceHelper.isNew(getEditedEntity())) {
            return getEditedEntity().getCandidateCv() != null ?
                    getEditedEntity().getCandidateCv() : Collections.emptyList();
        }

        List<CandidateCV> candidateCvs = dataManager.load(CandidateCV.class)
                .query("select e from hunttech_CandidateCV e " +
                        "where e.candidate = :candidate " +
                        "order by e.datePost desc")
                .parameter("candidate", getEditedEntity())
                .view("candidateCV-browse-view")
                .list();

        List<CandidateCV> mergedCandidateCvs = candidateCvs.stream()
                .map(dataContext::merge)
                .collect(Collectors.toList());
        getEditedEntity().setCandidateCv(mergedCandidateCvs);
        candidateCvLoaded = true;
        return mergedCandidateCvs;
    }

    private List<IteractionList> ensureInteractionsLoaded() {
        if (interactionsLoaded || PersistenceHelper.isNew(getEditedEntity())) {
            return getEditedEntity().getIteractionList() != null ?
                    getEditedEntity().getIteractionList() : Collections.emptyList();
        }

        List<IteractionList> interactions = dataManager.load(IteractionList.class)
                .query("select e from hunttech_IteractionList e " +
                        "where e.candidate = :candidate " +
                        "order by e.numberIteraction desc")
                .parameter("candidate", getEditedEntity())
                .view("iteractionList-job-candidate")
                .list();

        List<IteractionList> mergedInteractions = interactions.stream()
                .map(dataContext::merge)
                .collect(Collectors.toList());
        getEditedEntity().setIteractionList(mergedInteractions);
        jobCandidateIteractionDc.setDisconnectedItems(mergedInteractions);
        interactionsLoaded = true;
        return mergedInteractions;
    }

    /**
     * Инициализирует историю рассмотрения и подходящие вакансии только при первом
     * открытии вкладки. Открытие JobCandidateEdit не выполняет эти запросы.
     */
    private void initTabPositions() {
        TabSheet.Tab selectedTab = tabSheetSocialNetworks.getSelectedTab();
        if (selectedTab == null || !"tabPositions".equals(selectedTab.getName())) {
            return;
        }

        if (!positionsTabInitialized) {
            lastProjectTable = (Table) getWindow().getComponentNN("lastProjectTable");
            suggestVacancyTable = (Table<OpenPosition>) getWindow().getComponentNN("suggestVacancyTable");
            suggestVacancyTable.setItemDescriptionProvider(this::suggestVacancyTableItemDescriptionProvider);
            suggestVacancyTable.getColumn("notSendedIconColumn")
                    .setColumnGenerator(this::suggestVacancyTableNotSendedIconColumnColumnGenerator);
            positionsTabInitialized = true;
        }

        if (positionsTabLoaded || positionsTabLoading) {
            return;
        }

        if (PersistenceHelper.isNew(getEditedEntity()) || getEditedEntity().getId() == null) {
            positionsTabLoaded = true;
            lastProjectTable.setVisible(false);
            suggestVacancyTable.setVisible(false);
            return;
        }

        startPositionsBackgroundLoading();
    }

    /**
     * В фоне агрегирует только скалярные значения взаимодействий. Entity-графы
     * кандидата, резюме и вакансий между потоками не передаются.
     */
    private void startPositionsBackgroundLoading() {
        if (positionsTabLoading || positionsTabLoaded) {
            return;
        }

        UUID candidateId = getEditedEntity().getId();
        positionsTabLoading = true;

        BackgroundTask<Void, Map<UUID, HistoryRowData>> task =
                new BackgroundTask<Void, Map<UUID, HistoryRowData>>(
                        60, TimeUnit.SECONDS, this) {
                    @Override
                    public Map<UUID, HistoryRowData> run(TaskLifeCycle<Void> taskLifeCycle) {
                        DataManager bgDataManager = AppBeans.get(DataManager.class);
                        List<KeyValueEntity> rows = bgDataManager.loadValues(
                                "select vacancy.id, vacancy.vacansyName, e.dateIteraction, " +
                                        "interactionType.iterationName, " +
                                        "interactionType.signOurInterviewAssigned, " +
                                        "interactionType.signOurInterview, " +
                                        "recruiter.name, e.recrutierName " +
                                        "from hunttech_IteractionList e " +
                                        "left join e.vacancy vacancy " +
                                        "left join e.iteractionType interactionType " +
                                        "left join e.recrutier recruiter " +
                                        "where e.candidate.id = :candidateId " +
                                        "and vacancy.id is not null " +
                                        "and vacancy.vacansyName not like 'Default' " +
                                        "order by e.dateIteraction desc")
                                .properties(
                                        "vacancyId",
                                        "vacancyName",
                                        "dateIteraction",
                                        "interactionName",
                                        "signResearcher",
                                        "signRecruiter",
                                        "recruiterName",
                                        "legacyRecruiterName")
                                .parameter("candidateId", candidateId)
                                .list();
                        return buildHistoryRowData(rows);
                    }

                    @Override
                    public void done(Map<UUID, HistoryRowData> result) {
                        historyRowDataByVacancy = result != null
                                ? result : Collections.emptyMap();
                        positionsTabLoading = false;
                        positionsTabLoaded = true;

                        // UI-loader'ы запускаются только на UI-потоке и только
                        // после установки обязательных параметров.
                        try {
                            lastProjectDl.setParameter("candidate", getEditedEntity());
                            lastProjectDl.load();
                            setLastProjectOfCandidate();
                            setSuggestOpenPositionTable();
                            lastProjectTable.repaint();
                            suggestVacancyTable.repaint();
                        } catch (RuntimeException loaderException) {
                            // Разрешаем повторное открытие вкладки после временной ошибки БД.
                            positionsTabLoaded = false;
                            log.error("Не удалось применить данные вкладки позиций, candidateId={}",
                                    candidateId, loaderException);
                            notifications.create(Notifications.NotificationType.ERROR)
                                    .withCaption(messageBundle.getMessage("msgError"))
                                    .withDescription("Не удалось загрузить позиции и вакансии кандидата")
                                    .show();
                        }
                    }

                    @Override
                    public boolean handleException(Exception exception) {
                        positionsTabLoading = false;
                        log.error("Не удалось загрузить вкладку позиций кандидата, candidateId={}",
                                candidateId, exception);
                        notifications.create(Notifications.NotificationType.ERROR)
                                .withCaption(messageBundle.getMessage("msgError"))
                                .withDescription("Не удалось загрузить историю позиций кандидата")
                                .show();
                        return true;
                    }
                };

        backgroundWorker.handle(task).execute();
    }

    /** Один раз агрегирует значения для генераторов колонок истории. */
    private Map<UUID, HistoryRowData> buildHistoryRowData(List<KeyValueEntity> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<UUID, HistoryAccumulator> accumulators = new LinkedHashMap<>();
        for (KeyValueEntity row : rows) {
            UUID vacancyId = row.getValue("vacancyId");
            if (vacancyId == null) {
                continue;
            }

            HistoryAccumulator accumulator = accumulators.computeIfAbsent(
                    vacancyId,
                    id -> new HistoryAccumulator(id, row.getValue("vacancyName")));

            Date interactionDate = row.getValue("dateIteraction");
            if (accumulator.maxDate == null
                    || interactionDate != null && interactionDate.after(accumulator.maxDate)) {
                accumulator.maxDate = interactionDate;
                accumulator.lastInteractionName = row.getValue("interactionName");
            }

            String employeeName = row.getValue("recruiterName");
            if (employeeName == null || employeeName.trim().isEmpty()) {
                employeeName = row.getValue("legacyRecruiterName");
            }

            if (accumulator.researcherName == null
                    && Boolean.TRUE.equals(row.getValue("signResearcher"))) {
                accumulator.researcherName = employeeName;
            }
            if (accumulator.recruiterName == null
                    && Boolean.TRUE.equals(row.getValue("signRecruiter"))) {
                accumulator.recruiterName = employeeName;
            }
        }

        Map<UUID, HistoryRowData> result = new LinkedHashMap<>();
        for (HistoryAccumulator accumulator : accumulators.values()) {
            result.put(accumulator.vacancyId, new HistoryRowData(
                    accumulator.vacancyId,
                    accumulator.vacancyName,
                    accumulator.maxDate,
                    accumulator.lastInteractionName,
                    accumulator.researcherName,
                    accumulator.recruiterName));
        }
        return result;
    }

    /** Внутренняя изменяемая модель для единственного прохода по строкам JPQL. */
    private static final class HistoryAccumulator {
        final UUID vacancyId;
        final String vacancyName;
        Date maxDate;
        String lastInteractionName;
        String researcherName;
        String recruiterName;

        HistoryAccumulator(UUID vacancyId, String vacancyName) {
            this.vacancyId = vacancyId;
            this.vacancyName = vacancyName;
        }
    }

    private List<SocialNetworkURLs> ensureSocialNetworksLoaded() {
        if (socialNetworksLoaded || PersistenceHelper.isNew(getEditedEntity())) {
            return getEditedEntity().getSocialNetwork() != null ?
                    getEditedEntity().getSocialNetwork() : Collections.emptyList();
        }

        List<SocialNetworkURLs> socialNetworks = dataManager.load(SocialNetworkURLs.class)
                .query("select e from hunttech_SocialNetworkURLs e " +
                        "where e.jobCandidate = :candidate " +
                        "order by e.networkName")
                .parameter("candidate", getEditedEntity())
                .view("socialNetworkURLs-view")
                .list();

        List<SocialNetworkURLs> mergedSocialNetworks = socialNetworks.stream()
                .map(dataContext::merge)
                .collect(Collectors.toList());
        getEditedEntity().setSocialNetwork(mergedSocialNetworks);
        socialNetworksLoaded = true;
        return mergedSocialNetworks;
    }

    private List<JobCandidatePositionLists> ensurePositionListLoaded() {
        return getEditedEntity().getPositionList() != null ?
                getEditedEntity().getPositionList() : Collections.emptyList();
    }

    private void setupCurrentCompanySearchExecutor() {
        if (currentCompanyField == null) {
            return;
        }

        currentCompanyField.setSearchExecutor((searchString, searchParams) ->
                dataManager.load(Company.class)
                        .query("select e from hunttech_Company e " +
                                "where lower(e.comanyName) like lower(:searchString) " +
                                "order by e.comanyName, e.companyShortName")
                        .parameter("searchString", "%" + searchString + "%")
                        .view("company-picker-view")
                        .maxResults(50)
                        .list());
    }

    private void initTabComments() {
        if (commentsTabInitialized) {
            return;
        }
        TabSheet.Tab selectedTab = tabSheetSocialNetworks.getSelectedTab();
        if (selectedTab != null && "commentsTab".equals(selectedTab.getName())) {
            chatMessageTextField = (TextField<String>) getWindow().getComponentNN("chatMessageTextField");
            sendCommentButton = (Button) getWindow().getComponentNN("sendCommentButton");
            vacancyPopupPickerField = (LookupPickerField<OpenPosition>) getWindow()
                    .getComponentNN("vacancyPopupPickerField");
            jobCandidateCommentsContainer = (VBoxLayout) getWindow()
                    .getComponentNN("jobCandidateCommentsContainer");

            chatMessageTextField.addValueChangeListener(this::onChatMessageTextFieldValueChange);
            chatMessageTextField.addEnterPressListener(this::onChatMessageTextFieldEnterPress);
            vacancyPopupPickerField.setOptionIconProvider(this::vacancyPopupPickerFieldOptionIconProvider);

            commentsTabInitialized = true;
            ensureOpenPositionLoaded();
            initInteractionCommentDl();
        }
    }

    private void initTabContactInfo() {
        TabSheet.Tab selectedTab = tabSheetSocialNetworks.getSelectedTab();
        if (selectedTab == null || !"tabContactInfo".equals(selectedTab.getName())) {
            return;
        }
        if (tabContactInfoInitialized) {
            return;
        }

        emailField = (TextField<String>) getWindow().getComponentNN("emailField");
        phoneField = (TextField<String>) getWindow().getComponentNN("phoneField");
        mobilePhoneField = (TextField<String>) getWindow().getComponentNN("mobilePhoneField");
        telegramNameField = (TextField<String>) getWindow().getComponentNN("telegramNameField");
        whatsupNameField = (TextField<String>) getWindow().getComponentNN("whatsupNameField");
        wiberNameField = (TextField<String>) getWindow().getComponentNN("wiberNameField");
        skypeNameField = (TextField<String>) getWindow().getComponentNN("skypeNameField");
        priorityCommunicationMethodRadioButton = (RadioButtonGroup<Integer>) getWindow()
                .getComponentNN("priorityCommunicationMethodRadioButton");
        socialNetworkTable = (DataGrid<SocialNetworkURLs>) getWindow().getComponentNN("socialNetworkTable");
        addSocialNetworkListsButton = (Button) getWindow().getComponentNN("addSocialNetworkListsButton");

        emailField.addTextChangeListener(e -> enableDisableContacts());
        phoneField.addTextChangeListener(e -> enableDisableContacts());
        mobilePhoneField.addTextChangeListener(e -> enableDisableContacts());
        telegramNameField.addTextChangeListener(e -> enableDisableContacts());
        whatsupNameField.addTextChangeListener(e -> enableDisableContacts());
        wiberNameField.addTextChangeListener(e -> enableDisableContacts());
        skypeNameField.addTextChangeListener(e -> enableDisableContacts());

        phoneField.addValueChangeListener(this::onPhoneFieldValueChange);
        mobilePhoneField.addValueChangeListener(this::onMobilePhoneFieldValueChange1);
        emailField.addValueChangeListener(this::onEmailFieldValueChange);
        mobilePhoneField.addValueChangeListener(this::onMobilePhoneFieldValueChange);
        skypeNameField.addValueChangeListener(this::onSkypeNameFieldValueChange);
        telegramNameField.addValueChangeListener(this::onTelegramNameFieldValueChange);

        socialNetworkTable.addEditorCloseListener(e -> enableDisableContacts());
        socialNetworkTable.addEditorPostCommitListener(e -> enableDisableContacts());
        socialNetworkTable.addSelectionListener(e -> enableDisableContacts());

        priorityCommenicationMethodRadioButtonInit();
        ensureSocialNetworksLoaded();
        initSocialNeiworkTable();
        setAddSocialNetworkButtonEnable();
        trimTelegramName();
        tabContactInfoInitialized = true;
        enableDisableContacts();
    }

    public void initSocialNeiworkTable() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            if (!initSocialNetworkURLs) {
                List<SocialNetworkURLs> socialNetworkURLs = new ArrayList<>();
                List<SocialNetworkType> socialNetworkTypes = dataManager.load(SocialNetworkType.class)
                        .view("socialNetworkType-view")
                        .list();

                int endCount = socialNetworkTypes.size();
                for (int i = 0; i < endCount; i++) {
                    Boolean flag = true;
                    if (getEditedEntity().getSocialNetwork() != null) {
                        for (SocialNetworkURLs sn : getEditedEntity().getSocialNetwork()) {
                            if (socialNetworkTypes.get(i).equals(sn.getSocialNetworkURL())) {
                                flag = false;
                            }
                        }
                    }

                    if (flag) {
                        SocialNetworkURLs sn = metadata.create(SocialNetworkURLs.class);
                        sn.setSocialNetworkURL(socialNetworkTypes.get(i));
                        jobCandidateSocialNetworksDc.getMutableItems().add(sn);
                    }
                }

                dataContext.merge(jobCandidateSocialNetworksDc.getMutableItems());

                initSocialNetworkURLs = true;
            }
        }
    }

    private void initTabCandidate() {
        TabSheet.Tab selectedTab = tabSheetSocialNetworks.getSelectedTab();
        if (selectedTab == null || !"tabMain".equals(selectedTab.getName())) {
            return;
        }
        if (!candidateInitialized) {
            ensureReferenceLoadersLoaded();

            if (currentCompanyField == null) {
                currentCompanyField = (SuggestionPickerField<Company>) getWindow()
                        .getComponent("currentCompanyField");
            }
            setupCurrentCompanySearchExecutor();
            setupCurrentCompanyCreateAction();

            if (personPositionField == null) {
                personPositionField = (LookupPickerField<Position>) getWindow()
                        .getComponent("personPositionField");
            }

            if (jobCityCandidateField == null) {
                jobCityCandidateField = (LookupPickerField<City>) getWindow()
                        .getComponent("jobCityCandidateField");
            }

            if (birdhDateField == null) {
                birdhDateField = (DateField<Date>) getWindow().getComponent("birdhDateField");
            }

            if (secondNameField == null) {
                secondNameField = (SuggestionField<String>) getWindow().getComponent("secondNameField");
            }
            if (secondNameField != null) {
                secondNameField.setEnterActionHandler(searchString -> {
                    secondNameField.setValue(searchString);
                });

                secondNameField.addValueChangeListener(stringValueChangeEvent -> {
                    setLabelFullName(setFullName(null, null, getEditedEntity().getSecondName()));
                });
            }

            if (firstNameField == null) {
                firstNameField = (SuggestionField<String>) getWindow().getComponent("firstNameField");
            }
            if (firstNameField != null) {
                firstNameField.setEnterActionHandler(searchString -> {
                    firstNameField.setValue(searchString);
                });

                firstNameField.addValueChangeListener(stringValueChangeEvent -> {
                    setLabelFullName(setFullName(getEditedEntity().getFirstName(), null, null));
                });
            }

            if (middleNameField == null) {
                middleNameField = (SuggestionField<String>) getWindow().getComponent("middleNameField");
            }
            if (middleNameField != null) {
                middleNameField.setEnterActionHandler(searchString -> {
                    middleNameField.setValue(searchString);
                });

                middleNameField.addValueChangeListener(stringValueChangeEvent -> {
                    setLabelFullName(setFullName(null, getEditedEntity().getMiddleName(), null));
                });
            }

            if (positionsLabel == null) {
                positionsLabel = (CssLayout) getWindow().getComponent("positionsLabel");
            }
            if (positionsLabel != null) {
                setPositionsLabel();
            }

            setupNameSearchExecutors();
            checkNotUsePosition();
            updateFullNameField();
            updatePersonPositionLabel(personPositionField != null ? personPositionField.getValue() : null);

            if (firstNameField != null && secondNameField != null) {
                candidateInitialized = true;
            }
        }
    }

    private void setupCurrentCompanyCreateAction() {
        if (currentCompanyField == null || currentCompanyField.getAction(CREATE_COMPANY_ACTION_ID) != null) {
            return;
        }

        currentCompanyField.addAction(new BaseAction(CREATE_COMPANY_ACTION_ID)
                .withCaption(messageBundle.getMessage("msgCreateCompany"))
                .withDescription(messageBundle.getMessage("msgCreateCompanyDescription"))
                .withIcon(CubaIcon.CREATE_ACTION.source())
                .withHandler(actionPerformedEvent -> openCreateCompanyEditor()));
    }

    private void openCreateCompanyEditor() {
        if (companyEditorOpen) {
            return;
        }

        companyEditorOpen = true;
        Action createAction = currentCompanyField.getAction(CREATE_COMPANY_ACTION_ID);
        if (createAction != null) {
            createAction.setEnabled(false);
        }

        try {
            Screen companyEdit = screenBuilders.editor(Company.class, this)
                    .newEntity()
                    .withScreenId("hunttech_Company.edit")
                    .withOpenMode(OpenMode.DIALOG)
                    .withTransformation(this::mergeCreatedCompany)
                    .withField(currentCompanyField)
                    .build();

            companyEdit.addAfterCloseListener(afterCloseEvent -> resetCompanyCreateAction());
            companyEdit.show();
        } catch (RuntimeException e) {
            resetCompanyCreateAction();
            throw e;
        }
    }

    private void resetCompanyCreateAction() {
        if (companyEditorOpen) {
            companyEditorOpen = false;
            Action action = currentCompanyField.getAction(CREATE_COMPANY_ACTION_ID);
            if (action != null) {
                action.setEnabled(true);
            }
        }
    }

    private Company mergeCreatedCompany(Company company) {
        if (company == null || company.getId() == null) {
            return company;
        }

        Company mergedCompany = dataContext.merge(company);
        if (currentCompaniesDc != null && !currentCompaniesDc.containsItem(mergedCompany)) {
            currentCompaniesDc.getMutableItems().add(mergedCompany);
        } else if (currentCompaniesDc != null) {
            currentCompaniesDc.replaceItem(mergedCompany);
        }
        return mergedCompany;
    }

    public void repaintSocialNetworksTable() {
        if (socialNetworkTable != null) {
            socialNetworkTable.repaint();
        }
    }

    private void initTabInteractions() {
        TabSheet.Tab selectedTab = tabSheetSocialNetworks.getSelectedTab();
        if (selectedTab == null || !"tabIteraction".equals(selectedTab.getName())) {
            return;
        }
        if (!interationTabInitialized) {
            jobCandidateIteractionListTable = (DataGrid<IteractionList>) getWindow()
                    .getComponentNN("jobCandidateIteractionListTable");
            frequentInteractionPopupButton = (PopupButton) getWindow()
                    .getComponentNN("frequentInteractionPopupButton");
            copyIteractionButton = (Button) getWindow().getComponentNN("copyIteractionButton");
            vacancyFilterLookupPickerField = (LookupPickerField<OpenPosition>) getWindow()
                    .getComponentNN("vacancyFilterLookupPickerField");
            openPositionProjectDescriptionButton = (Button) getWindow()
                    .getComponentNN("openPositionProjectDescriptionButton");

            jobCandidateIteractionListTable.getColumn("currentOpenCloseColumn")
                    .setColumnGenerator(this::jobCandidateIteractionListTableCurrentOpenCloseColumnColumnGenerator);
            jobCandidateIteractionListTable.getColumn("currentOpenCloseColumn")
                    .setStyleProvider(this::jobCandidateIteractionListTableCurrentOpenCloseColumnStyleProvider);
            jobCandidateIteractionListTable.getColumn("currentOpenCloseColumn")
                    .setDescriptionProvider(this::jobCandidateIteractionListTableCurrentOpenCloseColumnDescriptionProvider);
            jobCandidateIteractionListTable.getColumn("vacancy")
                    .setStyleProvider(this::jobCandidateIteractionListTableVacancyStyleProvider);
            jobCandidateIteractionListTable.getColumn("iteractionType")
                    .setStyleProvider(this::jobCandidateIteractionListTableIteractionTypeStyleProvider);
            vacancyFilterLookupPickerField
                    .setOptionImageProvider(this::vacancyFilterLookupPickerFieldOptionImageProvider);

            ensureInteractionsLoaded();

            addIconColumn();

            jobCandidateIteractionListTable.setEnabled(
                    !(getEditedEntity().getBlockCandidate() == null ?
                            false : blockCandidateCheckBox.getValue()));

            if (getRoleService.isUserRoles(userSession.getUser(), StandartRoles.MANAGER) ||
                    getRoleService.isUserRoles(userSession.getUser(), StandartRoles.ADMINISTRATOR))
                jobCandidateIteractionListTable.setEnabled(true);

            jobCandidateIteractionListTable.getColumn("vacancy").setDescriptionProvider(iteractionList -> {
                String retStr = "";

                if (iteractionList.getVacancy() != null) {
                    if (iteractionList.getVacancy().getVacansyName() != null) {
                        retStr = iteractionList.getVacancy().getVacansyName();
                    }
                }

                return Jsoup.parse(retStr).text();
            });

            jobCandidateIteractionListTable.getColumn("rating").setColumnGenerator(event -> {
                return event.getItem().getRating() != null ?
                        starsAndOtherService.setStars(event.getItem().getRating() + 1) : "";
            });

            jobCandidateIteractionListTable.addEditorCloseListener(event -> {
                setRatingLabel(getEditedEntity());
            });

            jobCandidateIteractionListTable.getColumn("iteractionType")
                    .setDescriptionProvider(iteractionList -> {
                        String add = "";

                        if (iteractionList.getAddDate() != null) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy H:m");
                            add = dateFormat.format(iteractionList.getAddDate());
                        }

                        if (iteractionList.getAddString() != null)
                            add = iteractionList.getAddString();

                        if (iteractionList.getAddInteger() != null)
                            add = iteractionList.getAddInteger().toString();


                        return new StringBuilder(iteractionList.getComment() != null ?
                                iteractionList.getComment() : "").append(add).toString();
                    });

            jobCandidateIteractionListTable.addSelectionListener(e -> {
                if (e.getSelected() == null) {
                    openPositionProjectDescriptionButton.setEnabled(false);
                } else {
                    openPositionProjectDescriptionButton.setEnabled(true);
                }
            });

            jobCandidateIteractionListTable.addSelectionListener(e -> {
                if (e.getSelected() == null) {
                    openPositionProjectDescriptionButton.setEnabled(false);
                } else {
                    openPositionProjectDescriptionButton.setEnabled(true);
                }
            });

            jobCandidateIteractionListTable.getColumn("commentColumn").setDescriptionProvider(iteractionList -> {
                return iteractionList.getComment() != null && !iteractionList.getComment().equals("") ?
                        Jsoup.parse(iteractionList.getComment()).text() : null;
            });

            jobCandidateIteractionListTable.getColumn("commentColumn").setColumnGenerator(event -> {
                return event.getItem().getComment() != null && !event.getItem().getComment().equals("") ?
                        CubaIcon.PLUS_CIRCLE : CubaIcon.MINUS_CIRCLE;
            });

            jobCandidateIteractionListTable.getColumn("commentColumn").setStyleProvider(iteractionList -> {
                return iteractionList.getComment() != null && !iteractionList.getComment().equals("") ?
                        "pic-center-large-green" : "pic-center-large-red";
            });

            if (copyIteractionButton == null) {
                copyIteractionButton = (Button) getWindow().getComponent("copyIteractionButton");
            }
            copyIteractionButton.addClickListener(event -> copyIteractionJobCandidate());

            if (openPositionProjectDescriptionButton == null) {
                openPositionProjectDescriptionButton = (Button) getWindow()
                        .getComponent("openPositionProjectDescriptionButton");
            }
            openPositionProjectDescriptionButton.addClickListener(event -> openPositionDescription());

            if (frequentInteractionPopupButton == null) {
                frequentInteractionPopupButton = (PopupButton) getWindow()
                        .getComponent("frequentInteractionPopupButton");
            }
            setFrequentInteractionPopupButton();
            setIteractionListVacancyFilter();

            interationTabInitialized = true;
        }
    }

    private void initTabResume() {
        TabSheet.Tab selectedTab = tabSheetSocialNetworks.getSelectedTab();
        if (selectedTab == null || !"tabResume".equals(selectedTab.getName())) {
            return;
        }
        if (!cvTabInitialized) {
            jobCandidateCandidateCvTable = (DataGrid<CandidateCV>) getWindow()
                    .getComponentNN("jobCandidateCandidateCvTable");
            scanContactsFromCVButton = (Button) getWindow().getComponentNN("scanContactsFromCVButton");
            copyCVButton = (Button) getWindow().getComponentNN("copyCVButton");
            checkSkillFromJD = (Button) getWindow().getComponentNN("checkSkillFromJD");

            jobCandidateCandidateCvTable.getColumn("toVacancy")
                    .setDescriptionProvider(this::jobCandidateCandidateCvTableToVacancyDescriptionProvider);
            jobCandidateCandidateCvTable.getColumn("resumePosition")
                    .setDescriptionProvider(this::jobCandidateCandidateCvTableResumePositionDescriptionProvider);
            jobCandidateCandidateCvTable.getColumn("toVacancy")
                    .setStyleProvider(this::jobCandidateCandidateCvTableToVacancyStyleProvider);
            ensureCandidateCvLoaded();
            scanContactsFromCVButton.addClickListener(e -> scanContactsFromCVs());

            copyCVButton.addClickListener(e -> copyCVJobCandidate());
            setCopyCVButton();

            checkSkillFromJD.addClickListener(e -> checkSkillFromJD());

            jobCandidateCandidateCvTable.addItemClickListener(e -> {
                if (e.getItem() != null) {
                    copyCVButton.setEnabled(true);
                } else {
                    copyCVButton.setEnabled(false);
                }
            });

            jobCandidateCandidateCvTable.getColumn("letter")
                    .setDescriptionProvider(candidateCV -> {
                        String returnData = candidateCV.getLetter() != null ? Jsoup.parse(candidateCV.getLetter()).text() : "";
                        return returnData;
                    });

            jobCandidateCandidateCvTable.getColumn("iconOriginalCVFile")
                    .setDescriptionProvider(candidateCV -> {
                        return candidateCV.getLinkOriginalCv();
                    });

            jobCandidateCandidateCvTable.getColumn("iconOriginalCVFile")
                    .setColumnGenerator(event -> {
                        return event.getItem().getLinkOriginalCv() != null ?
                                CubaIcon.valueOf("FILE_TEXT") :
                                CubaIcon.valueOf("FILE");
                    });

            jobCandidateCandidateCvTable.getColumn("iconHuntTechCVFile")
                    .setColumnGenerator(event -> {
                        return event.getItem().getLinkHuntTechCV() != null ?
                                CubaIcon.valueOf("FILE_TEXT") :
                                CubaIcon.valueOf("FILE");
                    });

            jobCandidateCandidateCvTable.getColumn("letter")
                    .setColumnGenerator(event -> {
                        if (event.getItem().getLetter() != null) {
                            if (!Jsoup.parse(event.getItem().getLetter()).text().equals("")) {

                                if (event.getItem().getToVacancy() != null) {
                                    String letterTemplate = resumeRecognitionService.setTemplateLetter(event.getItem().getToVacancy());
                                    String letter = event.getItem().getLetter();

                                    if (!Jsoup.parse(event.getItem().getLetter()).text()
                                            .equals(Jsoup.parse(letterTemplate).text())) {
                                        return CubaIcon.valueOf("FILE_TEXT");
                                    } else {
                                        return CubaIcon.valueOf("FILE");
                                    }
                                } else {
                                    return CubaIcon.valueOf("FILE");
                                }
                            } else {
                                return CubaIcon.valueOf("FILE");
                            }
                        } else {
                            return CubaIcon.valueOf("FILE");
                        }
                    });

            jobCandidateCandidateCvTable.getColumn("iconHuntTechCVFile")
                    .setDescriptionProvider(candidateCV -> {
                        return candidateCV.getLinkHuntTechCV();
                    });

            jobCandidateCandidateCvTable.getColumn("iconHuntTechCVFile")
                    .setStyleProvider(candidateCV -> {
                        String style = "";

                        if (candidateCV.getLinkHuntTechCV() != null) {
                            style = "pic-center-large-green";
                        } else {
                            style = "pic-center-large-red";
                        }

                        return style;
                    });

            jobCandidateCandidateCvTable.getColumn("iconOriginalCVFile")
                    .setStyleProvider(candidateCV -> {
                        return (candidateCV.getLinkOriginalCv() != null ? "pic-center-large-green" : "pic-center-large-red");
                    });

            jobCandidateCandidateCvTable.getColumn("letter")
                    .setStyleProvider(candidateCV -> {
                        return candidateCV.getLetter() != null ? "pic-center-large-green" : "pic-center-large-red";
                    });

        }

        cvTabInitialized = true;
    }

    private List<IteractionList> getIteractionListFromCandidate(JobCandidate editedEntity) {
        return ensureInteractionsLoaded();
    }

    private void setCopyCVButton() {
        if (copyCVButton != null) {
            copyCVButton.setEnabled(false);
        }

    }

    private void setupNameSearchExecutors() {
        if (firstNameField != null) {
            firstNameField.setSearchExecutor((searchString, searchParams) ->
                    dataManager.loadValue(
                            "select distinct e.firstName from hunttech_JobCandidate e " +
                                    "where lower(e.firstName) like lower(:searchString) order by e.firstName",
                            String.class)
                            .parameter("searchString", "%" + searchString.toLowerCase() + "%")
                            .list());
        }
        if (secondNameField != null) {
            secondNameField.setSearchExecutor((searchString, searchParams) ->
                    dataManager.loadValue(
                            "select distinct e.secondName from hunttech_JobCandidate e " +
                                    "where lower(e.secondName) like lower(:searchString) order by e.secondName",
                            String.class)
                            .parameter("searchString", "%" + searchString.toLowerCase() + "%")
                            .list());
        }
        if (middleNameField != null) {
            middleNameField.setSearchExecutor((searchString, searchParams) ->
                    dataManager.loadValue(
                            "select distinct e.middleName from hunttech_JobCandidate e " +
                                    "where lower(e.middleName) like lower(:searchString) order by e.middleName",
                            String.class)
                            .parameter("searchString", "%" + searchString.toLowerCase() + "%")
                            .list());
        }
    }

    public void addIteractionJobCandidate() {
        screenBuilders.editor(jobCandidateIteractionListTable)
                .newEntity()
                .withOptions(new JobCandidateScreenOptions(false))
                .withParentDataContext(dataContext)
                .withInitializer(candidate -> {
                    candidate.setCandidate(getEditedEntity());

                    DataContext dataContext = jobCandidateDl.getDataContext();
                    IteractionList iteractionList = dataContext.merge(candidate);
                    jobCandidateDc.getItem().getIteractionList().add(iteractionList);
                })
                .build()
                .show();
    }

    /* private IteractionList getLastIteraction() {
        try {
            lastIteraction = dataManager.load(IteractionList.class)
                    .query(QUERY_GET_LAST_ITERACTION)
                    .parameter("candidate", getEditedEntity())
                    .cacheable(true)
                    .view("iteractionList-view")
                    .cacheable(true)
                    .one();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            lastIteraction = null;
        }

        return lastIteraction;
    } */


    public void copyIteractionJobCandidate() {
        if (jobCandidateIteractionListTable.getSingleSelected() == null) {
            if (lastIteraction != null) {
                IteractionList finalLastIteraction = lastIteraction;

                Screen copyIteractionScreen = screenBuilders.editor(jobCandidateIteractionListTable)
                        .withParentDataContext(dataContext)
                        .withInitializer(candidate -> {
                            candidate.setVacancy(finalLastIteraction.getVacancy());
                            candidate.setNumberIteraction(numBerIteractionForNewEntity());

                            IteractionList iteractionList = dataContext.merge(candidate);
                            jobCandidateDc.getItem().getIteractionList().add(iteractionList);
                        })
                        .newEntity()
                        .build();

                copyIteractionScreen.addAfterCloseListener(e -> {
                    reloadInteractions();
                });

                copyIteractionScreen.show();
            } else {
                dialogs.createOptionDialog()
                        .withCaption("Нет взаимодействий с кандидатом")
                        .withMessage("Назначить новое взаимодействие?")
                        .withActions(
                                new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                                    addIteractionJobCandidate();
                                }),
                                new DialogAction(DialogAction.Type.NO)
                        )
                        .show();
            }
        } else {
//            screenBuilders.editor(IteractionList.class, this)
            screenBuilders.editor(jobCandidateIteractionListTable)
                    .withParentDataContext(dataContext)
                    .withInitializer(candidate -> {
                        candidate.setCandidate(jobCandidateIteractionListTable.getSingleSelected().getCandidate());
                        candidate.setVacancy(jobCandidateIteractionListTable.getSingleSelected().getVacancy());
                        candidate.setNumberIteraction(numBerIteractionForNewEntity());

                        IteractionList iteractionList = dataContext.merge(candidate);
                        jobCandidateDc.getItem().getIteractionList().add(iteractionList);
                    })
                    .newEntity()
                    .build()
                    .show();
        }
    }

    private BigDecimal numBerIteractionForNewEntity() {
        OpenPosition openPosition = null;

        if (jobCandidateIteractionListTable.getSingleSelected() != null)
            openPosition = jobCandidateIteractionListTable.getSingleSelected().getVacancy();

        if (openPosition != null) {
            return dataManager.loadValue("select count(e.numberIteraction) " +
                    "from hunttech_IteractionList e " +
                    "where e.candidate = :candidate and " +
                    "e.vacancy = :vacancy", BigDecimal.class)
                    .parameter("candidate", getEditedEntity())
                    .parameter("vacancy", openPosition)
                    .one()
                    .add(BigDecimal.ONE);
        } else {
            return dataManager.loadValue("select count(e.numberIteraction) " +
                    "from hunttech_IteractionList e " +
                    "where e.candidate = :candidate", BigDecimal.class)
                    .parameter("candidate", getEditedEntity())
                    .one()
                    .add(BigDecimal.ONE);
        }
    }

    private String getIcon(IteractionList item) {
        if (item.getIteractionType() != null) {
            if (item.getIteractionType().getPic() != null) {
                return item.getIteractionType().getPic();
            } else {
                return null;
            }
        } else
            return null;
    }

    private void addIconColumn() {
        DataGrid.Column iconColumn = jobCandidateIteractionListTable.addGeneratedColumn("icon",
                new DataGrid.ColumnGenerator<IteractionList, String>() {
                    @Override
                    public String getValue(DataGrid.ColumnGeneratorEvent<IteractionList> event) {
                        return getIcon(event.getItem());
                    }

                    @Override
                    public Class<String> getType() {
                        return String.class;
                    }
                });

        iconColumn.setRenderer(jobCandidateIteractionListTable.createRenderer(DataGrid.ImageRenderer.class));
    }

    private void setLastProjectTable() {
        lastProjectDl.setParameter("candidate", getEditedEntity());
        lastProjectDl.load();
    }

    private void setBlockUnblockButton(boolean b) {
        blockCandidateCheckBox.setValue(b);
        blockCandidateButton.setCaption(b ? BLOCK_CANDIDATE_OFF : BLOCK_CANDIDATE_ON);
        blockCandidateButton.setIcon(b ? CubaIcon.ENABLE_EDITING.source() : CubaIcon.CLOSE.source());
        updateFullNameStyle(b);
    }

    private void setLinkButtonSkype() {
        if (getEditedEntity().getSkypeName() != null) {
            skypeLinkButton.setCaption(getEditedEntity().getSkypeName());
        }
    }

    private void setLinkButtonTelegrem() {
        if (getEditedEntity().getTelegramName() != null) {
            telegrammLinkButton.setCaption(getEditedEntity().getTelegramName());
        }
    }

    private void setLinkButtonTelegremGroup() {
        if (getEditedEntity().getTelegramGroup() != null) {
            telegrammGroupLinkButton.setCaption(getEditedEntity().getTelegramGroup());
        }
    }

    @Subscribe("emailLinkButton")
    public void onEmailLinkButtonClick(Button.ClickEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("mailto:")
                .append(event.getButton().getCaption());
        webBrowserTools.showWebPage(sb.toString(), null);
    }

    @Subscribe("telegrammLinkButton")
    public void onTelegrammLinkButtonClick(Button.ClickEvent event) {
//        String retStr = event.getButton().getCaption();
        StringBuilder sb = new StringBuilder(event.getButton().getCaption());

        if (sb.toString().charAt(0) != '@') {
            sb.insert(0, TELEGRAM_NAME_URL);
            webBrowserTools.showWebPage(sb.toString(), null);
        } else {
            sb = new StringBuilder(sb.substring(1));
//            retStr = retStr.substring(1);
            sb.insert(0, TELEGRAM_NAME_URL);
            webBrowserTools.showWebPage(sb.toString(), null);
        }
    }

    @Subscribe("skypeLinkButton")
    public void onSkypeLinkButtonClick(Button.ClickEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("skype:")
                .append(event.getButton().getCaption())
                .append("?chat");
        webBrowserTools.showWebPage(sb.toString(), null);
    }

    private void setLinkButtonEmail() {
        if (getEditedEntity().getEmail() != null) {
            emailLinkButton.setCaption(getEditedEntity().getEmail());
        }
    }

    public void addPositionList() {
        SelectPersonPositions selectPersonPositions = screens
                .create(SelectPersonPositions.class);

        selectPersonPositions.setJobCandidate(getEditedEntity());

        if (getEditedEntity().getPositionList() != null) {
            for (JobCandidatePositionLists p : getEditedEntity().getPositionList()) {
                setPos.add(p.getPositionList());
            }
        }

        selectPersonPositions.addAfterShowListener(e -> {
            selectPersonPositions.setPositionsList(setPos);
        });

        selectPersonPositions.addAfterCloseListener(e -> {
            List<Position> positions = selectPersonPositions.getPositionsList();

            for (Position p : positions) {
                JobCandidatePositionLists position = metadata.create(JobCandidatePositionLists.class);

                position.setJobCandidate(selectPersonPositions.getJobCandidate());
                position.setPositionList(p);

                Boolean flag = false;
                for (JobCandidatePositionLists s : getEditedEntity().getPositionList()) {
                    if (position.getPositionList().getPositionRuName().equals(
                            s.getPositionList().getPositionRuName())) {
                        flag = true;
                    }
                }

                if (!flag) {
                    jobCandidateDc.getItem().getPositionList().add(position);
                    dataContext.merge(position);
                }
            }

            setPositionsLabel();
        });

        selectPersonPositions.show();
    }

    private void setPositionsLabel() {
        positionsLabel.removeAll();
        StringBuilder sbDesc = new StringBuilder();

        if (getEditedEntity().getPositionList() != null) {
            for (JobCandidatePositionLists s : getEditedEntity().getPositionList()) {
                String name = s.getPositionList().getPositionRuName();
                if (name == null) {
                    continue;
                }
                // Каждая позиция — отдельный элемент flow-контейнера: перенос строк
                // происходит между позициями, а не по словам внутри одного Label.
                Label<String> positionChip = uiComponents.create(Label.class);
                positionChip.setValue(name);
                positionChip.setStyleName("job-candidate-position");
                positionsLabel.add(positionChip);

                if (sbDesc.length() > 0) {
                    sbDesc.append("\n");
                }
                sbDesc.append(name);
            }
        }

        if (sbDesc.length() > 0) {
            positionsLabel.setDescription(sbDesc.toString());
        }
    }

    public void copyCVJobCandidate() {
        if (jobCandidateCandidateCvTable != null) {
            if (jobCandidateCandidateCvTable.getSingleSelected() == null) {


                CandidateCV candidateCV = null;

                try {
                    candidateCV = dataManager.load(CandidateCV.class)
                            .query(QUERY_GET_CANDIDATE_CV)
                            .parameter("candidate",
                                    jobCandidateCandidateCvTable.getSingleSelected().getCandidate())
                            .cacheable(true)
                            .view("candidateCV-view")
                            .one();
                } catch (IllegalStateException e) {
                    candidateCV = null;
                }

                if (candidateCV != null) {
                    Screen screen = screenBuilders.editor(jobCandidateCandidateCvTable)
                            .withInitializer(candidate -> {
                                candidate.setCandidate(getEditedEntity());

                                DataContext dataContext = getScreenData().getDataContext();
                                CandidateCV cv = dataContext.merge(candidate);

                                jobCandidateDc.getItem().getCandidateCv().add(cv);
                            })
                            .newEntity()
                            .build();

                    screen.addAfterCloseListener(afterCloseEvent -> {
                        reloadCV();
                    });

                    screen.show();
                } else {
                    dialogs.createOptionDialog()
                            .withCaption("Нет резюме кандидата")
                            .withMessage("Создать резюме?")
                            .withActions(
                                    new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                                    }),
                                    new DialogAction(DialogAction.Type.NO)
                            )
                            .show();
                }
            } else {
//                Screen s = screenBuilders.editor(CandidateCV.class, this)
                Screen s = screenBuilders.editor(jobCandidateCandidateCvTable)
                        .withParentDataContext(dataContext)
                        .withInitializer(candidate -> {
                            candidate.setCandidate(jobCandidateCandidateCvTable.getSingleSelected().getCandidate());
                            candidate.setTextCV(jobCandidateCandidateCvTable.getSingleSelected().getTextCV());
                            candidate.setLetter(jobCandidateCandidateCvTable.getSingleSelected().getLetter());
                            candidate.setResumePosition(jobCandidateCandidateCvTable.getSingleSelected().getResumePosition());
                            candidate.setLinkOriginalCv(jobCandidateCandidateCvTable.getSingleSelected().getLinkOriginalCv());
                            candidate.setLinkHuntTechCV(jobCandidateCandidateCvTable.getSingleSelected().getLinkHuntTechCV());
                            candidate.setLintToCloudFile(jobCandidateCandidateCvTable.getSingleSelected().getLintToCloudFile());
                            candidate.setOwner((ExtUser) userSession.getUser());

                            DataContext dataContext = getScreenData().getDataContext();
                            CandidateCV cv = dataContext.merge(candidate);
                            jobCandidateDc.getItem().getCandidateCv().add(cv);
                        })
                        .newEntity()
                        .build()
                        .show();

                s.addAfterCloseListener(e -> {
                    jobCandidateDl.load();
                    scanContactsFromCVs();
                    initCandidateSkillsSidebar();
                });
            }

            jobCandidateDl.load();
        }
    }

    @Subscribe(id = "jobCandidateCandidateCvsDc", target = Target.DATA_CONTAINER)
    public void onJobCandidateCandidateCvsDcItemChange(InstanceContainer.ItemChangeEvent<CandidateCV> event) {
        scanContactsFromCVs();
    }

    public void checkSkillFromJD() {
        List<SkillTree> skillTrees = rescanResume();
        String inputText = null;

        if (jobCandidateCandidateCvTable != null) {
            if (jobCandidateCandidateCvTable.getSingleSelected() != null) {
                if (jobCandidateCandidateCvTable.getSingleSelected().getToVacancy() != null) {
                    if (jobCandidateCandidateCvTable.getSingleSelected().getToVacancy().getComment() != null) {
                        inputText = Jsoup.parse(jobCandidateCandidateCvTable.getSingleSelected().getToVacancy().getComment()).text();
                    }

                    List<SkillTree> skillTreesFromJD = new ArrayList<>();
                    if (inputText != null) {
                        skillTreesFromJD = pdfParserService.parseSkillTree(inputText);
                    }

                    if (jobCandidateCandidateCvTable.getSingleSelected().getToVacancy().getComment() != null) {
                        SkillTreeBrowseCheck s = screenBuilders.screen(this)
                                .withScreenClass(SkillTreeBrowseCheck.class)
                                .build();

                        s.setCandidateCVSkills(skillTrees);
                        s.setOpenPositionSkills(skillTreesFromJD);
                        s.setTitle(jobCandidateCandidateCvTable.getSingleSelected().getToVacancy().getVacansyName());

                        s.show();
                    } else {
                        notifications.create(Notifications.NotificationType.WARNING)
                                .withCaption("ВНИМАНИЕ!")
                                .withDescription("Для проверки навыков кандидата по резюме " +
                                        "\nнеобходимозаполнить поле \"Вакансия\".")
                                .show();
                    }
                } else {
                    notifications.create(Notifications.NotificationType.WARNING)
                            .withCaption("ВНИМАНИЕ!")
                            .withDescription("Для проверки навыков кандидата по резюме " +
                                    "\nнеобходимозаполнить поле \"Вакансия\".")
                            .show();
                }
            }
        }
    }

    public List<SkillTree> rescanResume() {
        if (jobCandidateCandidateCvTable != null) {
            if (jobCandidateCandidateCvTable.getSingleSelected() != null) {
                if (jobCandidateCandidateCvTable
                        .getSingleSelected()
                        .getTextCV() != null) {

                    String inputText = Jsoup.parse(jobCandidateCandidateCvTable
                            .getSingleSelected()
                            .getTextCV())
                            .text();

                    List<SkillTree> skillTrees = pdfParserService.parseSkillTree(inputText);

                    return skillTrees;
                } else {
                    return null;
                }
            }
        }

        return null;
    }

    private void setRatingLabel(JobCandidate editedEntity) {
        double avgRating = loadAverageRating();
        int intRating = (int) Math.round(avgRating);

        if (intRating > 0) {
            candidateRatingLabel.setValue("");
            switch (intRating) {
                case 1:
                    candidateRatingLabel.setStyleName("rating_candidate_red_1");
                    break;
                case 2:
                    candidateRatingLabel.setStyleName("rating_candidate_orange_2");
                    break;
                case 3:
                    candidateRatingLabel.setStyleName("rating_candidate_yellow_3");
                    break;
                case 4:
                    candidateRatingLabel.setStyleName("rating_candidate_green_4");
                    break;
                case 5:
                    candidateRatingLabel.setStyleName("rating_candidate_blue_5");
                    break;
                default:
                    break;
            }
        } else {
            candidateRatingLabel.setValue("");
            candidateRatingLabel.setStyleName("rating_red_1");
        }
    }

    public void scanContactsFromCVs() {
        ensureSocialNetworksLoaded();

        String newPhone = null,
                newEmail = null;
        Company newCompany = null;
        Boolean scanContacts = false;

        Set<String> newSocial = new HashSet<>();

        for (CandidateCV candidateCV : jobCandidateCandidateCvsDc.getItems()) {
            candidateCV.setContactInfoChecked(
                    candidateCV.getContactInfoChecked() == null ? false : candidateCV.getContactInfoChecked()
            );

            if (candidateCV.getContactInfoChecked() != true) {
                try {
                    String email = parseCVService.parseEmail(candidateCV.getTextCV());

                    if (email != null) {
                        newEmail = email;
                    }

                    String phone = parseCVService.parsePhone(candidateCV.getTextCV());

                    if (phone != null) {
                        newPhone = phone;
                    }

                    List<String> urls = parseCVService
                            .extractUrls(Jsoup.parse(candidateCV.getTextCV())
                                    .text());
                    Set<String> setUrls = new HashSet<>(urls);

                    urls.clear();
                    newSocial.addAll(setUrls);
                } catch (NullPointerException e) {
                    log.error("Error", e);
                }

                candidateCV.setContactInfoChecked(true);
                scanContacts = true;
            }
        }

        if (scanContacts) {
            makeDialogNewEmailPhone1(newEmail, newPhone, newCompany, newSocial);
        }
    }

    private String getCandidateEmailValue() {
        return emailField != null ? emailField.getValue() : getEditedEntity().getEmail();
    }

    private void setCandidateEmailValue(String email) {
        if (emailField != null) {
            emailField.setValue(email);
        } else {
            getEditedEntity().setEmail(email);
        }
    }

    private String getCandidatePhoneValue() {
        return phoneField != null ? phoneField.getValue() : getEditedEntity().getPhone();
    }

    private void setCandidatePhoneValue(String phone) {
        if (phoneField != null) {
            phoneField.setValue(phone);
        } else {
            getEditedEntity().setPhone(phone);
        }
    }

    private void makeDialogNewEmailPhone1(String newEmail,
                                          String newPhone,
                                          Company newCompany,
                                          Set<String> newSocial) {
//        String message = "В резюме есть новые контактные данные кандидата. Заменить на новые?";
        String message = messageBundle.getMessage("msgNewSontacts");
        String messageEmail = null,
                messagePhone = null,
                messageCompany = null;

        HashMap<String, List<String>> messageSocial = new HashMap<>();

        String newPhoneNew = parseCVService.normalizePhoneStr(newPhone);
        String oldEmail = getCandidateEmailValue();
        String oldPhone = parseCVService.normalizePhoneStr(getCandidatePhoneValue());

        Boolean flag = false;

        if (newEmail != null) {
            if (oldEmail == null && newEmail != null) {
                messageEmail = new StringBuilder()
                        .append("Добавить адрес электронной почты в карточку ")
                        .append(newEmail)
                        .append("? ")
                        .toString();

                flag = true;

            }
        }

        if (newPhone != null) {
            if (oldPhone == null && newPhone != null) {
/*                    messagePhone = "Добавить телефон в карточку "
                            + newPhoneNew + "? ";*/
                messagePhone = new StringBuilder()
                        .append(messageBundle.getMessage("msgAddPhone"))
                        .append(" ")
                        .append(newPhone)
                        .append("? ")
                        .toString();

                flag = true;
            } else {
                if (!oldPhone.equals(newPhone)) {
                    messagePhone = new StringBuilder()
                            .append(messageBundle.getMessage("msgReplacePhone"))
                            .append(" ")
                            .append(oldPhone)
                            .append(" ")
                            .append(messageBundle.getMessage("msgTo"))
                            .append(" ")
                            .append(newPhone)
                            .append("? ")
                            .toString();

                    flag = true;
                }
            }
        }

        // Социальные сети
        if (newSocial.size() != 0) {
            for (String sFromCV : newSocial) {
                Boolean flagSFromCV = false;

                for (SocialNetworkURLs urLs : getEditedEntity().getSocialNetwork()) {
                    if (urLs.getNetworkURLS() != null) {
                        if (urLs.getNetworkURLS().equals(sFromCV)) {
                            flagSFromCV = true;
                            break;
                        }
                    }
                }

                if (!flagSFromCV) {
                    StringBuilder sb = new StringBuilder(messageBundle.getMessage("msgAddNewSocialNetwork"));
                    sb.append(" ")
                            .append(sFromCV)
                            .append("? ");

/*                    String messageSN = messageBundle.getMessage("msgAddNewSocialNetwork")
                            + " "
                            + sFromCV
                            + "? ";*/

                    List<String> urls = new ArrayList<>();
                    urls.add(sb.toString());
                    urls.add(sFromCV);

                    messageSocial.put(sFromCV, urls);
                }
            }
        }

        if (flag) {
            Dialogs.InputDialogBuilder dialog = dialogs.createInputDialog(this)
                    .withCaption(message)
                    .withWidth("AUTO")
                    .withHeight("AUTO")
                    .withActions(DialogActions.OK_CANCEL);

            if (newEmail != null) {
                if (!StringUtils.equals(newEmail, oldEmail)) {
                    if (!newEmail.equals(oldEmail)) {
                        dialog.withParameter(InputParameter.booleanParameter("newEmail")
                                .withCaption(messageEmail).withRequired(true));
                    }
                }
            }

            if (newPhone != null) {
                if (!StringUtils.equals(parseCVService.normalizePhoneStr(newPhone),
                        parseCVService.normalizePhoneStr(oldPhone))) {
                    dialog.withParameter(InputParameter.booleanParameter("newPhone")
                            .withCaption(messagePhone).withRequired(true));
                }
            }

            if (messageSocial.size() > 0) {
                for (Map.Entry<String, List<String>> entry : messageSocial.entrySet()) {
                    dialog.withParameter(InputParameter.booleanParameter(entry.getKey())
                            .withCaption(entry.getValue().get(0)).withRequired(true));
                }
            }

            dialog.withCloseListener(closeEvent -> {
                if (closeEvent.closedWith(DialogOutcome.OK)) {
                    Boolean newEmailFlag = closeEvent.getValue("newEmail");
                    Boolean newPhoneFlag = closeEvent.getValue("newPhone");

                    if (newEmailFlag != null) {
                        if (newEmailFlag) {
                            setCandidateEmailValue(newEmail);
                        }
                    }

                    if (newPhoneFlag != null) {
                        if (newPhoneFlag) {
                            setCandidatePhoneValue(newPhoneNew);
                        }
                    }

                    HashMap<String, Boolean> getSocial = new HashMap<>();
                    if (messageSocial.size() > 0) {
                        for (Map.Entry<String, List<String>> entry : messageSocial.entrySet()) {
                            getSocial.put(entry.getKey(), closeEvent.getValue(entry.getKey()));
                        }


                        for (Map.Entry<String, Boolean> entry : getSocial.entrySet()) {
                            SocialNetworkType socialNetworkType = getSocialNetworkType(entry.getKey());

                            if (socialNetworkType != null) {
                                SocialNetworkURLs socialNetworkURL = metadata.create(SocialNetworkURLs.class);

                                socialNetworkURL.setNetworkURLS(entry.getKey());
                                socialNetworkURL.setSocialNetworkURL(socialNetworkType);
                                socialNetworkURL.setJobCandidate(getEditedEntity());
                                socialNetworkURL.setNetworkName(socialNetworkURL.getSocialNetworkURL().getSocialNetwork());

                                getEditedEntity().getSocialNetwork().add(socialNetworkURL);
                            } else {
                                String messageSoc = "";
                                try {
                                    messageSoc = messageSocial.get(entry.getKey()).get(2);
                                } catch (IndexOutOfBoundsException e) {
                                    e.getStackTrace();
                                } finally {
                                    notifications.create(Notifications.NotificationType.ERROR)
                                            .withType(Notifications.NotificationType.ERROR)
                                            .withHideDelayMs(15000)
                                            .withPosition(Notifications.Position.BOTTOM_RIGHT)
                                            .withCaption(messageBundle.getMessage("msgError"))
                                            .withDescription(messageBundle
                                                    .getMessage("msgNotFoundOtherSoclNetworkType")
                                                    + " "
                                                    + messageSoc)
                                            .show();
                                }
                            }
                        }
                    }
                }
            });

            dialog.show();
        }
    }

    private SocialNetworkType getSocialNetworkType(String key) {
        SocialNetworkType realSocialNetwork = null;
        URI uriNetworTypes = null;
        URI uriCandidate = null;

        List<SocialNetworkType> socialNetworkTypes = dataManager.load(SocialNetworkType.class)
                .view("socialNetworkType-view")
                .list();

        try {
            uriCandidate = new URI(key);
        } catch (URISyntaxException uriSyntaxException) {
            uriSyntaxException.printStackTrace();
        }

        Boolean flag = false;

        for (SocialNetworkType socialNetworkType : socialNetworkTypes) {
            try {
                uriNetworTypes = new URI(socialNetworkType.getSocialNetworkURL());
            } catch (URISyntaxException uriSyntaxException) {
                uriSyntaxException.printStackTrace();
                continue;
            }

            if (uriNetworTypes.getHost() != null) {
                if (uriNetworTypes.getHost().equals(uriCandidate.getHost())) {
                    realSocialNetwork = socialNetworkType;
                    flag = true;
                    break;
                }
            }
        }

        if (flag) {
            return realSocialNetwork;
        } else {
            SocialNetworkType socialNetworkType = null;

            try {
                socialNetworkType = dataManager.load(SocialNetworkType.class)
                        .query(QUERY_GET_OTHER_SOCIAL_NETWORK)
                        .parameter("other", "Other")
                        .view("socialNetworkType-view")
                        .one();
            } catch (IllegalStateException e) {
                e.printStackTrace();

                notifications.create(Notifications.NotificationType.ERROR)
                        .withType(Notifications.NotificationType.ERROR)
                        .withCaption(messageBundle.getMessage("msgError"))
                        .withDescription(messageBundle.getMessage("msgNotFindOtherSocialNetwork"))
                        .show();
            }

            return socialNetworkType;
        }
    }

    public void scanContactsFromCV() {
        ensureSocialNetworksLoaded();

        String message = "<b>В резюме есть новые контактные данные кандидата: </b><br><br>";
        StringBuilder messageSB = new StringBuilder(message);
        String textCVAll = "";
        StringBuilder sb = new StringBuilder("");
        // ОШИБКА ТУТ
        String newPhone = null;
        String newEmail = null;

        if (getEditedEntity().getCandidateCv() != null) {
            for (CandidateCV candidateCV : getEditedEntity().getCandidateCv()) {
                if (candidateCV.getTextCV() != null) {
                    sb.append(Jsoup.parse(candidateCV.getTextCV()).text());
//                    textCVAll = textCVAll + Jsoup.parse(candidateCV.getTextCV()).text();
                }
            }
        }

        textCVAll = sb.toString();

        if (textCVAll != null) {
            newPhone = parseCVService.parsePhone(textCVAll);
            newEmail = parseCVService.parseEmail(textCVAll);
        }

        List<String> urls = parseCVService.extractUrls(Jsoup.parse(textCVAll).text());
        Set<String> set = new HashSet<>(urls);

        urls.clear();
        urls.addAll(set);

        Boolean flag = false;

        if (urls.size() != 0) {
            for (String s : urls) {
                for (SocialNetworkURLs social : getEditedEntity().getSocialNetwork()) {
                    String a = social.getSocialNetworkURL().getSocialNetworkURL();
                    String hostCandidate = "";
                    String hostSocial = "";

                    try {
                        URI uriCandidate = new URI(s);
                        URI uriSocial = new URI(a);

                        hostCandidate = uriCandidate.getHost();
                        hostSocial = uriSocial.getHost();

                        if (hostCandidate != null && hostSocial != null) {
                            if (hostCandidate.equals(hostSocial)) {
                                social.setNetworkURLS(s);
                                social.setNetworkName(s);

                                flag = true;

/*                                message = message + "<i> социальная сеть </i>"
                                        + "<b>" + s + "</b><br>";*/
                                message = new StringBuilder(message)
                                        .append("<i> социальная сеть </i>")
                                        .append("<b>")
                                        .append(s)
                                        .append("</b><br>")
                                        .toString();
                            }
                        }
                    } catch (URISyntaxException e) {
                        log.error("Error", e);
                    }
                }
            }
        }

        if (newEmail != null) {
            if (!newEmail.equals(getCandidateEmailValue())) {
                message = new StringBuilder(message)
                        .append("<i> - адрес электронной почты старый </i>")
                        .append("<b>")
                        .append(getCandidateEmailValue())
                        .append("</b>")
                        .append(" новый ")
                        .append("<b>")
                        .append(newEmail)
                        .append("</b>")
                        .append("</i><br>")
                        .toString();
/*                message = message
                        + "<i> - адрес электронной почты старый </i>"
                        + "<b>" + emailField.getValue() + "</b>"
                        + " новый "
                        + "<b>" + newEmail + "</b>"
                        + "</i><br>";*/

                flag = true;
            }
        }

        if (newPhone != null) {
            if (parseCVService.normalizePhoneStr(newPhone)
                    .equals(parseCVService.normalizePhoneStr(getCandidatePhoneValue()))) {
/*                message = message
                        + messageBundle.getMessage("msgOldPhone")
                        + "<b>" + phoneField.getValue() + "</b>"
                        + " " + messageBundle.getMessage("msgNew") + " "
                        + "<b>" + newPhone + "</b>"
                        + "</i><br>";
 */
                message = new StringBuilder(message)
                        .append(messageBundle.getMessage("msgOldPhone"))
                        .append("<b>")
                        .append(getCandidatePhoneValue())
                        .append("</b>")
                        .append(" ")
                        .append(messageBundle.getMessage("msgNew"))
                        .append(" ")
                        .append("<b>")
                        .append(newPhone)
                        .append("</b>")
                        .append("</i><br>")
                        .toString();

                flag = true;
            }
        }

        if (flag) {
            String finalNewPhone = newPhone;
            String finalNewEmail = newEmail;

            dialogs.createOptionDialog()
                    .withType(Dialogs.MessageType.WARNING)
                    .withWidth("600px")
                    .withMessage(message
                            + messageBundle.getMessage("msgReplaseCandidateCard"))
/*                            + "<br><br>"
                            + "<b>Заменить в карточке кандидата?</b>")*/
                    .withContentMode(ContentMode.HTML)
                    .withActions(new DialogAction(DialogAction.Type.OK, Action.Status.PRIMARY).withHandler(e -> {
                        setCandidatePhoneValue(finalNewPhone);
                        setCandidateEmailValue(finalNewEmail);
                    }), new DialogAction(DialogAction.Type.CANCEL).withHandler(f -> {
                    }))
                    .show();
        } else {
            if (getEditedEntity().getCandidateCv() != null) {
                notifications.create(Notifications
                        .NotificationType.WARNING)
                        .withCaption("Не найдено новой контактной информации в резюме кандидата")
                        .show();
            }
        }
    }

    public void onPhoneFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        if (event.getValue() != null) {
            if (!phoneField
                    .getValue()
                    .equals(parseCVService.normalizePhoneStr(phoneField.getValue()))) {
                phoneField
                        .setValue(parseCVService.normalizePhoneStr(phoneField.getValue()));
            }
        }
    }


    public void onMobilePhoneFieldValueChange1(HasValue.ValueChangeEvent<String> event) {
        if (event.getValue() != null) {
            if (!mobilePhoneField
                    .getValue()
                    .equals(parseCVService.normalizePhoneStr(mobilePhoneField.getValue()))) {
                mobilePhoneField
                        .setValue(parseCVService.normalizePhoneStr(mobilePhoneField.getValue()));
            }
        }
    }

    private Component socialNetworkTableLinkToWebColumnGenerator
            (DataGrid.ColumnGeneratorEvent<SocialNetworkURLs> event) {
        LinkButton linkButton = uiComponents.create(LinkButton.class);
        linkButton.setAlignment(Component.Alignment.MIDDLE_LEFT);
        linkButton.setCaption(messageBundle.getMessage("msgGoTo"));
        linkButton.setWidthAuto();
        linkButton.setHeightAuto();

        if (event.getItem().getNetworkURLS() != null) {
            linkButton.addClickListener(e -> webBrowserTools.showWebPage(event.getItem().getNetworkURLS(), null));
        } else {
            linkButton.setVisible(false);
            linkButton.setEnabled(false);
        }

        return linkButton;
    }

    public void openPositionDescription() {
        QuickViewOpenPositionDescription quickViewOpenPositionDescription = screens.create(QuickViewOpenPositionDescription.class);
        quickViewOpenPositionDescription.setJobDescription(jobCandidateIteractionListTable.getSingleSelected() != null ?
                jobCandidateIteractionListTable.getSingleSelected().getVacancy().getComment() : "");

        if (jobCandidateIteractionListTable.getSingleSelected().getVacancy().getProjectName().getProjectDescription() != null) {
            quickViewOpenPositionDescription.setProjectDescription(jobCandidateIteractionListTable
                    .getSingleSelected()
                    .getVacancy()
                    .getProjectName()
                    .getProjectDescription() != null ?
                    jobCandidateIteractionListTable.getSingleSelected()
                            .getVacancy()
                            .getProjectName()
                            .getProjectDescription() : "");
        }

        if (jobCandidateIteractionListTable.getSingleSelected()
                .getVacancy()
                .getProjectName()
                .getProjectDepartment()
                .getCompanyName()
                .getWorkingConditions() != null) {
            quickViewOpenPositionDescription.setCompanyWorkConditions(jobCandidateIteractionListTable
                    .getSingleSelected()
                    .getVacancy()
                    .getProjectName()
                    .getProjectDepartment()
                    .getCompanyName()
                    .getWorkingConditions());
        }

        if (jobCandidateIteractionListTable.getSingleSelected()
                .getVacancy()
                .getProjectName()
                .getProjectDepartment()
                .getCompanyName() != null) {
            quickViewOpenPositionDescription.setCompanyDescription(jobCandidateIteractionListTable
                    .getSingleSelected()
                    .getVacancy()
                    .getProjectName()
                    .getProjectDepartment()
                    .getCompanyName().getCompanyDescription());
        }

        quickViewOpenPositionDescription.reloadDescriptions();
        screens.show(quickViewOpenPositionDescription);
    }

    public void blockCandidateButton() {

        final String DIALOG_MESSAGE_BLOCK_OFF = "Разрешить взаимодейтсвия с кандидатом?";
        final String DIALOG_MESSAGE_BLOCK_ON = "Запретить взаимодейтсвия с кандидатом?";

        String DIALOG_MESSAGE;

        Boolean checkBlockCanidate = blockCandidateCheckBox.getValue() == null ? false : blockCandidateCheckBox.getValue();
        DIALOG_MESSAGE = checkBlockCanidate ? DIALOG_MESSAGE_BLOCK_OFF : DIALOG_MESSAGE_BLOCK_ON;

        dialogs.createOptionDialog()
                .withCaption(messageBundle.getMessage("msgWarning"))
                .withMessage(DIALOG_MESSAGE)
                .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                    blockUnblockCandidate(checkBlockCanidate);
                }), new DialogAction(DialogAction.Type.NO))
                .show();
    }

    private void blockUnblockCandidate(Boolean checkBlockCanidate) {
        checkBlockCanidate = !checkBlockCanidate;

        blockCandidateCheckBox.setValue(checkBlockCanidate == null ? false : checkBlockCanidate);
        blockCandidateButton.setCaption(!checkBlockCanidate ? BLOCK_CANDIDATE_ON : BLOCK_CANDIDATE_OFF);
        blockCandidateButton.setIcon(!checkBlockCanidate ? CubaIcon.ENABLE_EDITING.source() : CubaIcon.CLOSE.source());
        if (jobCandidateIteractionListTable != null) {
            jobCandidateIteractionListTable.setEnabled(!checkBlockCanidate);
        }
        updateFullNameStyle(checkBlockCanidate);

    }

    /**
     * Applies the blocked/unblocked visual state without losing the profile layout class.
     * CUBA's setStyleName() replaces every existing style name, therefore the stable profile
     * class is restored first and the state-specific h2/h2-red class is added separately.
     */
    void updateFullNameStyle(boolean blocked) {
        fullNameField.setStyleName("job-candidate-profile-name edit-sidebar-title");
        fullNameField.addStyleName(blocked ? "h2-red" : "h2");
    }

    public void openPositionMasterBrowseStart() {
        OpenPositionMasterBrowse openPositionMasterBrowse = screens.create(OpenPositionMasterBrowse.class);
        openPositionMasterBrowse.setJobCandidate(getEditedEntity());
        openPositionMasterBrowse.show();
    }

    public void createCandidateCv() {
        ensureCandidateCvLoaded();

        screenBuilders.editor(CandidateCV.class, this)
                .newEntity()
                .withContainer(jobCandidateCandidateCvsDc)
                .withInitializer(candidateCv -> candidateCv.setCandidate(getEditedEntity()))
                .withScreenClass(CandidateCVEdit.class)
                .withAfterCloseListener(event -> {
                    if (jobCandidateCandidateCvTable != null) {
                        jobCandidateCandidateCvTable.repaint();
                    }
                    initCandidateSkillsSidebar();
                })
                .build()
                .show();
    }

    public void createCandidateIteraction() {
        ensureInteractionsLoaded();

        screenBuilders.editor(IteractionList.class, this)
                .newEntity()
                .withContainer(jobCandidateIteractionDc)
                .withInitializer(iteraction -> iteraction.setCandidate(getEditedEntity()))
                .withScreenClass(IteractionListEdit.class)
                .withAfterCloseListener(event -> {
                    if (jobCandidateIteractionListTable != null) {
                        jobCandidateIteractionListTable.repaint();
                    }
                })
                .build()
                .show();
    }

    public Component whoIsResearcherGeneratorColumn(Entity entity) {
        Label retLabel = uiComponents.create(Label.NAME);
        if (historyRowDataByVacancy != null && entity != null) {
            OpenPosition op = entity.getValue("vacancy");
            if (op != null) {
                HistoryRowData row = historyRowDataByVacancy.get(op.getId());
                if (row != null && row.researcherName != null) {
                    retLabel.setValue(row.researcherName);
                }
            }
        }
        return retLabel;
    }

    public Component whoIsRecruterGeneratorColumn(Entity entity) {
        Label retLabel = uiComponents.create(Label.NAME);
        if (historyRowDataByVacancy != null && entity != null) {
            OpenPosition op = entity.getValue("vacancy");
            if (op != null) {
                HistoryRowData row = historyRowDataByVacancy.get(op.getId());
                if (row != null && row.recruiterName != null) {
                    retLabel.setValue(row.recruiterName);
                }
            }
        }
        return retLabel;
    }

    public Component lastInteractionGeneratorColumn(Entity entity) {
        Label retLabel = uiComponents.create(Label.NAME);
        retLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);
        if (historyRowDataByVacancy != null && entity != null) {
            OpenPosition openPosition = entity.getValue("vacancy");
            if (openPosition != null) {
                HistoryRowData row = historyRowDataByVacancy.get(openPosition.getId());
                if (row != null && row.lastInteractionName != null) {
                    retLabel.setValue(row.lastInteractionName);
                }
            }
        }
        return retLabel;
    }

    public Component lastIteractionCount(Entity entity) {
        Label retLabel = uiComponents.create(Label.NAME);
        int lastIteractionCount = 0;

        for (int i = 0; i < lastProjectDc.getItems().size(); i++) {
            if (entity.equals(lastProjectDc.getItems().get(i))) {
                lastIteractionCount = i;
                break;
            }
        }

        retLabel.setValue(++lastIteractionCount);

        return retLabel;
    }

    public Component addInteractionsViewButton(Entity entity) {
        Button retButton = uiComponents.create(Button.NAME);
        retButton.setCaption("Просмотр");

        retButton.setAction(new BaseAction("listIteraction")
                .withHandler(actionPerformedEvent -> {
                    IteractionListSimpleBrowse iteractionListSimpleBrowse =
                            screens.create(IteractionListSimpleBrowse.class);

                    iteractionListSimpleBrowse.setSelectedCandidate(getEditedEntity());
                    iteractionListSimpleBrowse.setJobCandidate(getEditedEntity());

                    OpenPosition openPosition = lastProjectDc.getItem(lastProjectTable
                            .getSingleSelected()).getValue("vacancy");

                    iteractionListSimpleBrowse.setOpenPosition(openPosition);

                    screens.show(iteractionListSimpleBrowse);

                }));

        return retButton;
    }


    private Icons.Icon jobCandidateIteractionListTableCurrentOpenCloseColumnColumnGenerator(
            DataGrid.ColumnGeneratorEvent<IteractionList> columnGeneratorEvent) {

        if (columnGeneratorEvent.getItem().getCurrentOpenClose() != null) {
            return columnGeneratorEvent.getItem().getCurrentOpenClose()
                    ? CubaIcon.MINUS_CIRCLE : CubaIcon.PLUS_CIRCLE;
        } else {
            if (columnGeneratorEvent.getItem().getVacancy() != null) {
                if (columnGeneratorEvent.getItem().getVacancy().getOpenClose() != null) {
                    return columnGeneratorEvent.getItem().getVacancy().getOpenClose() ?
                            CubaIcon.MINUS_CIRCLE : CubaIcon.PLUS_CIRCLE;
                } else
                    return CubaIcon.PLUS_CIRCLE;
            } else
                return CubaIcon.MINUS_CIRCLE;
        }
    }

    private String jobCandidateIteractionListTableCurrentOpenCloseColumnStyleProvider(
            IteractionList iteractionList) {

        if (iteractionList.getCurrentOpenClose() != null) {
            return iteractionList.getCurrentOpenClose()
                    ? "pic-center-large-red" : "pic-center-large-green";
        } else {
            if (iteractionList.getVacancy() != null) {
                if (iteractionList.getVacancy().getOpenClose() != null) {
                    return iteractionList.getVacancy().getOpenClose() ?
                            "pic-center-large-red" : "pic-center-large-green";
                } else
                    return "pic-center-large-green";
            } else
                return "pic-center-large-red";
        }
    }

    private String jobCandidateIteractionListTableCurrentOpenCloseColumnDescriptionProvider(IteractionList iteractionList) {

        if (iteractionList.getCurrentOpenClose() != null) {
            return iteractionList.getCurrentOpenClose()
                    ? "Закрыта на момент создания взаимодействия" : "Открыта на момент создания взаимодействия";
        } else {
            if (iteractionList.getVacancy() != null) {
                if (iteractionList.getVacancy().getOpenClose() != null) {
                    return iteractionList.getVacancy().getOpenClose() ?
                            messageBundle.getMessage("msgCurrentlyClosed") :
                            messageBundle.getMessage("msgCurrentlyOpen");
                } else
                    return messageBundle.getMessage("msgCurrentlyOpen");
            } else
                return messageBundle.getMessage("msgCurrentlyClosed");
        }
    }

    private String suggestVacancyTableItemDescriptionProvider(OpenPosition openPosition, String string) {
        StringBuilder sb = new StringBuilder("<b>Вакансия:</b><br><br>");
        sb.append("<i>")
                .append(openPosition.getVacansyName() != null ? openPosition.getVacansyName() : "")
                .append("</i><br>");

        Project project = openPosition.getProjectName();
        if (project != null) {
            sb.append("<i>Проект: </i>")
                    .append(project.getProjectName() != null ? project.getProjectName() : "")
                    .append("<br>");

            Person projectOwner = project.getProjectOwner();
            if (projectOwner != null) {
                sb.append("<i>Ответственный за проект у заказчика: </i>")
                        .append(projectOwner.getSecondName() != null ? projectOwner.getSecondName() : "")
                        .append(" ")
                        .append(projectOwner.getFirstName() != null ? projectOwner.getFirstName() : "")
                        .append("<br>");
            }
        }

        if (openPosition.getOwner() != null) {
            sb.append("<i>Ответственный за проект на нашей стороне: </i>")
                    .append(openPosition.getOwner().getName() != null
                            ? openPosition.getOwner().getName() : "")
                    .append("<br>");
        }
        if (openPosition.getLastOpenDate() != null) {
            sb.append("<i>Дата открытия вакансии: </i>")
                    .append(openPosition.getLastOpenDate())
                    .append("<br>");
        }
        if (openPosition.getComment() != null) {
            sb.append("<br><i>Описание вакансии: </i><br>")
                    .append(openPosition.getComment());
        }

        return sb.toString();
    }

    private Component suggestVacancyTableNotSendedIconColumnColumnGenerator(OpenPosition openPosition) {
        String retStr = "font-icon:CHECK";
        String retStyle = "h2-green";
        String retDescriplion = "<b>Можно начинать процесс с кандидатом.</b><br> Кандидату не предлагали эту вакансию.";

        Label retIcon = uiComponents.create(Label.class);

        for (IteractionList list : jobCandidateIteractionDc.getItems()) {
            if (openPosition.equals(list.getVacancy())) {
                if (list.getIteractionType() != null) {
                    if (list.getIteractionType().getSignSendToClient() != null ?
                            list.getIteractionType().getSignSendToClient() != null : false) {
                        if (list.getIteractionType().getSignSendToClient()) {
                            retStr = "font-icon:REFRESH";
                            retStyle = "h2-blue";
                            retDescriplion = "<b>Можно послать еще раз.</b><br> Резюме отправлено клиенту, но не было ответа";
                            break;
                        }
                    }

                    if (list.getIteractionType() != null) {
                        if (list.getIteractionType().getSignEndCase() != null ?
                                list.getIteractionType().getSignEndCase() : false) {
                            retStr = "font-icon:CLOSE";
                            retStyle = "h2-red";
                            retDescriplion = "<b>Слать резюме не рекомендуется.</b><br> Процесс с заказчиком закончен.";
                            break;
                        }
                    }

                    retStr = "font-icon:QUESTION";
                    retStyle = "h2-orange";
                    retDescriplion = "<b>Можно выслать заказчику.</b><br> Процесс с кандидатом начат, но резюме не отослали.";
                }
            }
        }

        retIcon.setIcon(retStr);
        retIcon.setAlignment(Component.Alignment.MIDDLE_CENTER);
        retIcon.setStyleName(retStyle);
        retIcon.setDescriptionAsHtml(true);
        retIcon.setDescription(retDescriplion);
        return retIcon;
    }

    private String jobCandidateCandidateCvTableToVacancyDescriptionProvider(CandidateCV candidateCV) {
//        String retStr = (candidateCV.getToVacancy() != null ? candidateCV.getToVacancy().getVacansyName() : "");
        StringBuilder sb = new StringBuilder((candidateCV.getToVacancy() != null ? candidateCV.getToVacancy().getVacansyName() : ""));

        if (candidateCV.getToVacancy() != null) {
            if (candidateCV.getToVacancy().getLastOpenDate() != null) {
                sb.append("\nОткрыта: \n")
                        .append(candidateCV.getToVacancy().getLastOpenDate());
//                retStr += "\nОткрыта: \n" + candidateCV.getToVacancy().getLastOpenDate();
            }
        }

        return sb.toString();
    }

    private String jobCandidateIteractionListTableVacancyStyleProvider(IteractionList iteractionList) {
        return "table-wordwrap";
    }

    private String jobCandidateCandidateCvTableResumePositionDescriptionProvider(CandidateCV candidateCV) {
//        String retStr = "";
        StringBuilder sb = new StringBuilder();

        if (candidateCV.getResumePosition() != null) {
            if (candidateCV.getResumePosition().getPositionRuName() != null) {
                if (candidateCV.getResumePosition().getPositionEnName() != null) {
                    sb.append(candidateCV.getResumePosition().getPositionRuName())
                            .append(" / ")
                            .append(candidateCV.getResumePosition().getPositionEnName());
/*                    return candidateCV.getResumePosition().getPositionRuName()
                            + " / "
                            + candidateCV.getResumePosition().getPositionEnName(); */
                } else {
                    sb.append(candidateCV.getResumePosition().getPositionRuName());
//                    return candidateCV.getResumePosition().getPositionRuName();
                }
            }
        }

        return sb.length() != 0 ? sb.toString() : "";
    }

    private String jobCandidateCandidateCvTableToVacancyStyleProvider(CandidateCV candidateCV) {
        return "table-wordwrap";
    }

    private String jobCandidateIteractionListTableIteractionTypeStyleProvider(IteractionList iteractionList) {
        return "table-wordwrap";
    }

    private Component buildCommentComponent(IteractionList item) {
        VBoxLayout retBox = uiComponents.create(VBoxLayout.class);
        retBox.setWidthFull();
        retBox.setSpacing(false);
        retBox.setMargin(false);

        HBoxLayout innerBox = uiComponents.create(HBoxLayout.class);
        innerBox.setMargin(true);
        innerBox.setWidthAuto();
        innerBox.setSpacing(true);

        VBoxLayout outerBox = uiComponents.create(VBoxLayout.class);
        outerBox.setMargin(false);
        outerBox.setWidthAuto();
        outerBox.setSpacing(false);

        if (item.getComment() != null
                && !item.getComment().equals("")) {
            Label name = uiComponents.create(Label.class);
            if (item.getRecrutier() != null) {
                name.setValue(item.getRecrutier().getName() != null
                        ? item.getRecrutier().getName() :
                        (item.getRecrutierName() != null ? item.getRecrutierName() : ""));
            }
            name.setStyleName("tailName");

            Label vacancy = uiComponents.create(Label.class);
            vacancy.setValue(item.getVacancy() != null &&
                    !item.getVacancy().getVacansyName().equals("Default")
                    ? item.getVacancy().getVacansyName() : "");
            vacancy.setStyleName("tailVacancy");

            Label text = uiComponents.create(Label.class);
            text.setValue(item.getComment() != null ?
                    item.getComment().replaceAll("\n\n", "\n") : "");
            text.addStyleName("table-wordwrap");

            Label date = uiComponents.create(Label.class);
            date.setValue(item.getDateIteraction() != null ?
                    new SimpleDateFormat("dd.MM.yyyy HH:mm").format(item.getDateIteraction()) : "");
            date.setAlignment(Component.Alignment.BOTTOM_RIGHT);
            date.setStyleName("tailDate");

            Image image = uiComponents.create(Image.class);

            if (item.getRecrutier() != null) {
                FileDescriptorImageHelper.setUserProfilePhoto(image, fileLoader,
                        (ExtUser) item.getRecrutier());
            } else {
                image.setSource(ThemeResource.class)
                        .setPath("icons/no-programmer.jpeg");
            }

            image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
            image.setWidth("50px");
            image.setHeight("50px");
            image.setStyleName("circle-50px");

            innerBox.setStyleName("toolTip");

            Button replyButton = uiComponents.create(Button.class);
            replyButton.setWidthAuto();
            replyButton.setAlignment(Component.Alignment.BOTTOM_RIGHT);
            replyButton.setCaption(messageBundle.getMessage("msgReplyButton"));
            replyButton.setDescription(messageBundle.getMessage("msgReplyButtonDesc"));
            replyButton.addClickListener(e -> {
                dialogs.createInputDialog(this)
                        .withCaption(messageBundle.getMessage("msgComment"))
                        .withParameters(
                                InputParameter.stringParameter("comment")
                                        .withCaption(messageBundle.getMessage("msgInputComment"))
                                        .withRequired(true)
                        )
                        .withActions(DialogActions.OK_CANCEL)
                        .withCloseListener(closeEvent -> {
                            if (closeEvent
                                    .getCloseAction()
                                    .equals(InputDialog.INPUT_DIALOG_OK_ACTION)) {
                                replyButtonInvoke(e, new StringBuilder()
                                        .append("(")
                                        .append(") Re:")
                                        .append((String) closeEvent.getValue("comment"))
                                        .toString());
                            }
                        })
                        .show();
            });

            if (userSession.getUser().getLogin().equals(item.getCreatedBy())) {
                outerBox.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                date.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                vacancy.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                text.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                name.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                innerBox.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                innerBox.addStyleName("tailMyMessage");
            } else {
                outerBox.setAlignment(Component.Alignment.MIDDLE_LEFT);
                date.setAlignment(Component.Alignment.MIDDLE_LEFT);
                vacancy.setAlignment(Component.Alignment.MIDDLE_LEFT);
                text.setAlignment(Component.Alignment.MIDDLE_LEFT);
                name.setAlignment(Component.Alignment.MIDDLE_LEFT);
                innerBox.setAlignment(Component.Alignment.MIDDLE_LEFT);
                innerBox.addStyleName("tailOtherMessage");
            }

            outerBox.add(name);
            if (!vacancy.getValue().equals("")) {
                outerBox.add(vacancy);
            }

            outerBox.add(text);
            outerBox.add(date);
            outerBox.add(replyButton);

            if (!userSession.getUser().getLogin().equals(item.getCreatedBy())) {
                innerBox.add(image);
            }

            innerBox.add(outerBox);
            if (userSession.getUser().getLogin().equals(item.getCreatedBy())) {
                innerBox.add(image);
            }

            retBox.add(innerBox);
        }

        return retBox;
    }

    /**
     * Перерисовывает ленту комментариев из контейнера interactionCommentDc
     * (порядок — как в loader: order by deteIteraction desc). dataGrid не
     * поддерживает авто-высоту строк, поэтому пузыри рендерятся вертикально
     * в scrollBox и не перекрывают друг друга.
     */
    private void renderComments() {
        if (jobCandidateCommentsContainer == null || interactionCommentDc == null) {
            return;
        }
        jobCandidateCommentsContainer.removeAll();
        for (IteractionList comment : interactionCommentDc.getItems()) {
            jobCandidateCommentsContainer.add(buildCommentComponent(comment));
        }
    }

    private void replyButtonInvoke(Button.ClickEvent e, String replyStr) {
        createComment(replyStr);
    }

    public void onChatMessageTextFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        if (event.getValue() == null || event.getValue().equals("")) {
            sendCommentButton.setEnabled(false);
        } else {
            sendCommentButton.setEnabled(true);
        }
    }

    public void onChatMessageTextFieldEnterPress(TextInputField.EnterPressEvent event) {
        dialogs.createOptionDialog(Dialogs.MessageType.CONFIRMATION)
                .withCaption(messageBundle.getMessage("msgQuuestionSendMessage"))
                .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY)
                        .withHandler(e -> {
                            sendCommentButtonInvoke();
                        }), new DialogAction(DialogAction.Type.NO));
    }


    public void sendCommentButtonInvoke() {
        createComment(null);
    }

    private void createComment(String commentStr) {

        Iteraction iteractionComment = null;

        try {
            iteractionComment = dataManager
                    .loadValue("select e from hunttech_Iteraction e where e.signComment = true",
                            Iteraction.class)
                    .one();
        } catch (IllegalStateException e) {
        }

        if (iteractionComment != null) {
            BigDecimal numberInteraction = dataManager
                    .loadValue("select max(e.numberIteraction) from hunttech_IteractionList e",
                            BigDecimal.class)
                    .one();
            numberInteraction = numberInteraction.add(BigDecimal.ONE);

            IteractionList comment = metadata.create(IteractionList.class);
            comment.setCandidate(getEditedEntity());
            comment.setDateIteraction(new Date());
            comment.setCurrentOpenClose(vacancyPopupPickerField.getValue() != null ?
                    vacancyPopupPickerField.getValue().getOpenClose() : false);
            comment.setRecrutier((ExtUser) userSession.getUser());

            if (commentStr == null) {
                comment.setComment(chatMessageTextField.getValue());
            } else {
                comment.setComment(commentStr);
            }

            comment.setRecrutierName(userSession.getUser().getName());
            comment.setCurrentPriority(0);
            comment.setIteractionType(iteractionComment);
            comment.setRating(0);
            comment.setNumberIteraction(numberInteraction);

            if (vacancyPopupPickerField.getValue() != null) {
                comment.setVacancy(vacancyPopupPickerField.getValue());
            } else {
                comment.setVacancy(openPositionService.getOpenPositionDefault());

                /*
                try {
                    comment.setVacancy(dataManager
                            .loadValue("select e from hunttech_OpenPosition e where e.vacansyName like \'Default\'",
                                    OpenPosition.class)
                            .one());
                } catch (Exception e) {
                    notifications.create(Notifications.NotificationType.ERROR)
                            .withType(Notifications.NotificationType.ERROR)
                            .withCaption(messageBundle.getMessage("msgError"))
                            .withDescription(messageBundle.getMessage("msgNotFindDefaultOpenPosition"))
                            .withHideDelayMs(15000)
                            .show();
                } */
            }

            jobCandidateDc.getItem().getIteractionList().add(comment);
            reloadInteractions();
            chatMessageTextField.setValue(null);
            // Новый комментарий уходит наверх ленты: renderComments() рисует контейнер
            // в порядке interactionCommentDl (order by deteIteraction desc).
        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("msgError"))
                    .withDescription(messageBundle.getMessage("msgDoNotCommentInteraction"))
                    .withType(Notifications.NotificationType.ERROR)
                    .show();
        }

    }

    private void reloadCV() {
        if (!PersistenceHelper.isNew(getEditedEntity())) {
            dataContext.commit();
            jobCandidateDl.load();
        }
        if (jobCandidateCandidateCvTable != null) {
            jobCandidateCandidateCvTable.repaint();
        }
        initCandidateSkillsSidebar();
    }

    private void reloadInteractions() {
        if (!PersistenceHelper.isNew(getEditedEntity())) {
            dataContext.commit();
            interactionCommentDl.load();
            jobCandidateDl.load();
        }
        if (jobCandidateCommentsContainer != null) {
            renderComments();
        }
        if (jobCandidateIteractionListTable != null) {
            jobCandidateIteractionListTable.repaint();
        }
    }

    public void setFirstNameField(List<String> suggestFirstNames) {
        firstNameField.setValueSource((ValueSource<String>) suggestFirstNames);
    }

    @Subscribe("firstNameField")
    public void onFirstNameFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        if (event.getValue() != null) {
            setFullNameCandidate();
        }

        updateFullNameField();
    }

    @Subscribe("secondNameField")
    public void onSecondNameFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        if (event.getValue() != null) {
            setFullNameCandidate();
        }

        updateFullNameField();
    }

    @Subscribe("personPositionField")
    public void onPersonPositionFieldValueChange(HasValue.ValueChangeEvent<Position> event) {
        updatePersonPositionLabel(event.getValue());
    }

    private void updateFullNameField() {
        String secondName = secondNameField != null && secondNameField.getValue() != null
                ? secondNameField.getValue().trim() : "";
        String firstName = firstNameField != null && firstNameField.getValue() != null
                ? firstNameField.getValue().trim() : "";
        String fullName = formatCandidateProfileName(secondName, firstName);

        // Update both the denormalized entity property and the read-only sidebar presentation.
        getEditedEntity().setFullName(fullName);
        fullNameField.setValue(fullName);
//        if (iteractionListLabelCandidate != null) {
//            iteractionListLabelCandidate.setValue(fullName);
//        }
    }

    /**
     * Populates the read-only profile immediately from the edited entity. Reading the entity
     * instead of tab fields makes the sidebar independent from CUBA lazy-tab creation order.
     *
     * @param candidate current data-container item; null clears stale values during item replacement
     */
    void updateCandidateProfileLabels(JobCandidate candidate) {
        if (candidate == null) {
            fullNameField.setValue("");
            updatePersonPositionLabel(null);
            return;
        }

        fullNameField.setValue(formatCandidateProfileName(
                Objects.toString(readCandidatePropertySafely(candidate, "secondName"), ""),
                Objects.toString(readCandidatePropertySafely(candidate, "firstName"), "")));
        Object position = readCandidatePropertySafely(candidate, "personPosition");
        updatePersonPositionLabel(position instanceof Position ? (Position) position : null);
    }

    /**
     * Formats the sidebar name as "Фамилия Имя" without leading, trailing or duplicate separator
     * spaces when either source value is null or blank.
     */
    static String formatCandidateProfileName(String secondName, String firstName) {
        return (StringUtils.trimToEmpty(secondName) + " "
                + StringUtils.trimToEmpty(firstName)).trim();
    }

    /**
     * Displays the Russian position name and clears the label for an unassigned position.
     */
    private void updatePersonPositionLabel(Position position) {
        personPositionLabel.setValue(resolvePersonPositionLabel(position));
    }

    static String resolvePersonPositionLabel(Position position) {
        if (position == null) {
            return "";
        }
        try {
            return StringUtils.trimToEmpty(position.getPositionRuName());
        } catch (IllegalStateException e) {
            // Do not trigger an extra load while opening the form for an unfetched detached reference.
            return "";
        }
    }

    public void onEmailFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        if (event.getValue() != null) {
            emailLabel.setValue(event.getValue());
        }
    }

    @Subscribe("phoneLabel")
    public void onPhoneLabelValueChange(HasValue.ValueChangeEvent<String> event) {
        if (event.getValue() != null) {
            phoneLabel.setValue(event.getValue());
        }
    }

    public void onMobilePhoneFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        if (event.getValue() != null) {
            mobilePhoneLabel.setValue(event.getValue());
        }
    }

    public void onSkypeNameFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        if (event.getValue() != null) {
            skypuLabel.setValue(event.getValue());
        }

    }

    public void onTelegramNameFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        if (event.getValue() != null) {
            telegramLabel.setValue(event.getValue());
        }

    }

    public void addSocialNetworksListsInvoke() {
        if (getEditedEntity().getSocialNetwork().size() == 0) {
            initSocialNeiworkTable();
            addSocialNetworkListsButton.setEnabled(false);
        }
    }

    public void addMissingSocialNetworksListsInvoke() {
        List<SocialNetworkType> socialNetworkTypes = dataManager
                .load(SocialNetworkType.class)
                .view("socialNetworkType-view")
                .list();

        for (SocialNetworkType socialNetworkType : socialNetworkTypes) {
            boolean exists = jobCandidateSocialNetworksDc.getItems().stream()
                    .map(SocialNetworkURLs::getSocialNetworkURL)
                    .anyMatch(socialNetworkType::equals);

            if (!exists) {
                SocialNetworkURLs socialNetworkURLs = metadata.create(SocialNetworkURLs.class);
                socialNetworkURLs.setJobCandidate(getEditedEntity());
                socialNetworkURLs.setSocialNetworkURL(socialNetworkType);
                socialNetworkURLs.setNetworkName(socialNetworkType.getSocialNetwork());

                jobCandidateSocialNetworksDc.getMutableItems().add(dataContext.merge(socialNetworkURLs));
            }
        }

        socialNetworkTable.repaint();

        notifications.create(Notifications.NotificationType.WARNING)
                .withCaption(messageBundle.getMessage("msgWarning"))
                .withDescription(messageBundle.getMessage("msgAddMissingSocialNetwork"))
                .withHideDelayMs(15000)
                .withPosition(Notifications.Position.MIDDLE_CENTER)
                .show();
    }

    public void removeEmptySocialNetworkListsButton() {
        for (SocialNetworkURLs s : getEditedEntity().getSocialNetwork()) {
            if (s.getNetworkURLS() == null) {
                dataManager.remove(s);
            } else {
                if (s.getNetworkURLS().equals("")) {
                    dataManager.remove(s);
                }
            }
        }

        socialNetworkTable.repaint();
        notifications.create(Notifications.NotificationType.WARNING)
                .withCaption(messageBundle.getMessage("msgWarning"))
                .withDescription(messageBundle.getMessage("msgRemoveEmptySocialNetwork"))
                .withHideDelayMs(15000)
                .withPosition(Notifications.Position.MIDDLE_CENTER)
                .show();
    }

    private String vacancyPopupPickerFieldOptionIconProvider(OpenPosition openPosition) {
        if (!openPosition.getOpenClose()) {
            return CubaIcon.PLUS_CIRCLE.source();
        } else {
            return CubaIcon.MINUS_CIRCLE.source();
        }
    }

    private Component jobCandidateCandidateCvTableProjectLogoColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        retBox.setWidthFull();
        retBox.setHeightFull();

        Image image = uiComponents.create(Image.class);

        image.setDescriptionAsHtml(true);
        image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        image.setWidth("50px");
        image.setHeight("50px");
        image.setStyleName("icon-no-border-50px");
        image.setAlignment(Component.Alignment.MIDDLE_CENTER);
        image.setSource(ThemeResource.class).setPath("icons/no-company.png");

        if (event.getItem().getToVacancy() != null) {
            if (event.getItem()
                    .getToVacancy()
                    .getProjectName() != null) {
                if (event.getItem()
                        .getToVacancy()
                        .getProjectName()
                        .getProjectDescription() != null) {
                    image.setDescription(new StringBuilder()
                            .append("<h4>")
                            .append(event.getItem()
                                    .getToVacancy()
                                    .getProjectName()
                                    .getProjectName())
                            .append("</h4><br><br>")
                            .append(event.getItem()
                                    .getToVacancy()
                                    .getProjectName()
                                    .getProjectDescription()).toString());
                }

                if (event.getItem()
                        .getToVacancy()
                        .getProjectName()
                        .getProjectLogo() != null) {
                    FileDescriptorImageHelper.setCompanyLogo(image, fileLoader, event
                            .getItem()
                            .getToVacancy()
                            .getProjectName()
                            .getProjectLogo());
                }
            }
        }

        retBox.add(image);
        return retBox;
    }

    private Component jobCandidateCandidateCvTableCandidateHuntTechCvColumnGenerator(
            DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        Link link = uiComponents.create(Link.NAME);

        if (event.getItem().getLinkHuntTechCV() != null) {
            link.setUrl(event.getItem().getLinkHuntTechCV());
            link.setCaption("CV HuntTech");
            link.setTarget("_blank");
            link.setWidthAuto();
            link.setVisible(true);
        } else {
            link.setVisible(false);
        }

        return link;
    }

    private Component jobCandidateCandidateCvTableCandidateOriginalCvColumnGenerator(
            DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        Link link = uiComponents.create(Link.NAME);

        if (event.getItem().getLinkOriginalCv() != null) {
            link.setUrl(event.getItem().getLinkOriginalCv());
            link.setCaption("Оригинальное CV");
            link.setTarget("_blank");
            link.setWidthAuto();
            link.setVisible(true);
        } else {
            link.setVisible(false);
        }

        return link;
    }

    private Component jobCandidateIteractionListTableProjectLogoColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<IteractionList> event) {
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        retBox.setWidthFull();
        retBox.setHeightFull();

        Image image = uiComponents.create(Image.class);
        image.setDescriptionAsHtml(true);
        image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        image.setWidth("50px");
        image.setHeight("50px");
        image.setStyleName("icon-no-border-50px");
        image.setAlignment(Component.Alignment.MIDDLE_CENTER);
        image.setSource(ThemeResource.class).setPath("icons/no-company.png");

        if (event.getItem().getVacancy() != null) {
            if (event.getItem()
                    .getVacancy()
                    .getProjectName() != null) {
                if (event.getItem()
                        .getVacancy()
                        .getProjectName()
                        .getProjectDescription() != null) {
                    image.setDescription(new StringBuilder()
                            .append("<h4>")
                            .append(event.getItem()
                                    .getVacancy()
                                    .getProjectName()
                                    .getProjectName())
                            .append("</h4><br><br>")
                            .append(event.getItem()
                                    .getVacancy()
                                    .getProjectName()
                                    .getProjectDescription())
                            .toString());
                }

                if (event.getItem()
                        .getVacancy()
                        .getProjectName()
                        .getProjectLogo() != null) {
                    FileDescriptorImageHelper.setCompanyLogo(image, fileLoader, event
                            .getItem()
                            .getVacancy()
                            .getProjectName()
                            .getProjectLogo());
                }
            }
        }

        retBox.add(image);
        return retBox;
    }

    public String convertToText(String text) {
        String[] breakLine = {"<br>", "<br/>", "<br />", "<p>", "</p>", "</div>"};
        String break_line = "break_line";

        String str = text
                .replaceAll("<br>", new StringBuilder().append(break_line).append(break_line).toString())
                .replaceAll("<li>", "<li> - ")
                .replaceAll("</p>", new StringBuilder().append("</p>").append(break_line).append(break_line).toString())
                .replaceAll("</li>", new StringBuilder().append("</li>").append(break_line).toString())
                .replaceAll("</dd>", new StringBuilder().append("</dd>").append(break_line).toString())
                .replaceAll("</dt>", new StringBuilder().append("</dt>").append(break_line).toString())
                .replaceAll("</dl>", new StringBuilder().append("</dl>").append(break_line).toString())
                .replaceAll("</div>", new StringBuilder().append("</div>").append(break_line).append(break_line).toString());
        str = Jsoup.parse(str).text().replaceAll(break_line, "<br>");

        return str.replaceAll("\n", breakLine[0]);
    }

    private Component socialNetworkTableSocialNetworkLogoColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<SocialNetworkURLs> event) {
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        retBox.setWidthFull();
        retBox.setHeightFull();

        SocialNetworkURLs item = event.getItem();
        if (item == null) {
            return retBox;
        }

        SocialNetworkType socialNetworkType = item.getSocialNetworkURL();
        if (socialNetworkType == null) {
            return retBox;
        }

        Image image = uiComponents.create(Image.class);
        image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        image.setWidth("30px");
        image.setHeight("30px");
        image.setStyleName("icon-no-border-30px");
        image.setAlignment(Component.Alignment.MIDDLE_CENTER);

        if (socialNetworkType.getLogo() != null) {
            FileDescriptorImageHelper.setCompanyLogo(image, fileLoader, socialNetworkType.getLogo());
        } else {
            image.setSource(ThemeResource.class).setPath("icons/no-company.png");
        }

        String networkName = socialNetworkType.getSocialNetwork();
        String comment = socialNetworkType.getComment();
        if (networkName != null || comment != null) {
            StringBuilder sb = new StringBuilder("<h4>");
            if (networkName != null) {
                sb.append(networkName);
            }
            sb.append("</h4><br><br>");
            if (comment != null) {
                sb.append(comment);
            }
            image.setDescriptionAsHtml(true);
            image.setDescription(sb.toString());
        }

        retBox.add(image);
        return retBox;
    }

    // ── Вертикальная навигация по вкладкам (шаг 4) ──────────
    // Переключает tabSheetSocialNetworks на вкладку с указанным tabId.
    private void selectCandidateTab(String tabId) {
        TabSheet.Tab tab = tabSheetSocialNetworks.getTab(tabId);
        if (tab != null) {
            tabSheetSocialNetworks.setSelectedTab(tab);
            updateCandidateNavigationActiveState();
        }
    }

    /**
     * Keeps the shared label-navigation active state in sync with the selected CUBA tab.
     * The method only changes presentation classes; tab selection, loaders and validation
     * remain controlled by the existing screen lifecycle.
     */
    private void updateCandidateNavigationActiveState() {
        TabSheet.Tab selectedTab = tabSheetSocialNetworks.getSelectedTab();
        String selectedTabId = selectedTab == null ? null : selectedTab.getName();

        Map<String, Button> navigationByTabId = new LinkedHashMap<>();
        navigationByTabId.put("tabMain", candidateNavMain);
        navigationByTabId.put("tabPositions", candidateNavPositions);
        navigationByTabId.put("tabIteraction", candidateNavIteraction);
        navigationByTabId.put("tabResume", candidateNavResume);
        navigationByTabId.put("tabContactInfo", candidateNavContactInfo);
        navigationByTabId.put("commentsTab", candidateNavComments);
        navigationByTabId.put("tabHistory", candidateNavHistory);

        navigationByTabId.forEach((tabId, button) -> {
            button.removeStyleName("label-nav-item-active");
            if (Objects.equals(tabId, selectedTabId)) {
                button.addStyleName("label-nav-item-active");
            }
        });
    }

    public void candidateNavMain() {
        selectCandidateTab("tabMain");
    }

    public void candidateNavPositions() {
        selectCandidateTab("tabPositions");
    }

    public void candidateNavIteraction() {
        selectCandidateTab("tabIteraction");
    }

    public void candidateNavResume() {
        selectCandidateTab("tabResume");
    }

    public void candidateNavContactInfo() {
        selectCandidateTab("tabContactInfo");
    }

    public void candidateNavComments() {
        selectCandidateTab("commentsTab");
    }

    public void candidateNavHistory() {
        selectCandidateTab("tabHistory");
    }
}
