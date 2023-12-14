package com.company.itpearls.web.screens.internalemailer;

import com.company.itpearls.core.InteractionService;
import com.company.itpearls.core.OpenPositionService;
import com.company.itpearls.core.StrSimpleService;
import com.company.itpearls.entity.*;
import com.company.itpearls.web.screens.personelreserve.PersonelReserveEdit;
import com.company.itpearls.web.screens.signicons.SignIconsBrowse;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundTaskHandler;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.*;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.global.UserSession;
import com.vaadin.server.Page;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@UiController("itpearls_InternalEmailer.browse")
@UiDescriptor("internal-emailer-browse.xml")
@LookupComponent("emailersTable")
@LoadDataBeforeShow
public class InternalEmailerBrowse extends StandardLookup<InternalEmailer> {
    @Inject
    private CheckBox onlyMyLettersCheckBox;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private DataManager dataManager;
    @Inject
    private DataGrid<InternalEmailer> emailersTable;
    @Inject
    private CollectionLoader<InternalEmailer> emailersDl;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private UserSession userSession;
    @Inject
    private Metadata metadata;
    @Inject
    private DataContext dataContext;

    private static final String QUERY_GET_PERSONEL_RESERVE =
            "select e from itpearls_PersonelReserve e where e.jobCandidate = :jobCandidate and (e.endDate > :currDate or e.endDate is null)";
    private static final String QUERY_GET_DEFAULT_VACANCY =
            "select e from itpearls_OpenPosition e where e.vacansyName like \'Default\'";
    private final String QUERY_GET_SIGN_PERSONAL_RESERVE_PUT_INTERACTION =
            "select e from itpearls_Iteraction e where e.signPersonalReservePut = true";
    static final String QUERY_DEFAULT_OPEN_POSITION =
            "select e from itpearls_OpenPosition e where e.vacansyName like 'Default'";
    private static final String QUERY_GET_JOB_CANDIDATE_SIGN_ICONS =
            "select e from itpearls_JobCandidateSignIcon e where e.jobCandidate = :jobCandidate";
    private static final String QUERY_GET_REPLY =
            "select e from itpearls_InternalEmailer e where e.replyInternalEmailer = :replyInternalEmailer";
    private static final String QUERY_GET_COUNT_INTERACTION =
            "select e from itpearls_IteractionList e where e.numberIteraction = (select max(f.numberIteraction) from itpearls_IteractionList f)";

    private PersonelReserve currentPersonelReserve = null;
    static final String separatorChar = "⎯";
    final static String separator = separatorChar.repeat(22);
    final static String pic_center_large = "pic-center-large-";
    final static String pic_center_large_inject_css = ".pic-center-large-%s {color: #%s; text-align: center; text-color: gray; font-size: large; margin: 0 auto;}";

    @Inject
    private Notifications notifications;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private StrSimpleService strSimpleService;
    @Inject
    private CollectionContainer<SignIcons> signIconsDc;
    @Inject
    private CollectionLoader<SignIcons> signIconsDl;
    @Inject
    private PopupButton signFilterButton;
    @Inject
    private InteractionService interactionService;
    @Inject
    private OpenPositionService openPositionService;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        onlyMyLettersCheckBox.setValue(true);
        initSignIconsDataContainer();
        initSignFilterPopupButton();
    }


    private void initSignFilterPopupButton() {
        for (SignIcons icons : signIconsDc.getItems()) {
            StringBuilder sb = new StringBuilder(icons.getTitleEnd());
            sb.append(strAction);
            signFilterButton.addAction(new BaseAction(
                    strSimpleService.deleteExtraCharacters(sb.toString()))
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
                    screen.setParentJobCandidateTable(emailersTable);

                    screen.show();
                }));
    }


    private void removeSignFilterAction() {
        emailersDl.removeParameter("signIcon");
        emailersDl.load();
    }

    private void setSignFilter(SignIcons icons) {
        removeSignFilterAction();
        emailersDl.setParameter("signIcon", icons);
        emailersDl.load();
    }

    @Subscribe("onlyMyLettersCheckBox")
    public void onOnlyMyLettersCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (onlyMyLettersCheckBox.getValue()) {
            emailersDl.setParameter("fromEmail", userSession.getUser());
        } else {
            emailersDl.removeParameter("fromEmail");
        }

        emailersDl.load();
    }

    @Inject
    protected BackgroundWorker backgroundWorker;

    @Install(to = "emailersTable.replyInternalEmailerColumn", subject = "columnGenerator")
    private Component emailersTableReplyInternalEmailerColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<InternalEmailer> event) {
        HBoxLayout retHbox = getReplyHBox(event);

        return retHbox;
    }

    private HBoxLayout getReplyHBox(DataGrid.ColumnGeneratorEvent<InternalEmailer> event) {
        final HBoxLayout[] retHbox = {uiComponents.create(HBoxLayout.class)};
        retHbox[0].setHeightFull();
        retHbox[0].setWidthFull();
        retHbox[0].setSpacing(true);

        final ProgressBar[] progressBar = {uiComponents.create(ProgressBar.class)};
        progressBar[0].setWidthFull();
        progressBar[0].setHeightAuto();
        progressBar[0].setIndeterminate(true);
        progressBar[0].setVisible(true);
        progressBar[0].setAlignment(Component.Alignment.MIDDLE_CENTER);
        retHbox[0].add(progressBar[0]);

        final Label[] replyLabel = {uiComponents.create(Label.class)};
        replyLabel[0].setAlignment(Component.Alignment.MIDDLE_CENTER);
        replyLabel[0].setStyleName("h1-green");
        replyLabel[0].setVisible(false);
        retHbox[0].add(replyLabel);

        final Label[] signIconLabel = {uiComponents.create(Label.class)};
        signIconLabel[0].setVisible(false);
        retHbox[0].add(signIconLabel[0]);

        BackgroundTask<Integer, Void> task = new BackgroundTask<Integer, Void>(10, this) {
            List<InternalEmailer> internalEmailer;
            List<JobCandidateSignIcon> jobCandidateSignIcons;

            @Override
            public Void run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {

                internalEmailer = dataManager
                        .load(InternalEmailer.class)
                        .query(QUERY_GET_REPLY)
                        .parameter("replyInternalEmailer", event.getItem())
                        .cacheable(true)
                        .view("internalEmailer-view")
                        .list();

                jobCandidateSignIcons = dataManager.load(JobCandidateSignIcon.class)
                        .query(QUERY_GET_JOB_CANDIDATE_SIGN_ICONS)
                        .parameter("jobCandidate", event.getItem().getToEmail())
                        .view("jobCandidateSignIcon-view")
                        .cacheable(true)
                        .list();


                return null;
            }

            @Override
            public void canceled() {
                progressBar[0].setVisible(false);

                replyLabel[0].setVisible(true);
                replyLabel[0].setIconFromSet(CubaIcon.CANCEL);
            }

            @Override
            public void done(Void result) {
                progressBar[0].setVisible(false);

                if (internalEmailer.size() > 0) {
                    replyLabel[0].setVisible(true);
                    replyLabel[0].setIconFromSet(CubaIcon.MAIL_REPLY);
                } else {
                    replyLabel[0].setVisible(false);
                }

                if (jobCandidateSignIcons.size() > 0) {
                    StringBuilder sb = new StringBuilder(pic_center_large);
                    sb.append(jobCandidateSignIcons.get(0).getSignIcon().getIconColor());

                    signIconLabel[0].setAlignment(Component.Alignment.MIDDLE_CENTER);
                    signIconLabel[0].setIcon(jobCandidateSignIcons.get(0).getSignIcon().getIconName());

                    if (jobCandidateSignIcons.get(0).getSignIcon().getTitleDescription() != null) {
                        signIconLabel[0].setDescription(jobCandidateSignIcons.get(0).getSignIcon().getTitleDescription());
                    } else {
                        signIconLabel[0].setDescription(jobCandidateSignIcons.get(0).getSignIcon().getTitleRu());
                    }

                    injectColorCss(jobCandidateSignIcons.get(0).getSignIcon().getIconColor());
                    signIconLabel[0].setStyleName(sb.toString());

                    signIconLabel[0].setVisible(true);
                } else {
                    signIconLabel[0].setVisible(false);
                }
            }
        };

        BackgroundTaskHandler taskHandler = backgroundWorker.handle(task);
        taskHandler.execute();

        return retHbox[0];
    }

    private Label setSelectionLabel(InternalEmailer item) {
        Label star = uiComponents.create(Label.class);
        star.setIconFromSet(CubaIcon.STAR);
        star.setAlignment(Component.Alignment.MIDDLE_LEFT);
        star.setStyleName("pic-center-large-orange");


        if (item.getSelectedForAction() != null) {
            if (item.getSelectionSymbolForActions() == null) {
                if (item.getSelectedForAction()) {
                    star.setVisible(true);
                } else {
                    star.setVisible(false);
                }
            } else {
                StdSelections s = StdSelections.fromId(item.getSelectionSymbolForActions());

                switch (s) {
                    case STAR_RED:
                        star.setIconFromSet(CubaIcon.STAR);
                        star.setStyleName(StdSelectionsColor.STAR_RED.getId());
                        break;
                    case STAR_YELLOW:
                        star.setIconFromSet(CubaIcon.STAR);
                        star.setStyleName(StdSelectionsColor.STAR_YELLOW.getId());
                        break;
                    case STAR_GREEN:
                        star.setIconFromSet(CubaIcon.STAR);
                        star.setStyleName(StdSelectionsColor.STAR_GREEN.getId());
                        break;
                    case FLAG_RED:
                        star.setIconFromSet(CubaIcon.FLAG);
                        star.setStyleName(StdSelectionsColor.FLAG_RED.getId());
                        break;
                    case FLAG_YELLOW:
                        star.setIconFromSet(CubaIcon.FLAG);
                        star.setStyleName(StdSelectionsColor.FLAG_YELLOW.getId());
                        break;
                    case FLAG_GREEN:
                        star.setIconFromSet(CubaIcon.FLAG);
                        star.setStyleName(StdSelectionsColor.FLAG_GREEN.getId());
                        break;
                    default:
                        star.setIconFromSet(CubaIcon.STAR);
                        star.setStyleName(StdSelectionsColor.STAR_YELLOW.getId());
                        break;
                }
            }
        } else {
            star.setVisible(false);
        }

        return star;
    }

    @Install(to = "emailersTable.toEmail", subject = "columnGenerator")
    private Component emailersTableToEmailColumnGenerator(DataGrid.ColumnGeneratorEvent<InternalEmailer> event) {
        if (event.getItem().getToEmail().getFileImageFace() != null) {
            return generateImageWithLabel(event.getItem().getToEmail().getFileImageFace(),
                    event.getItem().getToEmail().getFullName());
        } else {
            return generateImageWithLabel(null, event.getItem().getToEmail().getFullName());
        }
    }

    @Install(to = "emailersTable.fromEmail", subject = "columnGenerator")
    private Component emailersTableFromEmailColumnGenerator(DataGrid.ColumnGeneratorEvent<InternalEmailer> event) {
        if (event.getItem().getFromEmail().getFileImageFace() != null) {
            return generateImageWithLabel(event.getItem().getFromEmail().getFileImageFace(),
                    event.getItem().getFromEmail().getName());
        } else {
            return generateImageWithLabel(null, event.getItem().getFromEmail().getName());
        }
    }

    private final static String width_50px = "50px";
    private final static String width_20px = "20px";
    private final static String height_50px = "50px";
    private final static String height_20px = "20px";
    private final static String icons_no_company_png = "icons/no-company.png";
    private final static String icons_no_programmer_jpeg = "icons/no-programmer.jpeg";
    private final static String style_table_wordwrap = "table-wordwrap";
    private final static String style_circle_20px = "circle-20px";

    private HBoxLayout generateImageWithLabel(FileDescriptor fileDescriptor, String name) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();
        retHBox.setHeightFull();
        retHBox.setSpacing(true);

        Image image = uiComponents.create(Image.class);
        image.setWidth(width_20px);
        image.setHeight(height_20px);
        image.setAlignment(Component.Alignment.MIDDLE_CENTER);
        image.setScaleMode(Image.ScaleMode.SCALE_DOWN);

        if (fileDescriptor != null) {
            image.setSource(FileDescriptorResource.class)
                    .setFileDescriptor(fileDescriptor);
        } else {
            image.setSource(ThemeResource.class)
                    .setPath(icons_no_programmer_jpeg);
        }

        image.setStyleName(style_circle_20px);

        Label label = uiComponents.create(Label.class);
        label.setValue(name);
        label.setStyleName(style_table_wordwrap);
        label.setAlignment(Component.Alignment.MIDDLE_LEFT);

        retHBox.add(image);
        retHBox.add(label);

        retHBox.expand(label);

        return retHBox;
    }


    private void selectForAction(StdSelections star) {
        InternalEmailer internalEmailer = emailersTable.getSingleSelected();
        internalEmailer.setSelectedForAction(true);
        internalEmailer.setSelectionSymbolForActions(star.getId());
        dataManager.commit(internalEmailer);

        emailersDl.load();
        emailersTable.repaint();
        try {
            emailersTable.setSelected(internalEmailer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearSelection() {
        InternalEmailer internalEmailer = emailersTable.getSingleSelected();
        internalEmailer.setSelectedForAction(null);
        internalEmailer.setSelectionSymbolForActions(null);
        dataManager.commit(internalEmailer);

        emailersDl.load();
        emailersTable.repaint();
        try {
            emailersTable.setSelected(internalEmailer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    final static String strAction = "Action";

    private void setActionToActionPopupButton(PopupButton actionButton, InternalEmailer internalEmailer) {
        actionButton.addAction(new BaseAction("editJobCandidateCard")
                .withIcon(CubaIcon.USER.source())
                .withCaption(messageBundle.getMessage("msgCandidate"))
                .withHandler(actionPerformedEvent -> editJobCandidateAction(internalEmailer)));

        actionButton.addAction(new BaseAction("replyEmail")
                .withIcon(CubaIcon.REPLY.source())
                .withCaption(messageBundle.getMessage("msgReplyEmail"))
                .withHandler(actionPerformedEvent -> resendEmailAction(internalEmailer)));

        actionButton.addAction(new BaseAction("addInteraction")
                .withIcon(CubaIcon.REORDER.source())
                .withCaption(messageBundle.getMessage("msgAddInteraction"))
                .withHandler(actionPerformedEvent -> addNewInteractionAction(internalEmailer)));

        actionButton.addAction(new BaseAction("separator1Action")
                .withCaption(separator));
        actionButton.getAction("separator1Action").setEnabled(false);

        actionButton.addAction(new BaseAction("addPersonalReserve")
                .withIcon(CubaIcon.ADD_TO_SET_ACTION.source())
                .withCaption(messageBundle.getMessage("msgAddPersonalReserve"))
                .withHandler(actionPerformedEvent -> {
                    emailersTable.setSelected((InternalEmailerTemplate) internalEmailer);

                    screenBuilders.editor(PersonelReserve.class, this)
                            .withScreenClass(PersonelReserveEdit.class)
                            .newEntity()
                            .withInitializer(event -> {
                                event = setToPersonelReserve(event);

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
                    emailersTable.scrollTo(emailersTable.getSingleSelected());
                }));

        actionButton.addAction(new BaseAction("addPersonalReserveWithoutConfirm")
                .withIcon(CubaIcon.ADD_TO_SET_ACTION.source())
                .withCaption(messageBundle.getMessage("msgAddPersonalReserveWithoutConfirm"))
                .withHandler(actionPerformedEvent -> {
                    emailersTable.setSelected((InternalEmailerTemplate) internalEmailer);
                    if (emailersTable.getSingleSelected() != null) {
                        putCandidatesToPersonelReserve(emailersTable.getSingleSelected());

                        emailersTable.scrollTo(emailersTable.getSingleSelected());
                    }
                }));

        actionButton.addAction(new BaseAction("separator2Action")
                .withCaption(separator));
        actionButton.getAction("separator2Action").setEnabled(false);

        for (SignIcons icons : signIconsDc.getItems()) {
            StringBuffer sb = new StringBuffer(icons.getTitleEnd());
            sb.append(strAction);
            actionButton.addAction(new BaseAction(
                    strSimpleService.deleteExtraCharacters(sb.toString()))
                    .withIcon(icons.getIconName())
                    .withCaption(icons.getTitleRu())
                    .withDescription(icons.getTitleDescription())
                    .withHandler(actionPerformedAction -> {
                        emailersTable.setSelected((InternalEmailerTemplate) internalEmailer);
                        setSignIcons(icons, emailersTable.getSingleSelected());
                    }));
        }

        actionButton.addAction(new BaseAction("removeSignAction")
                .withIcon(CubaIcon.REMOVE_ACTION.source())
                .withCaption(messageBundle.getMessage("msgRemoveSignAction"))
                .withDescription(messageBundle.getMessage("msgRemoveSignActionDesc"))
                .withHandler(actionPerformedAction -> {
                    emailersTable.setSelected((InternalEmailerTemplate) internalEmailer);
                    removeSignAction(emailersTable.getSingleSelected());
                }));

        actionButton.addAction(new BaseAction("separator3Action")
                .withCaption(separator));

        actionButton.addAction(new BaseAction("editSignIconsAction")
                .withCaption(messageBundle.getMessage("msgEditSignIconsAction"))
                .withDescription("msgEditSignIconsActionDesc")
                .withIcon(CubaIcon.FONTICONS.source())
                .withHandler(actionPerformedAction -> {
                    emailersTable.setSelected((InternalEmailerTemplate) internalEmailer);
                    SignIconsBrowse screen = (SignIconsBrowse) screenBuilders.lookup(SignIcons.class, this)
                            .withOpenMode(OpenMode.DIALOG)
                            .build();
                    screen.setParentJobCandidateTable(emailersTable);

                    screen.show();
                }));
    }

    private void removeSignAction(InternalEmailer internalEmailer) {
        List<JobCandidateSignIcon> jobCandidateSignIcons = dataManager.load(JobCandidateSignIcon.class)
                .query(QUERY_GET_JOB_CANDIDATE_SIGN_ICONS)
                .parameter("jobCandidate", internalEmailer.getToEmail())
                .view("jobCandidateSignIcon-view")
                .cacheable(true)
                .list();

        if (jobCandidateSignIcons.size() > 0) {
            for (JobCandidateSignIcon jcsi : jobCandidateSignIcons) {
                dataManager.remove(jcsi);
            }
        }

        emailersTable.repaint();
        emailersTable.setSelected(internalEmailer);
        emailersTable.scrollTo(internalEmailer);
    }

    private void setSignIcons(SignIcons icons, InternalEmailer internalEmailer) {
        List<JobCandidateSignIcon> jobCandidateSignIcon;

        jobCandidateSignIcon = dataManager.load(JobCandidateSignIcon.class)
                .query(QUERY_GET_JOB_CANDIDATE_SIGN_ICONS)
                .parameter("jobCandidate", internalEmailer.getToEmail())
                .cacheable(true)
                .view("jobCandidateSignIcon-view")
                .list();

        if (jobCandidateSignIcon.size() == 0) {
            JobCandidateSignIcon jcsi = metadata.create(JobCandidateSignIcon.class);
            jcsi.setJobCandidate(internalEmailer.getToEmail());
            jcsi.setSignIcon(icons);
            jcsi.setUser((ExtUser) userSession.getUser());

            dataManager.commit(jcsi);
        } else {
            jobCandidateSignIcon.get(0).setSignIcon(icons);
            dataManager.commit(jobCandidateSignIcon.get(0));
        }

        emailersTable.repaint();
        emailersTable.setSelected(internalEmailer);
        emailersTable.scrollTo(internalEmailer);
    }

    private void initSignIconsDataContainer() {
        signIconsDl.setParameter("user", (ExtUser) userSession.getUser());
        signIconsDl.load();
    }

    protected void putCandidatesToPersonelReserve(InternalEmailer singleSelected) {
        putCandidatesToPersonelReserve(singleSelected, getDefaultOpenPosition());
    }

    protected PersonelReserve setToPersonelReserve(PersonelReserve event) {
        event.setRecruter((ExtUser) userSession.getUser());
        event.setJobCandidate(emailersTable.getSingleSelected().getToEmail());
        event.setSelectionSymbolForActions(emailersTable
                .getSingleSelected()
                .getSelectionSymbolForActions());
        event.setPersonPosition(emailersTable
                .getSingleSelected()
                .getToEmail()
                .getPersonPosition());
        event.setRemovedFromReserve(false);
        event.setInProcess(true);
        return event;
    }

    protected OpenPosition getDefaultOpenPosition() {
        OpenPosition openPositionDefault = null;

        try {
            openPositionDefault = dataManager.load(OpenPosition.class)
                    .query(QUERY_DEFAULT_OPEN_POSITION)
                    .view("openPosition-view")
                    .cacheable(true)
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


    protected void putCandidatesToPersonelReserve(InternalEmailer internalEmailer, OpenPosition openPosition) {

        PersonelReserve personelReserveCheck = null;

        try {
            personelReserveCheck = dataManager.load(PersonelReserve.class)
                    .query(QUERY_GET_PERSONEL_RESERVE)
                    .view("personelReserve-view")
                    .parameter("jobCandidate", internalEmailer.getToEmail())
                    .parameter("currDate", new Date())
                    .cacheable(true)
                    .one();
        } catch (IllegalStateException e) {
            personelReserveCheck = null;
        }

        if (personelReserveCheck == null) {
            addPersonaLReserveMonth(internalEmailer, openPosition);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy");

            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withDescription(new StringBuilder()
                            .append(messageBundle.getMessage("msgCanNotAddToPersonalReserve"))
                            .append(": ")
                            .append(internalEmailer.getToEmail().getFullName())
                            .append("\n")
                            .append(messageBundle.getMessage("msgRecruterOwner"))
                            .append(" ")
                            .append(personelReserveCheck.getRecruter().getName())
                            .append("\n")
                            .append(messageBundle.getMessage("msgEndDateReserve"))
                            .append(" ")
                            .append(personelReserveCheck.getEndDate() != null
                                    ? sdf.format(personelReserveCheck.getEndDate())
                                    : messageBundle.getMessage("msgUnlimited")).toString())
                    .withType(Notifications.NotificationType.ERROR)
                    .show();
        }
    }

    protected void addPersonalReserveInteraction(JobCandidate jobCandidate,
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
                    .cacheable(true)
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
            iteractionList.setNumberIteraction(interactionService.getCountInteraction().add(BigDecimal.ONE));
            iteractionList.setIteractionType(interactionType);

            dataManager.commit(iteractionList);

            if (openPosition != null) {
                openPositionService.setOpenPositionNewsAutomatedMessage(openPosition,
                        iteractionList.getIteractionType().getIterationName(),
                        messageBundle.getMessage("msgJobCandidatePutToPersonalReserve"),
                        iteractionList.getDateIteraction(),
                        jobCandidate,
                        (ExtUser) userSession.getUser(),
                        interactionType.getSignPriorityNews());
            } else {
                openPositionService.setOpenPositionNewsAutomatedMessage(defaultPosition,
                        iteractionList.getIteractionType().getIterationName(),
                        messageBundle.getMessage("msgJobCandidatePutToPersonalReserve"),
                        iteractionList.getDateIteraction(),
                        jobCandidate,
                        (ExtUser) userSession.getUser(),
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

    /* private BigDecimal getCountIteraction() {
        IteractionList e = dataManager.load(IteractionList.class)
                .query(QUERY_GET_COUNT_INTERACTION)
                .view("iteractionList-view")
                .cacheable(true)
                .one();

        return e.getNumberIteraction().add(BigDecimal.ONE);
    } */

    private Label getSignIconLabel(Label retLabel, JobCandidate jobCandidate) {

        List<JobCandidateSignIcon> jobCandidateSignIcons = dataManager.load(JobCandidateSignIcon.class)
                .query(QUERY_GET_JOB_CANDIDATE_SIGN_ICONS)
                .parameter("jobCandidate", jobCandidate)
                .view("jobCandidateSignIcon-view")
                .cacheable(true)
                .list();

        if (jobCandidateSignIcons.size() > 0) {
            StringBuilder sb = new StringBuilder(pic_center_large);
            sb.append(jobCandidateSignIcons.get(0).getSignIcon().getIconColor());

            retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);
            retLabel.setIcon(jobCandidateSignIcons.get(0).getSignIcon().getIconName());

            if (jobCandidateSignIcons.get(0).getSignIcon().getTitleDescription() != null) {
                retLabel.setDescription(jobCandidateSignIcons.get(0).getSignIcon().getTitleDescription());
            } else {
                retLabel.setDescription(jobCandidateSignIcons.get(0).getSignIcon().getTitleRu());
            }

            injectColorCss(jobCandidateSignIcons.get(0).getSignIcon().getIconColor());
            retLabel.setStyleName(sb.toString());

            retLabel.setVisible(true);
        } else {
            retLabel.setVisible(false);
        }

        return retLabel;
    }

    protected void injectColorCss(String color) {
        Page.Styles styles = Page.getCurrent().getStyles();
        String style = String.format(pic_center_large_inject_css, color, color);

        styles.add(style);
    }

    private void addPersonaLReserveMonth(InternalEmailer internalEmailer, OpenPosition selectedOpenPosition) {
        PersonelReserve personelReserve = metadata.create(PersonelReserve.class);
        // сначала итерацию, а уж потом остальное
        addPersonalReserveInteraction(internalEmailer.getToEmail(), selectedOpenPosition);

        personelReserve.setDate(new Date());
        personelReserve.setJobCandidate(internalEmailer.getToEmail());
        personelReserve.setSelectedForAction(internalEmailer.getSelectedForAction());
        personelReserve.setSelectionSymbolForActions(internalEmailer.getSelectionSymbolForActions());
        personelReserve.setRecruter((ExtUser) userSessionSource.getUserSession().getUser());
        personelReserve.setInProcess(true);

        int noOfDays = 30;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_YEAR, noOfDays);
        Date endDate = calendar.getTime();
        personelReserve.setEndDate(endDate);

        personelReserve.setPersonPosition(internalEmailer.getToEmail().getPersonPosition());

        personelReserve.setOpenPosition(selectedOpenPosition);

        dataManager.commit(personelReserve);

        notifications.create(Notifications.NotificationType.TRAY)
                .withHideDelayMs(10000)
                .withCaption(messageBundle.getMessage("msgInfo"))
                .withContentMode(ContentMode.HTML)
                .withDescription(new StringBuilder()
                        .append(messageBundle.getMessage("msgCandidateAddedToPersonalReserve"))
                        .append(" <br><b>")
                        .append(internalEmailer.getToEmail().getFullName())
                        .append("</b>")
                        .toString())
                .withPosition(Notifications.Position.BOTTOM_RIGHT)
                .show();
    }


    private void editJobCandidateAction(InternalEmailer internalEmailer) {
        emailersTable.setSelected(internalEmailer);

        screenBuilders.editor(JobCandidate.class, this)
                .editEntity(internalEmailer.getToEmail())
                .build()
                .show();
    }

    protected void addNewInteractionAction(InternalEmailer internalEmailer) {
        emailersTable.setSelected(internalEmailer);
        screenBuilders.editor(IteractionList.class, this)
                .withParentDataContext(dataContext)
                .withInitializer(event -> {
                    event.setCandidate(internalEmailer.getToEmail());
                    event.setRecrutier(internalEmailer.getFromEmail());
                    event.setRecrutierName(internalEmailer.getFromEmail().getName());
                    event.setDateIteraction(new Date());
                })
                .newEntity()
                .build()
                .show();
    }

    @Install(to = "emailersTable.actionButtonColumn", subject = "columnGenerator")
    private Component emailersTableActionButtonColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<InternalEmailer> event) {
        HBoxLayout retHbox = uiComponents.create(HBoxLayout.class);
        retHbox.setWidthFull();
        retHbox.setHeightFull();

        PopupButton actionButton = uiComponents.create(PopupButton.class);
        actionButton.setIconFromSet(CubaIcon.BARS);
        actionButton.setShowActionIcons(true);
        actionButton.setWidthAuto();
        actionButton.setHeightAuto();
        actionButton.setAlignment(Component.Alignment.MIDDLE_CENTER);

        setActionToActionPopupButton(actionButton, event.getItem());

        retHbox.add(actionButton);

        return retHbox;
    }

    protected void resendEmailAction(InternalEmailer internalEmailer) {
        if (!internalEmailer.getToEmail().getBlockCandidate()) {
            screenBuilders.editor(InternalEmailer.class, this)
                    .newEntity()
                    .withInitializer(emailer -> {
                        if (internalEmailer != null) {
                            emailersTable.setSelected(internalEmailer);
                        }

                        emailer.setReplyInternalEmailer(internalEmailer);
                        emailer.setToEmail(internalEmailer.getToEmail());
                        emailer.setSelectedForAction(internalEmailer.getSelectedForAction() != null
                                ? internalEmailer.getSelectedForAction() : false);
                        emailer.setSelectionSymbolForActions(internalEmailer.getSelectionSymbolForActions() != null
                                ? internalEmailer.getSelectionSymbolForActions() : 0);
                    })
                    .withOpenMode(OpenMode.DIALOG)
                    .build()
                    .show();
        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("msgError"))
                    .withDescription(messageBundle.getMessage("msgBlockCandidate"))
                    .withPosition(Notifications.Position.MIDDLE_CENTER)
                    .show();
        }
    }

    @Install(to = "emailersTable.lastIteraction", subject = "columnGenerator")
    private Component emailersTableLastIteractionColumnGenerator(DataGrid.ColumnGeneratorEvent<InternalEmailer> event) {
        IteractionList iteractionList = interactionService.getLastIteraction(event.getItem().getToEmail());
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();
        retHBox.setHeightFull();

        Label retLabel = uiComponents.create(Label.class);
        retLabel.setWidthAuto();
        retLabel.setHeightAuto();
        retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);

        String date = null;
        String style = null;

        try {
            date = new SimpleDateFormat("dd-MM-yyyy").format(iteractionList.getDateIteraction());
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        StringBuilder sb = new StringBuilder();
        Boolean checkBlockCandidate = event.getItem().getToEmail().getBlockCandidate() == null
                ? false : event.getItem().getToEmail().getBlockCandidate();

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
                                style = "button_table_red";
                            } else {
//                                retStr = "button_table_yellow";
                                style = "button_table_yellow";
                            }
                        }
                    } else {
//                        retStr = "button_table_green";
                        style = "button_table_green";
                    }
                } else {
//                    retStr = "button_table_white";
                    style = "button_table_white";
                }
            } else {
//                retStr = "button_table_black";
                style = "button_table_black";
            }
        } else {
//            retStr = "button_table_green";
            style = "button_table_green";
        }

        retLabel.setValue(date != null ? date : "нет");
        retLabel.setStyleName(style);
        retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);
        retLabel.setDescription(iteractionList.getIteractionType().getIterationName());

        retHBox.add(retLabel);

        return retHBox;
    }

    @Install(to = "emailersTable.subjectEmail", subject = "columnGenerator")
    private Component emailersTableSubjectEmailColumnGenerator(DataGrid.ColumnGeneratorEvent<InternalEmailer> event) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();
        retHBox.setHeightFull();

        Label retLabel = uiComponents.create(Label.class);
        retLabel.setWidthAuto();
        retLabel.setHeightAuto();
        retLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);
        retLabel.addStyleName("table-wordwrap");
        retLabel.setValue(event.getItem().getSubjectEmail());

        retHBox.add(retLabel);
        return retHBox;
    }

    @Install(to = "emailersTable.dateCreateEmail", subject = "columnGenerator")
    private Component emailersTableDateCreateEmailColumnGenerator(DataGrid.ColumnGeneratorEvent<InternalEmailer> event) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();
        retHBox.setHeightFull();

        Label retLabel = uiComponents.create(Label.class);
        retLabel.setWidthAuto();
        retLabel.setHeightAuto();
        retLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);
        retLabel.addStyleName("table-wordwrap");
        retLabel.setValue(event.getItem().getDateCreateEmail().toString());

        retHBox.add(retLabel);
        return retHBox;
    }

    @Install(to = "emailersTable.dateSendEmail", subject = "columnGenerator")
    private Component emailersTableDateSendEmailColumnGenerator(DataGrid.ColumnGeneratorEvent<InternalEmailer> event) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();
        retHBox.setHeightFull();

        Label retLabel = uiComponents.create(Label.class);
        retLabel.setWidthAuto();
        retLabel.setHeightAuto();
        retLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);
        retLabel.addStyleName("table-wordwrap");
        if (event.getItem().getDateSendEmail() != null) {
            retLabel.setValue(event.getItem().getDateSendEmail().toString());
        }

        retHBox.add(retLabel);
        return retHBox;
    }

}
