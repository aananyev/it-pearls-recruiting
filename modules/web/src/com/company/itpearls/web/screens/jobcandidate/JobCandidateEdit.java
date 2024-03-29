package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.core.*;
import com.company.itpearls.entity.*;
import com.company.itpearls.service.GetRoleService;
import com.company.itpearls.web.StandartRoles;
import com.company.itpearls.web.screens.candidatecv.CandidateCVEdit;
import com.company.itpearls.web.screens.fragments.Skillsbar;
import com.company.itpearls.web.screens.iteractionlist.iteractionlistbrowse.IteractionListSimpleBrowse;
import com.company.itpearls.web.screens.openposition.OpenPositionMasterBrowse;
import com.company.itpearls.web.screens.openposition.openpositionviews.QuickViewOpenPositionDescription;
import com.company.itpearls.web.screens.skilltree.SkillTreeBrowseCheck;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogActions;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogOutcome;
import com.haulmont.cuba.gui.app.core.inputdialog.InputDialog;
import com.haulmont.cuba.gui.app.core.inputdialog.InputParameter;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.components.data.ValueSource;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundTaskHandler;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@UiController("itpearls_JobCandidate.edit")
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
    private Logger log;
    @Inject
    protected BackgroundWorker backgroundWorker;
    @Inject
    private SuggestionField<String> firstNameField;
    @Inject
    private SuggestionField<String> secondNameField;
    @Inject
    private SuggestionField<String> middleNameField;
    @Inject
    private Label<String> positionsLabel;
    @Inject
    private LookupPickerField<Company> currentCompanyField;
    @Inject
    private LookupPickerField<Position> personPositionField;
    @Inject
    private LookupPickerField<City> jobCityCandidateField;
    @Inject
    private DateField<Date> birdhDateField;
    @Inject
    private RadioButtonGroup<Integer> priorityCommunicationMethodRadioButton;
    @Inject
    private TextField<String> telegramGroupField;
    @Inject
    private TextField<String> mobilePhoneField;
    @Inject
    private DataGrid<IteractionList> jobCandidateIteractionListTable;
    @Inject
    private Button openPositionProjectDescriptionButton;
    @Inject
    private PopupButton frequentInteractionPopupButton;
    @Inject
    private DataGrid<CandidateCV> jobCandidateCandidateCvTable;
    @Inject
    private Button copyCVButton;
    @Inject
    private Button scanContactsFromCVButton;
    @Inject
    private Button checkSkillFromJD;
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
    private DataGrid<SocialNetworkURLs> socialNetworkTable;

    private static final String BLOCK_CANDIDATE_ON = "Запретить работу с кандидатом";
    private static final String BLOCK_CANDIDATE_OFF = "Разрешить работу с кандидатом";
    private static final String QUERY_GET_OTHER_SOCIAL_NETWORK = "select e from itpearls_SocialNetworkType e where e.socialNetwork = :other";
    private static final String QUERY_GET_CANDIDATE_CV = "select e from itpearls_CandidateCV e where e.candidate = :candidate";
    private static final String TELEGRAM_NAME_URL = "http://t.me/";
    private static final String QUERY_GET_LAST_ITERACTION = "select e from itpearls_IteractionList e where e.candidate = :candidate and e.numberIteraction = (select max(f.numberIteraction) from itpearls_IteractionList f where f.candidate = :candidate)";

    List<Position> setPos = new ArrayList<>();
    List<IteractionList> iteractionListFromCandidate = new ArrayList();
    IteractionList lastIteraction = null;

    @Inject
    private Button blockCandidateButton;
    @Inject
    private Label<String> iteractionListLabelCandidate;
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
    private Button copyIteractionButton;
    private boolean candidateInitialized = false;
    private boolean tabContactInfoInitialized = false;
    @Inject
    private Table lastProjectTable;
    @Inject
    private KeyValueCollectionContainer lastProjectDc;
    private boolean initSocialNetworkURLs = false;
    @Inject
    private CollectionLoader<OpenPosition> suggestOpenPositionDl;
    @Inject
    private Table<OpenPosition> suggestVacancyTable;
    @Inject
    private Image candidateDefaultPic;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private Button sendCommentButton;
    @Inject
    private TextField<String> chatMessageTextField;
    @Inject
    private LookupPickerField<OpenPosition> vacancyPopupPickerField;
    @Inject
    private DataGrid<IteractionList> jobCandidateCommentsDataGrid;
    @Inject
    private CollectionLoader<IteractionList> interactionCommentDl;
    private CollectionContainer<OpenPosition> suggestOpenPositionDc;
    @Inject
    private GridLayout dictionatysTavlesHBox;
    @Inject
    private GroupBoxLayout lastProjects;
    @Inject
    private Label<String> fullNameField;
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
    @Inject
    private Button addSocialNetworkListsButton;
    @Inject
    private LookupPickerField vacancyFilterLookupPickerField;
    @Inject
    private ResumeRecognitionService resumeRecognitionService;
    @Inject
    private OpenPositionService openPositionService;

    private Boolean ifCandidateIsExist() {
        setFullNameCandidate();
        // вдруг такой кандидат уже есть
        List<JobCandidate> candidates = dataManager.load(JobCandidate.class)
                .query("select e from itpearls_JobCandidate e where e.firstName like :firstName and e.secondName like :secondName")
                .cacheable(true)
                .parameter("firstName", firstNameField.getValue())
                .parameter("secondName", secondNameField.getValue())
                .view("jobCandidate-view")
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
        } */
    }

    private void setupSkillBox() {
        if (!PersistenceHelper.isNew(getEditedEntity())) {
            Skillsbar skillBoxFragment = fragments.create(this, Skillsbar.class);
            if (skillBoxFragment.generateSkillLabels(getLastCVText(getEditedEntity()))) {
                skillBox.add(skillBoxFragment.getFragment());
            }
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

    private void setCreatedUpdatedLabel() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");
        StringBuilder sb = new StringBuilder();

        if (!PersistenceHelper.isNew(getEditedEntity())) {
            sb.append("Создано: ")
                    .append(getEditedEntity().getCreatedBy())
                    .append(" (")
                    .append(simpleDateFormat.format(getEditedEntity().getCreateTs()))
                    .append(") ");

            if (getEditedEntity().getUpdatedBy() != null) {
                sb.append("/ Изменено: ")
                        .append(getEditedEntity().getUpdatedBy())
                        .append(" (")
                        .append(simpleDateFormat.format(getEditedEntity().getUpdateTs()))
                        .append(") ");
            }

/*            String retStr = "Создано: " + getEditedEntity().getCreatedBy()
                    + " (" + simpleDateFormat.format(getEditedEntity().getCreateTs()) + ") "
                    + (getEditedEntity().getUpdatedBy() != null ?
                    ("/ Изменено: " + getEditedEntity().getUpdatedBy() + " ("
                            + simpleDateFormat.format(getEditedEntity().getUpdateTs()) + ") ") : ""); */

            createdUpdatedLabel.setValue(sb.toString());
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
                    (telegramGroupField.getRawValue().equals("")) &&
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
        setPercentLabel();

        Boolean b = getEditedEntity().getBlockCandidate() == null ?
                false : blockCandidateCheckBox.getValue();
        setBlockUnblockButton(b);
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
            jobCandidateCandidateCvTable.repaint();
        });
    }

    private void setPercentLabel() {
        if (candidateInitialized) {
            // вычислить процент заполнения карточки кандидата
            Integer qualityPercent = setQualityPercent() * 100 / 14;

            if (!PersistenceHelper.isNew(getEditedEntity())) {
                labelQualityPercent.setValue(new StringBuilder()
                        .append("Процент заполнения карточки: ")
                        .append(qualityPercent.toString())
                        .append("%")
                        .toString());
            }
        }
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

/*        skypeNameField.setRequired(true);
        phoneField.setRequired(true);
        mobilePhoneField.setRequired(true);
        emailField.setRequired(true);
        telegramNameField.setRequired(true);
        whatsupNameField.setRequired(true);
        wiberNameField.setRequired(true);
        telegramGroupField.setRequired(true); */

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

    // загрузить таблицу взаимодействий
    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        initInteractionCommentDl();
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

        setSaveRecordOfViewCandidate();

        setSocialNetworkTable();
        enableDisableContacts();
        setLabelTitle();
        setCreatedUpdatedLabel();
        setRatingLabel(getEditedEntity());
        setupSkillBox();

        setLinkButtonEmail();
        setLinkButtonTelegrem();
        setLinkButtonTelegremGroup();
        setLinkButtonSkype();

        setSuggestOpenPositionTable();
        setLastProjectOfCandidate();
        setCandidatePicImage();
        setAddSocialNetworkButtonEnable();
        checkTelegramName();
        setIteractionListVacancyFilter();

        lastIteraction = interactionService.getLastIteraction(getEditedEntity());

        if (getRoleService.isUserRoles(userSession.getUser(), StandartRoles.MANAGER) ||
                getRoleService.isUserRoles(userSession.getUser(), StandartRoles.ADMINISTRATOR)) {
            blockCandidateButton.setVisible(true);
        } else {
            blockCandidateButton.setVisible(false);
        }

//        setLaborAgreement();
        setLastProjectTable();
    }


    @Install(to = "vacancyFilterLookupPickerField", subject = "optionImageProvider")
    private Resource vacancyFilterLookupPickerFieldOptionImageProvider(OpenPosition object) {
        Image imageResource = uiComponents.create(Image.class);
        imageResource.setWidth("20px");
        imageResource.setHeight("20px");
        imageResource.setScaleMode(Image.ScaleMode.FILL);

        if (object.getProjectName().getProjectLogo() == null) {
            return imageResource.createResource(ThemeResource.class)
                    .setPath("icons/no-company.png");
        } else {
            return imageResource.createResource(FileDescriptorResource.class)
                    .setFileDescriptor(object.getProjectName()
                            .getProjectLogo());
        }
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
    }

    private void setCandidatePicImage() {
        if (getEditedEntity().getFileImageFace() == null) {
            candidateDefaultPic.setVisible(true);
            candidatePic.setVisible(false);
        } else {
            candidateDefaultPic.setVisible(false);
            candidatePic.setVisible(true);
        }
    }

    @Subscribe("fileImageFaceUpload")
    public void onFileImageFaceUploadBeforeValueClear(FileUploadField.BeforeValueClearEvent event) {
        candidatePic.setVisible(false);
        candidateDefaultPic.setVisible(true);
    }

    @Subscribe("candidatePic")
    public void onCandidatePicSourceChange(ResourceView.SourceChangeEvent event) {
        setCandidatePicImage();
    }

    private void setSuggestOpenPositionTable() {
        List<Position> positions = new ArrayList<>();

        suggestVacancyTable.addStyleName("borderless");
        suggestVacancyTable.addStyleName("no-horizontal-lines");
        suggestVacancyTable.addStyleName("no-vertical-lines");

        if (!PersistenceHelper.isNew(getEditedEntity())) {
            if (getEditedEntity().getPositionList() != null) {
                for (JobCandidatePositionLists positionLists : getEditedEntity().getPositionList()) {
                    positions.add(positionLists.getPositionList());
                }

                suggestOpenPositionDl.setParameter("positionType", getEditedEntity().getPersonPosition());
                if (positions.size() > 0) {
                    suggestOpenPositionDl.setParameter("positionTypes", positions);
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
            jobCandidateCandidateCvTable.repaint();
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
        if (telegramNameField.getValue() != null) {
            if (telegramNameField.getValue().toLowerCase().startsWith(TELEGRAM_NAME_URL.toLowerCase())) {
                telegramNameField.setValue(
                        telegramNameField.getValue().substring(TELEGRAM_NAME_URL.length()));
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
        if (PersistenceHelper.isNew(getEditedEntity())) {
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
                        .loadValue("select max(e.numberIteraction) from itpearls_IteractionList e", BigDecimal.class)
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
                        .view("iteraction-view")
                        .parameter("iteractionName", "Новый контакт")
                        .one();
            } catch (Exception e) {
                notifications.create(Notifications.NotificationType.ERROR)
                        .withCaption("SQL ERROR")
                        .withDescription("Нет взаимодействия \"Новый контакт\"")
                        .show();
            }

            try {
                openPosition = openPositionService.getOpenPositionDefault();
/*                openPosition = dataManager.load(OpenPosition.class)
                        .query("select e from itpearls_OpenPosition e where e.vacansyName like :vacansyDefaultName")
                        .view("openPosition-view")
                        .parameter("vacansyDefaultName", "Default%")
                        .one(); */
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
                    "not e.id in " +
                    "(select f.id from itpearls_JobCandidate f where f.id = :uuid) and " +
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
        if (telegramNameField != null) {
            if (telegramNameField.getValue() != null) {
                String tn = telegramNameField.getValue();

                telegramNameField.setValue(telegramNameField.getValue().trim().charAt(0) == '@' ?
                        telegramNameField.getValue().trim().substring(1) :
                        telegramNameField.getValue().trim());
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
        Integer qPercent = 0;

        if (tabContactInfoInitialized) {

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

            if (jobCityCandidateField.getValue() != null)  // 7
                qPercent = ++qPercent;

            if (personPositionField.getValue() != null)    // 8
                qPercent = ++qPercent;

            if (phoneField.getValue() != null)             // 9
                qPercent = ++qPercent;

            if (mobilePhoneField.getValue() != null)       // 10
                qPercent = ++qPercent;

            if (skypeNameField.getValue() != null)         // 11
                qPercent = ++qPercent;

            if (telegramNameField.getValue() != null)      // 12
                qPercent = ++qPercent;

            if (whatsupNameField.getValue() != null)       // 13
                qPercent = ++qPercent;

            if (wiberNameField.getValue() != null)         // 14
                qPercent = ++qPercent;
        }

        return qPercent;
    }

    @Subscribe(id = "jobCandidateDc", target = Target.DATA_CONTAINER)
    private void onJobCandidateDcItemChange(InstanceContainer.ItemChangeEvent<JobCandidate> event) {
        setFullNameCandidate();
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
        tabSheetSocialNetworks.addSelectedTabChangeListener(selectedTabChangeEvent -> {
            initTabResume();
            initTabInteractions();
            initTabCandidate();
            initTabContactInfo();
        });
    }

    private void initTabContactInfo() {
        if (!tabContactInfoInitialized) {
            if (emailField == null) {
                emailField = (TextField<String>) getWindow().getComponent("emailField");
            }
            emailField.addTextChangeListener(e -> enableDisableContacts());

            if (phoneField == null) {
                phoneField = (TextField<String>) getWindow().getComponent("phoneField");
            }
            phoneField.addTextChangeListener(e -> enableDisableContacts());

            if (skypeNameField == null) {
                skypeNameField = (TextField<String>) getWindow().getComponent("skypeNameField");
            }
            skypeNameField.addTextChangeListener(e -> enableDisableContacts());

            if (telegramNameField == null) {
                telegramNameField = (TextField<String>) getWindow().getComponent("telegramNameField");
            }
            telegramNameField.addTextChangeListener(e -> enableDisableContacts());

            if (whatsupNameField == null) {
                whatsupNameField = (TextField<String>) getWindow().getComponent("whatsupNameField");
            }
            whatsupNameField.addTextChangeListener(e -> enableDisableContacts());

            if (wiberNameField == null) {
                wiberNameField = (TextField<String>) getWindow().getComponent("wiberNameField");
            }
            wiberNameField.addTextChangeListener(e -> enableDisableContacts());

            if (priorityCommunicationMethodRadioButton == null) {
                priorityCommunicationMethodRadioButton = (RadioButtonGroup) getWindow()
                        .getComponent("priorityCommunicationMethodRadioButton");
            }
            priorityCommenicationMethodRadioButtonInit();

            if (telegramGroupField == null) {
                telegramGroupField = (TextField<String>) getWindow().getComponent("telegramGroupField");
            }
            telegramGroupField.addTextChangeListener(e -> enableDisableContacts());

            if (mobilePhoneField == null) {
                mobilePhoneField = (TextField<String>) getWindow().getComponent("mobilePhoneField");
            }
            mobilePhoneField.addTextChangeListener(e -> enableDisableContacts());


            if (socialNetworkTable == null) {
                socialNetworkTable = (DataGrid<SocialNetworkURLs>) getWindow()
                        .getComponent("socialNetworkTable");
            }

            socialNetworkTable.addEditorCloseListener(e -> enableDisableContacts());
            socialNetworkTable.addEditorPostCommitListener(e -> enableDisableContacts());
            socialNetworkTable.addSelectionListener(e -> enableDisableContacts());

            trimTelegramName();
            enableDisableContacts();
            initSocialNeiworkTable();
        }

        tabContactInfoInitialized = true;
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
                positionsLabel = (Label<String>) getWindow().getComponent("positionsLabel");
            }
            if (positionsLabel != null) {
                setPositionsLabel();
            }

            addSuggestField();
            checkNotUsePosition();

            if (firstNameField != null && secondNameField != null) {
                candidateInitialized = true;
            }
        }
    }

    public void repaintSocialNetworksTable() {
        socialNetworkTable.repaint();
    }

    private void initTabInteractions() {
        if (!interationTabInitialized) {
            if (jobCandidateIteractionListTable == null) {
                jobCandidateIteractionListTable = (DataGrid<IteractionList>) getWindow()
                        .getComponent("jobCandidateIteractionListTable");
            }

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

            interationTabInitialized = true;
        }
    }

    private void initTabResume() {
        if (!cvTabInitialized) {
            if (scanContactsFromCVButton == null) {
                scanContactsFromCVButton = (Button) getWindow()
                        .getComponent("scanContactsFromCVButton");
            }
            scanContactsFromCVButton.addClickListener(e -> scanContactsFromCVs());

            if (copyCVButton == null) {
                copyCVButton = (Button) getWindow().getComponent("copyCVButton");
            }
            copyCVButton.addClickListener(e -> copyCVJobCandidate());
            setCopyCVButton();

            if (checkSkillFromJD == null) {
                checkSkillFromJD = (Button) getWindow().getComponent("checkSkillFromJD");
            }
            checkSkillFromJD.addClickListener(e -> checkSkillFromJD());

            if (jobCandidateCandidateCvTable == null) {
                jobCandidateCandidateCvTable = (DataGrid<CandidateCV>) getWindow()
                        .getComponent("jobCandidateCandidateCvTable");
            }

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

            jobCandidateCandidateCvTable.getColumn("iconITPearlsCVFile")
                    .setColumnGenerator(event -> {
                        return event.getItem().getLinkItPearlsCV() != null ?
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

            jobCandidateCandidateCvTable.getColumn("iconITPearlsCVFile")
                    .setDescriptionProvider(candidateCV -> {
                        return candidateCV.getLinkItPearlsCV();
                    });

            jobCandidateCandidateCvTable.getColumn("iconITPearlsCVFile")
                    .setStyleProvider(candidateCV -> {
                        String style = "";

                        if (candidateCV.getLinkItPearlsCV() != null) {
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

            jobCandidateCandidateCvTable.getColumn("candidateITPearlsCVColumn")
                    .setColumnGenerator(event -> {
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

            jobCandidateCandidateCvTable.getColumn("candidateOriginalCVColumn")
                    .setColumnGenerator(event -> {
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

        cvTabInitialized = true;
    }

    private List<IteractionList> getIteractionListFromCandidate(JobCandidate editedEntity) {
        return getEditedEntity().getIteractionList();
    }

    private void setCopyCVButton() {
        if (copyCVButton != null) {
            copyCVButton.setEnabled(false);
        }

    }

    private void addMiddleNameSuggestField() {
        String queryString = "select distinct e.middleName from itpearls_JobCandidate e order by e.middleName";

        List<String> middleName = dataManager.loadValue(queryString, String.class)
                .list();

        middleNameField.setSearchExecutor((searchString, searchParams) ->
                middleName.stream()
                        .filter(str -> StringUtils.containsIgnoreCase(str, searchString))
                        .collect(Collectors.toList()));
    }

    private void addSecondNameSuggestField() {
        String queryString = "select distinct e.secondName from itpearls_JobCandidate e order by e.secondName";

        List<String> secondName = dataManager.loadValue(queryString, String.class)
                .list();

        secondNameField.setSearchExecutor((searchString, searchParams) ->
                secondName.stream()
                        .filter(str -> StringUtils.containsIgnoreCase(str, searchString))
                        .collect(Collectors.toList()));
    }

    private void addFirstNameSuggestField() {
        String queryString = "select distinct e.firstName from itpearls_JobCandidate e order by e.firstName";

        List<String> firstName = dataManager.loadValue(queryString, String.class)
                .list();

        firstNameField.setSearchExecutor((searchString, searchParams) ->
                firstName.stream()
                        .filter(str -> StringUtils.containsIgnoreCase(str, searchString))
                        .collect(Collectors.toList()));
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
                    "from itpearls_IteractionList e " +
                    "where e.candidate = :candidate and " +
                    "e.vacancy = :vacancy", BigDecimal.class)
                    .parameter("candidate", getEditedEntity())
                    .parameter("vacancy", openPosition)
                    .one()
                    .add(BigDecimal.ONE);
        } else {
            return dataManager.loadValue("select count(e.numberIteraction) " +
                    "from itpearls_IteractionList e " +
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

    @Subscribe("fileImageFaceUpload")
    public void onFileImageFaceUploadFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {
        try {

            candidateDefaultPic.setVisible(false);
            candidatePic.setVisible(true);

            FileDescriptorResource fileDescriptorResource = candidatePic.createResource(FileDescriptorResource.class)
                    .setFileDescriptor(fileImageFaceUpload.getFileDescriptor());

            candidatePic.setSource(fileDescriptorResource);

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
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
                    dataContext.commit();
                }
            }

            setPositionsLabel();
        });

        selectPersonPositions.show();
    }

    private void setPositionsLabel() {
//        String outStr = "";
//        String description = "";
        StringBuilder sb = new StringBuilder();
        StringBuilder sbDesc = new StringBuilder();

        if (getEditedEntity().getPositionList() != null) {
            for (JobCandidatePositionLists s : getEditedEntity().getPositionList()) {
                if (!sb.toString().equals("")) {
                    sb.append(",");
//                   outStr = outStr + ",";
                    sbDesc.append("\n");
//                    description = description + "\n";
                }

                sb.append(s.getPositionList().getPositionRuName());
//                outStr = outStr + s.getPositionList().getPositionRuName();
                sbDesc.append(s.getPositionList().getPositionRuName());
//                description = description + s.getPositionList().getPositionRuName();
            }

        }

        if (!sb.toString().equals("")) {
            positionsLabel.setValue(sb.toString());
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
                            candidate.setLinkItPearlsCV(jobCandidateCandidateCvTable.getSingleSelected().getLinkItPearlsCV());
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
        String oldEmail = null;
        if (emailField == null) {
            initTabContactInfo();
        }

        oldEmail = emailField.getValue();

        String oldPhone = null;
        if (phoneField == null) {
            initTabContactInfo();
        }

        oldPhone = parseCVService.normalizePhoneStr(phoneField.getValue());

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

        // Social
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
                    if (!newEmail.equals(emailField != null ? emailField.getValue() : null)) {
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
                            if (emailField != null) {
                                emailField.setValue(newEmail);
                            } else {
                                initTabContactInfo();
                                emailField.setValue(newEmail);
                            }
                        }
                    }

                    if (newPhoneFlag != null) {
                        if (newPhoneFlag) {
                            if (phoneField != null) {
                                phoneField.setValue(newPhoneNew);
                            } else {
                                initTabContactInfo();
                                phoneField.setValue(newPhoneNew);
                            }
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
            if (!newEmail.equals(emailField.getValue())) {
                message = new StringBuilder(message)
                        .append("<i> - адрес электронной почты старый </i>")
                        .append("<b>")
                        .append(emailField.getValue())
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
                    .equals(parseCVService.normalizePhoneStr(phoneField.getValue()))) {
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
                        .append(phoneField.getValue())
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

    @Subscribe("phoneField")
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


    @Subscribe("mobilePhoneField")
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

    @Install(to = "socialNetworkTable.linkToWeb", subject = "columnGenerator")
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
                if (iteractionList.getIteractionType() != null) {
                    if (iteractionList.getIteractionType().getSignOurInterviewAssigned() != null) {
                        if (iteractionList.getVacancy() != null) {
                            if (iteractionList.getVacancy().equals(op) &&
                                    iteractionList.getIteractionType().getSignOurInterviewAssigned()) {
                                if (iteractionList.getRecrutier() != null) {
                                    retLabel.setValue(iteractionList.getRecrutier().getName());
                                } else {
                                    if (iteractionList.getRecrutierName() != null) {
                                        retLabel.setValue(iteractionList.getRecrutierName());
                                    }
                                }
                            }
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
                if (iteractionList.getIteractionType() != null) {
                    if (iteractionList.getIteractionType().getSignOurInterview() != null) {
                        if (iteractionList.getVacancy() != null) {
                            if (iteractionList.getVacancy().equals(op) &&
                                    iteractionList.getIteractionType().getSignOurInterview()) {
                                if (iteractionList.getRecrutier() != null) {
                                    if (iteractionList.getRecrutier().getName() != null) {
                                        retLabel.setValue(iteractionList.getRecrutier().getName());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return retLabel;
    }

    public Component lastInteractionGeneratorColumn(Entity entity) {
        Label retLabel = uiComponents.create(Label.NAME);
        retLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);
        OpenPosition openPosition = entity.getValue("vacancy");
        IteractionList lastInteraction = null;
        jobCandidateIteractionDc.setDisconnectedItems(getEditedEntity().getIteractionList());

        if (jobCandidateIteractionDc.getMutableItems().size() != 0) {
            for (int i = 0; i < jobCandidateIteractionDc.getMutableItems().size(); i++) {
                if (lastInteraction != null) {
                    if (openPosition.equals(jobCandidateIteractionDc
                            .getMutableItems()
                            .get(i)
                            .getVacancy())) {
                        if (jobCandidateIteractionDc
                                .getMutableItems()
                                .get(i)
                                .getDateIteraction() != null) {
                            if (lastInteraction.getDateIteraction().before(
                                    jobCandidateIteractionDc
                                            .getMutableItems()
                                            .get(i)
                                            .getDateIteraction())) {
                                lastInteraction = jobCandidateIteractionDc
                                        .getMutableItems()
                                        .get(i);
                            }
                        }
                    }
                } else {
                    if (jobCandidateIteractionDc
                            .getMutableItems()
                            .get(i)
                            .getVacancy() != null) {
                        if (openPosition.equals(jobCandidateIteractionDc
                                .getMutableItems()
                                .get(i)
                                .getVacancy())) {
                            lastInteraction = jobCandidateIteractionDc
                                    .getMutableItems()
                                    .get(i);
                        }
                    }
                }
            }
        }

        StringBuffer retStr = new StringBuffer("");
        if (lastInteraction.getIteractionType() != null) {
            if (lastInteraction.getIteractionType().getIterationName() != null) {
                retStr.append(lastInteraction.getIteractionType().getIterationName());
            }
        }

        retLabel.setValue(retStr);

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


    @Install(to = "jobCandidateIteractionListTable.currentOpenCloseColumn", subject = "columnGenerator")
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

    @Install(to = "jobCandidateIteractionListTable.currentOpenCloseColumn", subject = "styleProvider")
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

    @Install(to = "jobCandidateIteractionListTable.currentOpenCloseColumn", subject = "descriptionProvider")
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

    @Install(to = "suggestVacancyTable", subject = "itemDescriptionProvider")
    private String suggestVacancyTableItemDescriptionProvider(OpenPosition openPosition, String string) {
//        String retStr = "<b>Вакансия:</b><br><br>";
        StringBuilder sb = new StringBuilder("<b>Вакансия:</b><br><br>");

        sb.append("<i>")
                .append(openPosition.getVacansyName())
                .append("</i><br>")
                .append("<i>Проект: </i>")
                .append(openPosition.getProjectName().getProjectName())
                .append("<br><i>Ответственный за проект у заказчика:</i>")
                .append(openPosition.getProjectName().getProjectOwner().getSecondName())
                .append(" ")
                .append(openPosition.getProjectName().getProjectOwner().getFirstName())
                .append("<br><i>Ответственный за проект на нашей стороне: </i>")
                .append(openPosition.getOwner().getName())
                .append("<br><i>Дата открытия вакансии: ")
                .append(openPosition.getLastOpenDate())
                .append("<br><br><i>Описание вакансии: </i><br>")
                .append(openPosition.getComment());

/*        retStr += "<i>" + openPosition.getVacansyName() + "</i><br>"
                + "<i>Проект: </i>" + openPosition.getProjectName().getProjectName()
                + "<br><i>Ответственный за проект у заказчика:</i>"
                + openPosition.getProjectName().getProjectOwner().getSecondName()
                + openPosition.getProjectName().getProjectOwner().getSecondName()
                + "<br><i>Ответственный за проект на нашей стороне: </i>"
                + openPosition.getOwner().getName()
                + "<br><i>Дата открытия вакансии: "
                + openPosition.getLastOpenDate()
                + "<br><br><i>Описание вакансии: </i><br>" + openPosition.getComment();*/

        return sb.toString();
    }

    @Install(to = "suggestVacancyTable.notSendedIconColumn", subject = "columnGenerator")
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

    @Install(to = "jobCandidateCandidateCvTable.toVacancy", subject = "descriptionProvider")
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

    @Install(to = "jobCandidateIteractionListTable.vacancy", subject = "styleProvider")
    private String jobCandidateIteractionListTableVacancyStyleProvider(IteractionList iteractionList) {
        return "table-wordwrap";
    }

    @Install(to = "jobCandidateCandidateCvTable.resumePosition", subject = "descriptionProvider")
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

    @Install(to = "jobCandidateCandidateCvTable.toVacancy", subject = "styleProvider")
    private String jobCandidateCandidateCvTableToVacancyStyleProvider(CandidateCV candidateCV) {
        return "table-wordwrap";
    }

    @Install(to = "jobCandidateIteractionListTable.iteractionType", subject = "styleProvider")
    private String jobCandidateIteractionListTableIteractionTypeStyleProvider(IteractionList iteractionList) {
        return "table-wordwrap";
    }

    @Install(to = "jobCandidateCommentsDataGrid.comment", subject = "styleProvider")
    private String jobCandidateCommentsDataGridCommentStyleProvider(IteractionList iteractionList) {
        return "table-wordwrap";
    }

    @Install(to = "jobCandidateCommentsDataGrid.commentDialog", subject = "columnGenerator")
    private Component jobCandidateCommentsDataGridCommentDialogColumnGenerator(DataGrid.ColumnGeneratorEvent<IteractionList> event) {
        VBoxLayout retBox = uiComponents.create(VBoxLayout.class);
        retBox.setWidthFull();
        retBox.setSpacing(false);
        retBox.setMargin(false);
        retBox.setHeight("100px");

        HBoxLayout innerBox = uiComponents.create(HBoxLayout.class);
        innerBox.setMargin(true);
        innerBox.setWidthAuto();
        innerBox.setSpacing(true);

        VBoxLayout outerBox = uiComponents.create(VBoxLayout.class);
        outerBox.setMargin(false);
        outerBox.setWidthAuto();
        outerBox.setSpacing(false);

        if (event.getItem().getComment() != null
                && !event.getItem().getComment().equals("")) {
            Label name = uiComponents.create(Label.class);
            if (event.getItem().getRecrutier() != null) {
                name.setValue(event.getItem().getRecrutier().getName() != null
                        ? event.getItem().getRecrutier().getName() :
                        (event.getItem().getRecrutierName() != null ? event.getItem().getRecrutierName() : ""));
            }
            name.setStyleName("tailName");

            Label vacancy = uiComponents.create(Label.class);
            vacancy.setValue(event.getItem().getVacancy() != null &&
                    !event.getItem().getVacancy().getVacansyName().equals("Default")
                    ? event.getItem().getVacancy().getVacansyName() : "");
            vacancy.setStyleName("tailVacancy");

            Label text = uiComponents.create(Label.class);
            text.setValue(event.getItem().getComment() != null ?
                    event.getItem().getComment().replaceAll("\n\n", "\n") : "");
            text.addStyleName("table-wordwrap");

            Label date = uiComponents.create(Label.class);
            date.setValue(event.getItem().getDateIteraction() != null ?
                    event.getItem().getDateIteraction() : "");
            date.setAlignment(Component.Alignment.BOTTOM_RIGHT);
            date.setStyleName("tailDate");

            Image image = uiComponents.create(Image.class);

            if (event.getItem().getRecrutier() != null) {
                if (((ExtUser) event.getItem().getRecrutier()).getFileImageFace() != null) {
                    image.setSource(FileDescriptorResource.class)
                            .setFileDescriptor(((ExtUser) event.getItem().getRecrutier()).getFileImageFace());
                } else {
                    image.setSource(ThemeResource.class)
                            .setPath("icons/no-programmer.jpeg");
                }
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

            if (userSession.getUser().getLogin().equals(event.getItem().getCreatedBy())) {
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

            if (!userSession.getUser().getLogin().equals(event.getItem().getCreatedBy())) {
                innerBox.add(image);
            }

            innerBox.add(outerBox);
            if (userSession.getUser().getLogin().equals(event.getItem().getCreatedBy())) {
                innerBox.add(image);
            }

            retBox.add(innerBox);
        }

        return retBox;
    }

    private void replyButtonInvoke(Button.ClickEvent e, String replyStr) {
        createComment(replyStr);
    }

    @Subscribe("chatMessageTextField")
    public void onChatMessageTextFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        if (event.getValue() == null || event.getValue().equals("")) {
            sendCommentButton.setEnabled(false);
        } else {
            sendCommentButton.setEnabled(true);
        }
    }

    @Subscribe("chatMessageTextField")
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
                    .loadValue("select e from itpearls_Iteraction e where e.signComment = true",
                            Iteraction.class)
                    .one();
        } catch (IllegalStateException e) {
        }

        if (iteractionComment != null) {
            BigDecimal numberInteraction = dataManager
                    .loadValue("select max(e.numberIteraction) from itpearls_IteractionList e",
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
                            .loadValue("select e from itpearls_OpenPosition e where e.vacansyName like \'Default\'",
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
            jobCandidateCommentsDataGrid.setSelected(comment);
        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("msgError"))
                    .withDescription(messageBundle.getMessage("msgDoNotCommentInteraction"))
                    .withType(Notifications.NotificationType.ERROR)
                    .show();
        }

    }

    private void reloadCV() {
        dataContext.commit();

        jobCandidateDl.load();
        jobCandidateCandidateCvTable.repaint();
    }

    private void reloadInteractions() {
        dataContext.commit();

        interactionCommentDl.load();
        jobCandidateCommentsDataGrid.repaint();

        jobCandidateDl.load();
        jobCandidateIteractionListTable.repaint();
    }

    public void setFirstNameField(List<String> suggestFirstNames) {
        firstNameField.setValueSource((ValueSource<String>) suggestFirstNames);
    }

    @Subscribe("firstNameField")
    public void onFirstNameFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        if (event.getValue() != null) {
            setFullNameCandidate();
        }

        StringBuffer fullName = new StringBuffer();

        fullName.append(event.getValue())
                .append(" ")
                .append(secondNameField.getValue());

        iteractionListLabelCandidate.setValue(fullName.toString());
        fullNameField.setValue(fullName.toString());
    }

    @Subscribe("secondNameField")
    public void onSecondNameFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        if (event.getValue() != null) {
            setFullNameCandidate();
        }

        StringBuffer fullName = new StringBuffer();

        fullName.append(firstNameField.getValue())
                .append(" ")
                .append(event.getValue());

        iteractionListLabelCandidate.setValue(fullName.toString());
        fullNameField.setValue(fullName.toString());
    }

    @Subscribe("emailField")
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

    @Subscribe("mobilePhoneField")
    public void onMobilePhoneFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        if (event.getValue() != null) {
            mobilePhoneLabel.setValue(event.getValue());
        }
    }

    @Subscribe("skypeNameField")
    public void onSkypeNameFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        if (event.getValue() != null) {
            skypuLabel.setValue(event.getValue());
        }

    }

    @Subscribe("telegramNameField")
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
                .list();

        for (SocialNetworkType socialNetworkType : socialNetworkTypes) {
            Boolean flag = true;

            for (SocialNetworkURLs socialNetworkURLs : getEditedEntity().getSocialNetwork()) {
                if (socialNetworkURLs.getSocialNetworkURL().equals(socialNetworkType)) {
                    flag = false;
                }
            }

            if (flag) {
                SocialNetworkURLs socialNetworkURLs = metadata.create(SocialNetworkURLs.class);
                socialNetworkURLs.setJobCandidate(getEditedEntity());
                socialNetworkURLs.setSocialNetworkURL(socialNetworkType);
                socialNetworkURLs.setNetworkName(socialNetworkType.getSocialNetwork());

                getEditedEntity().getSocialNetwork().add(socialNetworkURLs);
                dataContext.merge(socialNetworkURLs);
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

    @Install(to = "vacancyPopupPickerField", subject = "optionIconProvider")
    private String vacancyPopupPickerFieldOptionIconProvider(OpenPosition openPosition) {
        if (!openPosition.getOpenClose()) {
            return CubaIcon.PLUS_CIRCLE.source();
        } else {
            return CubaIcon.MINUS_CIRCLE.source();
        }
    }

    @Install(to = "jobCandidateCandidateCvTable.projectLogoColumn", subject = "columnGenerator")
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
                    image.setSource(FileDescriptorResource.class)
                            .setFileDescriptor(event
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

    @Install(to = "jobCandidateIteractionListTable.projectLogoColumn", subject = "columnGenerator")
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
                    image.setSource(FileDescriptorResource.class)
                            .setFileDescriptor(event
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

    @Install(to = "socialNetworkTable.socialNetworkLogoColumn", subject = "columnGenerator")
    private Component socialNetworkTableSocialNetworkLogoColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<SocialNetworkURLs> event) {
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        retBox.setWidthFull();
        retBox.setHeightFull();

        StringBuilder sb = new StringBuilder();
        sb.append("<h4>")
                .append(event.getItem().getSocialNetworkURL().getSocialNetwork())
                .append("</h4><br><br>")
                .append((event.getItem().getSocialNetworkURL().getComment() != null
                        ? event.getItem().getSocialNetworkURL().getComment() : ""));

        Image image = uiComponents.create(Image.class);
        image.setDescriptionAsHtml(true);
        image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        image.setWidth("30px");
        image.setHeight("30px");
        image.setStyleName("icon-no-border-30px");
        image.setAlignment(Component.Alignment.MIDDLE_CENTER);
        image.setDescription(sb.toString());

        if (event.getItem().getSocialNetworkURL().getLogo() != null) {
            image.setSource(FileDescriptorResource.class)
                    .setFileDescriptor(event.getItem().getSocialNetworkURL()
                            .getLogo());
        } else {
            image.setSource(ThemeResource.class).setPath("icons/no-company.png");
        }

        retBox.add(image);
        return retBox;
    }
}
