package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.core.PdfParserService;
import com.company.itpearls.entity.*;
import com.company.itpearls.web.screens.openposition.SelectCitiesLocation;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.WebBrowserTools;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.*;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import javax.inject.Named;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
    private LookupPickerField<Specialisation> jobCandidateSpecialisationField;
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
    private CollectionLoader<IteractionList> jobCandidateIteractionListDataGridDl;
    @Inject
    private DataGrid<IteractionList> jobCandidateIteractionListTable;
    @Inject
    private CollectionContainer<IteractionList> jobCandidateIteractionListDataGridDc;
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
    private CollectionPropertyContainer<IteractionList> jobCandidateIteractionDc;
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
    private Screens screens;
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
                SocialNetworkURLs socialNetworkURLs = metadata.create(SocialNetworkURLs.class);

                socialNetworkURLs.setSocialNetworkURL(s);
                socialNetworkURLs.setNetworkName(s.getSocialNetwork());
                socialNetworkURLs.setJobCandidate(getEditedEntity());

                jobCandidateSocialNetworksDc.getMutableItems().add(socialNetworkURLs);
                sn.add(socialNetworkURLs);
            }

            DataContext dc = socialNetworkURLsesDl.getDataContext();

            dc.setParent(dataContext);

            dataContext.merge(sn);
        }

        enableDisableContacts();

        setLabelTitle();
        setPositionsLabel();
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

        for (SocialNetworkURLs a : jobCandidateSocialNetworksDc.getItems()) {
            if (a.getNetworkURLS() != null) {
                isEmptySN = true;
                break;
            }
        }

        return ((emailField.getValue() == null) &&
                (skypeNameField.getValue() == null) &&
                (telegramNameField.getValue() == null) &&
                (wiberNameField.getValue() == null) &&
                (whatsupNameField.getValue() == null) &&
                (phoneField.getValue() == null)) && !isEmptySN;
    }

    @Subscribe
    public void onAfterCommitChanges(AfterCommitChangesEvent event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            CommitContext commitContext = new CommitContext(getEditedEntity());

            for (SocialNetworkURLs s : jobCandidateSocialNetworksDc.getItems()) {
                commitContext.addInstanceToCommit(s);
            }

            dataManager.commit(commitContext);
        }
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
        String retStr = "Ответственный за проект: ";

        if (iteractionList.getVacancy() != null) {
            if (iteractionList.getVacancy().getProjectName() != null) {
                if (iteractionList.getVacancy().getProjectName().getProjectOwner() != null) {
                    if (iteractionList.getVacancy().getProjectName().getProjectOwner().getFirstName() != null) {
                        retStr = retStr + iteractionList.getVacancy().getProjectName().getProjectOwner().getFirstName();

                        if (iteractionList.getVacancy().getProjectName().getProjectOwner().getSecondName() != null) {
                            retStr = retStr + " " + iteractionList.getVacancy().getProjectName().getProjectOwner().getSecondName();
                        }
                    }
                }
            }
        }

        return Jsoup.parse(retStr).text();
    }

    private Boolean checkSubscibe(LayoutClickNotifier.LayoutClickEvent event) {
        Integer countSubscrine = dataManager
                .loadValue("select count(e.reacrutier) from itpearls_RecrutiesTasks e " +
                        "where e.reacrutier = :recrutier and " +
                        "e.openPosition = :openPosition and " +
                        ":nowDate between e.startDate and e.endDate", Integer.class)
//                .parameter( "recrutier", recrutiesTasksFieldUser.getValue() )
                .parameter("recrutier", userSession.getUser())
                .parameter("openPosition", getEditedEntity().getOpenPosition())
//                .parameter( "openPosition", openPositionField.getValue() )
                .parameter("nowDate", new Date())
                .one();

        // если нет соответствия, значит нет еще подписки, значит можно подписаться,
        // если уже есть, то не надо подписываться
        return countSubscrine > 0 ? true : false;
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

    @Subscribe("socialNetworkTable")
    public void onSocialNetworkTableSelection(Table.SelectionEvent<SocialNetworkURLs> event) {
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

    protected void enableDisableContacts() {
        // ХОТЯ БЫ ОДИН КОНТАКТ
        if (isRequiredAddresField()) {
            skypeNameField.setRequired(true);
            phoneField.setRequired(true);
            emailField.setRequired(true);
            telegramNameField.setRequired(true);
            whatsupNameField.setRequired(true);
            wiberNameField.setRequired(true);
        } else {
            skypeNameField.setRequired(false);
            phoneField.setRequired(false);
            emailField.setRequired(false);
            telegramNameField.setRequired(false);
            whatsupNameField.setRequired(false);
            wiberNameField.setRequired(false);
        }
    }

    // загрузить таблицу взаимодействий
    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {

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
        checkNotUsePosition();
    }

    private void checkNotUsePosition() {
        if(personPositionField.getValue() != null) {
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

        if(jobCandidate != null && PersistenceHelper.isNew(getEditedEntity())) {
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
        if(PersistenceHelper.isNew(getEditedEntity())) {
            // добавить сюда Iteraction "Новый кандиат"
        }
    }

    private JobCandidate checkDublicateCandidate() {
        String queryStr = "select e from itpearls_JobCandidate e " +
                            "where e.firstName = :firstName and " +
                                    "e.secondName = :secondName and " +
                                    "e.cityOfResidence = :cityOfResidence and " +
                                    "e.personPosition = :personPosition";
        List <JobCandidate> jobCandidates = dataManager.load(JobCandidate.class)
                .query(queryStr)
                .parameter("firstName", firstNameField.getValue())
                .parameter("secondName", secondNameField.getValue())
                .parameter("cityOfResidence", jobCityCandidateField.getValue())
                .parameter("personPosition", personPositionField.getValue())
                .view("jobCandidate-view")
                .list();

        if(jobCandidates.size() == 0) {
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

        if (jobCandidateSpecialisationField != null)   // 7
            qPercent = ++qPercent;

        if (jobCityCandidateField.getValue() != null)  // 8
            qPercent = ++qPercent;

        if (personPositionField.getValue() != null)    // 9
            qPercent = ++qPercent;

        if (phoneField.getValue() != null)             // 10
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

    @Subscribe
    public void onInit(InitEvent event) {
        addIconColumn();
        setCopyCVButton();
    }

    private void setCopyCVButton() {
        copyCVButton.setEnabled(false);
        jobCandidateCandidateCvTable.addItemClickListener(e -> {
                if(jobCandidateCandidateCvTable.getSingleSelected() == null) {
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

    private Boolean needDublicateDialog() {
        if (ifCandidateIsExist()) {
            Dialogs.OptionDialogBuilder d = dialogs.createOptionDialog()
                    .withCaption("WARNING")
                    .withMessage("Кандидат <b>" + getEditedEntity().getFullName() +
                            "</b> есть в базе!\n Вы точно хотите создать еще одного?")
                    .withActions(
                            new DialogAction(DialogAction.Type.YES, DialogAction.Status.PRIMARY)
                                    .withHandler(e -> {
                                        close(StandardOutcome.COMMIT);
                                        returnE.set(true);
                                    }),
                            new DialogAction(DialogAction.Type.NO)
                                    .withHandler(f -> {
                                        returnE.set(false);
                                    })
                    );

            d.withContentMode(ContentMode.HTML);
            d.show();
        } else
            close(StandardOutcome.COMMIT);

        return returnE.get();
    }

    public void onBtnOkAndCheck() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            needDublicateDialog();
            return;
        } else {
            close(StandardOutcome.COMMIT);
            return;
        }
    }

    public void addIteractionJobCandidate() {
        screenBuilders.editor(IteractionList.class, this)
                .newEntity()
//                .withOpenMode(OpenMode.DIALOG)
                .withOptions(new JobCandidateScreenOptions(false))
                .withParentDataContext(dataContext)
                .withContainer(jobCandidateIteractionListDataGridDc)
                .withInitializer(candidate -> {
                    candidate.setCandidate(getEditedEntity());
                })
                .build()
                .show();

        jobCandidateIteractionListDataGridDl.load();
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


    public void copyIteractionJobCandidate() {
        if (jobCandidateIteractionListTable.getSingleSelected() == null) {
            String QUERY_GET_LAST_ITERACTION = "select e " +
                    "from itpearls_IteractionList e " +
                    "where e.candidate = :candidate and " +
                    "e.numberIteraction = (select max(f.numberIteraction) from itpearls_IteractionList f where f.candidate = :candidate)";

            IteractionList lastIteraction = null;

            try {
                lastIteraction = dataManager.load(IteractionList.class)
                        .query(QUERY_GET_LAST_ITERACTION)
                        .parameter("candidate", getEditedEntity())
                        .view("iteractionList-view")
                        .one();
            } catch (IllegalStateException e) {
                lastIteraction = null;
            }

            if (lastIteraction != null) {
                IteractionList finalLastIteraction = lastIteraction;

                screenBuilders.editor(IteractionList.class, this)
//                        .withOpenMode(OpenMode.DIALOG)
                        .withParentDataContext(dataContext)
                        .withContainer(jobCandidateIteractionListDataGridDc)
                        .withInitializer(candidate -> {
                            candidate.setCandidate(getEditedEntity());
                            candidate.setVacancy(finalLastIteraction.getVacancy());
                            candidate.setNumberIteraction(numBerIteractionForNewEntity());
                        })
                        .newEntity()
                        .build()
                        .show();
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
//                    .withOpenMode(OpenMode.DIALOG)
                    .withParentDataContext(dataContext)
                    .withContainer(jobCandidateIteractionListDataGridDc)
                    .withInitializer(candidate -> {
                        candidate.setCandidate(jobCandidateIteractionListTable.getSingleSelected().getCandidate());
                        candidate.setVacancy(jobCandidateIteractionListTable.getSingleSelected().getVacancy());
                        candidate.setNumberIteraction(numBerIteractionForNewEntity());
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
        return item.getIteractionType().getPic();
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
                CubaIcon.valueOf("PLUS_CIRCLE") :
                CubaIcon.valueOf("MINUS_CIRCLE");
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
                CubaIcon.valueOf("PLUS_CIRCLE") :
                CubaIcon.valueOf("MINUS_CIRCLE");
    }

    @Install(to = "jobCandidateCandidateCvTable.iconITPearlsCVFile", subject = "columnGenerator")
    private Icons.Icon jobCandidateCandidateCvTableiconITPearlsCVFileColumnGenerator
            (DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        return event.getItem().getLinkItPearlsCV() != null ?
                CubaIcon.valueOf("PLUS_CIRCLE") :
                CubaIcon.valueOf("MINUS_CIRCLE");
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
        setLinkButtonSkype();

        addFirstNameSuggestField();
        addSecondNameSuggestField();
        addMiddleNameSuggestField();
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
        jobCandidateIteractionListDataGridDl.setParameter("candidate", getEditedEntity());
        jobCandidateIteractionListDataGridDl.load();
    }

    public void addPositionList() {
        SelectPersonPositions selectPersonPositions = screens.create(SelectPersonPositions.class);

        List<Position> setPos = new ArrayList<>();

        if(getEditedEntity().getPositionList() != null) {
            for (Position p : getEditedEntity().getPositionList()) {
                setPos.add(p);
            }
        }

        selectPersonPositions.addAfterShowListener(e -> {
            selectPersonPositions.setPositionsList(setPos);
        });

        selectPersonPositions.addAfterCloseListener( e -> {
            this.getEditedEntity().setPositionList(selectPersonPositions.getPositionsList());
            setPositionsLabel();
        });
        selectPersonPositions.show();
    }

    private void setPositionsLabel() {
        String outStr = "";
        String description = "";

        if(getEditedEntity().getPositionList() != null) {
            for (Position s : getEditedEntity().getPositionList()) {
                if (!outStr.equals("")) {
                    outStr = outStr + ",";
                    description = description + "\n";
                }

                outStr = outStr + s.getPositionRuName();
                description = description + s.getPositionRuName();
            }

        }
        if(!outStr.equals("")) {
            positionsLabel.setValue(outStr);
            positionsLabel.setDescription(description);
        }
    }

    @Install(to = "jobCandidateSkillListTable.skillName", subject = "descriptionProvider")
    private String jobCandidateSkillListTableSkillNameDescriptionProvider(SkillTree skillTree) {
        return skillTree.getComment();
    }

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
                        .withParentDataContext(dataContext)
                        .withContainer(jobCandidateCandidateCvsDc)
                        .withInitializer(candidate -> {
                            candidate.setCandidate(getEditedEntity());
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
            screenBuilders.editor(CandidateCV.class, this)
                    .withParentDataContext(dataContext)
                    .withContainer(jobCandidateCandidateCvsDc)
                    .withInitializer(candidate -> {
                        candidate.setCandidate(jobCandidateCandidateCvTable.getSingleSelected().getCandidate());
                        candidate.setTextCV(jobCandidateCandidateCvTable.getSingleSelected().getTextCV());
                        candidate.setLetter(jobCandidateCandidateCvTable.getSingleSelected().getLetter());
                        candidate.setResumePosition(jobCandidateCandidateCvTable.getSingleSelected().getResumePosition());
                        candidate.setLinkOriginalCv(jobCandidateCandidateCvTable.getSingleSelected().getLinkOriginalCv());
                        candidate.setLinkItPearlsCV(jobCandidateCandidateCvTable.getSingleSelected().getLinkItPearlsCV());
                        candidate.setLintToCloudFile(jobCandidateCandidateCvTable.getSingleSelected().getLintToCloudFile());
                        candidate.setOwner(userSession.getUser());
                    })
                    .newEntity()
                    .build()
                    .show();
        }
    }
}