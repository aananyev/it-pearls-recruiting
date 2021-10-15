package com.company.itpearls.web.screens.jobcandidate;
// TODO сделать сортировку по номеру взаимодействия или дате в таблице IteractioNList.Borwse

import com.company.itpearls.core.ParseCVService;
import com.company.itpearls.core.PdfParserService;
import com.company.itpearls.core.StarsAndOtherService;
import com.company.itpearls.entity.*;
import com.company.itpearls.web.screens.openposition.QuickViewOpenPositionDescription;
import com.company.itpearls.web.screens.skilltree.SkillTreeBrowseCheck;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogActions;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogOutcome;
import com.haulmont.cuba.gui.app.core.inputdialog.InputDialog;
import com.haulmont.cuba.gui.app.core.inputdialog.InputParameter;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
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
    @Inject
    private DataManager dataManager;
    @Inject
    private Dialogs dialogs;
    @Inject
    private Label<String> labelCV;
    @Named("tabIteraction")
    private VBoxLayout tabIteraction;
    @Named("tabResume")
    private VBoxLayout tabResume;
    @Inject
    private DateField<Date> birdhDateField;
    @Inject
    private LookupPickerField<Company> currentCompanyField;
    @Inject
    private TextField<String> emailField;
    @Inject
    private LookupPickerField<City> jobCityCandidateField;
    @Inject
    private LookupPickerField<Position> personPositionField;
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
    private DataGrid<IteractionList> jobCandidateIteractionListTable;
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
    private SuggestionField<String> firstNameField;
    @Inject
    private SuggestionField<String> secondNameField;
    @Inject
    private SuggestionField<String> middleNameField;
    @Inject
    private Label<String> positionsLabel;
    @Inject
    private PdfParserService pdfParserService;
    @Inject
    private DataGrid<CandidateCV> jobCandidateCandidateCvTable;
    @Inject
    private CollectionPropertyContainer<CandidateCV> jobCandidateCandidateCvsDc;
    @Inject
    private Button copyCVButton;
    private long msec = 0;
    @Inject
    private Notifications notifications;
    @Inject
    private Label<String> createdUpdatedLabel;
    @Inject
    private Button checkSkillFromJD;
    @Inject
    private LinkButton telegrammGroupLinkButton;
    @Inject
    private TextField<String> mobilePhoneField;
    @Inject
    private Label<String> candidateRatingLabel;

    List<Position> setPos = new ArrayList<>();
    List<IteractionList> iteractionListFromCandidate = new ArrayList();
    IteractionList lastIteraction = null;

    @Inject
    private StarsAndOtherService starsAndOtherService;
    @Inject
    private InstanceContainer<JobCandidate> jobCandidateDc;
    @Inject
    private InstanceLoader<JobCandidate> jobCandidateDl;
    @Inject
    private Screens screens;
    @Inject
    private Button scanContactsFromCVButton;
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
    private CollectionPropertyContainer<Position> positionsListDc;
    @Inject
    private CollectionPropertyContainer<IteractionList> jobCandidateIteractionDc;


    String QUERY_GET_LAST_ITERACTION = "select e " +
            "from itpearls_IteractionList e " +
            "where e.candidate = :candidate and " +
            "e.numberIteraction = (select max(f.numberIteraction) from itpearls_IteractionList f where f.candidate = :candidate)";
    @Inject
    private Button openPositionProjectDescriptionButton;

    private Boolean ifCandidateIsExist() {
        setFullNameCandidate();
        // вдруг такой кандидат уже есть
        List<JobCandidate> candidates = dataManager.load(JobCandidate.class)
                .query("select e from itpearls_JobCandidate e where e.firstName like :firstName and " +
                        "e.secondName like :secondName")
                .parameter("firstName", firstNameField.getValue())
                .parameter("secondName", secondNameField.getValue())
                .view("jobCandidate-view")
                .list();

        return candidates.size() == 0 ? false : true;
    }

    @Subscribe
    public void onBeforeShow1(BeforeShowEvent event) {
        // основные социальные сети показать
        List<SocialNetworkURLs> socialNetwork = getEditedEntity().getSocialNetwork();
        List<SocialNetworkType> socialNetworkType = dataManager.load(SocialNetworkType.class)
                .list();

        // тут либо если не новая запись, то проверить на наличие других записей, либо если новая запись, то пофигу
        if (!PersistenceHelper.isNew(getEditedEntity())) {
            if (socialNetwork.size() < socialNetworkType.size()) {

                List<SocialNetworkType> type = dataManager
                        .load(SocialNetworkType.class)
                        .query("select e.socialNetworkURL " +
                                "from itpearls_SocialNetworkURLs e " +
                                "where e.jobCandidate = :candidate")
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
        }

        enableDisableContacts();

        setLabelTitle();
        setPositionsLabel();
        setCreatedUpdatedLabel();
        setRatingLabel(getEditedEntity());
    }

    private void setCreatedUpdatedLabel() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");

        if (!PersistenceHelper.isNew(getEditedEntity())) {
            String retStr = "Создано: " + getEditedEntity().getCreatedBy() + " (" + simpleDateFormat.format(getEditedEntity().getCreateTs()) + ") "
                    + "/ Изменено: " + getEditedEntity().getUpdatedBy() + " (" + simpleDateFormat.format(getEditedEntity().getUpdateTs()) + ") ";

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

/*        for (SocialNetworkURLs a : jobCandidateSocialNetworksDc.getItems()) {
            if (a.getNetworkURLS() != null) {
                isEmptySN = true;
                break;
            }
        } */

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
        if (PersistenceHelper.isNew(getEditedEntity())) {
            CommitContext commitContext = new CommitContext(getEditedEntity());

            for (SocialNetworkURLs s : jobCandidateSocialNetworksDc.getItems()) {
                commitContext.addInstanceToCommit(s);
            }

            dataManager.commit(commitContext);
//            createFirstIteraction();
        }
    }

    private void createFirstIteraction() {
        String newCandidate = "Новый контакт";

        String queryNewCandidate = "select e from itpearls_Iteraction e "
                + "where e.iterationName like \'"
                + newCandidate
                + "\'";

        String queryNewVacancy = "select e from itpearls_OpenPosition e "
                + "where e.vacansyName like \'"
                + newCandidate
                + "\'";

        IteractionList iteractionList = new IteractionList();
        iteractionList.setRating(3);
        iteractionList.setCandidate(getEditedEntity());
        if (lastIteraction != null) {
            iteractionList.setNumberIteraction(lastIteraction
                    .getNumberIteraction()
                    .add(BigDecimal.ONE));
        }

        Iteraction iteraction = dataManager.load(Iteraction.class)
                .query(queryNewCandidate)
                .one();
        OpenPosition openPosition = dataManager.load(OpenPosition.class)
                .query(queryNewVacancy)
                .one();

        iteractionList.setIteractionType(iteraction);
        iteractionList.setVacancy(openPosition);

        CommitContext commitContext = new CommitContext(getEditedEntity());
        commitContext.addInstanceToCommit(iteractionList);
        dataManager.commit(commitContext);
    }

    private AtomicReference<Boolean> returnE = new AtomicReference<>(false);

    @Subscribe("tabIteraction")
    public void onTabIteractionLayoutClick(LayoutClickNotifier.LayoutClickEvent event) {
    }

    @Install(to = "jobCandidateIteractionListTable.vacancy", subject = "descriptionProvider")
    private String jobCandidateIteractionListTableVacancyDescriptionProvider(IteractionList iteractionList) {
        String retStr = "";

        if (iteractionList.getVacancy() != null) {
            if (iteractionList.getVacancy().getVacansyName() != null) {
                retStr = iteractionList.getVacancy().getVacansyName();
            }
        }

        return Jsoup.parse(retStr).text();
    }

    @Install(to = "jobCandidateIteractionListTable.projectName", subject = "descriptionProvider")
    private String jobCandidateIteractionListTableProjectNameDescriptionProvider(IteractionList iteractionList) {
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
    }

    @Subscribe("firstNameField")
    public void onFirstNameFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        setLabelFullName(setFullName(getEditedEntity().getFirstName(), null, null));
    }

    @Subscribe("middleNameField")
    public void onMiddleNameFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        setLabelFullName(setFullName(null, getEditedEntity().getMiddleName(), null));
    }

    @Subscribe("secondNameField")
    public void onSecondNameFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        setLabelFullName(setFullName(null, null, getEditedEntity().getSecondName()));
    }

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
        checkNotUsePosition();
    }

    @Subscribe
    public void onAfterClose(AfterCloseEvent event) {
        // чтоб после закрытия не возникало
        jobCandidateCandidateCvsDc.addCollectionChangeListener(e -> {
        });
    }

    private void setPercentLabel() {
        // вычислить процент заполнения карточки кандидата
        Integer qualityPercent = setQualityPercent() * 100 / 14;

        if (!PersistenceHelper.isNew(getEditedEntity())) {
            labelQualityPercent.setValue("Процент заполнения карточки: " + qualityPercent.toString()
                    + "%");
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

    // загрузить таблицу взаимодействий
    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {

        if (!PersistenceHelper.isNew(getEditedEntity())) {
            iteractionListFromCandidate = getIteractionListFromCandidate(getEditedEntity());
        }

        if (!PersistenceHelper.isNew(getEditedEntity())) {
            if (!getEditedEntity().getFullName().equals("")) {
                if (getEditedEntity().getFullName() == null)
                    getEditedEntity().setFullName("");
            } else {
            }
            // заблокировать вкладки с резюме и итеракицями
            tabIteraction.setVisible(true);
            tabResume.setVisible(true);
        } else {
            // заблокировать вкладки с резюме и итеракицями
            tabIteraction.setVisible(false);
            tabResume.setVisible(false);
        }

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
        if (personPositionField.getValue() != null) {
            if (personPositionField.getValue().getPositionRuName().contains("не использовать")) {
                personPositionField.setValue(null);
            }
        }
    }

    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        secondNameField.setValue(replaceE_E(secondNameField.getValue()));
        firstNameField.setValue(replaceE_E(firstNameField.getValue()));

        if (middleNameField.getValue() != null)
            middleNameField.setValue(replaceE_E(middleNameField.getValue()));

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

//        createFirstIteraction();
    }

    private void addIteractionOfNewCandidate() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            // добавить сюда Iteraction "Новый кандиат"
        }
    }

    private JobCandidate checkDublicateCandidate() {
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
                .view("jobCandidate-view")
                .list();

        if (jobCandidates.size() == 0) {
            return null;
        } else {
            return jobCandidates.get(0);
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

        addIconColumn();
        setCopyCVButton();

        msec = System.currentTimeMillis();

        jobCandidateCandidateCvTable.addSelectionListener(e -> {
            if (e.getSelected() == null) {
                checkSkillFromJD.setEnabled(false);
                scanContactsFromCVButton.setEnabled(false);
            } else {
                checkSkillFromJD.setEnabled(true);
                scanContactsFromCVButton.setEnabled(true);
            }
        });

        jobCandidateIteractionListTable.addSelectionListener(e -> {
            if (e.getSelected() == null) {
                openPositionProjectDescriptionButton.setEnabled(false);
            } else {
                openPositionProjectDescriptionButton.setEnabled(true);
            }
        });

        jobCandidateIteractionListTable.addEditorPostCommitListener(e -> {
//            jobCandidateDl.load();
        });
    }

    private List<IteractionList> getIteractionListFromCandidate(JobCandidate editedEntity) {
        String QUERY_GET_ITERCATION_LIST = "select e from itpearls_IteractionList e where e.candidate = :candidate";

        return dataManager.load(IteractionList.class)
                .query(QUERY_GET_ITERCATION_LIST)
                .parameter("candidate", editedEntity)
                .view("iteractionList-view")
                .list();
    }

    private void setCopyCVButton() {
        copyCVButton.setEnabled(false);
        jobCandidateCandidateCvTable.addItemClickListener(e -> {
            if (e.getItem() != null) {
                copyCVButton.setEnabled(true);
            } else {
                copyCVButton.setEnabled(false);
            }
        });
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

    @Install(to = "secondNameField", subject = "enterActionHandler")
    private void secondNameFieldEnterActionHandler(String searchString) {
        secondNameField.setValue(searchString);
    }

    @Install(to = "firstNameField", subject = "enterActionHandler")
    private void firstNameFieldEnterActionHandler(String searchString) {
        firstNameField.setValue(searchString);
    }

    @Install(to = "middleNameField", subject = "enterActionHandler")
    private void middleNameFieldEnterActionHandler(String searchString) {
        middleNameField.setValue(searchString);
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

//        jobCandidateIteractionListDataGridDl.load();
    }

    @Install(to = "jobCandidateCandidateCvTable.letter", subject = "descriptionProvider")
    private String jobCandidateCandidateCvTableLetterDescriptionProvider(CandidateCV candidateCV) {
        String returnData = candidateCV.getLetter() != null ? Jsoup.parse(candidateCV.getLetter()).text() : "";
        return returnData;
    }

    @Install(to = "jobCandidateCandidateCvTable.iconOriginalCVFile", subject = "descriptionProvider")
    private String jobCandidateCandidateCvTableIconOriginalCVFileDescriptionProvider(CandidateCV candidateCV) {
        return candidateCV.getLinkOriginalCv();
    }

    @Install(to = "jobCandidateCandidateCvTable.iconITPearlsCVFile", subject = "descriptionProvider")
    private String jobCandidateCandidateCvTableIconITPearlsCVFileDescriptionProvider(CandidateCV candidateCV) {
        return candidateCV.getLinkItPearlsCV();
    }

    private IteractionList getLastIteraction() {
        try {
            lastIteraction = dataManager.load(IteractionList.class)
                    .query(QUERY_GET_LAST_ITERACTION)
                    .parameter("candidate", getEditedEntity())
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


    @Install(to = "jobCandidateCandidateCvTable.iconITPearlsCVFile", subject = "styleProvider")
    private String jobCandidateCandidateCvTableIconITPearlsCVFileStyleProvider(CandidateCV candidateCV) {
        String style = "";

        if (candidateCV.getLinkItPearlsCV() != null) {
            style = "pic-center-large-green";
        } else {
            style = "pic-center-large-red";
        }

        return style;
    }

    @Install(to = "jobCandidateCandidateCvTable.iconOriginalCVFile", subject = "styleProvider")
    private String jobCandidateCandidateCvTableIconOriginalCVFileStyleProvider(CandidateCV candidateCV) {
        return candidateCV.getLinkOriginalCv() != null ? "pic-center-large-green" : "pic-center-large-red";
    }

    @Install(to = "jobCandidateCandidateCvTable.letter", subject = "styleProvider")
    private String jobCandidateCandidateCvTableLetterStyleProvider(CandidateCV candidateCV) {
        return candidateCV.getLetter() != null ? "pic-center-large-green" : "pic-center-large-red";
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

    @Install(to = "jobCandidateCandidateCvTable.letter", subject = "columnGenerator")
    private Icons.Icon jobCandidateCandidateCvTableLetterColumnGenerator
            (DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        return event.getItem().getLetter() != null ?
                CubaIcon.valueOf("FILE_TEXT") :
                CubaIcon.valueOf("FILE");
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

    @Install(to = "jobCandidateCandidateCvTable.iconOriginalCVFile", subject = "columnGenerator")
    private Icons.Icon jobCandidateCandidateCvTableIconOriginalCVFileColumnGenerator
            (DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        return event.getItem().getLinkOriginalCv() != null ?
                CubaIcon.valueOf("FILE_TEXT") :
                CubaIcon.valueOf("FILE");
    }

    @Install(to = "jobCandidateCandidateCvTable.iconITPearlsCVFile", subject = "columnGenerator")
    private Icons.Icon jobCandidateCandidateCvTableiconITPearlsCVFileColumnGenerator
            (DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        return event.getItem().getLinkItPearlsCV() != null ?
                CubaIcon.valueOf("FILE_TEXT") :
                CubaIcon.valueOf("FILE");
    }

    @Install(to = "jobCandidateIteractionListTable.iteractionType", subject = "descriptionProvider")
    private String jobCandidateIteractionListTableIteractionTypeDescriptionProvider(IteractionList iteractionList) {
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
    }

    @Subscribe
    public void onBeforeShow2(BeforeShowEvent event) {
        setCandidateInTables();
        trimTelegramName();

        setLinkButtonEmail();
        setLinkButtonTelegrem();
        setLinkButtonTelegremGroup();
        setLinkButtonSkype();

        addFirstNameSuggestField();
        addSecondNameSuggestField();
        addMiddleNameSuggestField();

        lastIteraction = getLastIteraction();
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

    private void setCandidateInTables() {
//        jobCandidateIteractionListDataGridDl.setParameter("candidate", getEditedEntity());
//        jobCandidateIteractionListDataGridDl.load();
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

//    @Install(to = "jobCandidateSkillListTable.skillName", subject = "descriptionProvider")
//    private String jobCandidateSkillListTableSkillNameDescriptionProvider(SkillTree skillTree) {
//        return skillTree.getComment();
//    }

    public void copyCVJobCandidate() {
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
//                                    addIteractionJobCandidate();
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

    @Subscribe(id = "jobCandidateCandidateCvsDc", target = Target.DATA_CONTAINER)
    public void onJobCandidateCandidateCvsDcItemChange(InstanceContainer.ItemChangeEvent<CandidateCV> event) {
        scanContactsFromCVs();
    }

    public void checkSkillFromJD() {
        List<SkillTree> skillTrees = rescanResume();
        String inputText = null;

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

    public List<SkillTree> rescanResume() {
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

        return null;
    }

    @Subscribe("jobCandidateIteractionListTable")
    public void onJobCandidateIteractionListTableEditorClose(DataGrid.EditorCloseEvent event) {
        setRatingLabel(getEditedEntity());
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

//            candidateRatingLabel.setValue(String.valueOf(avgRatiog.setScale(1)));
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

    @Install(to = "jobCandidateIteractionListTable.rating", subject = "columnGenerator")
    private String jobCandidateIteractionListTableRatingColumnGenerator(DataGrid.ColumnGeneratorEvent<IteractionList> event) {
        return event.getItem().getRating() != null ? starsAndOtherService.setStars(event.getItem().getRating() + 1) : "";
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

                newCompany = parseCVService.parseCompany(candidateCV.getTextCV());

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
        Company oldCompany = currentCompanyField.getValue();

        Boolean flag = false;

        if (newEmail != null) {
            if (oldEmail == null && newEmail != null) {
                messageEmail = "Добавить адрес электронной почты в карточку "
                        + newEmail + "? ";

                flag = true;

            } else {
                if (!StringUtils.equals(newEmail, oldEmail)) {
                    messageEmail = "Адрес электронной почты старый "
                            + emailField.getValue()
                            + " новый "
                            + newEmail + " ";

                    flag = true;
                }
            }
        }

        if (newPhone != null) {
            if (oldPhone == null && newPhone != null) {
                messagePhone = "Добавить телефон в карточку "
                        + newPhoneNew + "? ";

                flag = true;
            } else {
                if (!StringUtils.equals(newPhone, oldPhone)) {
                    if (!newPhoneNew.equals(oldPhone)) {
                        messagePhone = "Телефон старый "
                                + phoneField.getValue()
                                + " новый "
                                + newPhoneNew + " ";

                        flag = true;
                    }
                }
            }
        }

        if (newCompany != null) {
            if (oldCompany == null && newCompany != null) {
                messageCompany = "Добавить текущую компанию в карточку "
                        + newCompany.getComanyName() + "? ";

                flag = true;
            } else {
                if (!newCompany.equals(oldCompany)) {
                    messageCompany = "Компания была "
                            + currentCompanyField.getValue().getComanyName()
                            + ", в резюме отмечена "
                            + newCompany.getComanyName()
                            + " ";

                    flag = true;
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

                                flag = true;

                                if (hostCandidateFromCV != null) {
                                    String messageSN = "";

                                    if (socialOld != null) {
                                        if (!sFromCV.equals(socialOld)) {
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
        // -- Social

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

            if (newCompany != null) {
                if (!Objects.equals(newCompany, oldCompany)) {
                    dialog.withParameter(InputParameter.booleanParameter("newCompany")
                            .withCaption(messageCompany).withRequired(true));
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
                    Boolean newCompanyFlag = closeEvent.getValue("newCompany");

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

                    if (newCompanyFlag != null) {
                        if (newCompanyFlag) {
                            currentCompanyField.setValue(newCompany);
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

    @Install(to = "jobCandidateCandidateCvTable.candidateITPearlsCVColumn", subject = "columnGenerator")
    private Component jobCandidateCandidateCvTableCandidateITPearlsCVColumnColumnGenerator
            (DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        Link link = uiComponents.create(Link.NAME);

        if (event.getItem().getLinkItPearlsCV() != null) {
            link.setUrl(event.getItem().getLinkItPearlsCV());
            link.setCaption("CV IT Pearls");
            link.setTarget("_blank");
            link.setWidthAuto();
            link.setVisible(true);
        } else {
            link.setVisible(false);
        }

        return link;
    }

    @Install(to = "jobCandidateCandidateCvTable.candidateOriginalCVColumn", subject = "columnGenerator")
    private Component jobCandidateCandidateCvTableCandidateOriginalCVColumnColumnGenerator
            (DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
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
    }

    @Install(to = "jobCandidateIteractionListTable.commentColumn", subject = "descriptionProvider")
    private String jobCandidateIteractionListTableCommentColumnDescriptionProvider(IteractionList iteractionList) {
        return iteractionList.getComment() != null && !iteractionList.getComment().equals("") ? Jsoup.parse(iteractionList.getComment()).text() : null;
    }

    @Install(to = "jobCandidateIteractionListTable.commentColumn", subject = "columnGenerator")
    private Icons.Icon jobCandidateIteractionListTableCommentColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<IteractionList> event) {
        return event.getItem().getComment() != null && !event.getItem().getComment().equals("") ? CubaIcon.PLUS_CIRCLE : CubaIcon.MINUS_CIRCLE;
    }

    @Install(to = "jobCandidateIteractionListTable.commentColumn", subject = "styleProvider")
    private String jobCandidateIteractionListTableCommentColumnStyleProvider(IteractionList iteractionList) {
        return iteractionList.getComment() != null && !iteractionList.getComment().equals("") ? "pic-center-large-green" : "pic-center-large-red";
    }


    public void openPositionDescription() {
        QuickViewOpenPositionDescription quickViewOpenPositionDescription = screens.create(QuickViewOpenPositionDescription.class);
        quickViewOpenPositionDescription.setJobDescription(jobCandidateIteractionListTable.getSingleSelected() != null ?
                jobCandidateIteractionListTable.getSingleSelected().getVacancy().getComment() : "");

        if(jobCandidateIteractionListTable.getSingleSelected().getVacancy().getProjectName().getProjectDescription() != null) {
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

        if(jobCandidateIteractionListTable.getSingleSelected()
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

        if(jobCandidateIteractionListTable.getSingleSelected()
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
}