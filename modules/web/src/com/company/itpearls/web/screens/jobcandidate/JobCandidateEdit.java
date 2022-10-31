package com.company.itpearls.web.screens.jobcandidate;
// TODO сделать сортировку по номеру взаимодействия или дате в таблице IteractioNList.Borwse

import com.company.itpearls.core.InteractionService;
import com.company.itpearls.core.ParseCVService;
import com.company.itpearls.core.PdfParserService;
import com.company.itpearls.core.StarsAndOtherService;
import com.company.itpearls.entity.*;
import com.company.itpearls.service.GetRoleService;
import com.company.itpearls.web.StandartRoles;
import com.company.itpearls.web.screens.fragments.Skillsbar;
import com.company.itpearls.web.screens.openposition.OpenPositionMasterBrowse;
import com.company.itpearls.web.screens.openposition.QuickViewOpenPositionDescription;
import com.company.itpearls.web.screens.skilltree.SkillTreeBrowseCheck;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogActions;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogOutcome;
import com.haulmont.cuba.gui.app.core.inputdialog.InputParameter;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundTaskHandler;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.*;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@UiController("itpearls_JobCandidate.edit")
@UiDescriptor("job-candidate-edit.xml")
@EditedEntityContainer("jobCandidateDc")
@LoadDataBeforeShow
public class JobCandidateEdit extends StandardEditor<JobCandidate> {
    private static final String MANAGER_GROUP = "Менеджмент";
    private static final String RECRUTIER_GROUP = "Хантинг";
    private static final String RESEARCHER_GROUP = "Ресерчинг";

    String BLOCK_CANDIDATE_ON = "Запретить работу с кандидатом";
    String BLOCK_CANDIDATE_OFF = "Разрешить работу с кандидатом";

    @Inject
    private DataManager dataManager;
    @Inject
    private Dialogs dialogs;
    @Inject
    private Label<String> labelCV;

    @Inject
    private TextField<String> emailField;

    @Inject
    private TextField<String> phoneField;
    @Inject
    private TextField<String> skypeNameField;
    @Inject
    private TextField<String> telegramNameField;
    @Inject
    private TextField<String> whatsupNameField;
    @Inject
    private TextField<String> wiberNameField;
    @Inject
    private Label<String> labelQualityPercent;
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
    @Inject
    private CollectionLoader<SocialNetworkURLs> socialNetworkURLsesDl;
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
    @Inject
    private Image candidatePic;
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

    private SuggestionField<String> firstNameField;
    private SuggestionField<String> secondNameField;
    private SuggestionField<String> middleNameField;
    private Label<String> positionsLabel;
    private LookupPickerField<Company> currentCompanyField;
    private LookupPickerField<Position> personPositionField;
    private LookupPickerField<City> jobCityCandidateField;
    private DateField<Date> birdhDateField;

    @Inject
    private PdfParserService pdfParserService;
    @Inject
    private CollectionPropertyContainer<CandidateCV> jobCandidateCandidateCvsDc;
    @Inject
    private Notifications notifications;
    @Inject
    private Label<String> createdUpdatedLabel;
    @Inject
    private LinkButton telegrammGroupLinkButton;
    @Inject
    private TextField<String> mobilePhoneField;
    @Inject
    private Label<String> candidateRatingLabel;
    @Inject
    private StarsAndOtherService starsAndOtherService;
    @Inject
    private Screens screens;

    private DataGrid<IteractionList> jobCandidateIteractionListTable;
    private Button openPositionProjectDescriptionButton;
    private PopupButton frequentInteractionPopupButton;

    private DataGrid<CandidateCV> jobCandidateCandidateCvTable;
    private Button copyCVButton;
    private long msec = 0;
    private Button scanContactsFromCVButton;
    private Button checkSkillFromJD;

    @Inject
    private ParseCVService parseCVService;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private RadioButtonGroup<Integer> priorityCommunicationMethodRadioButton;
    @Inject
    private Logger log;
    @Inject
    private TextField<String> telegramGroupField;
    @Inject
    protected BackgroundWorker backgroundWorker;

    List<Position> setPos = new ArrayList<>();
    List<IteractionList> iteractionListFromCandidate = new ArrayList();
    IteractionList lastIteraction = null;

    String QUERY_GET_LAST_ITERACTION = "select e " +
            "from itpearls_IteractionList e " +
            "where e.candidate = :candidate and " +
            "e.numberIteraction = (select max(f.numberIteraction) from itpearls_IteractionList f where f.candidate = :candidate)";
    @Inject
    private Button blockCandidateButton;
    @Named("tabSheetSocialNetworks.jobCandidateCard")
    private VBoxLayout jobCandidateCard;
    @Named("tabSheetSocialNetworks.tabContactInfo")
    private VBoxLayout tabContactInfo;
    //    @Named("tabSheetSocialNetworks.tabCandidate")
//    private VBoxLayout tabCandidate;
    @Inject
    private Label<String> iteractionListLabelCandidate;
    @Inject
    private GetRoleService getRoleService;
    @Inject
    private CheckBox blockCandidateCheckBox;
    @Inject
    private CollectionPropertyContainer<IteractionList> jobCandidateIteractionDc;
    @Inject
    private RadioButtonGroup<Integer> workStatusRadioButton;
    @Inject
    private VBoxLayout outstaffingMainVBox;
    @Inject
    private InstanceLoader<JobCandidate> jobCandidateDl;
    @Inject
    private InstanceContainer<JobCandidate> jobCandidateDc;
    @Inject
    private InteractionService interactionService;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private Fragments fragments;
    @Inject
    private HBoxLayout skillBox;
    @Inject
    private KeyValueCollectionLoader lastProjectDl;
    private Integer lastIteractionCount = 1;
    @Inject
    private TabSheet tabSheetSocialNetworks;
    private boolean cvTabInitialized = false;
    private boolean interationTabInitialized = false;
    private Button copyIteractionButton;
    private boolean candidateInitialized = false;

    private Boolean ifCandidateIsExist() {
        setFullNameCandidate();
        // вдруг такой кандидат уже есть
        List<JobCandidate> candidates = dataManager.load(JobCandidate.class)
                .query("select e from itpearls_JobCandidate e where e.firstName like :firstName and " +
                        "e.secondName like :secondName")
                .cacheable(true)
                .parameter("firstName", firstNameField.getValue())
                .parameter("secondName", secondNameField.getValue())
                .view("jobCandidate-view")
                .list();

        return candidates.size() == 0 ? false : true;
    }

    private void setSocialNetworkTable() {
        List<SocialNetworkType> socialNetworkType = dataManager.load(SocialNetworkType.class)
                .list();

        if (!PersistenceHelper.isNew(getEditedEntity())) {
            // основные социальные сети показать
            List<SocialNetworkURLs> socialNetwork = getEditedEntity().getSocialNetwork();

            // тут либо если не новая запись, то проверить на наличие других записей, либо если новая запись, то пофигу
            if (socialNetwork.size() < socialNetworkType.size()) {
                List<SocialNetworkType> type = dataManager
                        .load(SocialNetworkType.class)
                        .query("select e.socialNetworkURL " +
                                "from itpearls_SocialNetworkURLs e " +
                                "where e.jobCandidate = :candidate")
                        .cacheable(true)
                        .parameter("candidate", getEditedEntity())
                        .list();

                for (SocialNetworkType s : socialNetworkType) {
                    // SocialNetworkURLs socialNetworkURLs = dataManager.create(SocialNetworkURLs.class);
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
        }
    }

    private void setupSkillBox() {
        if (!PersistenceHelper.isNew(getEditedEntity())) {
            Skillsbar skillBoxFragment = fragments.create(this, Skillsbar.class);
            setCountTimeStamp();
            if (skillBoxFragment.generateSkillLabels(getLastCVText(getEditedEntity()))) {
                skillBox.add(skillBoxFragment.getFragment());
            }
            ;
            setCountTimeStamp();
        }
    }

    private String getLastCVText(JobCandidate singleSelected) {
        if (singleSelected != null) {
            if (singleSelected.getCandidateCv().size() != 0) {
                CandidateCV lastCV = singleSelected.getCandidateCv().get(0);

                for (CandidateCV candidateCV : singleSelected.getCandidateCv()) {
                    if (lastCV.getDatePost().before((candidateCV.getDatePost()))) {
                        lastCV = candidateCV;
                    }
                }

                return lastCV.getTextCV();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private void setFrequentInteractionPopupButton() {
        Integer MAX_POPULAR_INTERACLION = 5;
        List<Iteraction> mostPopularInteraction = interactionService.getMostPolularIteraction(
                userSessionSource.getUserSession().getUser(), MAX_POPULAR_INTERACLION);

        if (mostPopularInteraction.size() != 0) {
            Integer count = 1;
            for (Iteraction iteraction : mostPopularInteraction) {
                frequentInteractionPopupButton.addAction(
                        new BaseAction("setMostPopularInteractionPopupButton" + "-" + count++)
                                .withCaption(iteraction.getIterationName())
                                .withHandler(actionPerformedEvent -> setMostPopularInteractionPopupButton(iteraction)));
            }
            frequentInteractionPopupButton.setEnabled(true);
        } else {
            frequentInteractionPopupButton.setEnabled(false);
        }
    }

    public void setMostPopularInteractionPopupButton(Iteraction iteraction) {
        if (jobCandidateIteractionListTable.getSingleSelected() != null) {
            screenBuilders.editor(IteractionList.class, this)
                    .newEntity()
                    .withInitializer(e -> {
                        e.setCandidate(getEditedEntity());
                        e.setIteractionType(iteraction);
                        e.setVacancy(jobCandidateIteractionListTable.getSingleSelected().getVacancy());
                    })
                    .build()
                    .show();
        } else {
            screenBuilders.editor(IteractionList.class, this)
                    .newEntity()
                    .withInitializer(e -> {
                        e.setCandidate(getEditedEntity());
                        e.setIteractionType(iteraction);
                    })
                    .build()
                    .show();
        }

    }

    private void setCreatedUpdatedLabel() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");

        if (!PersistenceHelper.isNew(getEditedEntity())) {
            String retStr = ("Создано: " + getEditedEntity().getCreatedBy()
                    + " (" + simpleDateFormat.format(getEditedEntity().getCreateTs()) + ") ")
                    + (getEditedEntity().getUpdatedBy() != null ?
                    ("/ Изменено: " + getEditedEntity().getUpdatedBy() + " ("
                            + simpleDateFormat.format(getEditedEntity().getUpdateTs()) + ") ") : "");

            createdUpdatedLabel.setValue(retStr);
        }
    }


    private void setLabelTitle() {
        String BEFORE = "";
        String AFTER = "&nbsp";

        jobTitleTitle.setValue(BEFORE + jobTitleTitle.getValue() + AFTER);
        personPositionTitle.setValue(BEFORE + personPositionTitle.getRawValue() + AFTER);
        emailTitle.setValue(BEFORE + emailTitle.getValue() + AFTER);
        phoneTitle.setValue(BEFORE + phoneTitle.getValue() + AFTER);
        telegramTitle.setValue(BEFORE + telegramTitle.getValue() + AFTER);
        skypeTitle.setValue(BEFORE + skypeTitle.getValue() + AFTER);
    }

    protected boolean isRequiredAddresField() {
        Boolean isEmptySN = false;

        isEmptySN = ((emailField.getValue() == null) &&
                (skypeNameField.getValue() == null) &&
                (telegramNameField.getValue() == null) &&
                (wiberNameField.getValue() == null) &&
                (whatsupNameField.getValue() == null) &&
                (mobilePhoneField.getValue() == null) &&
                (telegramGroupField.getValue() == null) &&
                (phoneField.getValue() == null));

        return isEmptySN;
    }

    @Subscribe
    public void onAfterCommitChanges(AfterCommitChangesEvent event) {
        CommitContext commitContext = new CommitContext(getEditedEntity());

        setupSocialNetworkURLs(commitContext);
        setupIteractionListCommit(commitContext);
        setupCandidateCVCommit(commitContext);

        dataManager.commit(commitContext);
    }

    private void setupCandidateCVCommit(CommitContext commitContext) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            if (jobCandidateCandidateCvsDc.getItems().size() != 0) {
                for (CandidateCV candidateCV : jobCandidateCandidateCvsDc.getItems()) {
                    commitContext.addInstanceToCommit(candidateCV);
                }
            }
        }
    }

    private void setupIteractionListCommit(CommitContext commitContext) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            if (jobCandidateIteractionDc.getItems().size() != 0) {
                for (IteractionList iteractionList : jobCandidateIteractionDc.getItems()) {
                    commitContext.addInstanceToCommit(iteractionList);
                }
            }
        }
    }

    private void setupSocialNetworkURLs(CommitContext commitContext) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            for (SocialNetworkURLs s : jobCandidateSocialNetworksDc.getItems()) {
                commitContext.addInstanceToCommit(s);
            }
        }
    }

    private AtomicReference<Boolean> returnE = new AtomicReference<>(false);


    private String setFullName(String firstName, String middleName, String secondName) {
        String fullName = "", localFirstName = "", localMiddleName = "", localSecondName = "";

        if (getEditedEntity().getFirstName() != null)
            localFirstName = getEditedEntity().getFirstName();
        else if (firstName != null)
            localFirstName = firstName;

        if (getEditedEntity().getSecondName() != null)
            localSecondName = getEditedEntity().getSecondName();
        else if (secondName != null)
            localSecondName = secondName;

        if (getEditedEntity().getMiddleName() != null)
            localMiddleName = getEditedEntity().getMiddleName();
        else if (middleName != null)
            localMiddleName = middleName;

        fullName = localSecondName + " " + localFirstName + " " + localMiddleName;

        return fullName;
    }

    private void setLabelFullName(String fullName) {
        getEditedEntity().setFullName(fullName);
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        setPercentLabel();

        msec = System.currentTimeMillis() - msec;

        System.out.println(msec);

        Boolean b = getEditedEntity().getBlockCandidate() == null ?
                false : blockCandidateCheckBox.getValue();
        setBlockUnblockButton(b);
        createStartInteraction();
    }

    private void createStartInteraction() {
    }

    private void addSuggestField() {
        BackgroundTask<Integer, Void> task1 = new BackgroundTask<Integer, Void>(10, this) {
            @Override
            public Void run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
                addFirstNameSuggestField();

                return null;
            }

            @Override
            public void canceled() {
                // Do something in UI thread if the task is canceled
            }

            @Override
            public void done(Void result) {
                // Do something in UI thread when the task is done
            }

            @Override
            public void progress(List<Integer> changes) {
                // Show current progress in UI thread
            }
        };

        BackgroundTask<Integer, Void> task2 = new BackgroundTask<Integer, Void>(10, this) {
            @Override
            public Void run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
                addSecondNameSuggestField();

                return null;
            }

            @Override
            public void canceled() {
                // Do something in UI thread if the task is canceled
            }

            @Override
            public void done(Void result) {
                // Do something in UI thread when the task is done
            }

            @Override
            public void progress(List<Integer> changes) {
                // Show current progress in UI thread
            }
        };

        BackgroundTask<Integer, Void> task3 = new BackgroundTask<Integer, Void>(10, this) {
            @Override
            public Void run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
                addMiddleNameSuggestField();

                return null;
            }

            @Override
            public void canceled() {
                // Do something in UI thread if the task is canceled
            }

            @Override
            public void done(Void result) {
                // Do something in UI thread when the task is done
            }

            @Override
            public void progress(List<Integer> changes) {
                // Show current progress in UI thread
            }
        };

        // Get task handler object and run the task
        BackgroundTaskHandler taskHandler1 = backgroundWorker.handle(task1);
        taskHandler1.execute();
        // Get task handler object and run the task
        BackgroundTaskHandler taskHandler2 = backgroundWorker.handle(task2);
        taskHandler2.execute();
        // Get task handler object and run the task
        BackgroundTaskHandler taskHandler3 = backgroundWorker.handle(task3);
        taskHandler3.execute();
    }

    @Subscribe
    public void onAfterClose(AfterCloseEvent event) {
        // чтоб после закрытия не возникало
        jobCandidateCandidateCvsDc.addCollectionChangeListener(e -> {
        });
    }

    private void setPercentLabel() {
        if (candidateInitialized) {
            // вычислить процент заполнения карточки кандидата
            Integer qualityPercent = setQualityPercent() * 100 / 14;

            if (!PersistenceHelper.isNew(getEditedEntity())) {
                labelQualityPercent.setValue("Процент заполнения карточки: " + qualityPercent.toString()
                        + "%");
            }
        }
    }

    @Subscribe("emailField")
    public void onEmailFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        enableDisableContacts();
    }

    @Subscribe("skypeNameField")
    public void onSkypeNameFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        enableDisableContacts();
    }

    @Subscribe("phoneField")
    public void onPhoneFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        enableDisableContacts();
    }

    @Subscribe("mobilePhoneField")
    public void onMobilePhoneFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        enableDisableContacts();
    }

    @Subscribe("socialNetworkTable")
    public void onSocialNetworkTableEditorPostCommit(DataGrid.EditorPostCommitEvent event) {
        enableDisableContacts();
    }

    @Subscribe("socialNetworkTable")
    public void onSocialNetworkTableEditorClose(DataGrid.EditorCloseEvent event) {
        enableDisableContacts();
    }

    @Subscribe("telegramNameField")
    public void onTelegramNameFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        enableDisableContacts();
    }

    @Subscribe("whatsupNameField")
    public void onWhatsupNameFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        enableDisableContacts();
    }

    @Subscribe("wiberNameField")
    public void onWiberNameFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        enableDisableContacts();
    }

    @Subscribe("tabSheetSocialNetworks")
    public void onTabSheetSocialNetworksSelectedTabChange(TabSheet.SelectedTabChangeEvent event) {
        enableDisableContacts();
    }

    protected void enableDisableContacts() {
        Boolean flag = true;

        for (SocialNetworkURLs s : jobCandidateSocialNetworksDc.getItems()) {
            if (s.getNetworkURLS() != null) {
                if (!s.getNetworkURLS().equals("")) {
                    flag = false;
                    break;
                }
            }
        }

        skypeNameField.setRequired(true);
        phoneField.setRequired(true);
        mobilePhoneField.setRequired(true);
        emailField.setRequired(true);
        telegramNameField.setRequired(true);
        whatsupNameField.setRequired(true);
        wiberNameField.setRequired(true);
        telegramGroupField.setRequired(true);

        if (!isRequiredAddresField() || !flag) {
            skypeNameField.setRequired(false);
            phoneField.setRequired(false);
            mobilePhoneField.setRequired(false);
            emailField.setRequired(false);
            telegramNameField.setRequired(false);
            whatsupNameField.setRequired(false);
            wiberNameField.setRequired(false);
            telegramGroupField.setRequired(false);
        }
    }

    @Subscribe
    public void onAfterShow1(AfterShowEvent event) {

        if (!PersistenceHelper.isNew(getEditedEntity())) {
            iteractionListFromCandidate = getIteractionListFromCandidate(getEditedEntity());
        }
    }

    private long time = 0, ctime;
    private Integer countTimeStamp = 0;

    private void setCountTimeStamp() {
        ctime = System.currentTimeMillis();
        String retStr = "Время запуска " + countTimeStamp++ + ": " + (ctime - time) + " msec";

        log.info(retStr);
        System.err.println(retStr);

        time = ctime;
    }

    // загрузить таблицу взаимодействий
    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        time = System.currentTimeMillis();

        // если есть резюме, то поставить галку
        if (!PersistenceHelper.isNew(getEditedEntity())) {
            if (getEditedEntity().getCandidateCv().isEmpty()) {
                labelCV.setValue("Резюме: НЕТ");
            } else {
                labelCV.setValue("Резюме: ДА");
            }
        }

        // обнулить статус для вновь создаваемного кандидата
        if (PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setStatus(0);
        }
        enableDisableContacts();

        // проверить в названии должности (не использовать)
        priorityCommenicationMethodRadioButtonInit();
        workStatusRadioButtonInit();

        if (blockCandidateCheckBox.getValue() == null) {
            blockCandidateCheckBox.setValue(false);
        }

        setSocialNetworkTable();
        enableDisableContacts();
        setLabelTitle();
        setCreatedUpdatedLabel();
        setRatingLabel(getEditedEntity());
        setupSkillBox();
        setCountTimeStamp();

        trimTelegramName();

        setLinkButtonEmail();
        setLinkButtonTelegrem();
        setLinkButtonTelegremGroup();
        setLinkButtonSkype();

        lastIteraction = getLastIteraction();

        if (getRoleService.isUserRoles(userSession.getUser(), StandartRoles.MANAGER) ||
                getRoleService.isUserRoles(userSession.getUser(), StandartRoles.ADMINISTRATOR)) {
            blockCandidateButton.setVisible(true);
        } else {
            blockCandidateButton.setVisible(false);
        }

        setLaborAgreement();
        setLastProjectTable();
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
        });
    }

    private void workStatusRadioButtonInit() {
        Map<String, Integer> workStatusMap = new LinkedHashMap<>();

        workStatusMap.put("Неопределен", 0);
        workStatusMap.put("Самозанятый", 1);
        workStatusMap.put("Индивидуальный предприниматель", 2);
        workStatusMap.put("Срочный трудовой договор", 3);
        workStatusMap.put("Договор ГПХ", 4);
        workStatusMap.put("В штат по ТК РФ", 5);

        workStatusRadioButton.setOptionsMap(workStatusMap);

        lastIteractionCount = 1;
    }

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
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        if (!candidateInitialized) {
            initTabCandidate();
        }

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

        trimTelegramName();
        addIteractionOfNewCandidate();

        JobCandidate jobCandidate = checkDublicateCandidate();

        if (jobCandidate != null && PersistenceHelper.isNew(getEditedEntity())) {
            dialogs.createOptionDialog()
                    .withCaption("ВНИМАНИЕ!")
                    .withMessage("В базе уже присутствует кандидат "
                            + firstNameField.getValue() + " " + secondNameField.getValue()
                            + "\n с заимаемой позицией "
                            + personPositionField.getValue().getPositionRuName()
                            + " из города "
                            + jobCityCandidateField.getValue().getCityRuName() + "."
                            + "\nПродолжить сохранение?")
                    .withActions(new DialogAction(DialogAction.Type.OK, Action.Status.PRIMARY).withHandler(e -> {
                        event.resume();
                        // вернуться и не закомитить
                    }), new DialogAction(DialogAction.Type.CANCEL).withHandler(f -> {
                        // закончить
                    }))
                    .show();

            event.preventCommit();
        }
    }


    private void addIteractionOfNewCandidate() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            // добавить сюда Iteraction "Новый кандиат"
            IteractionList iteractionList = metadata.create(IteractionList.class);

            iteractionList.setCandidate(getEditedEntity());
            iteractionList.setDateIteraction(new Date());
            iteractionList.setRecrutier(userSession.getUser());
            iteractionList.setRecrutierName(userSession.getUser().getName());
            iteractionList.setRating(4);

            BigDecimal numberIteraction;

            try {
                numberIteraction = dataManager.loadValue("select max(e.numberIteraction) from itpearls_IteractionList e", BigDecimal.class)
                        .one();
            } catch (Exception e) {
                numberIteraction = BigDecimal.ONE;
            }

            iteractionList.setNumberIteraction(numberIteraction);

            Iteraction iteraction = null;
            OpenPosition openPosition = null;

            try {
                iteraction = dataManager.load(Iteraction.class)
                        .query("select e from itpearls_Iteraction e where e.iterationName like :iteractionName")
                        .parameter("iteractionName", "Новый контакт")
                        .one();
            } catch (Exception e) {
                notifications.create(Notifications.NotificationType.ERROR)
                        .withCaption("SQL ERROR")
                        .withDescription("Нет взаимодействия \"Новый контакт\"")
                        .show();
            }

            try {
                openPosition = dataManager.load(OpenPosition.class)
                        .query("select e from itpearls_OpenPosition e where e.vacansyName like :vacansyDefaultName")
                        .parameter("vacansyDefaultName", "Default%")
                        .one();
            } catch (Exception e) {
                notifications.create(Notifications.NotificationType.ERROR)
                        .withCaption("SQL ERROR")
                        .withDescription("Нет вакансии \"по умолчанию\" Default")
                        .show();
            }

            if (iteraction != null && openPosition != null) {
                iteractionList.setIteractionType(iteraction);
                iteractionList.setVacancy(openPosition);

                jobCandidateIteractionDc.getMutableItems().add(dataContext.merge(iteractionList));
            }
        }
    }

    private JobCandidate checkDublicateCandidate() {
        if (firstNameField != null &&
                secondNameField != null &&
                jobCityCandidateField != null &&
                personPositionField != null) {
            String queryStr = "select e from itpearls_JobCandidate e " +
                    "where e.firstName = :firstName and " +
                    "e.secondName = :secondName and " +
                    "e.cityOfResidence = :cityOfResidence and " +
                    "e.personPosition = :personPosition";
            List<JobCandidate> jobCandidates = dataManager.load(JobCandidate.class)
                    .query(queryStr)
                    .parameter("firstName", firstNameField.getValue())
                    .parameter("secondName", secondNameField.getValue())
                    .parameter("cityOfResidence", jobCityCandidateField.getValue())
                    .parameter("personPosition", personPositionField.getValue())
                    .cacheable(true)
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
        if (telegramNameField.getValue() != null) {
            telegramNameField.setValue(telegramNameField.getValue().trim().charAt(0) == '@' ?
                    telegramNameField.getValue().trim().substring(1) :
                    telegramNameField.getValue().trim());
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
        Integer qPercent = 0;

        if (birdhDateField.getValue() != null)         // 1
            qPercent = ++qPercent;

        if (currentCompanyField.getValue() != null)    // 2
            qPercent = ++qPercent;

        if (emailField.getValue() != null)             // 3
            qPercent = ++qPercent;

        if (firstNameField.getValue() != null)         // 4
            qPercent = ++qPercent;

        if (middleNameField.getValue() != null)        // 5
            qPercent = ++qPercent;

        if (secondNameField.getValue() != null)        // 6
            qPercent = ++qPercent;

//        if (jobCandidateSpecialisationField != null)   // 7
//            qPercent = ++qPercent;

        if (jobCityCandidateField.getValue() != null)  // 8
            qPercent = ++qPercent;

        if (personPositionField.getValue() != null)    // 9
            qPercent = ++qPercent;

        if (phoneField.getValue() != null)             // 10
            qPercent = ++qPercent;

        if (mobilePhoneField.getValue() != null)             // 10
            qPercent = ++qPercent;

        if (skypeNameField.getValue() != null)         // 12
            qPercent = ++qPercent;

        if (telegramNameField.getValue() != null)      // 13
            qPercent = ++qPercent;

        if (whatsupNameField.getValue() != null)       // 14
            qPercent = ++qPercent;

        if (wiberNameField.getValue() != null)         // 15
            qPercent = ++qPercent;

        return qPercent;

    }

    @Subscribe(id = "jobCandidateDc", target = Target.DATA_CONTAINER)
    private void onJobCandidateDcItemChange(InstanceContainer.ItemChangeEvent<JobCandidate> event) {
        setFullNameCandidate();
    }

    @Subscribe
    public void onBeforeClose(BeforeCloseEvent event) {
        setFullNameCandidate();
    }

    private void setFullNameCandidate() {
        String space = " ";

        if (getEditedEntity().getSecondName() != null &&
                getEditedEntity().getFirstName() != null) {
            getEditedEntity().setFullName(
                    getEditedEntity().getSecondName() + space +
                            getEditedEntity().getFirstName());

        }
    }

    public void onButtonSubscribeClick() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            dialogs.createOptionDialog()
                    .withCaption("WARNING!")
                    .withMessage("Записать изменения?")
                    .withActions(
                            new DialogAction(DialogAction.Type.YES, DialogAction.Status.PRIMARY)
                                    .withHandler(e -> {
                                        commitChanges();

                                        screenBuilders.editor(SubscribeCandidateAction.class, this)
                                                .newEntity()
                                                .withInitializer(g -> {
                                                    g.setCandidate(getEditedEntity());
                                                    g.setSubscriber(userSession.getUser());
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
                                    })
                    )
                    .show();
        } else {
            screenBuilders.editor(SubscribeCandidateAction.class, this)
                    .newEntity()
                    .withOpenMode(OpenMode.DIALOG)
                    .withParentDataContext(dataContext)
                    .withInitializer(e -> {
                        e.setCandidate(getEditedEntity());
                        e.setSubscriber(userSession.getUser());
                        e.setStartDate(new Date());
                    })
                    .build()
                    .show();
        }
    }

    long startSec = System.currentTimeMillis();
    ;

    private void printSec() {
        startSec = System.currentTimeMillis() - startSec;
        notifications.create(Notifications.NotificationType.WARNING)
                .withDescription("Время выполнения: " + startSec)
                .show();
        startSec = System.currentTimeMillis();
    }

    @Subscribe
    public void onInit(InitEvent event) {

        time = System.currentTimeMillis();
        msec = System.currentTimeMillis();

        tabSheetSocialNetworks.addSelectedTabChangeListener(selectedTabChangeEvent -> {
            if ("tabResume".equals(selectedTabChangeEvent.getSelectedTab().getName())) {
                initTabResume();
            }

            if ("tabIteraction".equals(selectedTabChangeEvent.getSelectedTab().getName())) {
                initTabInteractions();
            }

            if ("tabCandidate".equals(selectedTabChangeEvent.getSelectedTab().getName())) {
                initTabCandidate();
            }
        });
    }

    private void initTabCandidate() {
        if (!candidateInitialized) {
            if (currentCompanyField == null) {
                currentCompanyField = (LookupPickerField<Company>) getWindow()
                        .getComponent("currentCompanyField");
            }

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
                if (secondNameField != null) {
                    secondNameField.setEnterActionHandler(searchString -> {
                        secondNameField.setValue(searchString);
                    });

                    secondNameField.addValueChangeListener(stringValueChangeEvent -> {
                        setLabelFullName(setFullName(null, null, getEditedEntity().getSecondName()));
                    });
                }
            }

            if (firstNameField == null) {
                firstNameField = (SuggestionField<String>) getWindow().getComponent("firstNameField");
                if (firstNameField != null) {
                    firstNameField.setEnterActionHandler(searchString -> {
                        firstNameField.setValue(searchString);
                    });

                    firstNameField.addValueChangeListener(stringValueChangeEvent -> {
                        setLabelFullName(setFullName(getEditedEntity().getFirstName(), null, null));
                    });
                }
            }

            if (middleNameField == null) {
                middleNameField = (SuggestionField<String>) getWindow().getComponent("middleNameField");
                if (middleNameField != null) {
                    middleNameField.setEnterActionHandler(searchString -> {
                        middleNameField.setValue(searchString);
                    });

                    middleNameField.addValueChangeListener(stringValueChangeEvent -> {
                        setLabelFullName(setFullName(null, getEditedEntity().getMiddleName(), null));
                    });
                }
            }

            if (positionsLabel == null) {
                positionsLabel = (Label<String>) getWindow().getComponent("positionsLabel");
                if (positionsLabel != null) {
                    setPositionsLabel();
                }
            }

            addSuggestField();
            checkNotUsePosition();

            if (firstNameField != null && secondNameField != null) {
                candidateInitialized = true;
            }
        }
    }

    private void initTabInteractions() {
        if (!interationTabInitialized) {
            if (jobCandidateIteractionListTable == null) {
                jobCandidateIteractionListTable = (DataGrid) getWindow()
                        .getComponent("jobCandidateIteractionListTable");

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

                jobCandidateIteractionListTable.getColumn("projectName").setDescriptionProvider(iteractionList -> {
                    String retStr = "";

                    try {
                        retStr = "Ответственный за проект: ";

                        if (iteractionList.getVacancy() != null) {
                            if (iteractionList.getVacancy().getProjectName() != null) {
                                if (iteractionList.getVacancy().getProjectName().getProjectOwner() != null) {
                                    if (iteractionList.getVacancy().getProjectName().getProjectOwner().getFirstName() != null) {
                                        retStr = retStr + iteractionList.getVacancy().getProjectName().getProjectOwner().getFirstName();
                                    }
                                }
                            }
                        }

                        if (iteractionList.getVacancy() != null) {
                            if (iteractionList.getVacancy().getProjectName() != null) {
                                if (iteractionList.getVacancy().getProjectName().getProjectOwner() != null) {
                                    if (iteractionList.getVacancy().getProjectName().getProjectOwner().getSecondName() != null) {
                                        retStr = retStr + " " + iteractionList.getVacancy().getProjectName().getProjectOwner().getSecondName();
                                    }
                                }
                            }
                        }
                    } catch (IllegalStateException | NullPointerException e) {
                        log.error("Error", e);
                    }

                    return Jsoup.parse(retStr).text();
                });


                jobCandidateIteractionListTable.getColumn("rating").setColumnGenerator(event -> {
                    return event.getItem().getRating() != null ? starsAndOtherService.setStars(event.getItem().getRating() + 1) : "";
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


                            return (iteractionList.getComment() != null ? iteractionList.getComment() : "") + add;
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
            }

            if (copyIteractionButton == null) {
                copyIteractionButton = (Button) getWindow().getComponent("copyIteractionButton");
                copyIteractionButton.addClickListener(event -> copyIteractionJobCandidate());
            }

            if (openPositionProjectDescriptionButton == null) {
                openPositionProjectDescriptionButton = (Button) getWindow()
                        .getComponent("openPositionProjectDescriptionButton");
                openPositionProjectDescriptionButton.addClickListener(event -> openPositionDescription());
            }

            if (frequentInteractionPopupButton == null) {
                frequentInteractionPopupButton = (PopupButton) getWindow()
                        .getComponent("frequentInteractionPopupButton");
                setFrequentInteractionPopupButton();
            }

            interationTabInitialized = true;
        }
    }

    private void initTabResume() {
        if (!cvTabInitialized) {
            if (scanContactsFromCVButton == null) {
                scanContactsFromCVButton = (Button) getWindow().getComponent("scanContactsFromCVButton");
                scanContactsFromCVButton.addClickListener(e -> scanContactsFromCVs());
            }

            if (copyCVButton == null) {
                copyCVButton = (Button) getWindow().getComponent("copyCVButton");
                copyCVButton.addClickListener(e -> copyCVJobCandidate());
                setCopyCVButton();
            }

            if (checkSkillFromJD == null) {
                checkSkillFromJD = (Button) getWindow().getComponent("checkSkillFromJD");
                checkSkillFromJD.addClickListener(e -> checkSkillFromJD());
            }

            if (jobCandidateCandidateCvTable == null) {
                jobCandidateCandidateCvTable = (DataGrid) getWindow().getComponent("jobCandidateCandidateCvTable");

                jobCandidateCandidateCvTable.addItemClickListener(e -> {
                    if (e.getItem() != null) {
                        copyCVButton.setEnabled(true);
                    } else {
                        copyCVButton.setEnabled(false);
                    }
                });

                jobCandidateCandidateCvTable.getColumn("letter").setDescriptionProvider(candidateCV -> {
                    String returnData = candidateCV.getLetter() != null ? Jsoup.parse(candidateCV.getLetter()).text() : "";
                    return returnData;
                });

                jobCandidateCandidateCvTable.getColumn("iconOriginalCVFile").setDescriptionProvider(candidateCV -> {
                    return candidateCV.getLinkOriginalCv();
                });

                jobCandidateCandidateCvTable.getColumn("iconOriginalCVFile").setColumnGenerator(event -> {
                    return event.getItem().getLinkOriginalCv() != null ?
                            CubaIcon.valueOf("FILE_TEXT") :
                            CubaIcon.valueOf("FILE");
                });

                jobCandidateCandidateCvTable.getColumn("iconITPearlsCVFile").setColumnGenerator(event -> {
                    return event.getItem().getLinkItPearlsCV() != null ?
                            CubaIcon.valueOf("FILE_TEXT") :
                            CubaIcon.valueOf("FILE");
                });

                jobCandidateCandidateCvTable.getColumn("letter").setColumnGenerator(event -> {
                    return event.getItem().getLetter() != null ?
                            CubaIcon.valueOf("FILE_TEXT") :
                            CubaIcon.valueOf("FILE");
                });

                jobCandidateCandidateCvTable.getColumn("iconITPearlsCVFile").setDescriptionProvider(candidateCV -> {
                    return candidateCV.getLinkItPearlsCV();
                });

                jobCandidateCandidateCvTable.getColumn("iconITPearlsCVFile").setStyleProvider(candidateCV -> {
                    String style = "";

                    if (candidateCV.getLinkItPearlsCV() != null) {
                        style = "pic-center-large-green";
                    } else {
                        style = "pic-center-large-red";
                    }

                    return style;
                });

                jobCandidateCandidateCvTable.getColumn("iconOriginalCVFile").setStyleProvider(candidateCV -> {
                    return (candidateCV.getLinkOriginalCv() != null ? "pic-center-large-green" : "pic-center-large-red");
                });

                jobCandidateCandidateCvTable.getColumn("letter").setStyleProvider(candidateCV -> {
                    return candidateCV.getLetter() != null ? "pic-center-large-green" : "pic-center-large-red";
                });

                jobCandidateCandidateCvTable.getColumn("candidateITPearlsCVColumn").setColumnGenerator(event -> {
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
                });

                jobCandidateCandidateCvTable.getColumn("candidateOriginalCVColumn").setColumnGenerator(event -> {
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
                });

            }

            return;
        }

        cvTabInitialized = true;
    }

    private List<IteractionList> getIteractionListFromCandidate(JobCandidate editedEntity) {
        String QUERY_GET_ITERCATION_LIST = "select e from itpearls_IteractionList e where e.candidate = :candidate";

        return dataManager.load(IteractionList.class)
                .query(QUERY_GET_ITERCATION_LIST)
                .parameter("candidate", editedEntity)
                .cacheable(true)
                .view("iteractionList-view")
                .list();
    }

    private void setCopyCVButton() {
        if (copyCVButton != null) {
            copyCVButton.setEnabled(false);
        }

    }

    private void addMiddleNameSuggestField() {
        String queryString = "select distinct e.middleName " +
                "from itpearls_JobCandidate e " +
                "order by e.middleName";

        List<String> middleName = dataManager.loadValue(queryString, String.class)
                .list();

        middleNameField.setSearchExecutor((searchString, searchParams) ->
                middleName.stream()
                        .filter(str -> StringUtils.containsIgnoreCase(str, searchString))
                        .collect(Collectors.toList()));

    }

    private void addSecondNameSuggestField() {
        String queryString = "select distinct e.secondName " +
                "from itpearls_JobCandidate e " +
                "order by e.secondName";

        List<String> secondName = dataManager.loadValue(queryString, String.class)
                .list();

        secondNameField.setSearchExecutor((searchString, searchParams) ->
                secondName.stream()
                        .filter(str -> StringUtils.containsIgnoreCase(str, searchString))
                        .collect(Collectors.toList()));
    }

    private void addFirstNameSuggestField() {
        String queryString = "select distinct e.firstName " +
                "from itpearls_JobCandidate e " +
                "order by e.firstName";

        List<String> firstName = dataManager.loadValue(queryString, String.class)
                .list();

        firstNameField.setSearchExecutor((searchString, searchParams) ->
                firstName.stream()
                        .filter(str -> StringUtils.containsIgnoreCase(str, searchString))
                        .collect(Collectors.toList()));
    }

    public void addIteractionJobCandidate() {
        screenBuilders.editor(IteractionList.class, this)
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

    private IteractionList getLastIteraction() {
        try {
            lastIteraction = dataManager.load(IteractionList.class)
                    .query(QUERY_GET_LAST_ITERACTION)
                    .parameter("candidate", getEditedEntity())
                    .cacheable(true)
                    .view("iteractionList-view")
                    .one();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            lastIteraction = null;
        }

        return lastIteraction;
    }


    public void copyIteractionJobCandidate() {
        if (jobCandidateIteractionListTable.getSingleSelected() == null) {
            if (lastIteraction != null) {
                IteractionList finalLastIteraction = lastIteraction;

                Screen copyIteractionScreen = screenBuilders.editor(IteractionList.class, this)
                        .withParentDataContext(dataContext)
                        .withInitializer(candidate -> {
                            candidate.setVacancy(finalLastIteraction.getVacancy());
                            candidate.setNumberIteraction(numBerIteractionForNewEntity());
                            candidate.setLaborAgreement(finalLastIteraction.getLaborAgreement());

                            IteractionList iteractionList = dataContext.merge(candidate);
                            jobCandidateDc.getItem().getIteractionList().add(iteractionList);
                        })
                        .newEntity()
                        .build();

                copyIteractionScreen.addAfterCloseListener(e -> {
                    jobCandidateDl.load();
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
            screenBuilders.editor(IteractionList.class, this)
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
                    "from itpearls_IteractionList e " +
                    "where e.candidate = :candidate and " +
                    "e.vacancy = :vacancy", BigDecimal.class)
                    .parameter("candidate", getEditedEntity())
                    .parameter("vacancy", openPosition)
                    .one().add(BigDecimal.ONE);
        } else {
            return dataManager.loadValue("select count(e.numberIteraction) " +
                    "from itpearls_IteractionList e " +
                    "where e.candidate = :candidate", BigDecimal.class)
                    .parameter("candidate", getEditedEntity())
                    .one().add(BigDecimal.ONE);
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

    @Subscribe("fileImageFaceUpload")
    public void onFileImageFaceUploadFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {
        try {
            FileDescriptorResource fileDescriptorResource = candidatePic.createResource(FileDescriptorResource.class)
                    .setFileDescriptor(fileImageFaceUpload.getFileDescriptor());

            candidatePic.setSource(fileDescriptorResource);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private void setLaborAgreement() {
        if (getRoleService.isUserRoles(userSession.getUser(), StandartRoles.OUSTAFF_NAMAGER) ||
                getRoleService.isUserRoles(userSession.getUser(), StandartRoles.ADMINISTRATOR)) {
            outstaffingMainVBox.setVisible(true);
        } else {
            outstaffingMainVBox.setVisible(false);
        }
    }

    private void setLastProjectTable() {
        lastProjectDl.setParameter("candidate", getEditedEntity());
        lastProjectDl.load();
    }

    private void setBlockUnblockButton(boolean b) {
        blockCandidateCheckBox.setValue(b);
        blockCandidateButton.setCaption(b ? BLOCK_CANDIDATE_OFF : BLOCK_CANDIDATE_ON);
        blockCandidateButton.setIcon(b ? CubaIcon.ENABLE_EDITING.source() : CubaIcon.CLOSE.source());
        iteractionListLabelCandidate.setStyleName(b ? "h2-red" : "h2");
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
        webBrowserTools.showWebPage("mailto:" + event.getButton().getCaption(), null);
    }

    @Subscribe("telegrammLinkButton")
    public void onTelegrammLinkButtonClick(Button.ClickEvent event) {
        String retStr = event.getButton().getCaption();

        if (retStr.charAt(0) != '@') {
            webBrowserTools.showWebPage("http://t.me/" + retStr, null);
        } else {
            retStr = retStr.substring(1);
            webBrowserTools.showWebPage("http://t.me/" + retStr.substring(1, retStr.length() - 1), null);
        }
    }

    @Subscribe("skypeLinkButton")
    public void onSkypeLinkButtonClick(Button.ClickEvent event) {
        webBrowserTools.showWebPage("skype:" + event.getButton().getCaption() + "?chat", null);

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
                    dataContext.commit();
                }
            }

            setPositionsLabel();
        });

        selectPersonPositions.show();
    }

    private void setPositionsLabel() {
        String outStr = "";
        String description = "";

        if (getEditedEntity().getPositionList() != null) {
            for (JobCandidatePositionLists s : getEditedEntity().getPositionList()) {
                if (!outStr.equals("")) {
                    outStr = outStr + ",";
                    description = description + "\n";
                }

                outStr = outStr + s.getPositionList().getPositionRuName();
                description = description + s.getPositionList().getPositionRuName();
            }

        }

        if (!outStr.equals("")) {
            positionsLabel.setValue(outStr);
            positionsLabel.setDescription(description);
        }
    }

    public void copyCVJobCandidate() {
        if (jobCandidateCandidateCvTable != null) {
            if (jobCandidateCandidateCvTable.getSingleSelected() == null) {
                String QUERY_GET_CANDIDATE_CV = "select e " +
                        "from itpearls_CandidateCV e " +
                        "where e.candidate = :candidate";

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
                    screenBuilders.editor(CandidateCV.class, this)
                            .withInitializer(candidate -> {
                                candidate.setCandidate(getEditedEntity());

                                DataContext dataContext = getScreenData().getDataContext();
                                CandidateCV cv = dataContext.merge(candidate);

                                jobCandidateDc.getItem().getCandidateCv().add(cv);
                            })
                            .newEntity()
                            .build()
                            .show();
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
                Screen s = screenBuilders.editor(CandidateCV.class, this)
                        .withParentDataContext(dataContext)
                        .withInitializer(candidate -> {
                            candidate.setCandidate(jobCandidateCandidateCvTable.getSingleSelected().getCandidate());
                            candidate.setTextCV(jobCandidateCandidateCvTable.getSingleSelected().getTextCV());
                            candidate.setLetter(jobCandidateCandidateCvTable.getSingleSelected().getLetter());
                            candidate.setResumePosition(jobCandidateCandidateCvTable.getSingleSelected().getResumePosition());
                            candidate.setLinkOriginalCv(jobCandidateCandidateCvTable.getSingleSelected().getLinkOriginalCv());
                            candidate.setLinkItPearlsCV(jobCandidateCandidateCvTable.getSingleSelected().getLinkItPearlsCV());
                            candidate.setLintToCloudFile(jobCandidateCandidateCvTable.getSingleSelected().getLintToCloudFile());
                            candidate.setOwner(userSession.getUser());

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

        Integer sumRating = 0,
                countRating = 0;

        for (IteractionList iteractionList : iteractionListFromCandidate) {
            if (iteractionList.getRating() != null) {
                sumRating = sumRating + iteractionList.getRating() + 1;
                countRating++;
            }
        }

        if (countRating != 0) {
            float avgRating = (float) sumRating / (float) countRating;
            BigDecimal avgRatiog = new BigDecimal(String.valueOf(avgRating));

            candidateRatingLabel.setValue(String.valueOf(avgRatiog.intValue()));

            switch ((int) Integer.valueOf(avgRatiog.intValue())) {
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
            candidateRatingLabel.setValue("0.0");
            candidateRatingLabel.setStyleName("rating_red_1");
        }
    }

    public void scanContactsFromCVs() {
        String newPhone = null,
                newEmail = null;
        Company newCompany = null;

        Set<String> newSocial = new HashSet<>();

        for (CandidateCV candidateCV : jobCandidateCandidateCvsDc.getItems()) {
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
        }

        makeDialogNewEmailPhone1(newEmail, newPhone, newCompany, newSocial);
    }

    private void makeDialogNewEmailPhone1(String newEmail,
                                          String newPhone,
                                          Company newCompany,
                                          Set<String> newSocial) {
        String message = "В резюме есть новые контактные данные кандидата. Заменить на новые?";
        String messageEmail = null,
                messagePhone = null,
                messageCompany = null;

        HashMap<String, List<String>> messageSocial = new HashMap<>();

        String newPhoneNew = parseCVService.normalizePhoneStr(newPhone);
        String oldEmail = emailField.getValue();
        String oldPhone = parseCVService.normalizePhoneStr(phoneField.getValue());

        Boolean flag = false;

        if (newEmail != null) {
            if (oldEmail == null && newEmail != null) {
                messageEmail = "Добавить адрес электронной почты в карточку "
                        + newEmail + "? ";

                flag = true;

            } else {
            }

            if (newPhone != null) {
                if (oldPhone == null && newPhone != null) {
                    messagePhone = "Добавить телефон в карточку "
                            + newPhoneNew + "? ";

                    flag = true;
                } else {

                }
            }
        }

        // Social
        if (newSocial.size() != 0) {
            for (String sFromCV : newSocial) {
                for (SocialNetworkURLs social : getEditedEntity().getSocialNetwork()) {
                    String socialOld = social.getNetworkURLS();
                    String aSocialOld = social.getSocialNetworkURL().getSocialNetworkURL();
                    String hostCandidateFromCV = "";
                    String hostSocialFromCandidate = "";

                    try {
                        URI uriCandidate = new URI(sFromCV);
                        URI uriSocial = new URI(aSocialOld);

                        hostCandidateFromCV = uriCandidate.getHost();
                        hostSocialFromCandidate = uriSocial.getHost();

                        if (hostCandidateFromCV != null && hostSocialFromCandidate != null) {
                            if (hostCandidateFromCV.equals(hostSocialFromCandidate)) {


                                if (hostCandidateFromCV != null) {
                                    String messageSN = "";

                                    if (socialOld != null) {
                                        // убрать новая - старая
                                        URI uriOld = new URI(socialOld);
                                        String a = uriOld.getRawPath();
                                        if (!sFromCV.equals(socialOld)) {
                                            flag = true;
                                            messageSN = "Ссылка на социальную сеть старая "
                                                    + socialOld
                                                    + " новая "
                                                    + sFromCV + " ";

                                            List<String> urls = new ArrayList<>();
                                            urls.add(messageSN);
                                            urls.add(socialOld);
                                            urls.add(sFromCV);

                                            messageSocial.put(hostSocialFromCandidate, urls);
                                        }
                                    } else {
                                        messageSN = "Добавить новую ссылку: " + sFromCV + "? ";

                                        List<String> urls = new ArrayList<>();
                                        urls.add(messageSN);
                                        urls.add(socialOld);
                                        urls.add(sFromCV);

                                        messageSocial.put(hostSocialFromCandidate, urls);
                                    }
                                }
                            }
                        }
                    } catch (URISyntaxException e) {
                        log.error("Error", e);
                    }
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
                    if (!newEmail.equals(emailField.getValue())) {
                        dialog.withParameter(InputParameter.booleanParameter("newEmail")
                                .withCaption(messageEmail).withRequired(true));
                    }
                }
            }

            if (newPhone != null) {
                if (!StringUtils.equals(newPhone, oldPhone)) {
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
                            emailField.setValue(newEmail);
                        }
                    }

                    if (newPhoneFlag != null) {
                        if (newPhoneFlag) {
                            phoneField.setValue(newPhoneNew);
                        }
                    }

                    HashMap<String, Boolean> getSocial = new HashMap<>();
                    if (messageSocial.size() > 0) {
                        for (Map.Entry<String, List<String>> entry : messageSocial.entrySet()) {
                            getSocial.put(entry.getKey(), closeEvent.getValue(entry.getKey()));
                        }

                        for (SocialNetworkURLs social : getEditedEntity().getSocialNetwork()) {
                            for (Map.Entry<String, Boolean> entry : getSocial.entrySet()) {
                                if (social.getSocialNetworkURL().getSocialNetworkURL().contains(entry.getKey())) {
                                    if (entry.getValue()) {
                                        social.setNetworkURLS(messageSocial.get(entry.getKey()).get(2));
                                    }
                                }
                            }
                        }
                    }
                }
            });

            dialog.show();
        }

    }

    public void scanContactsFromCV() {
        String message = "<b>В резюме есть новые контактные данные кандидата: </b><br><br>";
        String textCVAll = "";
        // ОШИБКА ТУТ
        String newPhone = null;
        String newEmail = null;

        if (getEditedEntity().getCandidateCv() != null) {
            for (CandidateCV candidateCV : getEditedEntity().getCandidateCv()) {
                if (candidateCV.getTextCV() != null) {
                    textCVAll = textCVAll + Jsoup.parse(candidateCV.getTextCV()).text();
                }
            }
        }

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

                                message = message + "<i> социальная сеть </i>"
                                        + "<b>" + s + "</b><br>";
                            }
                        }
                    } catch (URISyntaxException e) {
                        log.error("Error", e);
                    }
                }
            }
        }

        if (newEmail != null) {
            if (!newEmail.equals(emailField.getValue())) {
                message = message
                        + "<i> - адрес электронной почты старый </i>"
                        + "<b>" + emailField.getValue() + "</b>"
                        + " новый "
                        + "<b>" + newEmail + "</b>"
                        + "</i><br>";

                flag = true;
            }
        }

        if (newPhone != null) {
            if (newPhone.equals(phoneField.getValue())) {
                message = message
                        + "<i> - телефон старый </i>"
                        + "<b>" + phoneField.getValue() + "</b>"
                        + " новый "
                        + "<b>" + newPhone + "</b>"
                        + "</i><br>";

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
                            + "<br><br>"
                            + "<b>Заменить в карточке кандидата?</b>")
                    .withContentMode(ContentMode.HTML)
                    .withActions(new DialogAction(DialogAction.Type.OK, Action.Status.PRIMARY).withHandler(e -> {
                        phoneField.setValue(finalNewPhone);
                        emailField.setValue(finalNewEmail);
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

    @Install(to = "socialNetworkTable.linkToWeb", subject = "columnGenerator")
    private Component socialNetworkTableLinkToWebColumnGenerator
            (DataGrid.ColumnGeneratorEvent<SocialNetworkURLs> event) {
        Link link = uiComponents.create(Link.NAME);

        if (!PersistenceHelper.isNew(getEditedEntity())) {
            if (event.getItem().getNetworkURLS() != null) {
                String urlS = "";
                if (!event.getItem().getNetworkURLS().contains("http")) {
                    URI uri = null;
                    URL url = null;

                    try {
                        uri = new URI("https", event.getItem().getNetworkURLS(), null, null);
                        url = uri.toURL();
                    } catch (URISyntaxException | MalformedURLException e) {
                        log.error("Error", e);
                    }

                    if (url != null) {
                        urlS = url.toString();
                    } else {
                        urlS = "";
                    }
                }

                link.setUrl(urlS);
                link.setCaption("Перейти");
                link.setTarget("_blank");
                link.setWidthAuto();
                link.setVisible(true);
            } else {
                link.setVisible(false);
            }
        } else {
            link.setVisible(false);
        }

        return link;
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

        String DIALOG_MESSAGE_BLOCK_OFF = "Разрешить взаимодейтсвия с кандидатом?";
        String DIALOG_MESSAGE_BLOCK_ON = "Запретить взаимодейтсвия с кандидатом?";

        String DIALOG_MESSAGE;

        Boolean checkBlockCanidate = blockCandidateCheckBox.getValue() == null ? false : blockCandidateCheckBox.getValue();
        DIALOG_MESSAGE = checkBlockCanidate ? DIALOG_MESSAGE_BLOCK_OFF : DIALOG_MESSAGE_BLOCK_ON;

        dialogs.createOptionDialog()
                .withCaption("ВНИМАНИЕ!")
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
        jobCandidateIteractionListTable.setEnabled(!checkBlockCanidate);
        iteractionListLabelCandidate.setStyleName(checkBlockCanidate ? "h2-red" : "h2");

    }

    public void openPositionMasterBrowseStart() {
        OpenPositionMasterBrowse openPositionMasterBrowse = screens.create(OpenPositionMasterBrowse.class);
        openPositionMasterBrowse.setJobCandidate(getEditedEntity());
        openPositionMasterBrowse.show();
    }

    public Component whoIsResearcherGeneratorColumn(Entity entity) {
        Label retLabel = uiComponents.create(Label.NAME);

        for (IteractionList iteractionList : jobCandidateIteractionDc.getMutableItems()) {
            OpenPosition op = entity.getValue("vacancy");

            if (op != null) {
                if (iteractionList.getIteractionType().getSignOurInterviewAssigned() != null) {
                    if (iteractionList.getVacancy() != null) {
                        if (iteractionList.getVacancy().equals(op) &&
                                iteractionList.getIteractionType().getSignOurInterviewAssigned()) {
                            retLabel.setValue(iteractionList.getRecrutier().getName());
                        }
                    }
                }
            }
        }

        return retLabel;
    }

    public Component whoIsRecruterGeneratorColumn(Entity entity) {
        Label retLabel = uiComponents.create(Label.NAME);

        for (IteractionList iteractionList : jobCandidateIteractionDc.getMutableItems()) {
            OpenPosition op = entity.getValue("vacancy");

            if (op != null) {
                if (iteractionList.getIteractionType().getSignOurInterview() != null) {
                    if (iteractionList.getVacancy() != null) {
                        if (iteractionList.getVacancy().equals(op) &&
                                iteractionList.getIteractionType().getSignOurInterview()) {
                            retLabel.setValue(iteractionList.getRecrutier().getName());
                        }
                    }
                }
            }
        }

        return retLabel;
    }

    public Component lastInteractionGeneratorColumn(Entity entity) {
        Label retLabel = uiComponents.create(Label.NAME);
        OpenPosition openPosition = entity.getValue("vacancy");

        if (jobCandidateIteractionDc.getMutableItems().size() != 0) {
            IteractionList lastInteraction = jobCandidateIteractionDc.getMutableItems().get(0);

            for (IteractionList iteractionList : jobCandidateIteractionDc.getMutableItems()) {
                if (openPosition.equals(iteractionList.getVacancy())) {
                    if (lastInteraction.getDateIteraction().before(iteractionList.getDateIteraction())) {
                        lastInteraction = iteractionList;
                    }
                }
            }
        }

        retLabel.setValue(lastIteraction.getIteractionType().getIterationName());

        return retLabel;
    }

    public Component lastIteractionCount(Entity entity) {
        Label retLabel = uiComponents.create(Label.NAME);
        retLabel.setValue(lastIteractionCount++);

        return retLabel;
    }

    public Component addInteractionsViewButton(Entity entity) {
        Button retButton = uiComponents.create(Button.NAME);
        retButton.setCaption("Просмотр");

        return retButton;
    }
}