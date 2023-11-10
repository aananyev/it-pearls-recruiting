package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.core.*;
import com.company.itpearls.entity.*;
import com.company.itpearls.service.GetRoleService;
import com.company.itpearls.service.OpenPositionNewsService;
import com.company.itpearls.web.screens.SelectedCloseAction;
import com.company.itpearls.web.screens.candidatecv.CandidateCVEdit;
import com.company.itpearls.web.screens.candidatecv.CandidateCVSimpleBrowse;
import com.company.itpearls.web.screens.candidatecv.SelectRenderedImagesFromList;
import com.company.itpearls.web.screens.fragments.OnlyTextPersonPosition;
import com.company.itpearls.web.screens.fragments.OnlyTextPersonPositionLoadPdf;
import com.company.itpearls.web.screens.fragments.Onlytext;
import com.company.itpearls.web.screens.fragments.Skillsbar;
import com.company.itpearls.web.screens.internalemailertemplate.InternalEmailerTemplateEdit;
import com.company.itpearls.web.screens.iteractionlist.IteractionListEdit;
import com.company.itpearls.web.screens.iteractionlist.iteractionlistbrowse.IteractionListSimpleBrowse;
import com.company.itpearls.web.screens.jobcandidate.jobcandidatecomments.JobCandidateComment;
import com.company.itpearls.web.screens.personelreserve.PersonelReserveEdit;
import com.company.itpearls.web.screens.signicons.SignIconsBrowse;
import com.haulmont.cuba.core.app.FileStorageService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.*;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import com.vaadin.server.Page;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import java.awt.image.RenderedImage;
import java.io.*;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@UiController("itpearls_JobCandidate.browse")
@UiDescriptor("job-candidate-browse.xml")
@LookupComponent("jobCandidatesTable")
@LoadDataBeforeShow
public class JobCandidateBrowse extends StandardLookup<JobCandidate> {

    @Inject
    private CheckBox checkBoxShowOnlyMy;
    @Inject
    private CollectionLoader<JobCandidate> jobCandidatesDl;
    @Inject
    private UserSession userSession;
    @Inject
    private CheckBox checkBoxOnWork;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private GetRoleService getRoleService;
    @Inject
    private Button buttonExcel;
    @Inject
    private DataGrid<JobCandidate> jobCandidatesTable;
    @Inject
    private DataManager dataManager;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private Fragments fragments;
    @Inject
    private DataComponents dataComponents;
    @Inject
    private Screens screens;
    @Inject
    private Notifications notifications;
    @Inject
    private Dialogs dialogs;
    @Inject
    private LookupField ratingFieldNotLower;
    @Inject
    private CheckBox withCVCheckBox;
    @Inject
    private InteractionService interactionService;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private Button sendEmailButton;
    @Inject
    private ParseCVService parseCVService;
    @Inject
    private DataContext dataContext;
    @Inject
    private Metadata metadata;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private StarsAndOtherService starsAndOtherService;
    @Inject
    private FileStorageService fileStorageService;

    private static final String QUERY_DEFAULT_OPEN_POSITION = "select e from itpearls_OpenPosition e where e.vacansyName like 'Default'";
    private static final String QUERY_RESUME = "select e from itpearls_CandidateCV e where e.candidate = :candidate";
    private static final String QUERY_GET_OTHER_SOCIAL_NETWORK = "select e from itpearls_SocialNetworkType e where e.socialNetwork = :other";
    private static final String QUERY_GET_LASTRECRUTIER = "select e from itpearls_IteractionList e where e.candidate = :candidate order by e.numberIteraction desc";

    private CollectionContainer<IteractionList> iteractionListDc;
    private CollectionLoader<IteractionList> iteractionListDl;
    private CollectionContainer<CandidateCV> candidateCVDc;
    private CollectionLoader<CandidateCV> candidateCVDl;
    private List<IteractionList> iteractionList = new ArrayList<>();
    private CandidateCVEdit candidateCVEdit;
    private OnlyTextPersonPosition screenOnlytext;
    private JobCandidate jobCandidatesTableDetailsGeneratorOpened = null;
    private List<Employee> employees;

    private static final String EXTENSION_PDF = "pdf";
    private static final String EXTENSION_DOC = "doc";
    private static final String EXTENSION_DOCX = "docx";
    private static final String[] breakLine = {"<br>", "<br/>", "<br />", "<p>", "</p>", "</div>"};

    private static final String separatorChar = "⎯";
    private static final String separator = separatorChar.repeat(22);


    @Inject
    private FileUploadingAPI fileUploadingAPI;
    @Inject
    private FileLoader fileLoader;
    private OnlyTextPersonPositionLoadPdf screenOnlytextLoadFromPdf;
    @Inject
    private PopupButton actionsWithCandidateButton;
    @Inject
    private OpenPositionNewsService openPositionNewsService;
    @Inject
    private CollectionLoader<SignIcons> signIconsDl;
    @Inject
    private CollectionContainer<SignIcons> signIconsDc;
    @Inject
    private StrSimpleService strSimpleService;
    @Inject
    private PopupButton signFilterButton;
    @Inject
    private WebBrowserTools webBrowserTools;

    @Subscribe
    public void onInit1(InitEvent event) {
        initActionsWithCandidateButton();
    }

    private void initActionsWithCandidateButton() {
        initActionButton(actionsWithCandidateButton);
    }

    private PersonelReserve currentPersonelReserve = null;

    private void initActionButton(PopupButton actionsWithCandidateButton) {
//        final String separatorChar = "⎯";
//        String separator = separatorChar.repeat(22);

        actionsWithCandidateButton.addAction(new BaseAction("addPersonalReserve")
                .withIcon(CubaIcon.ADD_TO_SET_ACTION.source())
                .withCaption(messageBundle.getMessage("msgAddPersonalReserve"))
                .withHandler(actionPerformedEvent -> {
                    screenBuilders.editor(PersonelReserve.class, this)
                            .withScreenClass(PersonelReserveEdit.class)
                            .newEntity()
                            .withInitializer(event -> {
                                event.setRecruter((ExtUser) userSession.getUser());
                                event.setJobCandidate(jobCandidatesTable.getSingleSelected());
                                event.setPersonPosition(jobCandidatesTable.getSingleSelected().getPersonPosition());
                                event.setRemovedFromReserve(false);
                                event.setInProcess(true);
                            })
                            .withAfterCloseListener(afterCloseEvent -> {
                                if (afterCloseEvent.closedWith(StandardOutcome.COMMIT)) {
                                    currentPersonelReserve = afterCloseEvent.getScreen().getEditedEntity();

                                    if (currentPersonelReserve.getOpenPosition() != null)
                                        addPersonalReserveInteraction(currentPersonelReserve.getJobCandidate(), currentPersonelReserve.getOpenPosition());
                                    else
                                        addPersonalReserveInteraction(currentPersonelReserve.getJobCandidate(), getDefaultOpenPosition());
                                }
                            })
                            .build()
                            .show();
                    jobCandidatesTable.scrollTo(jobCandidatesTable.getSingleSelected());
                }));

        actionsWithCandidateButton.addAction(new BaseAction("addPersonalReserveWithoutConfirm")
                .withIcon(CubaIcon.ADD_TO_SET_ACTION.source())
                .withCaption(messageBundle.getMessage("msgAddPersonalReserveWithoutConfirm"))
                .withHandler(actionPerformedEvent -> {
                    if (jobCandidatesTable.getSingleSelected() != null) {
                        putCandidatesToPersonelReserve(jobCandidatesTable.getSingleSelected(), getDefaultOpenPosition());

                        jobCandidatesTable.scrollTo(jobCandidatesTable.getSingleSelected());
                    }
                }));

        actionsWithCandidateButton.addAction(new BaseAction("separator1Action")
                .withCaption(separator));

        actionsWithCandidateButton.addAction(new BaseAction("sendEmailAction")
                .withIcon(CubaIcon.ENVELOPE.source())
                .withCaption(messageBundle.getMessage("msgSendEmail"))
                .withHandler(actionPerformedAction -> {
                    screenBuilders.editor(InternalEmailerTemplate.class, this)
                            .newEntity()
                            .withInitializer(event -> {
                                event.setToEmail(jobCandidatesTable.getSingleSelected());
                            })
                            .build()
                            .show();

                    jobCandidatesTable.scrollTo(jobCandidatesTable.getSingleSelected());
                }));

        actionsWithCandidateButton.addAction(new BaseAction("addCommentAction")
                .withIcon(CubaIcon.COMMENTING.source())
                .withCaption(messageBundle.getMessage("msgComment"))
                .withHandler(actionPerformedEvent -> {

                }));

        actionsWithCandidateButton.addAction(new BaseAction("viewCommentAction")
                .withIcon(CubaIcon.COMMENTS.source())
                .withCaption(messageBundle.getMessage("msgViewComments"))
                .withHandler(actionPerformedAction -> {
                    jobCandidatesTable.scrollTo(jobCandidatesTable.getSingleSelected());
                    JobCandidateComment screen = screens.create(JobCandidateComment.class);
//                    jobCandidatesTable.setSelected(((JobCandidate)actionPerformedAction.getSource()));
                    screen.setJobCandidate(jobCandidatesTable.getSingleSelected());
                    screen.show();
                }));

        actionsWithCandidateButton.addAction(new BaseAction("separator2Action")
                .withCaption(separator));

        for (SignIcons icons : signIconsDc.getItems()) {
            actionsWithCandidateButton.addAction(new BaseAction(
                    strSimpleService.deleteExtraCharacters(icons.getTitleEnd() + "Action"))
                    .withIcon(icons.getIconName())
                    .withCaption(icons.getTitleRu())
                    .withDescription(icons.getTitleDescription())
                    .withHandler(actionPerformedAction -> {
                        setSignIcons(icons, jobCandidatesTable.getSingleSelected());
                    }));
        }

        actionsWithCandidateButton.addAction(new BaseAction("removeSignAction")
                .withIcon(CubaIcon.REMOVE_ACTION.source())
                .withCaption(messageBundle.getMessage("msgRemoveSignAction"))
                .withDescription(messageBundle.getMessage("msgRemoveSignActionDesc"))
                .withHandler(actionPerformedAction -> {
                    removeSignAction(jobCandidatesTable.getSingleSelected());
                }));

        actionsWithCandidateButton.addAction(new BaseAction("separator3Action")
                .withCaption(separator));

        actionsWithCandidateButton.addAction(new BaseAction("editSignIconsAction")
                .withCaption(messageBundle.getMessage("msgEditSignIconsAction"))
                .withDescription("msgEditSignIconsActionDesc")
                .withIcon(CubaIcon.FONTICONS.source())
                .withHandler(actionPerformedAction -> {
                    SignIconsBrowse screen = (SignIconsBrowse) screenBuilders.lookup(SignIcons.class, this)
                            .withOpenMode(OpenMode.DIALOG)
                            .build();
                    screen.setParentJobCandidateTable(jobCandidatesTable);

                    screen.show();
                }));

        actionsWithCandidateButton.getAction("addCommentAction").setEnabled(false);
        actionsWithCandidateButton.getAction("addCommentAction").setVisible(false);
        actionsWithCandidateButton.getAction("viewCommentAction").setEnabled(true);
    }

    @Subscribe(id = "signIconsDc", target = Target.DATA_CONTAINER)
    public void onSignIconsDcCollectionChange(CollectionContainer.CollectionChangeEvent<SignIcons> event) {
        List<JobCandidateSignIcon> jobCandidateSignIcons = dataManager.load(JobCandidateSignIcon.class)
                .query(QUERY_GET_JOB_CANDIDATE_SIGN_ICONS)
                .parameter("jobCandidate", jobCandidatesTable.getSingleSelected())
                .view("jobCandidateSignIcon-view")
                .cacheable(true)
                .list();

        if (jobCandidateSignIcons.size() > 0) {
            actionsWithCandidateButton.getAction("removeSignAction").setEnabled(true);
        } else {
            actionsWithCandidateButton.getAction("removeSignAction").setEnabled(false);
        }
    }

    private void removeSignAction(JobCandidate jobCandidate) {
        List<JobCandidateSignIcon> jobCandidateSignIcons = dataManager.load(JobCandidateSignIcon.class)
                .query(QUERY_GET_JOB_CANDIDATE_SIGN_ICONS)
                .parameter("jobCandidate", jobCandidate)
                .view("jobCandidateSignIcon-view")
                .cacheable(true)
                .list();

        if (jobCandidateSignIcons.size() > 0) {
            for (JobCandidateSignIcon jcsi : jobCandidateSignIcons) {
                dataManager.remove(jcsi);
            }

            actionsWithCandidateButton.getAction("removeSignAction").setEnabled(false);
        } else {
            actionsWithCandidateButton.getAction("removeSignAction").setEnabled(true);
        }

        jobCandidatesTable.repaint();
        jobCandidatesTable.setSelected(jobCandidate);
        jobCandidatesTable.scrollTo(jobCandidate);
    }

    private static final String QUERY_GET_JOB_CANDIDATE_SIGN_ICONS =
            "select e from itpearls_JobCandidateSignIcon e where e.jobCandidate = :jobCandidate";

    private void setSignIcons(SignIcons icons, JobCandidate jobCandidate) {
        List<JobCandidateSignIcon> jobCandidateSignIcon;

        jobCandidateSignIcon = dataManager.load(JobCandidateSignIcon.class)
                .query(QUERY_GET_JOB_CANDIDATE_SIGN_ICONS)
                .parameter("jobCandidate", jobCandidate)
                .view("jobCandidateSignIcon-view")
                .cacheable(true)
                .list();

        if (jobCandidateSignIcon.size() == 0) {
            JobCandidateSignIcon jcsi = metadata.create(JobCandidateSignIcon.class);
            jcsi.setJobCandidate(jobCandidate);
            jcsi.setSignIcon(icons);
            jcsi.setUser((ExtUser) userSession.getUser());

            dataManager.commit(jcsi);
        } else {
            jobCandidateSignIcon.get(0).setSignIcon(icons);
            dataManager.commit(jobCandidateSignIcon.get(0));
        }

        jobCandidatesTable.repaint();
        jobCandidatesTable.setSelected(jobCandidate);
        jobCandidatesTable.scrollTo(jobCandidate);
    }

    private static final String QUERY_GET_PERSONEL_RESERVE =
            "select e from itpearls_PersonelReserve e " +
                    "where e.jobCandidate = :jobCandidate " +
                    "and " +
                    "(e.endDate > :currDate or e.endDate is null)";

    private void putCandidatesToPersonelReserve(JobCandidate jobCandidate, OpenPosition openPosition) {

        PersonelReserve personelReserveCheck = null;

        try {
            personelReserveCheck = dataManager.load(PersonelReserve.class)
                    .query(QUERY_GET_PERSONEL_RESERVE)
                    .view("personelReserve-view")
                    .parameter("jobCandidate", jobCandidate)
                    .parameter("currDate", new Date())
                    .cacheable(true)
                    .one();
        } catch (IllegalStateException e) {
            personelReserveCheck = null;
        }

        if (personelReserveCheck == null) {
            addPersonaLReserveMonth(jobCandidate, openPosition);
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
                            ? sdf.format(personelReserveCheck.getEndDate())
                            : messageBundle.getMessage("msgUnlimited")))
                    .withType(Notifications.NotificationType.ERROR)
                    .show();
        }
    }

    private static final String QUERY_GET_DEFAULT_VACANCY =
            "select e from itpearls_OpenPosition e " +
                    "where e.vacansyName like \'Default\'";
    private final String QUERY_GET_SIGN_PERSONAL_RESERVE_PUT_INTERACTION =
            "select e " +
                    "from itpearls_Iteraction e " +
                    "where e.signPersonalReservePut = true";

/*    private void setOpenPositionNewsAutomatedMessage(OpenPosition editedEntity,
                                                     String subject,
                                                     String comment,
                                                     Date date,
                                                     JobCandidate jobCandidate,
                                                     User user,
                                                     Boolean priority) {
        try {
            OpenPositionNews openPositionNews = metadata.create(OpenPositionNews.class);

            openPositionNews.setOpenPosition(editedEntity);
            openPositionNews.setAuthor(user);
            openPositionNews.setDateNews(date);
            openPositionNews.setSubject(subject);
            openPositionNews.setComment(comment);
            openPositionNews.setCandidates(jobCandidate);
            openPositionNews.setPriorityNews(priority != null ? priority : false);

            CommitContext commitContext = new CommitContext();
            commitContext.addInstanceToCommit(openPositionNews);
            dataManager.commit(commitContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
    } */

    private BigDecimal getCountIteraction() {
        IteractionList e = dataManager.load(IteractionList.class)
                .query("select e from itpearls_IteractionList e where e.numberIteraction = " +
                        "(select max(f.numberIteraction) from itpearls_IteractionList f)")
                .view("iteractionList-view")
                .cacheable(true)
                .one();

        return e.getNumberIteraction().add(BigDecimal.ONE);
    }

    private void addPersonalReserveInteraction(JobCandidate jobCandidate,
                                               OpenPosition openPosition) {

        OpenPosition defaultPosition = null;
        Iteraction interactionType = null;
        IteractionList iteractionList = metadata.create(IteractionList.class);

        try {
            defaultPosition = dataManager
                    .loadValue(QUERY_GET_DEFAULT_VACANCY, OpenPosition.class)
                    .one();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        try {
            interactionType = dataManager.load(Iteraction.class)
                    .query(QUERY_GET_SIGN_PERSONAL_RESERVE_PUT_INTERACTION)
                    .cacheable(true)
                    .view("iteraction-view")
                    .one();
        } catch (NoResultException e) {
            e.printStackTrace();
        }

        if (interactionType != null) {
            iteractionList.setCandidate(jobCandidate);
            iteractionList.setDateIteraction(new Date());
            iteractionList.setRating(4);

            if (openPosition != null) {
                iteractionList.setVacancy(openPosition);
                iteractionList.setCurrentOpenClose(openPosition.getOpenClose());
                iteractionList.setCurrentPriority(openPosition.getPriority());
            } else {
                if (defaultPosition != null) {
                    iteractionList.setVacancy(defaultPosition);
                    iteractionList.setCurrentOpenClose(defaultPosition.getOpenClose());
                    iteractionList.setCurrentPriority(defaultPosition.getPriority());
                } else {
                    notifications.create(Notifications.NotificationType.ERROR)
                            .withCaption(messageBundle.getMessage("msg://msgError"))
                            .withDescription(messageBundle.getMessage("msgNotDefaultInteraction"))
                            .withType(Notifications.NotificationType.ERROR)
                            .show();
                }
            }

            iteractionList.setRecrutierName(userSession.getUser().getName());
            iteractionList.setRecrutier((ExtUser) userSession.getUser());
            iteractionList.setNumberIteraction(getCountIteraction());
            iteractionList.setIteractionType(interactionType);

            dataManager.commit(iteractionList);

            if (openPosition != null) {
                openPositionNewsService.setOpenPositionNewsAutomatedMessage(openPosition,
                        iteractionList.getIteractionType().getIterationName(),
                        messageBundle.getMessage("msgJobCandidatePutToPersonalReserve"),
                        iteractionList.getDateIteraction(),
                        jobCandidate,
                        userSession.getUser(),
                        interactionType.getSignPriorityNews());
            } else {
                openPositionNewsService.setOpenPositionNewsAutomatedMessage(defaultPosition,
                        iteractionList.getIteractionType().getIterationName(),
                        messageBundle.getMessage("msgJobCandidatePutToPersonalReserve"),
                        iteractionList.getDateIteraction(),
                        jobCandidate,
                        userSession.getUser(),
                        interactionType.getSignPriorityNews());

            }

        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("msg://msgError"))
                    .withDescription(messageBundle.getMessage("msgNotSignPersonalReserveInteractions"))
                    .withType(Notifications.NotificationType.ERROR)
                    .show();
        }
    }

    private void addPersonaLReserveMonth(JobCandidate jobCandidate, OpenPosition selectedOpenPosition) {
        PersonelReserve personelReserve = metadata.create(PersonelReserve.class);
        // сначала итерацию, а уж потом остальное
        addPersonalReserveInteraction(jobCandidate, selectedOpenPosition);

        personelReserve.setDate(new Date());
        personelReserve.setJobCandidate(jobCandidate);
        personelReserve.setRecruter((ExtUser) userSessionSource.getUserSession().getUser());
        personelReserve.setInProcess(true);

        int noOfDays = 30;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_YEAR, noOfDays);
        Date endDate = calendar.getTime();
        personelReserve.setEndDate(endDate);

        personelReserve.setPersonPosition(jobCandidate.getPersonPosition());

        personelReserve.setOpenPosition(selectedOpenPosition);

        dataManager.commit(personelReserve);
    }

    private OpenPosition getDefaultOpenPosition() {
        OpenPosition openPositionDefault = null;

        try {
            openPositionDefault = dataManager.load(OpenPosition.class)
                    .query(QUERY_DEFAULT_OPEN_POSITION)
                    .view("openPosition-view")
                    .one();
        } catch (NullPointerException e) {
            e.printStackTrace();

            notifications.create(Notifications.NotificationType.WARNING)
                    .withType(Notifications.NotificationType.WARNING)
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withDescription(messageBundle.getMessage("msgNotFindDefaultOpenPosition"))
                    .show();
        }

        return openPositionDefault;
    }

    @Install(to = "jobCandidatesTable.actionsWithCandidate", subject = "columnGenerator")
    private Component jobCandidatesTableActionsWithCandidateColumnGenerator
            (DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();
        retHBox.setHeightFull();

        PopupButton actionPopupButton = uiComponents.create(PopupButton.class);
        actionPopupButton.setShowActionIcons(true);
        actionPopupButton.setIconFromSet(CubaIcon.BARS);
        actionPopupButton.setAlignment(Component.Alignment.MIDDLE_CENTER);
        actionPopupButton.addPopupVisibilityListener(e -> {
            jobCandidatesTable.setSelected(event.getItem());
        });

        initActionButton(actionPopupButton);

        retHBox.add(actionPopupButton);


        return retHBox;
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        jobCandidatesTable.setDescriptionAsHtml(true);
        if (userSession.getUser().getGroup().getName().equals("Стажер")) {
            jobCandidatesDl.setParameter("userName", "%" + userSession.getUser().getLogin() + "%");

            checkBoxShowOnlyMy.setValue(true);
            checkBoxShowOnlyMy.setEditable(false);
        }

        checkBoxOnWork.setValue(false);
        jobCandidatesDl.removeParameter("param1");
        jobCandidatesDl.removeParameter("param3");

        jobCandidatesDl.load();

//        buttonExcel.setVisible(getRoleService.isUserRoles(userSession.getUser(), "Manager"));

        employees = dataManager.load(Employee.class)
                .view("employee-view")
                .list();

        initSignIconsDataContainer();
        initSignFilterPopupButton();
    }

    private void initSignFilterPopupButton() {
//        final String separatorChar = "⎯";
//        String separator = separatorChar.repeat(22);

        for (SignIcons icons : signIconsDc.getItems()) {
            signFilterButton.addAction(new BaseAction(
                    strSimpleService.deleteExtraCharacters(icons.getTitleEnd() + "Action"))
                    .withIcon(icons.getIconName())
                    .withCaption(icons.getTitleRu())
                    .withDescription(icons.getTitleDescription())
                    .withHandler(actionPerformedAction -> {
                        setSignFilter(icons);
                    }));
        }

        signFilterButton.addAction(new BaseAction("separator1Action")
                .withCaption(separator));

        signFilterButton.addAction(new BaseAction(
                strSimpleService.deleteExtraCharacters("removeFilterSignAction"))
                .withIcon(CubaIcon.REMOVE_ACTION.source())
                .withCaption(messageBundle.getMessage("msgRemoveSignIconFilterDesc"))
                .withDescription(messageBundle.getMessage("msgRemoveSignIconFilterDesc"))
                .withHandler(actionPerformedAction -> {
                    removeSignFilterAction();
                }));

        signFilterButton.addAction(new BaseAction("separator3Action")
                .withCaption(separator));

        signFilterButton.addAction(new BaseAction("editSignIconsAction")
                .withCaption(messageBundle.getMessage("msgEditSignIconsAction"))
                .withDescription("msgEditSignIconsActionDesc")
                .withIcon(CubaIcon.FONTICONS.source())
                .withHandler(actionPerformedAction -> {
                    SignIconsBrowse screen = (SignIconsBrowse) screenBuilders.lookup(SignIcons.class, this)
                            .withOpenMode(OpenMode.DIALOG)
                            .build();
                    screen.setParentJobCandidateTable(jobCandidatesTable);

                    screen.show();
                }));
    }

    private void removeSignFilterAction() {
        jobCandidatesDl.removeParameter("signIcon");
        jobCandidatesDl.load();
    }

    private void setSignFilter(SignIcons icons) {
        removeSignFilterAction();
        jobCandidatesDl.setParameter("signIcon", icons);
        jobCandidatesDl.load();
    }

    private void initSignIconsDataContainer() {
        signIconsDl.setParameter("user", (ExtUser) userSession.getUser());
        signIconsDl.load();
    }

    @Subscribe("checkBoxOnWork")
    public void onCheckBoxOnWorkValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (!checkBoxOnWork.getValue()) {
            jobCandidatesDl.removeParameter("param1");
            jobCandidatesDl.removeParameter("param3");
        } else {
            jobCandidatesDl.setParameter("param1", null);
            jobCandidatesDl.setParameter("param3", 10);
        }

        jobCandidatesDl.load();

    }

    @Subscribe("checkBoxShowOnlyMy")
    public void onCheckBoxShowOnlyMyValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        StringBuilder sb = new StringBuilder();
        sb.append("%")
                .append(userSession.getUser().getLogin())
                .append("%");
        if (checkBoxShowOnlyMy.getValue()) {
//            jobCandidatesDl.setParameter("userName", "%" + userSession.getUser().getLogin() + "%");
            jobCandidatesDl.setParameter("userName", sb.toString());
        } else {
            jobCandidatesDl.removeParameter("userName");
        }

        jobCandidatesDl.load();
    }

    @Install(to = "jobCandidatesTable.lastIteraction", subject = "columnGenerator")
    private String jobCandidatesTableLastIteractionColumnGenerator
            (DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        IteractionList iteractionList = getLastIteraction(event.getItem());

        String date = null;

        try {
            date = new SimpleDateFormat("dd-MM-yyyy").format(iteractionList.getDateIteraction());
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

//        String retStr = "";
        StringBuilder sb = new StringBuilder();
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
                        if (iteractionList.getRecrutier() != null) {
                            if (!iteractionList.getRecrutier().equals(userSession.getUser())) {
//                                retStr = "button_table_red";
                                sb.append("button_table_red");
                            } else {
//                                retStr = "button_table_yellow";
                                sb.append("button_table_yellow");
                            }
                        }
                    } else {
//                        retStr = "button_table_green";
                        sb.append("button_table_green");
                    }
                } else {
//                    retStr = "button_table_white";
                    sb.append("button_table_white");
                }
            } else {
//                retStr = "button_table_black";
                sb.append("button_table_black");
            }
        } else {
//            retStr = "button_table_green";
            sb.append("button_table_green");
        }

        return sb.insert(0, "<div class=\"")
                .append("\">")
                .append((date != null ? date : "нет"))
                .append("</div>")
                .toString();

/*        return
                "<div class=\"" +
                        retStr
                        + "\">" +
                        (date != null ? date : "нет")
                        + "</div>"
                ; */
    }

    @Install(to = "jobCandidatesTable.lastIteraction", subject = "styleProvider")
    private String jobCandidatesTableLastIteractionStyleProvider(JobCandidate jobCandidate) {
        IteractionList iteractionList = getLastIteraction(jobCandidate);

        if (iteractionList != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(iteractionList.getDateIteraction());
            calendar.add(Calendar.MONTH, 1);

            Calendar calendar1 = Calendar.getInstance();

            if (calendar.after(calendar1)) {
                if (!iteractionList.getRecrutier().equals(userSession.getUser())) {
                    return "button_table_red";
                } else {
                    return "button_table_yellow";
                }
            } else {
                return "button_table_green";
            }
        } else {
            return "button_table_yellow";
        }
    }

    @Install(to = "jobCandidatesTable.lastIteraction", subject = "descriptionProvider")
    private String jobCandidatesTableLastIteractionDescriptionProvider(JobCandidate jobCandidate) {
        IteractionList iteractionList = getLastIteraction(jobCandidate);
        String recrutierName = "";

        if (iteractionList != null) {
            if (iteractionList.getRecrutier() != null) {
                if (iteractionList.getRecrutier().getName() != null) {
                    recrutierName = iteractionList.getRecrutier().getName();
                }
            }
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        Boolean checkBlockCandidate = jobCandidate.getBlockCandidate() == null ? false : jobCandidate.getBlockCandidate();
        StringBuilder sb = new StringBuilder();

        if (!checkBlockCandidate) {
            if (iteractionList != null) {
                if (iteractionList.getIteractionType() != null) {
                    return iteractionList != null ?
                            sb
                                    .append(simpleDateFormat.format(iteractionList.getDateIteraction()))
                                    .append("\n")
                                    .append(iteractionList.getIteractionType().getIterationName())
                                    .append("\n")
                                    .append(recrutierName)
                                    .toString() : "";
/*                    return iteractionList != null ?
                            simpleDateFormat.format(iteractionList.getDateIteraction())
                                    + "\n"
                                    + iteractionList.getIteractionType().getIterationName()
                                    + "\n"
                                    + recrutierName : ""; */
                } else {
                    return iteractionList != null ?
                            sb.append(simpleDateFormat.format(iteractionList.getDateIteraction()))
                                    .append("\n")
                                    .append(messageBundle.getMessage("msgInteractionUndefined"))
                                    .append("\n")
                                    .append(recrutierName).toString() : "";
/*                    return iteractionList != null ?
                            simpleDateFormat.format(iteractionList.getDateIteraction())
                                    + "\n"
                                    + messageBundle.getMessage("msgInteractionUndefined")
                                    + "\n"
                                    + recrutierName : ""; */
                }
            } else {
                return iteractionList != null ?
                        sb.append(simpleDateFormat.format(iteractionList.getDateIteraction()))
                                .append("\n")
                                .append(messageBundle.getMessage("msgInteractionUndefined"))
                                .append("\n")
                                .append(recrutierName).toString() : "";
/*                return iteractionList != null ?
                        simpleDateFormat.format(iteractionList.getDateIteraction())
                                + "\n"
                                + messageBundle.getMessage("msgInteractionUndefined")
                                + "\n"
                                + recrutierName : ""; */
            }
        } else {
            return messageBundle.getMessage("msgInteractionProhibited");
        }
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

    @Install(to = "jobCandidatesTable.resume", subject = "columnGenerator")
    private Icons.Icon jobCandidatesTableResumeColumnGenerator
            (DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        String retStr = "";

        try {
            if (event.getItem().getCandidateCv() != null) {
                if (event.getItem().getCandidateCv().size() == 0) {
                    retStr = "FILE";
                } else {
                    retStr = "FILE_TEXT";
                }
            } else {
                retStr = "FILE";
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return CubaIcon.valueOf(retStr);
    }

    @Install(to = "jobCandidatesTable.resume", subject = "styleProvider")
    private String jobCandidatesTableResumeStyleProvider(JobCandidate jobCandidate) {
        if (dataManager.loadValues(QUERY_RESUME)
                .parameter("candidate", jobCandidate)
                .list()
                .size() == 0) {
            return "pic-center-large-red";
        } else {
            return "pic-center-large-green";
        }
    }

    private JobCandidate jobCandidateFragment = null;

    @Install(to = "jobCandidatesTable", subject = "detailsGenerator")
    private Component jobCandidatesTableDetailsGenerator(JobCandidate entity) throws
            IOException, ClassNotFoundException {

        if (jobCandidateFragment != null) {
            jobCandidatesTable.setDetailsVisible(jobCandidateFragment, false);
            jobCandidatesTable.repaint();
            jobCandidatesTable.setSelected(entity);
        }

        jobCandidatesTable.scrollTo(entity);
        jobCandidateFragment = entity;

        VBoxLayout mainLayout = uiComponents.create(VBoxLayout.NAME);
        mainLayout.setWidth("100%");
        mainLayout.setMargin(true);

        JobCanidateDetailScreenFragment jobCanidateDetailScreenFragment = fragments.create(this,
                JobCanidateDetailScreenFragment.class);
        jobCanidateDetailScreenFragment.setJobCandidate(entity);
        jobCanidateDetailScreenFragment.setVisibleContactsLabels();

        HBoxLayout headerBox = uiComponents.create(HBoxLayout.NAME);
        headerBox.setWidthAuto();
        headerBox.setWidth("100%");
        headerBox.setHeight("100%");

        FlowBoxLayout footerBox = uiComponents.create(FlowBoxLayout.NAME);
        footerBox.setWidthAuto();
        footerBox.setHeight("100%");
        footerBox.setSpacing(false);

        HBoxLayout header2Box = uiComponents.create(HBoxLayout.NAME);
        header2Box.setWidth("100%");
        header2Box.setHeight("100%");

        Label<String> infoLabel = uiComponents.create(Label.NAME);
        infoLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);
        infoLabel.setHtmlEnabled(true);
        infoLabel.setStyleName("h3");
        infoLabel.setValue("Информация о кандидате:");

        Boolean checkBlockCandidate = false;
        if (jobCandidatesTable.getSingleSelected() != null) {
            checkBlockCandidate = !(jobCandidatesTable.getSingleSelected().getBlockCandidate() == null
                    ? false : jobCandidatesTable.getSingleSelected().getBlockCandidate());
        }

        Label<String> candidateStatusLabel = uiComponents.create(Label.NAME);
        candidateStatusLabel.setHtmlEnabled(true);
        candidateStatusLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);
        candidateStatusLabel.setStyleName(checkBlockCandidate ? "h2" : "h2-red");
        candidateStatusLabel.setValue(checkBlockCandidate ? "Нормально" : "Заблокирован");

        Label<String> candidateTitle = uiComponents.create(Label.NAME);
        candidateTitle.setHtmlEnabled(true);
        candidateTitle.setStyleName("h3");
        candidateTitle.setValue("Кандидат:");

        Component suitableButton = findSuitableButton(entity);

        Label<String> iteractionLabelHeader = uiComponents.create(Label.NAME);
        iteractionLabelHeader.setHtmlEnabled(true);
        iteractionLabelHeader.setStyleName("h3");
        iteractionLabelHeader.setValue("Взаимодействия:");

        Label<String> resumeLabelHeader = uiComponents.create(Label.NAME);
        resumeLabelHeader.setHtmlEnabled(true);
        resumeLabelHeader.setStyleName("h3");
        resumeLabelHeader.setValue("Взаимодействия:");

        Component closeButton = createCloseButton(entity);
        Component editButton = createEditButton(entity);
        Component newIteraction = createNewIteractionButton(entity);
        Component copyLastIteraction = createButtonCopyLastIteraction(entity);
        Component listIteraction = createListIteractionButton(entity);
        Component cvSimpleBrowseButton = createCvSimpleBrowse(entity);
        Component popupButtonCopyLastInteraction = createPopupButtonCopyLastInteraction(entity);

        Label<String> cvLabelHeader = uiComponents.create(Label.NAME);
        cvLabelHeader.setHtmlEnabled(true);
        cvLabelHeader.setStyleName("h3");
        cvLabelHeader.setValue("Резюме:");

        Component newResumeButton = addNewResume(entity);
        Component editLastResumeButton = editLastResume(entity);

        headerBox.add(infoLabel);
        headerBox.add(candidateStatusLabel);

        headerBox.add(candidateTitle);
        headerBox.add(editButton);

        if (suitableButton != null)
            headerBox.add(suitableButton);

        headerBox.add(iteractionLabelHeader);
        headerBox.add(newIteraction);
        headerBox.add(copyLastIteraction);
        headerBox.add(listIteraction);
        headerBox.add(popupButtonCopyLastInteraction);

        headerBox.add(cvLabelHeader);
        headerBox.add(newResumeButton);
        headerBox.add(editLastResumeButton);
        headerBox.add(cvSimpleBrowseButton);

        headerBox.add(closeButton);
        headerBox.expand(infoLabel);
        headerBox.setSpacing(true);

        mainLayout.add(headerBox);
        mainLayout.add(header2Box);

        jobCanidateDetailScreenFragment.setVisibleLogo();
        jobCanidateDetailScreenFragment.setLastSalaryLabel("Зарплатные ожидания");
        jobCanidateDetailScreenFragment.setStatistics();
        jobCanidateDetailScreenFragment.setLinkButtonTelegrem();
        jobCanidateDetailScreenFragment.setLinkButtonTelegremGroup();
        jobCanidateDetailScreenFragment.setLinkButtonEmail();
        jobCanidateDetailScreenFragment.setLinkButtonSkype();
        jobCanidateDetailScreenFragment.setStatisticsLabel();

        Fragment fragment = jobCanidateDetailScreenFragment.getFragment();
        fragment.setWidth("100%");
        fragment.setAlignment(Component.Alignment.BOTTOM_LEFT);
        mainLayout.add(fragment);

        Skillsbar skillBoxFragment = fragments.create(this, Skillsbar.class);
        if (skillBoxFragment.generateSkillLabels(getLastCVText(jobCandidatesTable.getSingleSelected()))) {
            mainLayout.add(skillBoxFragment.getFragment());
        }

        mainLayout.expand(fragment);

        return mainLayout;
    }

    private String getLastCVText(JobCandidate singleSelected) {
        if (singleSelected != null) {
            if (singleSelected.getCandidateCv() != null) {
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
        } else {
            return null;
        }
    }

    private Component createCvSimpleBrowse(JobCandidate entity) {
        Button cvSimpleBrowseButton = uiComponents.create(Button.class);
        cvSimpleBrowseButton.setDescription("Копировать резюме кандидата");
        cvSimpleBrowseButton.setIconFromSet(CubaIcon.FILE_TEXT_O);

        createDataComponentsResume();

        cvSimpleBrowseButton.setAction(new BaseAction("candidateCV")
                .withHandler(actionPerformedEvent -> {
                    candidateCVDl.setParameter("candidate", entity);
                    candidateCVDl.load();

                    CandidateCVSimpleBrowse candidateCVSimpleBrowse = screens.create(CandidateCVSimpleBrowse.class);
                    candidateCVSimpleBrowse.setSelectedCandidate(entity);
                    candidateCVSimpleBrowse.setJobCandidate(entity);
                    screens.show(candidateCVSimpleBrowse);

                }));

        return cvSimpleBrowseButton;
    }

    private Component createPopupButtonCopyLastInteraction(JobCandidate entity) {
        PopupButton lastInteractionPopupButton = uiComponents.create(PopupButton.class);
        lastInteractionPopupButton.setDescription("Копировать последнее взаимодействие с кандидатом");
        lastInteractionPopupButton.setIconFromSet(CubaIcon.FILE_TEXT);

        jobCandidatesTable.addSelectionListener(e -> {
            if (jobCandidatesTable.getSingleSelected() == null) {
                lastInteractionPopupButton.setEnabled(false);
                actionsWithCandidateButton.setEnabled(false);
            } else {
                lastInteractionPopupButton.setEnabled(true);
                actionsWithCandidateButton.setEnabled(true);

                if (jobCandidatesTable.getSingleSelected().getEmail() == null) {
                    actionsWithCandidateButton.getAction("sendEmailAction").setEnabled(false);
                } else {
                    actionsWithCandidateButton.getAction("sendEmailAction").setEnabled(true);
                }
            }

        });

        Integer MAX_POPULAR_INTERACLION = 5;
        List<Iteraction> mostPopularInteraction = interactionService.getMostPolularIteraction(
                userSessionSource.getUserSession().getUser(), MAX_POPULAR_INTERACLION);

        if (mostPopularInteraction.size() != 0) {
            Integer count = 1;
            for (Iteraction iteraction : mostPopularInteraction) {
                lastInteractionPopupButton.addAction(
                        new BaseAction("setMostPopularInteractionPopupButton" + "-" + count++)
                                .withCaption(iteraction.getIterationName())
                                .withHandler(actionPerformedEvent -> setMostPopularInteractionPopupButton(iteraction)));
            }
            lastInteractionPopupButton.setEnabled(true);
        } else {
            lastInteractionPopupButton.setEnabled(false);
        }

        return lastInteractionPopupButton;
    }

    public void setMostPopularInteractionPopupButton(Iteraction iteraction) {
        if (jobCandidatesTable.getSingleSelected() != null) {
            screenBuilders.editor(IteractionList.class, this)
                    .newEntity()
                    .withInitializer(e -> {
                        e.setCandidate(jobCandidatesTable.getSingleSelected());
                        e.setIteractionType(iteraction);
                        BigDecimal maxNumberIteraction = BigDecimal.ZERO;
                        IteractionList lastIteraction = null;

                        if (jobCandidatesTable.getSingleSelected() != null) {
                            for (IteractionList list : jobCandidatesTable
                                    .getSingleSelected()
                                    .getIteractionList()) {
                                if (maxNumberIteraction.compareTo(list.getNumberIteraction()) < 0) {
                                    maxNumberIteraction = list.getNumberIteraction();
                                    lastIteraction = list;
                                }
                            }

                            if (lastIteraction != null) {
                                e.setVacancy(lastIteraction.getVacancy());
                                e.setNumberIteraction(dataManager.loadValue(
                                        "select max(e.numberIteraction) " +
                                                "from itpearls_IteractionList e", BigDecimal.class)
                                        .one().add(BigDecimal.ONE));
                            }
                        }
                    })
                    .build()
                    .show();
        }
    }

    private Component createButtonCopyLastIteraction(JobCandidate entity) {
        Button lastIteractionButton = uiComponents.create(Button.class);
        lastIteractionButton.setDescription("Копировать последнее взаимодействие с кандидатом");
        lastIteractionButton.setIconFromSet(CubaIcon.COPY);


        lastIteractionButton.setAction(new BaseAction("copyLastIteraction")
                .withHandler(actionPerformedEvent -> {
                    screenBuilders.editor(IteractionList.class, this)
                            .newEntity()
                            .withScreenClass(IteractionListEdit.class)
                            .withInitializer(iteractionList1 -> {
                                iteractionList1.setCandidate(jobCandidatesTable.getSingleSelected());

                                BigDecimal maxNumberIteraction = BigDecimal.ZERO;
                                IteractionList lastIteraction = null;

                                if (jobCandidatesTable.getSingleSelected() != null) {
                                    for (IteractionList list : jobCandidatesTable
                                            .getSingleSelected()
                                            .getIteractionList()) {
                                        if (maxNumberIteraction.compareTo(list.getNumberIteraction()) < 0) {
                                            maxNumberIteraction = list.getNumberIteraction();
                                            lastIteraction = list;
                                        }
                                    }
                                } else {
                                    notifications.create(Notifications.NotificationType.WARNING)
                                            .withDescription("ВНИМАНИЕ!")
                                            .withCaption(messageBundle.getMessage("msgNeedSelectRow"))
                                            .show();
                                }

                                if (lastIteraction != null) {
                                    iteractionList1.setVacancy(lastIteraction.getVacancy());
                                    iteractionList1.setNumberIteraction(dataManager.loadValue(
                                            "select max(e.numberIteraction) " +
                                                    "from itpearls_IteractionList e", BigDecimal.class)
                                            .one().add(BigDecimal.ONE));
                                }
                            })
                            .withAfterCloseListener(e -> {
                                jobCandidatesDl.load();
                            })
                            .build()
                            .show();
                }));

        return lastIteractionButton;
    }

    private Component findSuitableButton(JobCandidate entity) {
        if (dataManager.load(CandidateCV.class)
                .query("select e from itpearls_CandidateCV e where e.candidate = :candidate")
                .cacheable(true)
                .parameter("candidate", entity)
                .view("candidateCV-view")
                .cacheable(true)
                .list().size() != 0) {
            Button suitableButton = uiComponents.create(Button.class);
            suitableButton.setDescription("Подобрать вакансию по резюме");
            suitableButton.setIconFromSet(CubaIcon.EYE);

            suitableButton.setAction(new BaseAction("findSuitable")
                    .withHandler(actionPerformedEvent -> {
                        FindSuitable findSuitable = screens.create(FindSuitable.class);
                        findSuitable.setJobCandidate(entity);

                        findSuitable.show();
                    }));

            return suitableButton;
        } else
            return null;
    }

    private Component getLastCVtoClipboard(JobCandidate entity) {
        Button getLastLetterButton = uiComponents.create(Button.class);
        getLastLetterButton.setDescription(messageBundle.getMessage("msgGelLastLetter"));
        getLastLetterButton.setIconFromSet(CubaIcon.TABLET);

        getLastLetterButton.setAction(new BaseAction("candidateCV")
                .withHandler(actionPerformedEvent -> {
                    notifications.create().withCaption("Пока не реализовано");
                }));

        return getLastLetterButton;
    }

    private Component getLastLetterToClipboard(JobCandidate entity) {
        Button getLastResumeButton = uiComponents.create(Button.class);
        getLastResumeButton.setDescription(messageBundle.getMessage("msgGetLastCV"));
        getLastResumeButton.setIconFromSet(CubaIcon.GET_POCKET);

        getLastResumeButton.setAction(new BaseAction("candidateCV")
                .withHandler(actionPerformedEvent -> {
                    notifications.create().withCaption("Пока не реализовано");
                }));

        return getLastResumeButton;
    }

    private Component editLastResume(JobCandidate entity) {
        Button editLastResumeButton = uiComponents.create(Button.class);
        editLastResumeButton.setDescription("Редактировать последнее резюме");
        editLastResumeButton.setIconFromSet(CubaIcon.EDIT_ACTION);
        createDataComponentsResume();

        editLastResumeButton.setAction(new BaseAction("candidateCV")
                .withHandler(actionPerformedEvent -> {
                    candidateCVDl.setParameter("candidate", entity);
                    candidateCVDl.load();

                    List<CandidateCV> candidateCV = candidateCVDc.getItems();

                    if (candidateCV.size() != 0) {
                        CandidateCV lastCV = null;
                        for (CandidateCV a : candidateCV) {
                            lastCV = a;
                        }

                        Screen screenCV = screenBuilders.editor(CandidateCV.class, this)
                                .withScreenClass(CandidateCVEdit.class)
                                .editEntity(lastCV)
                                .withLaunchMode(OpenMode.NEW_TAB)
                                .withContainer(candidateCVDc)
                                .build();

                        try {
                            screenCV.show();
                        } catch (NullPointerException e) {
                            notifications.create(Notifications.NotificationType.ERROR)
                                    .withCaption(messageBundle.getMessage("msgError"))
                                    .withDescription("Не могу открыть форму резюме для редактирования.\n" +
                                            "Зайдите в карточку кандидата " +
                                            "и продолжите редактирование резюме во вкладке \"Резюме кандидата\"")
                                    .show();

                            e.printStackTrace();
                        }

                    } else {
                        dialogs.createOptionDialog()
                                .withCaption(messageBundle.getMessage("msgWarning"))
                                .withMessage("У кандидата нет ни одного резюме.\nСоздать резюме?")
                                .withActions(
                                        new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                                            addNewResumeAction(entity);
                                        }),
                                        new DialogAction(DialogAction.Type.NO)
                                )
                                .show();
                    }
                }));

        return editLastResumeButton;
    }

    protected void addNewResumeAction(JobCandidate entity) {
        candidateCVDl.setParameter("candidate", entity);
        candidateCVDl.load();

        screenBuilders.editor(CandidateCV.class, this)
                .withScreenClass(CandidateCVEdit.class)
                .withInitializer(candidateCV -> {
                    candidateCV.setCandidate(entity);
                })
                .newEntity()
                .withContainer(candidateCVDc)
                .build()
                .show();
    }

    private Component addNewResume(JobCandidate entity) {
        Button addResumeButton = uiComponents.create(Button.class);
        addResumeButton.setDescription("Добавить новое резюме кандидата");
        addResumeButton.setIconFromSet(CubaIcon.ADD_ACTION);

        createDataComponentsResume();

        addResumeButton.setAction(new BaseAction("candidateCV")
                .withHandler(actionPerformedEvent -> {
                    addNewResumeAction(entity);
                }));

        return addResumeButton;
    }

    private Component createListIteractionButton(JobCandidate entity) {
        Button listIteractionButton = uiComponents.create(Button.class);
        listIteractionButton.setDescription("Список взаимодействий");
        listIteractionButton.setIconFromSet(CubaIcon.FILE_TEXT_O);
        createDataComponents();

        listIteractionButton.setAction(new BaseAction("listIteraction")
                .withHandler(actionPerformedEvent -> {
                    iteractionListDl.setParameter("candidate", entity);
                    iteractionListDl.load();

                    IteractionListSimpleBrowse iteractionListSimpleBrowse = screens.create(IteractionListSimpleBrowse.class);
                    iteractionListSimpleBrowse.setSelectedCandidate(entity);
                    iteractionListSimpleBrowse.setJobCandidate(entity);
                    screens.show(iteractionListSimpleBrowse);

                }));

        return listIteractionButton;

    }

    private void createDataComponentsResume() {
        DataContext dataContext = dataComponents.createDataContext();
        getScreenData().setDataContext(dataContext);

        candidateCVDc = dataComponents.createCollectionContainer(CandidateCV.class);

        candidateCVDl = dataComponents.createCollectionLoader();
        candidateCVDl.setContainer(candidateCVDc);
        iteractionListDl.setDataContext(dataContext);
        candidateCVDl.setView("candidateCV-view");
        candidateCVDl.setQuery("select e from itpearls_CandidateCV e " +
                "where e.candidate = :candidate order by e.datePost");
    }

    private void createDataComponents() {
        DataContext dataContext = dataComponents.createDataContext();
        getScreenData().setDataContext(dataContext);

        iteractionListDc = dataComponents.createCollectionContainer(IteractionList.class);

        iteractionListDl = dataComponents.createCollectionLoader();
        iteractionListDl.setContainer(iteractionListDc);
        iteractionListDl.setDataContext(dataContext);
        iteractionListDl.setView("iteractionList-view");
        iteractionListDl.setQuery("select e from itpearls_IteractionList e " +
                "where e.candidate = :candidate " +
                "order by e.numberIteraction desc");
    }

    private Component createNewIteractionButton(JobCandidate entity) {
        Button newIteractionButton = uiComponents.create(Button.class);
        newIteractionButton.setDescription("Новое взаимодействие");
        newIteractionButton.setIconFromSet(CubaIcon.CREATE_ACTION);


        newIteractionButton.setAction(new BaseAction("newIteraction")
                .withHandler(actionPerformedEvent ->
                        screenBuilders.editor(IteractionList.class, this)
                                .newEntity()
                                .withScreenClass(IteractionListEdit.class)
                                .withInitializer(iteractionList1 -> {
                                    iteractionList1.setCandidate(jobCandidatesTable.getSingleSelected());
                                })
                                .withAfterCloseListener(e -> {
                                    jobCandidatesDl.load();
                                })
                                .build()
                                .show())
        );

        return newIteractionButton;
    }

    private HBoxLayout getLastIteraction(List<IteractionList> iteractionList) {
        HBoxLayout retBox = uiComponents.create(HBoxLayout.NAME);
        Label<String> dateIteraction = uiComponents.create(Label.NAME);
        Label<String> retLab = uiComponents.create(Label.NAME);
        Label<String> add = uiComponents.create((Label.NAME));
        String addInfo = "";

        if (iteractionList.size() != 0) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");

            dateIteraction.setValue(simpleDateFormat.format(iteractionList.get(0).getDateIteraction()) + ":");
            retLab.setValue(iteractionList.get(0).getIteractionType().getIterationName());

            if (iteractionList.get(0).getAddDate() != null) {
                add.setValue(" (" + simpleDateFormat.format(iteractionList.get(0).getAddDate()) + ")");
            }
        } else {
            return null;
        }

        retBox.add(dateIteraction);
        retBox.add(retLab);
        if (!addInfo.equals(""))
            retBox.add(add);

        return retBox;
    }

    private void setIteraction(VBoxLayout iteraction) {
    }

    private void setStatistics(VBoxLayout statistics) {
        Label<String> titleStatistics = uiComponents.create(Label.NAME);
        titleStatistics.setValue("Статистика по кандидату");
        titleStatistics.setStyleName("h3");

        String QUERY_LAST_SALARY = "select e from itpearls_IteractionList e where e.iteractionType = " +
                "(select f from itpearls_Iteraction f where f.iterationName like :iteractionName) and " +
                "e.candidate = :candidate";
        String iteractionName = "Зарплатные ожидания";

        IteractionList iteractionList = null;

        try {
            iteractionList = dataManager.load(IteractionList.class)
                    .query(QUERY_LAST_SALARY)
                    .cacheable(true)
                    .view("iteractionList-view")
                    .parameter("iteractionName", iteractionName)
                    .parameter("candidate", jobCandidatesTable.getSingleSelected())
                    .cacheable(true)
                    .one();
        } catch (Exception e) {
            e.printStackTrace();
        }


        VBoxLayout vBoxLayout = uiComponents.create(VBoxLayout.NAME);
        vBoxLayout.setWidth("100%");

        if (iteractionList != null) {
            Label<String> lastSalary = uiComponents.create(Label.NAME);
            lastSalary.setValue("Зарплатные ожидания: " + iteractionList.getAddString());
            vBoxLayout.add(lastSalary);
        }

        statistics.add(titleStatistics);
        statistics.add(vBoxLayout);
    }

    private List<IteractionList> getIteractionLists(JobCandidate singleSelected) {

        List<IteractionList> listIteracion = dataManager.load(IteractionList.class)
                .query(QUERY_GET_LASTRECRUTIER)
                .cacheable(true)
                .parameter("candidate", singleSelected)
                .view("iteractionList-job-candidate")
                .cacheable(true)
                .list();

        return listIteracion;
    }

    private Component createEditButton(JobCandidate entity) {
        Button editButton = uiComponents.create(Button.class);
        editButton.setDescription("Редактирование");
        editButton.setIconFromSet(CubaIcon.EDIT_ACTION);

        BaseAction editAction = new BaseAction("edit")
                .withHandler(actionPerformedEvent -> {
                    screenBuilders.editor(JobCandidate.class, this)
                            .editEntity(entity)
                            .build()
                            .show();
                })
                .withCaption("");
        editButton.setAction(editAction);
        return editButton;
    }

    private Component createCloseButton(JobCandidate entity) {
        Button closeButton = uiComponents.create(Button.class);
        closeButton.setDescription(messageBundle.getMessage("msgClose"));
        closeButton.setIcon("icons/close.png");
        BaseAction closeAction = new BaseAction("closeAction")
                .withHandler(actionPerformedEvent ->
                        jobCandidatesTable.setDetailsVisible(entity, false))
                .withCaption("");
        closeButton.setAction(closeAction);
        return closeButton;
    }

    @Install(to = "jobCandidatesTable.status", subject = "columnGenerator")
    private Component jobCandidatesTableStatusColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);

        Label contactsStatusLabel = uiComponents.create(Label.class);
        CubaIcon icon = getContactsStatusIcon(event);
        if (icon != null)
            contactsStatusLabel.setIconFromSet(icon);
        contactsStatusLabel.setStyleName(getContactsStatusStyle(event));
        contactsStatusLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);
        if (contactsInfoStyle.equals("pic-center-large-grey")) { // костыль, лениво рефакторить пока
            contactsStatusLabel.setDescription(messageBundle.getMessage("msgNoContactsInfo"));
        } else {
            contactsStatusLabel.setDescription(messageBundle.getMessage("msgHaveContactsInfo"));
        }

        Label employeeStatusLabel = genEmployeeStatusLabel(event);
        Label signIconLabel = getSignIconLabel(event);
        Label commentCandidateLabel = getCommentCandidateLabel(event);
        Label phoneCandidateLabel = getPhoneCandidateLabel(event);
        Label telegramCLabel = genTelegramLabel(event);
        Label skypeLabel = genSkypeLabel(event);
        Label emailLabel = getEmailLabel(event);
        Label blackListLabel = getBlackList(event);
        Label cvLabel = getCVLabel(event);
        List<Image> snImages = getSNLabels(event);

        retHBox.add(blackListLabel);
        retHBox.add(employeeStatusLabel);
        retHBox.add(signIconLabel);
        retHBox.add(contactsStatusLabel);
        retHBox.add(phoneCandidateLabel);
        retHBox.add(emailLabel);
        retHBox.add(telegramCLabel);
        retHBox.add(skypeLabel);
        retHBox.add(cvLabel);
        retHBox.add(commentCandidateLabel);

        for (Image image : snImages) {
            retHBox.add(image);
        }

        return retHBox;
    }

    private List<Image> getSNLabels(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        List<Image> retImage = new ArrayList<>();

        for (SocialNetworkURLs snt : event.getItem().getSocialNetwork()) {
            if (snt.getNetworkURLS() != null) {
                Image image = uiComponents.create(Image.class);
                image.setDescriptionAsHtml(true);
                image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
                image.setWidth("20px");
                image.setHeight("20px");
                image.setStyleName("icon-no-border-30px");
                image.setAlignment(Component.Alignment.BOTTOM_CENTER);
                image.setDescription(snt.getNetworkURLS());
                image.addClickListener(e -> webBrowserTools.showWebPage(snt.getNetworkURLS(), null));

                if (snt.getSocialNetworkURL().getLogo() != null) {
                    image
                            .setSource(FileDescriptorResource.class)
                            .setFileDescriptor(snt.getSocialNetworkURL().getLogo());
                } else {
                    image.setSource(ThemeResource.class).setPath("icons/no-company.png");

                }

                retImage.add(image);
            }
        }

        return retImage;
    }

    private Label getSignIconLabel(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        Label retLabel = uiComponents.create(Label.class);

        List<JobCandidateSignIcon> jobCandidateSignIcons = dataManager.load(JobCandidateSignIcon.class)
                .query(QUERY_GET_JOB_CANDIDATE_SIGN_ICONS)
                .parameter("jobCandidate", event.getItem())
                .view("jobCandidateSignIcon-view")
                .cacheable(true)
                .list();

        if (jobCandidateSignIcons.size() > 0) {
            retLabel.setAlignment(Component.Alignment.BOTTOM_CENTER);
            retLabel.setIcon(jobCandidateSignIcons.get(0).getSignIcon().getIconName());

            if (jobCandidateSignIcons.get(0).getSignIcon().getTitleDescription() != null) {
                retLabel.setDescription(jobCandidateSignIcons.get(0).getSignIcon().getTitleDescription());
            } else {
                retLabel.setDescription(jobCandidateSignIcons.get(0).getSignIcon().getTitleRu());
            }

            injectColorCss(jobCandidateSignIcons.get(0).getSignIcon().getIconColor());
            retLabel.setStyleName("pic-center-large-"
                    + jobCandidateSignIcons.get(0).getSignIcon().getIconColor());
        }

        return retLabel;
    }

    protected void injectColorCss(String color) {
        Page.Styles styles = Page.getCurrent().getStyles();
        String style = String.format(
                ".pic-center-large-%s {" +
                        "color: #%s;" +
                        "text-align: center;" +
                        "text-color: gray;" +
                        "font-size: large;" +
                        "margin: 0 auto;" +
                        "}",
                color, color);

        styles.add(style);
    }

    private Label getCVLabel(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        Label retLabel = uiComponents.create(Label.class);
        retLabel.setAlignment(Component.Alignment.BOTTOM_CENTER);

        String retStr = "";
        String retStrStyle = "";
        String retStrDesc = "";

        try {
            if (event.getItem().getCandidateCv() != null) {
                if (event.getItem().getCandidateCv().size() == 0) {
                    retStr = "FILE";
                    retStrStyle = "pic-center-large-red";
                    retStrDesc = messageBundle.getMessage("msgNoResumeAttached");
                    retLabel.setVisible(false);
                } else {
                    retStr = "FILE_TEXT";
                    retStrStyle = "pic-center-large-green";
                    retStrDesc = messageBundle.getMessage("msgResumeAttached");
                }
            } else {
                retStr = "FILE";
                retStrStyle = "pic-center-large-red";
                retStrDesc = messageBundle.getMessage("msgNoResumeAttached");
                retLabel.setVisible(false);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        } finally {

            retLabel.setIconFromSet(CubaIcon.valueOf(retStr));
            retLabel.setStyleName(retStrStyle);
            retLabel.setDescription(retStrDesc);

            return retLabel;
        }
    }

    private CubaIcon getStarIconPersonalReserve(PersonelReserve personelReserve) {
        StdSelections s = StdSelections.fromId(personelReserve.getSelectionSymbolForActions());
        CubaIcon retIcon;

        if (s != null) {
            switch (s) {
                case STAR_RED:
                case STAR_YELLOW:
                case STAR_GREEN:
                    retIcon = CubaIcon.STAR;
                    break;
                case FLAG_RED:
                case FLAG_YELLOW:
                case FLAG_GREEN:
                    retIcon = CubaIcon.FLAG;
                    break;
                default:
                    retIcon = CubaIcon.STAR;
                    break;
            }
        } else {
            retIcon = CubaIcon.STAR;
        }

        return retIcon;
    }

    private String getStarIconPersonalReserveStyle(PersonelReserve personelReserve) {
        StdSelections s = StdSelections.fromId(personelReserve.getSelectionSymbolForActions());

        if (s != null) {
            switch (s) {
                case STAR_RED:
                    return StdSelectionsColor.STAR_RED.getId();
                case STAR_YELLOW:
                    return StdSelectionsColor.STAR_YELLOW.getId();
                case STAR_GREEN:
                    return StdSelectionsColor.STAR_GREEN.getId();
                case FLAG_RED:
                    return StdSelectionsColor.FLAG_RED.getId();
                case FLAG_YELLOW:
                    return StdSelectionsColor.FLAG_YELLOW.getId();
                case FLAG_GREEN:
                    return StdSelectionsColor.FLAG_GREEN.getId();
                default:
                    return StdSelectionsColor.STAR_YELLOW.getId();
            }
        } else {
            return StdSelectionsColor.STAR_YELLOW.getId();
        }
    }

    private Label getPhoneCandidateLabel(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        Label retLabel = uiComponents.create(Label.class);
        retLabel.setVisible(false);
        retLabel.setIconFromSet(CubaIcon.PHONE);
        retLabel.setStyleName("pic-center-large-blue");
        retLabel.setAlignment(Component.Alignment.BOTTOM_CENTER);

        if (event.getItem().getMobilePhone() != null) {
            if (!event.getItem().getMobilePhone().equals("")) {
                retLabel.setDescription(event.getItem().getMobilePhone());
                retLabel.setVisible(true);
            }
        }

        if (event.getItem().getPhone() != null) {
            if (!event.getItem().getPhone().equals("")) {
                retLabel.setDescription(event.getItem().getPhone());
                retLabel.setVisible(true);
            }
        }

        return retLabel;
    }

    private Label getBlackList(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        Label retLabel = uiComponents.create(Label.class);
        retLabel.setVisible(false);
        retLabel.setIconFromSet(CubaIcon.STOP_CIRCLE_O);
        retLabel.setStyleName("pic-center-large-black");
        retLabel.setAlignment(Component.Alignment.BOTTOM_CENTER);
        retLabel.setDescription(messageBundle.getMessage("msgBlackList"));

        if (event.getItem().getBlockCandidate() != null) {
            if (event.getItem().getBlockCandidate())
                retLabel.setVisible(true);
        }

        return retLabel;
    }

    private Label getEmailLabel(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        Label retLabel = uiComponents.create(Label.class);
        retLabel.setVisible(false);
        retLabel.setIconFromSet(CubaIcon.ENVELOPE);
        retLabel.setStyleName("pic-center-large-gray");
        retLabel.setDescription(event.getItem().getEmail());
        retLabel.setAlignment(Component.Alignment.BOTTOM_CENTER);


        if (event.getItem().getEmail() != null) {
            if (!event.getItem().getEmail().equals(""))
                retLabel.setVisible(true);
        }

        return retLabel;
    }

    private Label genSkypeLabel(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        Label retLabel = uiComponents.create(Label.class);
        retLabel.setVisible(false);
        retLabel.setIconFromSet(CubaIcon.SKYPE);
        retLabel.setStyleName("pic-center-large-blue");
        retLabel.setDescription(event.getItem().getSkypeName());
        retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);

        if (event.getItem().getSkypeName() != null) {
            if (!event.getItem().getSkypeName().equals("")) {
                retLabel.setVisible(true);
            }
        }

        return retLabel;
    }

    private Label genTelegramLabel(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        Label retLabel = uiComponents.create(Label.class);
        retLabel.setVisible(false);
        retLabel.setIconFromSet(CubaIcon.TELEGRAM);
        retLabel.setStyleName("pic-center-large-lightblue");
        retLabel.setDescription(event.getItem().getTelegramName());
        retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);

        if (event.getItem().getTelegramName() != null) {
            if (!event.getItem().getTelegramName().equals("")) {
                retLabel.setVisible(true);
            }
        }

        return retLabel;
    }

    private Label getCommentCandidateLabel(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        Label retLabel = uiComponents.create(Label.class);
        retLabel.setVisible(false);
        retLabel.setDescription(messageBundle.getMessage("msgHaveComment"));
        retLabel.setIconFromSet(CubaIcon.COMMENTS);
        retLabel.setStyleName("pic-center-large-black");
        retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);

        Boolean isComment = false;

        if (event.getItem().getIteractionList() != null) {
            for (IteractionList iteractionList : event.getItem().getIteractionList()) {
                if (iteractionList.getComment() != null) {
                    if (!iteractionList.getComment().equals("")) {
                        isComment = true;
                        break;
                    }
                }
            }

            if (isComment) {
                retLabel.setVisible(true);
            }
        }

        return retLabel;
    }

    private Label genEmployeeStatusLabel(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        return setEployeeStatusIcon(event);
    }


    private Label setEployeeStatusIcon(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        Label retLabel = uiComponents.create(Label.class);
        retLabel.setIconFromSet(CubaIcon.CIRCLE_O);
        retLabel.setDescription(messageBundle.getMessage("msgCandidate"));
        retLabel.setVisible(false);

        for (Employee employee : employees) {
            if (event.getItem().equals(employee.getJobCandidate())) {
                retLabel.setIconFromSet(CubaIcon.CHILD);
                retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);
                retLabel.setVisible(true);

                if (employee.getWorkStatus() != null) {
                    if (employee.getWorkStatus().getInStaff() != null) {
                        if (employee.getWorkStatus().getInStaff()) {
                            retLabel.setStyleName("pic-center-large-green");
                            retLabel.setDescription(messageBundle.getMessage("msgOurWorker"));
                            break;
                        } else {
                            retLabel.setStyleName("pic-center-large-red");
                            retLabel.setDescription(messageBundle.getMessage("msgDissmised"));
                            break;
                        }
                    } else {
                        retLabel.setStyleName("pic-center-large-yellow");
                        retLabel.setDescription(messageBundle.getMessage("msgUndefined"));
                        break;
                    }
                } else {
                    retLabel.setStyleName("pic-center-large-yellow");
                    retLabel.setDescription(messageBundle.getMessage("msgUndefined"));
                    break;
                }
            } else {
                retLabel.setStyleName("pic-center-large-grey");
            }
        }

        return retLabel;
    }

    private String contactsInfoStyle;

    private String getContactsStatusStyle(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        Integer s = getPictString(event.getItem());
        String retStr = "";

        if (s != null) {
            switch (s) {
                case 0: // WHITE
                    retStr = "pic-center-large-grey";
                    break;
                case 1: // red
                    retStr = "pic-center-large-red";
                    break;
                case 2: // yellow
                    retStr = "pic-center-large-yellow";
                    break;
                case 3: // green
                    retStr = "pic-center-large-green";
                    break;
                case 4: // to client
                    retStr = "pic-center-large-grey";
                    break;
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10: // recruiting
                    retStr = "pic-center-large-grey";
                    break;
                default:
                    retStr = "pic-center-large";
                    break;
            }
        }

        contactsInfoStyle = retStr;
        return retStr;
    }

    private CubaIcon getContactsStatusIcon(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        Integer s = getPictString(event.getItem());
        String retStr = "";

        if (s != null) {
            switch (s) {
                case 0: // WHITE
                    retStr = "QUESTION_CIRCLE";
                    break;
                case 1: // red
                    retStr = "BOMB";
                    break;
                case 2: // yellow
//                    retStr = "BOMB";
                    break;
                case 3: // green
//                    retStr = "BOMB";
                    break;
                case 4: // to client
                    retStr = "BOMB";
                    break;
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10: // recruiting
                    retStr = "BOMB";
                    break;
                default:
                    retStr = "QUESTION_CIRCLE";
                    break;
            }
        }

        return retStr.equals("") ? null : CubaIcon.valueOf(retStr);
    }


    private Integer getPictString(JobCandidate jobCandidate) {
        // если только имя и отчество - красный сигнал светофора
        // если имя, день рождения и один из контактов - желтый
        // если больше двух контактов - зеленый, если нет др - все равно желтый
        if (((jobCandidate.getEmail() != null && jobCandidate.getPhone() != null && jobCandidate.getSkypeName() != null) ||
                (jobCandidate.getEmail() != null && jobCandidate.getPhone() != null) ||
                (jobCandidate.getEmail() != null && jobCandidate.getSkypeName() != null) ||
                (jobCandidate.getEmail() != null && jobCandidate.getTelegramName() != null) ||
                (jobCandidate.getPhone() != null && jobCandidate.getSkypeName() != null)) &&
                jobCandidate.getBirdhDate() != null) {
            return 3;
        } else {
            if (jobCandidate.getPhone() != null ||
                    jobCandidate.getEmail() != null ||
                    jobCandidate.getTelegramName() != null ||
                    jobCandidate.getSkypeName() != null) {
                return 2;
            } else {
                if (jobCandidate.getFirstName() == null ||
                        jobCandidate.getMiddleName() == null ||
                        jobCandidate.getCityOfResidence() == null ||
                        jobCandidate.getCurrentCompany() == null ||
                        jobCandidate.getTelegramName() == null ||
                        jobCandidate.getPersonPosition() == null) {
                    return 0;
                }
            }
        }

        return 0;
    }

    public void onButtonSubscribeClick() {
        screenBuilders.editor(SubscribeCandidateAction.class, this)
                .newEntity()
                .withInitializer(e -> {
                    e.setCandidate(jobCandidatesTable.getSingleSelected());
                    e.setSubscriber(userSession.getUser());
                    e.setStartDate(new Date());
                })
                .withOpenMode(OpenMode.DIALOG)
                .build()
                .show();
    }

    @Subscribe
    public void onInit(InitEvent event) {
        jobCandidatesTable.setItemClickAction(new BaseAction("itemClickAction")
                .withHandler(actionPerformedEvent -> {
                    jobCandidatesTable.setDetailsVisible(jobCandidatesTable.getSingleSelected(), true);
                }));


        candidateImageColumnRenderer();

        DataGrid.ClickableTextRenderer<JobCandidate> jobCandidatesTableLastIteractionRenderer =
                jobCandidatesTable.createRenderer(DataGrid.ClickableTextRenderer.class);

        jobCandidatesTableLastIteractionRenderer.setRendererClickListener(clickableTextRendererClickEvent -> {
            IteractionListSimpleBrowse iteractionListSimpleBrowse = screens.create(IteractionListSimpleBrowse.class);
            iteractionListSimpleBrowse.setSelectedCandidate(clickableTextRendererClickEvent.getItem());
            screens.show(iteractionListSimpleBrowse);
        });

        jobCandidatesTable.getColumn("lastIteraction").setStyleProvider(e -> {
            IteractionList iteractionList = getLastIteraction(e);

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
                            return "button_table_red";
                        } else {
                            return "button_table_yellow";
                        }
                    } else {
                        return "button_table_white";
                    }
                } else {
                    return "button_table_green";
                }
            } else {
                return "button_table_white";
            }
        });


        jobCandidatesTable.getColumn("lastIteraction")
                .setRenderer(jobCandidatesTable.createRenderer(DataGrid.HtmlRenderer.class));

        setRatingField();
        setWithCVCheckBox();
        setSendEmailButton();
    }

    private void setSendEmailButton() {
        jobCandidatesTable.addSelectionListener(e -> {
            if (jobCandidatesTable.getSingleSelected() != null) {
                actionsWithCandidateButton.setEnabled(true);

                if (jobCandidatesTable.getSingleSelected().getEmail() == null) {
                    actionsWithCandidateButton.getAction("sendEmailAction").setEnabled(false);
                } else {
                    actionsWithCandidateButton.getAction("sendEmailAction").setEnabled(true);
                }

                if (jobCandidatesTable.getSingleSelected().getEmail() != null) {
                    sendEmailButton.setEnabled(true);
                } else {
                    sendEmailButton.setEnabled(false);
                }
            } else {
                actionsWithCandidateButton.setEnabled(false);
            }
        });
    }

    private void setWithCVCheckBox() {
        withCVCheckBox.addValueChangeListener(e -> {
            if (withCVCheckBox.getValue() != null) {
                if (!withCVCheckBox.getValue()) {
                    if (ratingFieldNotLower.getValue() == null) {
                        jobCandidatesDl.setQuery("select e from itpearls_JobCandidate e "
                                + "order by e.secondName, e.firstName");
                    } else {
                        jobCandidatesDl.setQuery("select e from itpearls_JobCandidate e "
                                + "where e in (select f.candidate "
                                + "from itpearls_IteractionList f "
                                + "where f.candidate = e) "
                                + "and e in (select g.candidate from itpearls_IteractionList g where g.candidate = e and g.rating >= " + ratingFieldNotLower.getValue().toString() + ") "
                                + "order by e.secondName, e.firstName");
                    }
                } else {
                    if (ratingFieldNotLower.getValue() == null) {
                        jobCandidatesDl.setQuery("select e from itpearls_JobCandidate e "
                                + "where e in (select g.candidate "
                                + "from itpearls_CandidateCV g "
                                + "where g.candidate = e) "
                                + "order by e.secondName, e.firstName");
                    } else {
                        jobCandidatesDl.setQuery("select e from itpearls_JobCandidate e "
                                + "where e in (select g.candidate "
                                + "from itpearls_CandidateCV g "
                                + "where g.candidate = e) "
                                + "and e in (select g.candidate from itpearls_IteractionList g where g.candidate = e and g.rating >= " + ratingFieldNotLower.getValue().toString() + ") "
                                + "order by e.secondName, e.firstName");
                    }
                }

                jobCandidatesDl.load();
            }
        });
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
            } else {
                image.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
            }

            image.setWidth("20px");
            image.setStyleName("circle-20px");

            image.setScaleMode(Image.ScaleMode.CONTAIN);
            image.setAlignment(Component.Alignment.MIDDLE_CENTER);

            hBox.setWidthFull();
            hBox.setHeightFull();
            hBox.add(image);

            return hBox;
        });
    }

    @Install(to = "jobCandidatesTable.personPosition", subject = "descriptionProvider")
    private String jobCandidatesTablePersonPositionDescriptionProvider(JobCandidate jobCandidate) {
        StringBuilder sb = new StringBuilder();

        if (jobCandidate.getPositionList() != null) {
            for (JobCandidatePositionLists s : jobCandidate.getPositionList()) {
                if (!sb.toString().equals("")) {
                    sb.append(",");
                }

                sb.append(s.getPositionList().getPositionRuName());
            }
        }

        return sb.toString();
    }

    @Install(to = "jobCandidatesTable.rating", subject = "columnGenerator")
    private String jobCandidatesTableRatingColumnGenerator(DataGrid.ColumnGeneratorEvent<JobCandidate> event) {
        return avgRating(event.getItem());
    }

    private String avgRating(JobCandidate jobCandidate) {
        float countRating = 0,
                sumRating = 0;

        if (jobCandidate.getIteractionList() != null) {
            for (IteractionList iteractionList : jobCandidate.getIteractionList()) {
                if (iteractionList.getRating() != null) {
                    countRating++;
                    sumRating = sumRating + iteractionList.getRating();
                }
            }
        }

        if (countRating != 0) {
            int avgRating = (int) (sumRating / countRating + 1);

            return starsAndOtherService.setStars(avgRating);
        } else
            return "";
    }

    @Install(to = "jobCandidatesTable.rating", subject = "styleProvider")
    private String jobCandidatesTableRatingStyleProvider(JobCandidate jobCandidate) {
//        String retStr = "rating";
        StringBuilder sb = new StringBuilder("rating");

        String avg = avgRating(jobCandidate);
        if (!avg.equals("")) {
            String s = avg.substring(0, 1);

            switch (s) {
                case "1":
                    sb.append("_red_")
                            .append(s);
//                    retStr = retStr + "_red_" + s;
                    break;
                case "2":
                    sb.append("_orange_")
                            .append(s);
//                    retStr = retStr + "_orange" + s;
                    break;
                case "3":
                    sb.append("_yellow_")
                            .append(s);
//                    retStr = retStr + "_yellow_" + s;
                    break;
                case "4":
                    sb.append("_green_")
                            .append(s);
//                    retStr = retStr + "_green_" + s;
                    break;
                case "5":
                    sb.append("_blue_")
                            .append(s);
//                    retStr = retStr + "_blue_" + s;
                    break;
                default:
                    break;
            }

            return sb.toString();
        } else
            return null;
    }

    private void setRatingField() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put(starsAndOtherService.setStars(1) + " Полный негатив", 0);
        map.put(starsAndOtherService.setStars(2) + " Сомнительно", 1);
        map.put(starsAndOtherService.setStars(3) + " Нейтрально", 2);
        map.put(starsAndOtherService.setStars(4) + " Положительно", 3);
        map.put(starsAndOtherService.setStars(5) + " Отлично!", 4);
        ratingFieldNotLower.setOptionsMap(map);

        ratingFieldNotLower.addValueChangeListener(e -> {
            if (ratingFieldNotLower.getValue() != null) {
                if (withCVCheckBox.getValue() != null) {
                    if (!withCVCheckBox.getValue()) {
                        jobCandidatesDl.setQuery("select e from itpearls_JobCandidate e "
                                + "order by e.secondName, e.firstName");
                    } else {
                        jobCandidatesDl.setQuery("select e from itpearls_JobCandidate e "
                                + "where e in (select f.candidate "
                                + "from itpearls_IteractionList f "
                                + "where f.candidate = e) "
                                + "and e in (select g.candidate from itpearls_IteractionList g where g.candidate = e and g.rating >= " + ratingFieldNotLower.getValue().toString() + ") "
                                + "order by e.secondName, e.firstName");
                    }
                } else {
                    jobCandidatesDl.setQuery("select e from itpearls_JobCandidate e "
                            + "order by e.secondName, e.firstName");
                }
            } else {
                if (withCVCheckBox.getValue() != null) {
                    if (!withCVCheckBox.getValue()) {
                        jobCandidatesDl.setQuery("select e from itpearls_JobCandidate e "
                                + "where e in (select g.candidate "
                                + "from itpearls_CandidateCV g "
                                + "where g.candidate = e) "
                                + "order by e.secondName, e.firstName");
                    } else {
                        jobCandidatesDl.setQuery("select e from itpearls_JobCandidate e "
                                + "where e in (select g.candidate "
                                + "from itpearls_CandidateCV g "
                                + "where g.candidate = e) "
                                + "and e in (select g.candidate from itpearls_IteractionList g where g.candidate = e and g.rating >= " + ratingFieldNotLower.getValue().toString() + ") "
                                + "order by e.secondName, e.firstName");
                    }
                } else {
                    jobCandidatesDl.setQuery("select e from itpearls_JobCandidate e "
                            + "where e in (select g.candidate "
                            + "from itpearls_CandidateCV g "
                            + "where g.candidate = e) "
                            + "order by e.secondName, e.firstName");
                }
            }

            jobCandidatesDl.load();
        });
    }

    @Subscribe("showOnlyWithMyParticipationCheckBox")
    public void onShowOnlyWithMyParticipationCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (event.getValue()) {
            jobCandidatesDl.setParameter("recrutier", userSession.getUser());
        } else {
            jobCandidatesDl.removeParameter("recrutier");
        }

        jobCandidatesDl.load();
    }

    public void quickLoadCVButton() {
    }

    public void onSendEmail() {
        InternalEmailerTemplateEdit screen = (InternalEmailerTemplateEdit) screenBuilders.editor(InternalEmailerTemplate.class, this)
                .newEntity()
                .withInitializer(e -> {
                    e.setFromEmail((ExtUser) userSession.getUser());
                    e.setToEmail(jobCandidatesTable.getSingleSelected());
                })
                .build();
        screen.setJobCandidate(jobCandidatesTable.getSingleSelected());
        screen.show();
    }

    protected FileDescriptor loadCVFileForQuickLoad() {
        AtomicReference<FileDescriptor> fileDescriptor = new AtomicReference<>();

/*        FileUploadDialog dialog = (FileUploadDialog) screens.create("fileUploadDialog", OpenMode.DIALOG);

        dialog.addCloseWithCommitListener(() -> {
            UUID fileId = dialog.getFileId();
            String fileName = dialog.getFileName();

            File file = fileUploadingAPI.getFile(fileId);

            fileDescriptor.set(fileUploadingAPI.getFileDescriptor(fileId, fileName));
            try {
                fileUploadingAPI.putFileIntoStorage(fileId, fileDescriptor.get());
                dataManager.commit(fileDescriptor.get());
            } catch (FileStorageException e) {
                throw new RuntimeException(e);
            }
        });

        screens.show(dialog); */

        return fileDescriptor.get();
    }

    public List<RenderedImage> getImagesFromResources(PDResources resources) throws IOException {
        List<RenderedImage> images = new ArrayList<>();

        for (COSName xObjectName : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(xObjectName);

            if (xObject instanceof PDFormXObject) {
                images.addAll(getImagesFromResources(((PDFormXObject) xObject).getResources()));
            } else if (xObject instanceof PDImageXObject) {
                images.add(((PDImageXObject) xObject).getImage());
            }
        }

        return images;
    }

    private String parsePdfCV(InputStream inputStream) {
        String parsedText = "";

        try {
            RandomAccessRead rad = new RandomAccessReadBuffer(inputStream);

            PDFParser parser = new PDFParser(rad);
            PDFTextStripper pdfStripper = new PDFTextStripper();
            PDDocument pdDoc = parser.parse();
            parsedText = pdfStripper.getText(pdDoc).replace("\n", "<br>");
        } catch (IOException e) {
            e.printStackTrace();

            return null;
        }

        return parsedText;
    }

    private String getTextCVFromPDF(FileDescriptor fileDescriptor) {
        List<RenderedImage> images = new ArrayList<>();
        String textResume = "";

        try {
            InputStream inputStream = fileLoader.openStream(fileDescriptor);

            textResume = parsePdfCV(inputStream);
            RandomAccessRead rad = new RandomAccessReadBuffer(fileLoader.openStream(fileDescriptor));

            PDFParser parser = new PDFParser(rad);
            PDFTextStripper pdfStripper = new PDFTextStripper();
            PDDocument pdDoc = parser.parse();

            for (PDPage page : pdDoc.getPages()) {
                images.addAll(getImagesFromResources(page.getResources()));
            }

            if (images.size() > 0) {
                SelectRenderedImagesFromList selectRenderedImagesFromList =
                        screens.create(SelectRenderedImagesFromList.class);
                selectRenderedImagesFromList.setRenderedImages(images);
// заголовок окна выбора                    selectRenderedImagesFromList.setCandidateCV(getEditedEntity());

                selectRenderedImagesFromList.addAfterCloseListener(evnt -> {
                    if (((SelectedCloseAction) evnt.getCloseAction()).getResult()
                            .equals(CandidateCV.SelectedCloseActionType.SELECTED)) {
                        Image selectedImage = selectRenderedImagesFromList
                                .getSelectedImage();

                        FileDescriptor fd = dataManager
                                .commit(selectRenderedImagesFromList.getSelectedImageFileDescriptor());

                        try {
                            fileUploadingAPI.putFileIntoStorage(fd.getUuid(), fd);
                        } catch (FileStorageException e) {
                            e.printStackTrace();
                        }

                        FileDescriptorResource resource = selectedImage
                                .createResource(FileDescriptorResource.class)
                                .setFileDescriptor(fd);

/* фото кандидата                            getEditedEntity().setFileImageFace(fd);

                            candidatePic
                                    .setSource(FileDescriptorResource.class)
                                    .setFileDescriptor(fd); */
                    }
                });

                selectRenderedImagesFromList.show();
            }
        } catch (FileStorageException | IOException | IllegalArgumentException e) {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withDescription("Ошибка распознавания документа " + fileDescriptor.getName())
                    .show();

            throw new RuntimeException(e);
        }

        if (textResume != null) {
            return textResume.replaceAll("\n", breakLine[0]);
        } else
            return null;
    }

    @Subscribe("quickLoadCV.loadFromPdf")
    public void onQuickLoadCVLoadFromPdf(Action.ActionPerformedEvent event) {
        screenOnlytextLoadFromPdf = screenBuilders.screen(this)
                .withScreenClass(OnlyTextPersonPositionLoadPdf.class)
                .withOpenMode(OpenMode.NEW_TAB)
                .build();

        AtomicReference<JobCandidate> loadedJobCandidate = null;

        screenOnlytextLoadFromPdf.addAfterCloseListener(afterCloseEvent -> {
        });

        screenOnlytextLoadFromPdf.show();
    }

    @Subscribe("quickLoadCV.loadFromClipboard")
    public void onQuickLoadCVLoadFromClipboard(Action.ActionPerformedEvent event) {
        screenOnlytext = screenBuilders.screen(this)
                .withScreenClass(OnlyTextPersonPosition.class)
                .withOpenMode(OpenMode.NEW_TAB)
                .build();

        AtomicReference<JobCandidate> loadedJobCandidate = null;

        screenOnlytext.addAfterCloseListener(afterCloseEvent -> {

            Screen jobCandidateEdit = screenBuilders.editor(JobCandidate.class, this)
                    .withOpenMode(OpenMode.NEW_TAB)
                    .withScreenClass(JobCandidateEdit.class)
                    .withAfterCloseListener(eventAfterClose -> {
                        // тут бы установить на вновь сделанную строку курсор
                    })
                    .withInitializer(e -> {
                        if (!((OnlyTextPersonPosition) screenOnlytext).getCancel()) {
                            String textCV = ((OnlyTextPersonPosition) screenOnlytext).getResultText();
                            // нашел ФИО
                            if (textCV != null) {
                                if (!textCV.equals("")) {
                                    selectFirstNames(textCV, e);
                                    selectMiddleNames(textCV, e);
                                    selectSecondNames(textCV, e);

                                    e.setEmail(parseCVService
                                            .parseEmail(textCV));
                                    e.setPhone(parseCVService
                                            .parsePhone(textCV));
                                    e.setBirdhDate(parseCVService
                                            .parseDate(textCV));
                                    e.setCurrentCompany(parseCVService
                                            .parseCompany(textCV));
                                    e.setCityOfResidence(parseCVService
                                            .parseCity(textCV));
                                    e.setPersonPosition(((OnlyTextPersonPosition) screenOnlytext)
                                            .getPersonPosition());
                                    e.setTelegramName(parseCVService
                                            .parseTelegram(textCV));
                                    e.setSkypeName(parseCVService
                                            .parseSkype(textCV));
//                                initSocialNeiworkTable(e);
                                    addSocialNetworkList(e, ((OnlyTextPersonPosition) screenOnlytext).getResultText());

                                    CandidateCV candidateCV = metadata.create(CandidateCV.class);
                                    candidateCV.setResumePosition(e.getPersonPosition());
                                    candidateCV.setTextCV(((OnlyTextPersonPosition) screenOnlytext).getResultText());
                                    candidateCV.setOwner(userSession.getUser());
                                    candidateCV.setCandidate(e);
                                    candidateCV.setDatePost(new Date());

                                    List<CandidateCV> candidateCVS = new ArrayList<>();
                                    candidateCVS.add(candidateCV);
                                    dataContext.merge(candidateCV);
                                    e.setCandidateCv(candidateCVS);
                                    dataContext.merge(e);

                                    candidateCVEdit = screenBuilders.editor(CandidateCV.class, this)
                                            .withScreenClass(CandidateCVEdit.class)
                                            .withAddFirst(true)
                                            .editEntity(candidateCV)
                                            .withOpenMode(OpenMode.DIALOG)
                                            .build();

                                }
                            }
                        }
                    })
                    .newEntity()
                    .build();

            jobCandidateEdit.addAfterShowListener(eventJobCandidateEdit -> {
                updateSuggestionFieldsScreenQuickLoadedJobCandidates(eventJobCandidateEdit);
                openScreenCVQuickLoadedCandidate(eventJobCandidateEdit, candidateCVEdit);
            });

            ((JobCandidateEdit) jobCandidateEdit).repaintSocialNetworksTable();

            jobCandidateEdit.show();
        });

        screenOnlytext.show();
    }

    private void initSocialNeiworkTable(JobCandidate e) {
        if (e.getSocialNetwork() == null) {
            List<SocialNetworkType> socialNetworkTypes = dataManager.load(SocialNetworkType.class)
                    .view("socialNetworkType-view")
                    .list();

            List<SocialNetworkURLs> networks = new ArrayList<>();

            for (SocialNetworkType socialNetworkType : socialNetworkTypes) {
                SocialNetworkURLs sn = metadata.create(SocialNetworkURLs.class);
                sn.setSocialNetworkURL(socialNetworkType);
                sn.setJobCandidate(e);
                sn.setNetworkName(socialNetworkType.getSocialNetwork());
                networks.add(sn);
            }

            e.setSocialNetwork(networks);
            dataContext.merge(networks);

            jobCandidatesDl.load();
        }
    }

    private SocialNetworkType getOtherSocialNetwork() {
        SocialNetworkType socialNetworkType = null;

        try {
            socialNetworkType = dataManager.load(SocialNetworkType.class)
                    .query(QUERY_GET_OTHER_SOCIAL_NETWORK)
                    .parameter("other", "Other")
                    .view("socialNetworkType-view")
                    .cacheable(true)
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

    private void addSocialNetworkList(JobCandidate e, String resultText) {
        Set<String> socialNetworks = parseCVService.scanSocialNetworksFromCVs(resultText);
        List<SocialNetworkType> socialNetworkTypes = dataManager.load(SocialNetworkType.class)
                .view("socialNetworkType-view")
                .list();
        SocialNetworkType otherSocialNetworkType = getOtherSocialNetwork();

        for (String network : socialNetworks) {
            URI uriCandidate = null;
            URI uriNetworTypes = null;
            SocialNetworkType realSocialNetwork = null;
            Boolean flag = false;

            try {
                uriCandidate = new URI(network);
            } catch (URISyntaxException uriSyntaxException) {
                uriSyntaxException.printStackTrace();
            }

            for (SocialNetworkType socialNetworkType : socialNetworkTypes) {
                try {
                    uriNetworTypes = new URI(socialNetworkType.getSocialNetworkURL());
                } catch (URISyntaxException uriSyntaxException) {
                    uriSyntaxException.printStackTrace();
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
                if (realSocialNetwork != null) {
                    SocialNetworkURLs socialNetworkURLs = metadata.create(SocialNetworkURLs.class);
                    socialNetworkURLs.setJobCandidate(e);
                    socialNetworkURLs.setSocialNetworkURL(realSocialNetwork);
                    socialNetworkURLs.setNetworkURLS(network);

                    if (e.getSocialNetwork() == null) {
                        e.setSocialNetwork(new ArrayList<>());
                    }

                    // dataContext.merge(socialNetworkURLs);
                    e.getSocialNetwork().add(socialNetworkURLs);
                }
            } else {
                SocialNetworkURLs socialNetworkURLs = metadata.create(SocialNetworkURLs.class);
                socialNetworkURLs.setJobCandidate(e);
                socialNetworkURLs.setSocialNetworkURL(otherSocialNetworkType);
                socialNetworkURLs.setNetworkURLS(network);

                if (e.getSocialNetwork() == null) {
                    e.setSocialNetwork(new ArrayList<>());
                }

                e.getSocialNetwork().add(socialNetworkURLs);
            }
        }
    }

    private void setSocialNetworkList(JobCandidate e, String resultText) {
        Set<String> socialNetworks = parseCVService.scanSocialNetworksFromCVs(resultText);
        List<SocialNetworkType> socialNetworkTypes = dataManager.load(SocialNetworkType.class)
                .view("socialNetworkType-view")
                .list();

        for (String network : socialNetworks) {
            for (SocialNetworkURLs networkURL : e.getSocialNetwork()) {
                URI uriCandidate = null;
                URI uriNetworTypes = null;

                try {
                    uriNetworTypes = new URI(networkURL
                            .getSocialNetworkURL()
                            .getSocialNetworkURL());
                } catch (URISyntaxException uriSyntaxException) {
                    uriSyntaxException.printStackTrace();
                }

                try {
                    uriCandidate = new URI(network);
                } catch (URISyntaxException uriSyntaxException) {
                    uriSyntaxException.printStackTrace();
                }

                String hostCandidateFromCV = uriCandidate.getHost();
                String hostNetworkTypes = uriNetworTypes.getHost();

                if (hostCandidateFromCV.equals(hostNetworkTypes)) {
                    if (networkURL.getSocialNetworkURL() != null || !networkURL.getSocialNetworkURL().equals("")) {
                        networkURL.setNetworkURLS(network);
                        networkURL.setJobCandidate(e);
                        networkURL.setNetworkName(hostNetworkTypes);
                    } else {
                        notifications.create(Notifications.NotificationType.WARNING)
                                .withPosition(Notifications.Position.BOTTOM_RIGHT)
                                .withHideDelayMs(15000)
                                .withStyleName("create-jobcandidate-warning")
                                .withDescription(messageBundle
                                        .getMessage("msgAdditionalSocialNetwork")
                                        + network)
                                .show();

                    }
                }
            }
        }
    }

    private void selectFirstNames(String textCV, JobCandidate e) {
        List<String> namesList = parseCVService.getFirstNameList(textCV);
        StringBuffer names = new StringBuffer();

        if (namesList.size() > 1) {
            for (String name : namesList) {

                if (names.length() != 0) {
                    names.append(", ");
                }

                names.append(name);
            }

            notifications.create(Notifications.NotificationType.TRAY)
                    .withType(Notifications.NotificationType.TRAY)
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withHideDelayMs(15000)
                    .withStyleName("create-jobcandidate-warning")
                    .withDescription(messageBundle.getMessage("msgSeveralVariantsFirstNames")
                            .concat(names.toString())
                            .concat("\n")
                            .concat(messageBundle.getMessage("msgAddedFirst")))
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .show();

            e.setFirstName(namesList.get(0));
        } else {
            if (namesList.size() == 0) {
                notifications.create(Notifications.NotificationType.TRAY)
                        .withType(Notifications.NotificationType.TRAY)
                        .withHideDelayMs(15000)
                        .withStyleName("create-jobcandidate-warning")
                        .withCaption(messageBundle.getMessage("msgWarning"))
                        .withDescription(messageBundle.getMessage("msgNotFoundFirstName"))
                        .withPosition(Notifications.Position.BOTTOM_RIGHT)
                        .show();
            } else {
                e.setFirstName(namesList.get(0));
            }
        }
    }


    private void selectMiddleNames(String textCV, JobCandidate e) {
        List<String> namesList = parseCVService.getMiddleNameList(textCV);
        StringBuffer names = new StringBuffer();

        if (namesList.size() > 1) {
            for (String name : namesList) {

                if (names.length() != 0) {
                    names.append(", ");
                }

                names.append(name);
            }

            notifications.create(Notifications.NotificationType.TRAY)
                    .withType(Notifications.NotificationType.TRAY)
                    .withHideDelayMs(15000)
                    .withStyleName("create-jobcandidate-warning")
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withDescription(messageBundle.getMessage("msgSeveralVariantsMiddleNames")
                            .concat(names.toString())
                            .concat("\n")
                            .concat(messageBundle.getMessage("msgAddedFirst")))
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .show();

            e.setMiddleName(namesList.get(0));
        } else {
            if (namesList.size() == 0) {
                notifications.create(Notifications.NotificationType.TRAY)
                        .withType(Notifications.NotificationType.TRAY)
                        .withStyleName("create-jobcandidate-warning")
                        .withCaption(messageBundle.getMessage("msgWarning"))
                        .withDescription(messageBundle.getMessage("msgNotFoundMiddleName"))
                        .withPosition(Notifications.Position.BOTTOM_RIGHT)
                        .withHideDelayMs(15000)
                        .show();
            } else {
                e.setMiddleName(namesList.get(0));
            }
        }
    }

    private void selectSecondNames(String textCV, JobCandidate e) {
        List<String> namesList = parseCVService.getSecondNameList(textCV);
        StringBuffer names = new StringBuffer();

        if (namesList.size() > 1) {
            for (String name : namesList) {

                if (names.length() != 0) {
                    names.append(", ");
                }

                names.append(name);
            }

            notifications.create(Notifications.NotificationType.TRAY)
                    .withType(Notifications.NotificationType.TRAY)
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withStyleName("create-jobcandidate-warning")
                    .withHideDelayMs(15000)
                    .withDescription(messageBundle.getMessage("msgSeveralVariantsSecondNames")
                            .concat(names.toString())
                            .concat("\n")
                            .concat(messageBundle.getMessage("msgAddedFirst")))
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .show();

            StringBuffer buff = new StringBuffer(namesList.get(0));
            e.setSecondName(buff.toString());
        } else {
            if (namesList.size() == 0) {
                notifications.create(Notifications.NotificationType.TRAY)
                        .withType(Notifications.NotificationType.TRAY)
                        .withStyleName("create-jobcandidate-warning")
                        .withHideDelayMs(15000)
                        .withCaption(messageBundle.getMessage("msgWarning"))
                        .withDescription(messageBundle.getMessage("msgNotFoundSecondName"))
                        .withPosition(Notifications.Position.BOTTOM_RIGHT)
                        .show();
            } else {
                StringBuffer buff = new StringBuffer(namesList.get(0));
                e.setSecondName(buff.toString());
            }
        }
    }

    private void updateSuggestionFieldsScreenQuickLoadedJobCandidates(AfterShowEvent eventJobCandidateEdit) {
        ((SuggestionField<Object>) eventJobCandidateEdit
                .getSource()
                .getWindow()
                .getComponentNN("firstNameField"))
                .setSearchExecutor((searchString, searchParams) ->
                        parseCVService.getFirstNameList(
                                ((Onlytext) screenOnlytext).getResultText())
                                .stream()
                                .filter(str ->
                                        StringUtils
                                                .containsAnyIgnoreCase(str, searchString))
                                .collect(Collectors.toList()));

        ((SuggestionField<Object>) eventJobCandidateEdit
                .getSource()
                .getWindow()
                .getComponentNN("secondNameField"))
                .setSearchExecutor((searchString, searchParams) ->
                        parseCVService.getSecondNameList(
                                ((Onlytext) screenOnlytext).getResultText())
                                .stream()
                                .filter(str ->
                                        StringUtils
                                                .containsAnyIgnoreCase(str, searchString))
                                .collect(Collectors.toList()));
    }

    private void openScreenCVQuickLoadedCandidate(AfterShowEvent eventJobCandidateEdit,
                                                  CandidateCVEdit candidateCVEdit) {
        DataContext dataContext = UiControllerUtils.getScreenData(eventJobCandidateEdit
                .getSource()
                .getWindow()
                .getFrame()
                .getFrameOwner())
                .getDataContext();

        candidateCVEdit.setParentDataContext(dataContext);
        candidateCVEdit.show();
    }

    @Install(to = "jobCandidatesTable.fileImageFace", subject = "descriptionProvider")
    private String jobCandidatesTableFileImageFaceDescriptionProvider(JobCandidate jobCandidate) {
        FileDescriptor fd = jobCandidate.getFileImageFace();
        return getImage(fd);
    }

    private String getImage(FileDescriptor fd) {
        //UUID id = UuidProvider.fromString("f5fb2eef-bf8f-af1d-dfed-5b381001579f");
        byte[] image;

        if (fd != null) {
            if (fd.getCreateDate() == null) {
                fd.setCreateDate(new Date());
            }

            try {
                image = fileStorageService.loadFile(fd);
            } catch (FileStorageException e) {
                return "";
            }

            Base64.Encoder encoder = Base64.getEncoder();
            String encodedString = encoder.encodeToString(image);

            return getMailHTMLHeader()
                    + "\n<img src=\"data:image/" + fd.getExtension() + ";base64, " +
                    encodedString +
                    "\"" +
                    " width=\"220\" height=\"292\">\n"
                    + getMailHTMLFooter();
        } else
            return null;
    }


    private String getMailHTMLFooter() {
        return "</body>\n" +
                "</html>";
    }

    private String getMailHTMLHeader() {
        return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
                "<html>\n" +
                "<head>\n" +
                "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" >\n" +
                "<title></title>\n" +
                "<style type=\"text/css\">\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>";
    }
}