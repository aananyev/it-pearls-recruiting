package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.entity.*;
import com.company.itpearls.web.screens.iteractionlist.IteractionListEdit;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.actions.picker.LookupAction;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.components.data.ValueSource;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.*;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;

import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
    private TextField<String> firstNameField;
    @Inject
    private LookupPickerField<Specialisation> jobCandidateSpecialisationField;
    @Inject
    private TextField<String> middleNameField;
    @Inject
    private TextField<String> secondNameField;
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
    @Named("jobCityCandidateField.lookup")
    private LookupAction jobCityCandidateFieldLookup;
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
    private InstanceLoader<JobCandidate> jobCandidateDl;
    @Inject
    private CollectionLoader<IteractionList> jobCandidateIteractionListDataGridDl;
    @Inject
    private InstanceContainer<JobCandidate> jobCandidateDc;
    @Inject
    private DataGrid<IteractionList> jobCandidateIteractionListTable;
    @Inject
    private CollectionContainer<IteractionList> jobCandidateIteractionListDataGridDc;
    @Named("tabSheetSocialNetworks.jobCandidateCard")
    private VBoxLayout jobCandidateCard;
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
    private VBoxLayout dropZone;
    @Inject
    private HBoxLayout cardBox;
    @Inject
    private GroupBoxLayout cardTextBox;

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
        setImageListener();

    }

    private void setImageListener() {
        candidatePic.addClickListener(clickEvent -> {
            if(clickEvent.isDoubleClick()) {

            }
        });
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
        dialogs.createOptionDialog()
                .withCaption("Warning")
                .withMessage("Подписатся на изменение вакансии?")
                .withActions(
                        new DialogAction(DialogAction.Type.YES,
                                Action.Status.PRIMARY).withHandler(e -> {
                            this.commitChanges();
                        }),
                        new DialogAction(DialogAction.Type.NO)
                )
                .show();
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
            labelQualityPercent.setValue("| Процент заполнения карточки: " + qualityPercent.toString()
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


    protected void enableDisableContacts() {
        // ХОТЯ БЫ ОДИН КОНТАКТ
        if (isRequiredAddresField()) {
            skypeNameField.setRequired(true);
            phoneField.setRequired(true);
            emailField.setRequired(true);
        } else {
            skypeNameField.setRequired(false);
            phoneField.setRequired(false);
            emailField.setRequired(false);
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
                labelCV.setValue("| Резюме: НЕТ");
            } else {
                labelCV.setValue("| Резюме: ДА");
            }
        }

        // обнулить статус для вновь создаваемного кандидата
        if (PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setStatus(0);
        }
        enableDisableContacts();
    }

    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        secondNameField.setValue(replaceE_E(secondNameField.getValue()));
        firstNameField.setValue(replaceE_E(firstNameField.getValue()));

        if (middleNameField.getValue() != null)
            middleNameField.setValue(replaceE_E(middleNameField.getValue()));
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
                .withOpenMode(OpenMode.DIALOG)
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
                        .withOpenMode(OpenMode.DIALOG)
                        .withParentDataContext(dataContext)
                        .withContainer(jobCandidateIteractionListDataGridDc)
                        .withInitializer(candidate -> {
                            candidate.setCandidate(getEditedEntity());
                            candidate.setVacancy(finalLastIteraction.getVacancy());
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
                    .withOpenMode(OpenMode.DIALOG)
                    .withParentDataContext(dataContext)
                    .withContainer(jobCandidateIteractionListDataGridDc)
                    .withInitializer(candidate -> {
                        candidate.setCandidate(jobCandidateIteractionListTable.getSingleSelected().getCandidate());
                        candidate.setVacancy(jobCandidateIteractionListTable.getSingleSelected().getVacancy());
                    })
                    .newEntity()
                    .build()
                    .show();
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
        FileDescriptorResource fileDescriptorResource = candidatePic.createResource(FileDescriptorResource.class)
                .setFileDescriptor(fileImageFaceUpload.getFileDescriptor());

        candidatePic.setSource(fileDescriptorResource);
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
    }

    private void setCandidateInTables() {
        jobCandidateIteractionListDataGridDl.setParameter("candidate", getEditedEntity());
        jobCandidateIteractionListDataGridDl.load();
    }
}