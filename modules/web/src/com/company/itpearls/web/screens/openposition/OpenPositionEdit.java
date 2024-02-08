package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.UiNotificationEvent;
import com.company.itpearls.core.*;
import com.company.itpearls.entity.*;
import com.company.itpearls.service.GetRoleService;
import com.company.itpearls.web.StandartRegistrationForWork;
import com.company.itpearls.web.screens.position.PositionEdit;
import com.haulmont.bpm.entity.ProcAttachment;
import com.haulmont.bpm.gui.procactionsfragment.ProcActionsFragment;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogActions;
import com.haulmont.cuba.gui.app.core.inputdialog.InputDialog;
import com.haulmont.cuba.gui.app.core.inputdialog.InputParameter;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Timer;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.*;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import org.jsoup.Jsoup;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.NoResultException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.Calendar;


@UiController("itpearls_OpenPosition.edit")
@UiDescriptor("open-position-edit.xml")
@EditedEntityContainer("openPositionDc")
@LoadDataBeforeShow
public class OpenPositionEdit extends StandardEditor<OpenPosition> {
    @Inject
    private Label<String> closedVacancyInfoLabel;
    @Inject
    private DateField<Date> closingDateDateField;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private Timer closedVacancyTimer;
    @Inject
    private TelegramService telegramService;
    @Inject
    private TextManipulationService textManipulationService;
    @Inject
    private StandartMapsService standartMapsService;
    @Inject
    private TelegramBotService telegramBotService;
    @Inject
    private LookupPickerField<City> cityOpenPositionField;
    @Inject
    private LookupPickerField<CompanyDepartament> companyDepartamentField;
    @Inject
    private LookupPickerField<Company> companyNameField;
    @Inject
    private TextField<Integer> numberPositionField;
    @Inject
    private LookupPickerField<Position> positionTypeField;
    @Inject
    private CheckBox needExerciseCheckBox;
    @Inject
    private RichTextArea exerciseRichTextArea;
    @Inject
    private LookupPickerField<Project> projectNameField;
    @Inject
    private TextField<String> vacansyNameField;
    @Inject
    private Notifications notifications;
    @Inject
    private LookupField<Integer> priorityField;
    @Inject
    private CheckBox openClosePositionCheckBox;
    @Inject
    private DataManager dataManager;
    @Inject
    private ScreenBuilders screenBuilders;

    private Boolean booOpenClosePosition = false;
    private Boolean entityIsChanged = false;
    private String emails = "";
    private Boolean setOK;
    @Inject
    private Dialogs dialogs;
    private boolean r;
    @Inject
    private Events events;
    @Inject
    private GetRoleService getRoleService;
    @Inject
    private UserSession userSession;
    @Inject
    private RadioButtonGroup radioButtonGroupPaymentsType;
    @Inject
    private RadioButtonGroup radioButtonGroupResearcherSalary;
    @Inject
    private RadioButtonGroup radioButtonGroupRecrutierSalary;
    @Inject
    private GroupBoxLayout groupBoxPaymentsDetail;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private GroupBoxLayout groupBoxPaymentsResearcher;
    @Inject
    private GroupBoxLayout groupBoxPaymentsRecrutier;
    @Inject
    private TextField<String> textFieldPercentOrSum;
    @Inject
    private TextField<String> textFieldCompanyPayment;
    @Inject
    private TextField<BigDecimal> openPositionFieldSalaryMin;
    @Inject
    private TextField<BigDecimal> openPositionFieldSalaryMax;
    @Inject
    private CheckBox checkBoxUseNDFL;
    @Inject
    private TextField<String> textFieldResearcherSalaryPercentOrSum;
    @Inject
    private TextField<String> textFieldResearcherSalary;
    @Inject
    private TextField<String> textFieldRecrutierPercentOrSum;
    @Inject
    private TextField<String> textFieldRecrutierSalary;
    @Inject
    private Label<String> labelResearcherSalary;
    @Inject
    private Label<String> labelRecrutierSalary;
    @Inject
    private CollectionLoader<Project> projectNamesLc;
    @Inject
    private CollectionLoader<CompanyDepartament> companyDepartamentsLc;
    @Inject
    private LookupField<Integer> remoteWorkField;
    @Inject
    private Label<String> labelOpenPosition;
    @Inject
    private Label<String> labelTopComissionResearcher;
    @Inject
    private Label<String> labelTopComissionRecrutier;
    @Inject
    private RadioButtonGroup workExperienceRadioButton;
    @Inject
    private RadioButtonGroup commanExperienceRadioButton;
    @Inject
    private CheckBox internalProjectCheckBox;
    @Inject
    private DataContext dataContext;
    @Inject
    private CollectionLoader<Position> positionTypesLc;
    @Inject
    private Screens screens;
    @Inject
    private RadioButtonGroup commandOrPosition;
    @Inject
    private LookupPickerField<OpenPosition> parentOpenPositionField;
    @Inject
    private Label<String> citiesLabel;
    @Named("tabSheetOpenPosition.tabPayments")
    private VBoxLayout tabPayments;
    @Inject
    private PdfParserService pdfParserService;
    @Inject
    private RichTextArea openPositionRichTextArea;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private TreeDataGrid<SkillTree> openPositionSkillsListTable;
    @Inject
    private TextField<String> shortDescriptionTextArea;
    @Inject
    private RichTextArea openPositionStandartDescriptionRichTextArea;
    @Named("openPositionAccordion.openPositionStandartDescriptionAccorden")
    private VBoxLayout openPositionStandartDescriptionAccorden;
    @Inject
    private InstanceContainer<OpenPosition> openPositionDc;
    @Named("openPositionAccordion.openPositionWhoIsThisGuyAccorden")
    private VBoxLayout openPositionWhoIsThisGuyAccorden;
    @Inject
    private RichTextArea openPositionWhoIsThisGuyRichTextArea;

    static String RESEARCHER = "Researcher";
    static String RECRUITER = "Recruiter";
    static String MANAGER = "Manager";
    static String ADMINISTRATOR = "Administrators";
    private static final String QUERY_OUTSTAFF_RATES = "select e from itpearls_OutstaffingRates e where e.rate = :rate";
    private final static String QUERY_SELECT_COMMAND =
            "select e from itpearls_OpenPosition e where e.parentOpenPosition = :parentOpenPosition and e.openClose = false";
    private final static String QUERY_OPEN_POSITION_INTERACTIONS =
            "select e from itpearls_IteractionList e where e.vacancy = :vacancy and e.iteractionType.signFeedback = true";
    private final static String QUERY_CHECK_VACANCY_ID =
            "select e from itpearls_OpenPosition e where e.vacansyID like :vacancyID";
    private final static String QUERY_OPEN_POSITION = "select e from itpearls_OpenPosition e where e.positionType = :positionType and e.vacansyName like :vacansyName and e.projectName = :projectName and e.parentOpenPosition = :parentOpenPosition and e.vacansyName = :vacansyName and e.remoteWork = :remoteWork and e.cityPosition = :cityPosition";
    private OpenPosition beforeEdit = null;
    List<SkillTree> skillTrees;
    protected Boolean openCloseStartStatus = false;
    protected Boolean openCloseCurrentStatus;

    private static final String PROCESS_CODE = "openpositionApproval";

    @Inject
    private Logger log;
    @Inject
    private CheckBox needMemoCheckBox;
    @Inject
    private LookupField<Integer> registrationForWorkField;
    @Inject
    private DateField<Date> lastOpenVacancyDateField;
    @Inject
    private CollectionLoader<OpenPositionNews> openPositionNewsLc;
    @Inject
    private DataGrid<OpenPositionNews> openPostionNewsDataGrid;
    @Inject
    private RichTextArea templateLetterRichTextArea;
    @Inject
    private TextField<ExtUser> ownerTextField;
    private String startVacansyName = null;
    @Inject
    private CheckBox signDraftCheckBox;
    @Inject
    private Label<String> signDraftLabel;
    @Inject
    private Metadata metadata;
    @Inject
    private CollectionLoader<ProcAttachment> procAttachmentsDl;
    @Inject
    private ProcActionsFragment procActionsFragment;
    @Inject
    private LookupPickerField<Grade> gradeLookupPickerField;
    @Inject
    private CollectionContainer<Grade> gradeDc;
    @Inject
    private TextField<BigDecimal> openPositionFieldSalaryIE;
    @Inject
    private TextField<String> vacansyIDTextField;
    @Inject
    private TextField<BigDecimal> outstaffingCostTextField;
    @Inject
    private TextField<String> salaryCommentTextFiels;
    @Inject
    private ScrollBoxLayout commentsScrollBox;
    @Inject
    private StarsAndOtherService starsAndOtherService;
    @Inject
    private Messages messages;
    @Named("tabSheetOpenPosition.tabFiles")
    private VBoxLayout tabFiles;
    @Inject
    private CollectionPropertyContainer<SomeFilesOpenPosition> someFilesesDc;
    @Inject
    private OpenPositionService openPositionService;
    @Inject
    private CheckBox onlyOpenProjectCheckBox;
    @Inject
    private CheckBox withOpenPositionCheckBox;
    @Inject
    private Image projectLogoImage;
    @Inject
    private Image projectOwnerImage;

    @Subscribe("closedVacancyTimer")
    public void onClosedVacancyTimerTimerAction(Timer.TimerActionEvent event) {
        if (closingDateDateField.getValue() != null) {
            closedVacancyInfoLabel.setValue(new StringBuilder()
                    .append(messageBundle.getMessage("msgClosingVacancyAfter"))
                    .append(" ")
                    .append(getTimerClosingVacancyValue(closingDateDateField.getValue()))
                    .toString());
        }
    }
    public String getTimerClosingVacancyValue(Date closingDate) {
        StringBuilder sb = new StringBuilder();

        if (closingDate != null) {
            long diffDate = closingDate.getTime() - new Date().getTime();
            int days = (int) diffDate / (24 * 60 * 60 * 1000);
            int hours = (int) ((diffDate - days * (24 * 60 * 60 * 1000)) / (60 * 60 * 1000));
            int minutes = (int) ((diffDate - (days * (24 * 60 * 60 * 1000) + hours * (60 * 60 * 1000))) / (60 * 1000));
            int seconds = (int) ((diffDate - (days * (24 * 60 * 60 * 1000) + hours * (60 * 60 * 1000) + minutes * (60 * 1000))) / 1000);

            sb.append(days)
                    .append(" ")
                    .append(messageBundle.getMessage("msgDays"))
                    .append(" ")
                    .append(hours)
                    .append(" ")
                    .append(messageBundle.getMessage("msgHours"))
                    .append(" ")
                    .append(minutes)
                    .append(" ")
                    .append(messageBundle.getMessage("msgMinutes"))
                    .append(" ")
                    .append(seconds)
                    .append(" ")
                    .append(messageBundle.getMessage("msgSeconds"));

            return sb.toString();
        }

        return "";
    }
    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {

        beforeEdit = getEditedEntity();
        booOpenClosePosition = getEditedEntity().getOpenClose();
        parentOpenPositionField.setEditable(commandOrPosition.getValue() != null);

        // проверка на ноль
        booOpenClosePosition = booOpenClosePosition == null ? false : booOpenClosePosition;


        setTopLabel();
        setInternalProject();
        setHiddeField();
        setDisableTwoField();
        setWorkExperienceRadioButton();
        setCommandExperienceRadioButton();
        setCommentToVacancy();
        changeCityListsLabel();
        standartDescriptionDisable(event);
        whiIsThisGuyDisable(event);
        setOpenPositionNews(event);
        setOpenCloseStart();
        setCommentsOpenPositionScroll(getEditedEntity(), commentsScrollBox);
        setCommentOpenPositionScrollIteractionList(getEditedEntity(), commentsScrollBox);

        initProjectNameField();
        initClosedVacancyTimerFacet();
        setMapOfPriority();
    }

    private void setMapOfPriority() {
        priorityMap = standartMapsService.setPriorityMap();
    }

    private void initClosedVacancyTimerFacet() {
        if (closingDateDateField.getValue() != null) {
            closedVacancyTimer.start();
        }
    }

    private void initProjectNameField() {
        projectNameField.setOptionImageProvider(this::projectFielsImageProvider);
        companyDepartamentField.setOptionImageProvider(this::companyDepartamentFieldImageProvider);
        companyNameField.setOptionImageProvider(this::companyNameFieldImageProvider);
        withOpenPositionCheckBox.setValue(true);
    }

    private Resource companyNameFieldImageProvider(Company company) {
        Image retImage = uiComponents.create(Image.class);
        retImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        retImage.setWidth("30px");

        if (company.getFileCompanyLogo() != null) {
            return retImage.createResource(FileDescriptorResource.class)
                    .setFileDescriptor(company.getFileCompanyLogo());
        } else {
            return retImage.createResource(ThemeResource.class).setPath("icons/no-company.png");
        }
    }

    private Resource companyDepartamentFieldImageProvider(CompanyDepartament companyDepartament) {
        Image retImage = uiComponents.create(Image.class);
        retImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        retImage.setWidth("30px");

        if (companyDepartament.getCompanyName().getFileCompanyLogo() != null) {
            return retImage.createResource(FileDescriptorResource.class)
                    .setFileDescriptor(companyDepartament.getCompanyName().getFileCompanyLogo());
        } else {
            return retImage.createResource(ThemeResource.class).setPath("icons/no-company.png");
        }
    }

    protected Resource projectFielsImageProvider(Project project) {
        Image retImage = uiComponents.create(Image.class);
        retImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        retImage.setWidth("30px");

        if (project.getProjectLogo() != null) {
            return retImage.createResource(FileDescriptorResource.class)
                    .setFileDescriptor(project
                            .getProjectLogo());
        } else {
            return retImage.createResource(ThemeResource.class).setPath("icons/no-company.png");
        }
    }

    private void setCommentOpenPositionScrollIteractionList(OpenPosition editedEntity, ScrollBoxLayout commentsScrollBox) {

        List<IteractionList> iteractionLists = dataManager.load(IteractionList.class)
                .query(QUERY_OPEN_POSITION_INTERACTIONS)
                .view("iteractionList-view")
                .parameter("vacancy", editedEntity)
                .list();

        if (iteractionLists.size() > 0) {
            for (IteractionList iteractionList : iteractionLists) {
                if (iteractionList.getComment() != null) {
                    if (!iteractionList.getComment().equals("")) {
                        VBoxLayout commentBox = getCommentBox(iteractionList);
                        commentsScrollBox.add(commentBox);
                    }
                }
            }
        }
    }

    public void setCommentsOpenPositionScroll(OpenPosition editedEntity, ScrollBoxLayout commentsScrollBox) {
        if (editedEntity.getOpenPositionComments() != null) {
            for (OpenPositionComment openPositionComment : editedEntity.getOpenPositionComments()) {
                if (openPositionComment.getComment() != null) {
                    VBoxLayout commentBox = getCommentBox(openPositionComment);
                    commentsScrollBox.add(commentBox);
                }
            }
        }
    }

    private void setProjectClosedFilter() {
        if (onlyOpenProjectCheckBox.getValue()) {
            projectNamesLc.removeParameter("projectClosed");
            withOpenPositionCheckBox.setValue(false);
        } else {
            projectNamesLc.setParameter("projectClosed", false);
        }

        projectNamesLc.load();
    }

    @Subscribe("onlyOpenProjectCheckBox")
    public void onOnlyOpenProjectCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        setProjectClosedFilter();
    }

    @Subscribe("withOpenPositionCheckBox")
    public void onWithOpenPositionCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (event.getValue()) {
            projectNamesLc.setParameter("withOpenPosition", true);
            onlyOpenProjectCheckBox.setValue(false);
        } else {
            projectNamesLc.removeParameter("withOpenPosition");
        }

        projectNamesLc.load();
    }

    private VBoxLayout getCommentBox(IteractionList iteractionList) {
        VBoxLayout retBox = uiComponents.create(VBoxLayout.class);
        retBox.setWidthFull();
        retBox.setSpacing(false);
        retBox.setMargin(true);
        retBox.setMargin(false);
//        retBox.setHeight("100px");

        HBoxLayout innerBox = uiComponents.create(HBoxLayout.class);
        innerBox.setMargin(true);
        innerBox.setWidthAuto();
        innerBox.setSpacing(true);

        VBoxLayout outerBox = uiComponents.create(VBoxLayout.class);
        outerBox.setMargin(false);
        outerBox.setWidthAuto();
        outerBox.setSpacing(false);

        if (iteractionList.getComment() != null
                && !iteractionList.getComment().equals("")) {
            Label name = uiComponents.create(Label.class);

            if (iteractionList.getRecrutier() != null) {
                name.setValue(iteractionList.getRecrutier().getName() != null
                        ? iteractionList.getRecrutier().getName() :
                        (iteractionList.getRecrutier().getName() != null
                                ? iteractionList.getRecrutier().getName() : ""));
            }
            name.setStyleName("tailName");

            HBoxLayout starsAndCommentHBox = uiComponents.create(HBoxLayout.class);
            starsAndCommentHBox.setWidthAuto();
            starsAndCommentHBox.setSpacing(true);
            Label candidateName = uiComponents.create(Label.class);
            candidateName.addStyleName("table-wordwrap");
            candidateName.setValue(new StringBuilder(iteractionList.getCandidate().getFullName())
                    .append(" / ")
                    .append(iteractionList.getCandidate().getPersonPosition().getPositionRuName())
                    .toString());

            Label stars = uiComponents.create(Label.class);
            stars.addStyleName("table-wordwrap");

            if (iteractionList.getRating() != null) {
                stars.setValue(starsAndOtherService.setStars(iteractionList.getRating() + 1));
            } else {
                stars.setValue(starsAndOtherService.noneStars());
            }

            Label text = uiComponents.create(Label.class);
            text.setValue(iteractionList.getComment() != null ?
                    iteractionList.getComment().replaceAll("\n\n", "\n") : "");
            text.addStyleName("table-wordwrap");

            starsAndCommentHBox.add(stars);
            starsAndCommentHBox.add(text);

            Label date = uiComponents.create(Label.class);
            date.setValue(iteractionList.getDateIteraction() != null ?
                    iteractionList.getDateIteraction() : "");
            date.setAlignment(Component.Alignment.BOTTOM_RIGHT);
            date.setStyleName("tailDate");

            Image image = uiComponents.create(Image.class);

            if (iteractionList.getRecrutier() != null) {
                if (((ExtUser) iteractionList.getRecrutier()).getFileImageFace() != null) {
                    image.setSource(FileDescriptorResource.class)
                            .setFileDescriptor(((ExtUser) iteractionList.getRecrutier()).getFileImageFace());
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
                                        .append(name.getValue())
                                        .append(") Re:")
                                        .append((String) closeEvent.getValue("comment"))
                                        .toString());
                            }
                        })
                        .show();
            });

            if (userSession.getUser().getLogin().equals(iteractionList.getCreatedBy())) {
                outerBox.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                date.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                // vacancy.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                text.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                name.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                innerBox.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                innerBox.addStyleName("tailMyMessage");
            } else {
                outerBox.setAlignment(Component.Alignment.MIDDLE_LEFT);
                date.setAlignment(Component.Alignment.MIDDLE_LEFT);
                // vacancy.setAlignment(Component.Alignment.MIDDLE_LEFT);
                text.setAlignment(Component.Alignment.MIDDLE_LEFT);
                name.setAlignment(Component.Alignment.MIDDLE_LEFT);
                innerBox.setAlignment(Component.Alignment.MIDDLE_LEFT);
                innerBox.addStyleName("tailOtherMessage");
            }

            outerBox.add(name);
            /* if (!vacancy.getValue().equals("")) {
                outerBox.add(vacancy);
            } */

            outerBox.add(candidateName);
            outerBox.add(starsAndCommentHBox);
            outerBox.add(date);
            outerBox.add(replyButton);

            if (!userSession.getUser().getLogin().equals(iteractionList.getCreatedBy())) {
                innerBox.add(image);
            }

            innerBox.add(outerBox);
            if (userSession.getUser().getLogin().equals(iteractionList.getCreatedBy())) {
                innerBox.add(image);
            }

            retBox.add(innerBox);
        }

        return retBox;
    }

    private VBoxLayout getCommentBox(OpenPositionComment openPositionComment) {
        VBoxLayout retBox = uiComponents.create(VBoxLayout.class);
        retBox.setWidthFull();
        retBox.setSpacing(false);
        retBox.setMargin(true);
        retBox.setMargin(false);
//        retBox.setHeight("100px");

        HBoxLayout innerBox = uiComponents.create(HBoxLayout.class);
        innerBox.setMargin(true);
        innerBox.setWidthAuto();
        innerBox.setSpacing(true);

        VBoxLayout outerBox = uiComponents.create(VBoxLayout.class);
        outerBox.setMargin(false);
        outerBox.setWidthAuto();
        outerBox.setSpacing(false);

        if (openPositionComment.getComment() != null
                && !openPositionComment.getComment().equals("")) {
            Label name = uiComponents.create(Label.class);

            if (openPositionComment.getUser() != null) {
                name.setValue(openPositionComment.getUser().getName() != null
                        ? openPositionComment.getUser().getName() :
                        (openPositionComment.getUser().getName() != null ? openPositionComment.getUser().getName() : ""));
            }
            name.setStyleName("tailName");

            HBoxLayout starsAndCommentHBox = uiComponents.create(HBoxLayout.class);
            starsAndCommentHBox.setWidthAuto();
            starsAndCommentHBox.setSpacing(true);

            Label stars = uiComponents.create(Label.class);
            stars.addStyleName("table-wordwrap");

            if (openPositionComment.getRating() != null) {
                stars.setValue(starsAndOtherService.setStars(openPositionComment.getRating() + 1));
            } else {
                stars.setValue(starsAndOtherService.noneStars());
            }

            Label text = uiComponents.create(Label.class);
            text.setValue(openPositionComment.getComment() != null ?
                    openPositionComment.getComment().replaceAll("\n\n", "\n") : "");
            text.addStyleName("table-wordwrap");

            starsAndCommentHBox.add(stars);
            starsAndCommentHBox.add(text);

            Label date = uiComponents.create(Label.class);
            date.setValue(openPositionComment.getDateComment() != null ?
                    openPositionComment.getDateComment() : "");
            date.setAlignment(Component.Alignment.BOTTOM_RIGHT);
            date.setStyleName("tailDate");

            Image image = uiComponents.create(Image.class);

            if (openPositionComment.getUser() != null) {
                if (((ExtUser) openPositionComment.getUser()).getFileImageFace() != null) {
                    image.setSource(FileDescriptorResource.class)
                            .setFileDescriptor(((ExtUser) openPositionComment.getUser()).getFileImageFace());
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
                                        .append(name.getValue())
                                        .append(") Re:")
                                        .append((String) closeEvent.getValue("comment"))
                                        .toString());
                            }
                        })
                        .show();
            });

            if (userSession.getUser().getLogin().equals(openPositionComment.getCreatedBy())) {
                outerBox.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                date.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                // vacancy.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                text.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                name.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                innerBox.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                innerBox.addStyleName("tailMyMessage");
            } else {
                outerBox.setAlignment(Component.Alignment.MIDDLE_LEFT);
                date.setAlignment(Component.Alignment.MIDDLE_LEFT);
                // vacancy.setAlignment(Component.Alignment.MIDDLE_LEFT);
                text.setAlignment(Component.Alignment.MIDDLE_LEFT);
                name.setAlignment(Component.Alignment.MIDDLE_LEFT);
                innerBox.setAlignment(Component.Alignment.MIDDLE_LEFT);
                innerBox.addStyleName("tailOtherMessage");
            }

            outerBox.add(name);
            /* if (!vacancy.getValue().equals("")) {
                outerBox.add(vacancy);
            } */

            outerBox.add(starsAndCommentHBox);
            outerBox.add(date);
            outerBox.add(replyButton);

            if (!userSession.getUser().getLogin().equals(openPositionComment.getCreatedBy())) {
                innerBox.add(image);
            }

            innerBox.add(outerBox);
            if (userSession.getUser().getLogin().equals(openPositionComment.getCreatedBy())) {
                innerBox.add(image);
            }

            retBox.add(innerBox);
        }

        return retBox;
    }


    private void replyButtonInvoke(Button.ClickEvent e, String replyStr) {
        createComment(replyStr);

        events.publish(new UiNotificationEvent(this,
                new StringBuilder()
                        .append(messageBundle.getMessage("msgPublishOpenPositionComment"))
                        .append(":")
                        .append(getEditedEntity().getVacansyName())
                        .toString()));
    }

    private void createComment(String commentStr) {

        OpenPositionComment comment = metadata.create(OpenPositionComment.class);
        comment.setOpenPosition(getEditedEntity());
        comment.setDateComment(new Date());
        comment.setUser((ExtUser) userSession.getUser());

        if (commentStr != null) {
            comment.setComment(commentStr);

            List<OpenPositionComment> openPositionComments = getEditedEntity().getOpenPositionComments();
            openPositionComments.add(comment);

            dataContext.merge(comment);

        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("msgError"))
                    .withDescription(messageBundle.getMessage("msgDoNotCommentMessage"))
                    .withType(Notifications.NotificationType.ERROR)
                    .show();
        }
    }

    private void setCommentToVacancy() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            String defComment = new StringBuilder()
                    .append("<i>")
                    .append("<!-- НЕ ДЛЯ КАНДИДАТА:<br><br>")
                    .append("-->")
                    .append("</i>")
                    .toString();

            openPositionRichTextArea.setValue(defComment);
        }
    }

    private void setOpenCloseStart() {
        if (getEditedEntity().getOpenClose() != null) {
            openCloseStartStatus = getEditedEntity().getOpenClose();
            openCloseCurrentStatus = getEditedEntity().getOpenClose();
        } else {
            openCloseStartStatus = false;
            openCloseCurrentStatus = false;
        }
    }

    @Install(to = "openPostionNewsDataGrid", subject = "detailsGenerator")
    private Component openPostionNewsDataGridDetailsGenerator(OpenPositionNews entity) {
        VBoxLayout mainLayout = uiComponents.create(VBoxLayout.NAME);
        mainLayout.setWidth("100%");
        mainLayout.setMargin(true);

        HBoxLayout headerBox = uiComponents.create(HBoxLayout.NAME);
        headerBox.setWidth("100%");

        Label infoLabel = uiComponents.create(Label.NAME);
        infoLabel.setHtmlEnabled(true);
        infoLabel.setStyleName("h1");
        infoLabel.setValue("News:");

        Component closeButton = createCloseButton(entity);
        headerBox.add(infoLabel);
        headerBox.add(closeButton);
        headerBox.expand(infoLabel);

        Component content = getContent(entity);

        mainLayout.add(headerBox);
        mainLayout.add(content);
        mainLayout.expand(content);

        return mainLayout;
    }

    private Component getContent(OpenPositionNews entity) {
        Label<String> content = uiComponents.create(Label.TYPE_STRING);
        content.setHtmlEnabled(true);
        StringBuilder sb = new StringBuilder();

        sb.append(entity.getDateNews())
                .append("  ")
                .append(entity.getAuthor().getName())
                .append("<tr>")
                .append("<tr>")
                .append(entity.getComment());

        content.setValue(sb.toString());
        return content;
    }

    private Component createCloseButton(OpenPositionNews entity) {
        Button closeButton = uiComponents.create(Button.class);
        closeButton.setIcon("icons/close.png");
        BaseAction closeAction = new BaseAction("closeAction")
                .withHandler(actionPerformedEvent ->
                        openPostionNewsDataGrid.setDetailsVisible(entity, false))
                .withCaption("");
        closeButton.setAction(closeAction);
        return closeButton;
    }

    private void setOpenPositionNews(BeforeShowEvent event) {
        if (getEditedEntity() != null) {
            openPositionNewsLc.setParameter("openPosition", getEditedEntity());
        } else {
            openPositionNewsLc.removeParameter("openPosition");
        }

        openPositionNewsLc.load();
    }

    private void standartDescriptionDisable(BeforeShowEvent event) {
        if (getEditedEntity().getPositionType() != null) {
            if (getEditedEntity().getPositionType().getStandartDescription() == null) {
                openPositionStandartDescriptionAccorden.setVisible(false);
                openPositionStandartDescriptionRichTextArea.setEnabled(false);
            } else {
                openPositionStandartDescriptionAccorden.setVisible(true);
                openPositionStandartDescriptionRichTextArea.setEnabled(true);
            }
        }
    }

    private void whiIsThisGuyDisable(BeforeShowEvent event) {
        if (getEditedEntity().getPositionType() != null) {
            if (getEditedEntity().getPositionType().getStandartDescription() == null) {
                openPositionWhoIsThisGuyAccorden.setVisible(false);
                openPositionWhoIsThisGuyRichTextArea.setEnabled(false);
            } else {
                openPositionWhoIsThisGuyAccorden.setVisible(true);
                openPositionWhoIsThisGuyRichTextArea.setEnabled(true);
            }
        }
    }


    private void setInternalProject() {
        if (getRoleService.isUserRoles(userSession.getUser(), MANAGER) ||
                getRoleService.isUserRoles(userSession.getUser(), ADMINISTRATOR)) {
            internalProjectCheckBox.setVisible(true);
        } else {
            internalProjectCheckBox.setVisible(false);
        }
    }

    private void setCommandExperienceRadioButton() {
        commanExperienceRadioButton.setOptionsMap(openPositionService.setCommandExperienceMap());
    }

    private void setWorkExperienceRadioButton() {
        workExperienceRadioButton.setOptionsMap(openPositionService.setWorkExperienceMap());
    }

    Boolean onNeedExercise;

    @Subscribe("needExerciseCheckBox")
    public void onNeedExerciseCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (needExerciseCheckBox.getValue() != null) {
            exerciseRichTextArea.setEditable(needExerciseCheckBox.getValue());
            exerciseRichTextArea.setRequired(needExerciseCheckBox.getValue());
        } else {
            exerciseRichTextArea.setRequired(false);
            exerciseRichTextArea.setEditable(false);
        }

        if (onNeedExercise != null) {
            if (needExerciseCheckBox.getValue() != null) {
                if (!onNeedExercise.equals(needExerciseCheckBox.getValue())) {
                    onNeedExercise = needExerciseCheckBox.getValue();

                    openPositionService.setOpenPositionNewsAutomatedMessage(getEditedEntity(),
                            (needExerciseCheckBox.getValue().equals(Boolean.TRUE)
                                    ? "Необходимо выполнение тестового задания"
                                    : "Тестовое задание не нужно"),
                            (exerciseRichTextArea.getValue() != null
                                    ? Jsoup.parse(exerciseRichTextArea.getValue()).wholeText()
                                    : ""),
                            new Date(),
                            (ExtUser) userSession.getUser());
                }
            }
        }
    }

    BigDecimal startSalaryMinValue;

    @Subscribe("openPositionFieldSalaryMin")
    public void onOpenPositionFieldSalaryMinValueChange1(HasValue.ValueChangeEvent<BigDecimal> event) {
        if (openPositionFieldSalaryMin.getValue() != null) {
            if (startSalaryMinValue != null) {
                if (!startSalaryMinValue.equals(openPositionFieldSalaryMin.getValue())) {

                    openPositionService.setOpenPositionNewsAutomatedMessage(getEditedEntity(),
                            new StringBuilder()
                                    .append("Изменены зарплатные предложение (MIN): старое ")
                                    .append(startSalaryMinValue.toString().substring(0, startSalaryMinValue.toString().length() - 3))
                                    .append(" на новое ")
                                    .append(openPositionFieldSalaryMin.getValue()).toString(),
                            new StringBuilder()
                                    .append("Изменены зарплатные предложение (MIN): старое ")
                                    .append(startSalaryMinValue.toString().substring(0, startSalaryMinValue.toString().length() - 3))
                                    .append(" на новое ")
                                    .append(openPositionFieldSalaryMin.getValue()).toString(),
                            new Date(),
                            (ExtUser) userSession.getUser());
                    startSalaryMinValue = openPositionFieldSalaryMin.getValue();
                }
            }
        }
    }

    private Boolean flagMinGreaterMax = false;

    @Install(to = "openPositionFieldSalaryMax", subject = "validator")
    private void openPositionFieldSalaryMaxValidator(BigDecimal bigDecimal) {
        if (!flagMinGreaterMax) {
            if (openPositionFieldSalaryMin.getValue() != null) {
                if (openPositionFieldSalaryMax.getValue() != null) {
                    if (openPositionFieldSalaryMin.getValue().compareTo(openPositionFieldSalaryMax.getValue()) > 0) {
                        flagMinGreaterMax = !flagMinGreaterMax;
                        throw new ValidationException(messageBundle.getMessage("msgSalaryMinGreaterMax"));
                    }
                }
            }
        }
    }

    @Install(to = "openPositionFieldSalaryMin", subject = "validator")
    private void openPositionFieldSalaryMinValidator(BigDecimal bigDecimal) {
        if (!flagMinGreaterMax) {
            if (openPositionFieldSalaryMin.getValue() != null) {
                if (openPositionFieldSalaryMax.getValue() != null) {
                    if (openPositionFieldSalaryMin.getValue().compareTo(openPositionFieldSalaryMax.getValue()) > 0) {
                        flagMinGreaterMax = !flagMinGreaterMax;
                        throw new ValidationException(messageBundle.getMessage("msgSalaryMinGreaterMax"));
                    }
                }
            }
        }
    }

    BigDecimal startSalaryMaxValue;

    @Subscribe("openPositionFieldSalaryMax")
    public void onOpenPositionFieldSalaryMaxValueChange1(HasValue.ValueChangeEvent<BigDecimal> event) {
        if (openPositionFieldSalaryMax.getValue() != null) {
            if (startSalaryMaxValue != null) {
                if (!startSalaryMaxValue.equals(openPositionFieldSalaryMax.getValue())) {

                    openPositionService.setOpenPositionNewsAutomatedMessage(getEditedEntity(),
                            new StringBuilder("Изменены зарплатные предложение (MAX): старое ")
                                    .append(startSalaryMaxValue.toString().length() >= 3
                                            ? startSalaryMaxValue.toString().substring(0, startSalaryMaxValue.toString().length() - 3)
                                            : "НЕ ОПРЕДЕЛЕНО")
                                    .append(" на новое ")
                                    .append(openPositionFieldSalaryMax.getValue())
                                    .toString(),
                            new StringBuilder("Изменены зарплатные предложение (MAX): старое ")
                                    .append(startSalaryMaxValue.toString().length() >= 3
                                            ? startSalaryMaxValue.toString().substring(0, startSalaryMaxValue.toString().length() - 3)
                                            : "НЕ ОПРЕДЕЛЕНО")
                                    .append(" на новое ")
                                    .append(openPositionFieldSalaryMax.getValue())
                                    .toString(),
                            new Date(),
                            (ExtUser) userSession.getUser());
                    startSalaryMaxValue = openPositionFieldSalaryMin.getValue();
                }
            }
        }
    }

    String openPositionText;

    @Subscribe("openPositionRichTextArea")
    public void onOpenPositionRichTextAreaValueChange1(HasValue.ValueChangeEvent<String> event) {
        if (!PersistenceHelper.isNew(getEditedEntity())) {
            if (openPositionRichTextArea.getValue() != null) {
                if (openPositionText != null) {
                    if (!openPositionText.equals(Jsoup.parse(openPositionRichTextArea.getValue()).wholeText())) {
                        openPositionService.setOpenPositionNewsAutomatedMessage(getEditedEntity(),
                                "Изменено описание вакансии",
                                Jsoup.parse(openPositionRichTextArea.getValue()).wholeText(),
                                new Date(),
                                (ExtUser) userSession.getUser());
                        openPositionText = Jsoup.parse(openPositionRichTextArea.getValue()).wholeText();
                    }
                }
            }
        }
    }

    String startLetterText;

    @Subscribe("templateLetterRichTextArea")
    public void onTemplateLetterRichTextAreaValueChange(HasValue.ValueChangeEvent<String> event) {
        if (templateLetterRichTextArea.getValue() != null) {
            if (startLetterText != null) {
                if (!startLetterText.equals(Jsoup.parse(templateLetterRichTextArea.getValue()).wholeText())) {

                    openPositionService.setOpenPositionNewsAutomatedMessage(getEditedEntity(),
                            "Изменен шаблон сопроводительного письма",
                            Jsoup.parse(templateLetterRichTextArea.getValue()).wholeText(),
                            new Date(),
                            (ExtUser) userSession.getUser());
                    startLetterText = Jsoup.parse(templateLetterRichTextArea.getValue()).wholeText();
                }
            }
        }
    }


    private void setDisableTwoField() {
    }

    @Subscribe("vacansyNameField")
    public void onVacansyNameFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        setTopLabel();
    }

    @Subscribe("openPositionFieldSalaryMin")
    public void onOpenPositionFieldSalaryMinValueChange(HasValue.ValueChangeEvent<BigDecimal> event) {
        setCalculateCompanyPercentField();
        calculateResearcherSalary();
        calculateRecrutierSalary();
    }

    @Subscribe("openPositionFieldSalaryMax")
    public void onOpenPositionFieldSalaryMaxValueChange(HasValue.ValueChangeEvent<BigDecimal> event) {
        setCalculateCompanyPercentField();
        calculateResearcherSalary();
        calculateRecrutierSalary();
    }

    private void setCompanyDepartmentFromProject() {
        if (projectNameField.getValue() != null) {
            companyDepartamentField.setValue(projectNameField.getValue().getProjectDepartment());
        }
    }

    private void setCompanyNameFromDepartment() {
        if (companyDepartamentField.getValue() != null) {
            companyNameField.setValue(companyDepartamentField.getValue().getCompanyName());
        }
    }

    @Subscribe("companyDepartamentField")
    public void onCompanyDepartamentFieldValueChange(HasValue.ValueChangeEvent<CompanyDepartament> event) {
        // сократить список проектов
        if (projectNameField.getValue() == null) {
            if (companyDepartamentField.getValue() != null) {
                projectNamesLc.setParameter("department", companyDepartamentField.getValue());
            } else {
                setCompanyNameFromDepartment();
                setPersonTableEmpty();
            }
        } else {
            projectNamesLc.removeParameter("department");

            setCompanyNameFromDepartment();
            setPersonTableEmpty();
        }

        projectNamesLc.load();
        setTopLabel();
    }

    private void setPersonTableEmpty() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
//            personTable.setVisible(false);
        } else {
            if (companyDepartamentField.getValue() != null) {
//                personsDl.setParameter("companyDepartment", companyDepartamentField.getValue());
//                personsDl.load();

//                personTable.setVisible(true);
            }
        }
    }

    private void setCityNameOfCompany() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            if (companyNameField.getValue() != null && cityOpenPositionField.getValue() == null) {
                if (companyDepartamentField.getValue() != null) {
                    if (companyDepartamentField.getValue().getCompanyName() != null) {
                        if (companyDepartamentField.getValue().getCompanyName().getCityOfCompany() != null)
                            cityOpenPositionField.setValue(companyDepartamentField.getValue().getCompanyName().getCityOfCompany());
                    }
                }
            }
        }
    }

    @Subscribe("companyNameField")
    public void onCompanyNameFieldValueChange(HasValue.ValueChangeEvent<Company> event) {
        // сократить список департаментов
        if (companyNameField.getValue() != null) {
            companyDepartamentsLc.setParameter("company", companyNameField.getValue());
        } else {
            companyDepartamentsLc.removeParameter("company");
        }

        setCityNameOfCompany();
        companyDepartamentsLc.load();

        setTopLabel();
    }

    @Subscribe("commandOrPosition")
    public void onCommandOrPositionValueChange(HasValue.ValueChangeEvent event) {
        switch ((int) commandOrPosition.getValue()) {
            case 0: // вакансия
                parentOpenPositionField.setEditable(true);
                openPositionFieldSalaryMin.setEditable(true);
                openPositionFieldSalaryMax.setEditable(true);
                tabPayments.setEnabled(false);
                tabPayments.setVisible(false);
                numberPositionField.setCaption("Количество персонала");
                break;
            case 1: // команда
                parentOpenPositionField.setEditable(false);
                openPositionFieldSalaryMax.setEditable(false);
                openPositionFieldSalaryMin.setEditable(false);
                tabPayments.setEnabled(true);
                tabPayments.setVisible(true);
                numberPositionField.setCaption("Количество команд");
                break;
        }
    }

    @Subscribe("parentOpenPositionField")
    public void onParentOpenPositionFieldValueChange(HasValue.ValueChangeEvent<OpenPosition> event) {
        try {
            if (projectNameField.getValue() == null
                    && parentOpenPositionField.getValue() != null) {
                projectNameField.setValue(parentOpenPositionField.getValue().getProjectName());
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onAfterCommitChanges(AfterCommitChangesEvent event) {

        if (!openClosePositionCheckBox.getValue().equals(openCloseStartStatus)) {
            if (!openClosePositionCheckBox.getValue()) {
                Date lastOpenDate = new Date();

                lastOpenVacancyDateField.setValue(lastOpenDate);
                ownerTextField.setValue((ExtUser) userSession.getUser());
                openPositionService.setOpenPositionNewsAutomatedMessage(getEditedEntity(),
                        "Открылась вакансия",
                        "Открыта вакансия",
                        new Date(),
                        (ExtUser) userSession.getUser());
            } else {
                if (openClosePositionCheckBox.getValue()) {
                    lastOpenVacancyDateField.setValue(null);
                    ownerTextField.setValue(null);
                    openPositionService.setOpenPositionNewsAutomatedMessage(getEditedEntity(),
                            "Закрылась вакансия",
                            "Закрыта вакансия",
                            new Date(),
                            (ExtUser) userSession.getUser());
                }
            }
        } else {
            openPositionService.setOpenPositionNewsAutomatedMessage(getEditedEntity(),
                    "Открыта новая вакансия", new StringBuilder(vacansyNameField.getValue())
                            .append("\n")
                            .append(positionTypeField.getValue().getPositionEnName())
                            .append(" \\ ")
                            .append(positionTypeField.getValue().getPositionRuName())
                            .append("\n\n")
                            .append("Salary MIN: ")
                            .append(openPositionFieldSalaryMin.getValue())
                            .append("\n")
                            .append("Salary MAX: ")
                            .append(openPositionFieldSalaryMax.getValue())
                            .append("\n\n")
                            .append(shortDescriptionTextArea.getValue() != null ?
                                    Jsoup.parse(shortDescriptionTextArea.getValue()).wholeText() : "")
                            .toString(),
                    new Date(),
                    (ExtUser) userSession.getUser());
        }

        if (!PersistenceHelper.isNew(getEditedEntity())) {
            if (!vacansyNameField.getValue().equals(startVacansyName)) {
                openPositionService.setOpenPositionNewsAutomatedMessage(getEditedEntity(),
                        new StringBuilder(userSession.getUser().getName())
                                .append(" изменил наименование вакансии")
                                .toString(),
                        new StringBuilder("Старое: ")
                                .append(startVacansyName)
                                .append("<br>Новое: ")
                                .append(vacansyNameField.getValue())
                                .toString(),
                        new Date(),
                        (ExtUser) userSession.getUser());
            }
        }
    }

    @Subscribe
    public void onBeforeCommitChanges4(BeforeCommitChangesEvent event) {

/*        telegramService.sendMessageToBotWithSetting(new StringBuilder(telegramBotService.getBotName())
                .append("\n")
                .append("ПРОВЕРКА СООБЩЕНИЯ!!!")
                .toString()); */

        if (PersistenceHelper.isNew(getEditedEntity())) {
            StringBuilder sb = new StringBuilder(telegramBotService.getBotName())
                    .append("\n")
                    .append("<b>НОВАЯ ВАКАНСИЯ:</b> ")
                    .append(vacansyIDTextField.getValue() != null ? "(" + vacansyIDTextField.getValue() + ") " : "")
                    .append(vacansyNameField.getValue())
                    .append("\n\n")
                    .append(openPositionRichTextArea.getValue());

            if (openPositionFieldSalaryMax.getValue() != null
                    && openPositionFieldSalaryMin.getValue() != null) {

                sb.append("\n\n<b>Зарплатное предложение:</b>");

                if (openPositionFieldSalaryMin.getValue() != null) {
                    sb.append(" от ")
                            .append(openPositionFieldSalaryMin.getValue());
                }

                if (openPositionFieldSalaryMax.getValue() != null) {
                    sb.append(" до ")
                            .append(openPositionFieldSalaryMax.getValue());
                }
            }


            telegramService.sendMessageToBotWithSetting(new StringBuilder(telegramBotService.getBotName())
                    .append("\n")
                    .append(textManipulationService.formattedHtml2text(sb.toString()))
                    .toString());
        } else {

            Boolean flag = getEditedEntity().getOpenClose() != null ? getEditedEntity().getOpenClose() : false;

            if (false) {
//                if (!flag) {
                telegramService.sendMessageToBotWithSetting(new StringBuilder(telegramBotService.getBotName())
                        .append("\n")
                        .append(textManipulationService
                                .formattedHtml2text(new StringBuilder(telegramBotService.getBotName())
                                        .append("\n")
                                        .append("<B>ИЗМЕНЕНА ВАКАНСИЯ:</b> ")
                                        .append(vacansyIDTextField.getValue() != null
                                                ? "(" + vacansyIDTextField.getValue() + ")" : "")
                                        .append(" ")
                                        .append(vacansyNameField.getValue())
                                        .append(userSession.getUser().getName())
                                        .toString())).toString());
            }
        }
    }

    @Subscribe("priorityNewsCheckBox")
    public void onPriorityNewsCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (event.getValue()) {
            openPositionNewsLc.setParameter("priorityNews", true);
        } else {
            openPositionNewsLc.removeParameter("priorityNews");
        }

        openPositionNewsLc.load();
    }


    @Subscribe("openClosePositionCheckBox")
    public void onOpenClosePositionCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        openCloseCurrentStatus = openClosePositionCheckBox.getValue();

        disableEnableFields(event);

        if (!event.getValue()) {
            if (getEditedEntity().getProjectName() != null) {
                getEditedEntity().getProjectName().setProjectIsClosed(false);
            }
        }
    }

    private void disableEnableFields(HasValue.ValueChangeEvent<Boolean> event) {
/*        if (getEditedEntity().getOpenClose()) {
            cityOpenPositionField.setEditable(false);
            companyDepartamentField.setEditable(false);
            companyNameField.setEditable(false);
            numberPositionField.setEditable(false);
            positionTypeField.setEditable(false);
            projectNameField.setEditable(false);
            vacansyNameField.setEditable(false);
            companyDepartamentField.setEditable(false);
        } else {
            cityOpenPositionField.setEditable(true);
            companyDepartamentField.setEditable(true);
            companyNameField.setEditable(true);
            numberPositionField.setEditable(true);
            positionTypeField.setEditable(true);
            projectNameField.setEditable(true);
            vacansyNameField.setEditable(true);
            companyDepartamentField.setEditable(true);
        } */
    }

    @Subscribe
    public void onAfterCommitChanges1(AfterCommitChangesEvent event) {
        openCloseChildVacancy(event);

    }

    private void openCloseChildVacancy(AfterCommitChangesEvent event) {
        List<OpenPosition> openPositions = dataManager.load(OpenPosition.class)
                .query(QUERY_SELECT_COMMAND)
                .parameter("parentOpenPosition", getEditedEntity())
                .view("openPosition-view")
                .list();

        StringBuilder magPos = new StringBuilder();

        if (openPositions.size() != 0) {
            for (OpenPosition a : openPositions) {
                magPos.append("<li><i>")
                        .append(a.getVacansyName())
                        .append("</i></li>")
                        .toString();
            }

            dialogs.createOptionDialog()
                    .withType(Dialogs.MessageType.WARNING)
                    .withContentMode(ContentMode.HTML)
                    .withCaption("ВНИМАНИЕ")
                    .withMessage((!getEditedEntity().getOpenClose() ? "Открыть" : "Закрыть") +
                            " вакансии группы?<br><ul>" + magPos + "</ul>")
                    .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                        for (OpenPosition a : openPositions) {
                            a.setOpenClose(getEditedEntity().getOpenClose());
                        }
                    }), new DialogAction(DialogAction.Type.NO))
                    .show();

        }
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setOpenClose(false);
        }

        if (openPositionRichTextArea.getValue() != null &&
                !openPositionRichTextArea.getValue().trim().equals("")) {
            rescanJobDescription();

            openPositionRichTextArea.setValue(showKeyCompetition(openPositionRichTextArea.getValue()));
        }

        startPriorityStatus = priorityField.getValue();
        onNeedExercise = needExerciseCheckBox.getValue();
        startSalaryMinValue = openPositionFieldSalaryMin.getValue();
        startSalaryMaxValue = openPositionFieldSalaryMax.getValue();
        openPositionText = openPositionRichTextArea.getValue() != null ?
                Jsoup.parse(openPositionRichTextArea.getValue()).wholeText() : null;
        startLetterText = templateLetterRichTextArea.getValue() != null
                ? Jsoup.parse(templateLetterRichTextArea.getValue()).wholeText() : null;
        startVacansyName = vacansyNameField.getValue();
    }

    private String showKeyCompetition(String value) {
        for (SkillTree skillTree : skillTrees) {
            String keyWithStyle = new StringBuilder("<b><font color=\"brown>\" face=\"serif\">")
                    .append(skillTree.getSkillName())
                    .append("</font></b>")
                    .toString();
            if (!value.contains(keyWithStyle)) {
                value = value.replaceAll("(?i)" + skillTree.getSkillName(), keyWithStyle);
            }
        }

        return value;
    }

    private void sendMessage() {
        events.publish(new UiNotificationEvent(this, "Открыта новая позиция: " +
                getEditedEntity().getVacansyName()));
    }

    private Boolean sendOpenCloseMessage() {
        OpenPosition openPosition = getEditedEntity();
        r = false;

        setOK = getEditedEntity().getOpenClose();

        int a = setOK ? 1 : 0;
        int b = booOpenClosePosition ? 1 : 0;


        // Админ пусть правит без последствий
        if (!getRoleService.isUserRoles(userSession.getUser(), ADMINISTRATOR)) {
            // если что-то изменилось
            if (PersistenceHelper.isNew(getEditedEntity())) {
                emails = getAllSubscibers();
                // позиция открылась
                events.publish(new UiNotificationEvent(this, "Открыта новая позиция: " +
                        getEditedEntity().getVacansyName()));
                r = true;
            } else {
                if (a != b) {
                    if (!getEditedEntity().getOpenClose()) {

                        emails = getAllSubscibers();
                        // позиция открылась
//                        events.publish(new UiNotificationEvent(this, "Открыта новая позиция: " +
//                                getEditedEntity().getVacansyName()));

                        r = true;
                    } else {
                        setOK = true;
                    }

                    r = true;
                } else
                    r = false;
            }
        }

        return r;
    }

    @Subscribe("memoForInterviewRichTextArea")
    public void onMemoForInterviewRichTextAreaValueChange(HasValue.ValueChangeEvent<String> event) {
        if (event.getValue() != null && !event.getValue().equals("")) {
            needMemoCheckBox.setValue(true);
        } else {
            needMemoCheckBox.setValue(false);
        }
    }

    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {

        if (PersistenceHelper.isNew(getEditedEntity())) {
            OpenPosition dublicateOpenPosition = checkDublicateOpenPosition(event);

            if (dublicateOpenPosition != null) {
                dialogs.createOptionDialog()
                        .withCaption(messageBundle.getMessage("msgWarning"))
                        .withMessage("Вакансия " + vacansyNameField.getValue() + "\n" + "уже есть в базе.\n" +
                                "\nОткрыта ранее: " + dublicateOpenPosition.getCreatedBy() +
                                "\nСтатус: " + (dublicateOpenPosition.getOpenClose() ? "Закрыта" : "Открыта" +
                                "\nПродолжить сохранение?"))
                        .withActions(new DialogAction(DialogAction.Type.OK, Action.Status.PRIMARY).withHandler(e -> {
                            event.resume();
                            // вернуться и не закомитить
                        }), new DialogAction(DialogAction.Type.CANCEL).withHandler(f -> {
                            // закончить
                        }))
                        .show();

                event.preventCommit();
            }

            if (checkDublicatePositionID()) {
                dialogs.createOptionDialog()
                        .withCaption(messageBundle.getMessage("msgWarning"))
                        .withMessage(messageBundle.getMessage("msgDublicateVacancyID"))
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
    }

    private boolean checkDublicatePositionID() {


        if (dataManager.load(OpenPosition.class)
                .query(QUERY_CHECK_VACANCY_ID)
                .parameter("vacancyID", vacansyIDTextField.getValue())
                .view("openPosition-view")
                .list()
                .size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    private void publishEventMessage(BeforeCommitChangesEvent event) {
        if (getEditedEntity().getOpenClose() == null) {
            getEditedEntity().setOpenClose(false);
        }

        if (PersistenceHelper.isNew(getEditedEntity())) {
            if (getEditedEntity().getOpenClose()) {
                sendClosePositionMessage();
            } else {
                sendOpenPositionMessage();
            }
        } else {
            if (getEditedEntity().getOpenClose()) {
                if (!beforeEdit.getOpenClose().equals(getEditedEntity().getOpenClose())) {
                    sendClosePositionMessage();
                }
            } else {
                if (!beforeEdit.getOpenClose().equals(getEditedEntity().getOpenClose())) {
                    sendOpenPositionMessage();
                }
            }
        }
    }

    @Subscribe
    public void onBeforeCommitChanges3(BeforeCommitChangesEvent event) {
        publishEventMessage(event);

    }

    private void sendClosePositionMessage() {
        events.publish(new UiNotificationEvent(this, "Закрыта вакансия: " +
                getEditedEntity().getVacansyName()));
    }

    private void sendOpenPositionMessage() {
        events.publish(new UiNotificationEvent(this, "Открыта новая вакансия: " +
                getEditedEntity().getVacansyName()));
    }

    @Subscribe
    public void onBeforeCommitChanges1(BeforeCommitChangesEvent event) {
        if (openClosePositionCheckBox.getValue() == null)
            openClosePositionCheckBox.setValue(false);

        if (internalProjectCheckBox.getValue() == null)
            internalProjectCheckBox.setValue(false);

        if (PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setLastOpenDate(new Date());
        }
    }

    private OpenPosition checkDublicateOpenPosition(BeforeCommitChangesEvent event) {
        // StringIndexOutOfBoundsException: begin 0, end -1, length 2
        List<OpenPosition> openPositions = new ArrayList<>();

        try {
            openPositions = dataManager.load(OpenPosition.class)
                    .query(QUERY_OPEN_POSITION)
                    .parameter("vacansyName", vacansyNameField.getValue())
                    .parameter("positionType", positionTypeField.getValue())
                    .parameter("projectName", projectNameField.getValue())
                    .parameter("cityPosition", cityOpenPositionField.getValue())
                    .parameter("parentOpenPosition", parentOpenPositionField.getValue())
                    .parameter("remoteWork", remoteWorkField.getValue())
                    .parameter("vacansyName", vacansyNameField.getValue())
                    .view("openPosition-view")
                    .list();
        } catch (NullPointerException e) {
            log.error("Error", e);
        }

        if (openPositions.size() == 0) {
            return null;
        } else {
            return openPositions.get(0);
        }
    }

    private String getAllSubscibers() {
        LoadContext<ExtUser> loadContext = LoadContext.create(ExtUser.class)
                .setQuery(LoadContext.createQuery("select e from sec$User e"));

        List<ExtUser> listManagers = dataManager.loadList(loadContext);

        StringBuilder maillist = new StringBuilder();

        for (User user : listManagers) {
            if (user.getEmail() != null && user.getActive())
                maillist.append(user.getEmail()).append(";");
        }

        return maillist.toString();
    }

    private String getSubscriberMaillist(Entity entity) {
        List<RecrutiesTasks> listResearchers = dataManager.load(RecrutiesTasks.class)
                .query("select e " +
                        "from itpearls_RecrutiesTasks e " +
                        "where e.endDate  >= :currentDate and " +
                        "e.openPosition = :openPosition")
                .parameter("currentDate", new Date())
                .parameter("openPosition", entity)
                .view("recrutiesTasks-view")
                .list();


        StringBuilder maillist = new StringBuilder();
        Boolean subs = false;

        for (RecrutiesTasks address : listResearchers) {
            String email = address.getReacrutier().getEmail();

            if (address.getSubscribe() == null ? false : address.getSubscribe())
                // if( address.getSubscribe() )
                if (email != null)
                    maillist.append(email)
                            .append(";");
        }

        return maillist.toString();
    }

    private String getRecrutiersMaillist() {
        return "alan@itpearls.ru";
    }

    Boolean flagPriority = true;
    Integer startPriorityStatus;

    @Subscribe("priorityField")
    public void onPriorityFieldValueChange(HasValue.ValueChangeEvent<Integer> event) {
        if (event.getValue().equals(OpenPositionPriority.LOW.getId())) {
            setClosingWeek();
        }

        if (priorityField.getValue() != null) {
            if (startPriorityStatus != null) {
                if (!startPriorityStatus.equals(priorityField.getValue())) {
                    if (flagPriority) {
                        int value = (int) priorityField.getValue();
                        Optional<String> result = priorityMap.entrySet()
                                .stream()
                                .filter(entry -> value == entry.getValue())
                                .map(Map.Entry::getKey)
                                .findFirst();

                            if (event.getValue() >= 0) {
                            openPositionService.setOpenPositionNewsAutomatedMessage(getEditedEntity(),
                                    new StringBuilder("Изменен приоритет вакансии на ")
                                            .append(result.get()).toString(),
                                    "Закрыта вакансия",
                                    new Date(),
                                    (ExtUser) userSession.getUser());
                        }

                        flagPriority = false;
                    } else {
                        flagPriority = true;
                    }
                }
            }
        }
    }

    private void setClosingWeek() {
        if (closingDateDateField.getValue() == null) {
            dialogs.createOptionDialog()
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withMessage(messageBundle.getMessage("msgSetClosingVacancyWeek"))
                    .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY)
                            .withHandler(e -> {
                                GregorianCalendar calendar = new GregorianCalendar();
                                calendar.add(7, Calendar.DAY_OF_WEEK);

                                closingDateDateField.setValue(calendar.getTime());
                                initClosedVacancyTimerFacet();

                                notifications.create(Notifications.NotificationType.WARNING)
                                        .withDescription(messageBundle.getMessage("msgSetClosingVacancyWeek"))
                                        .withCaption(messageBundle.getMessage("msgWarning"))
                                        .withHideDelayMs(5000)
                                        .withPosition(Notifications.Position.DEFAULT)
                                        .show();
                            }), new DialogAction(DialogAction.Type.NO))
                    .show();
        }
    }

    Map<String, Integer> priorityMap = new LinkedHashMap<>();

    private void setRadioButtons() {
        Map<String, Integer> rwMap = new LinkedHashMap<>();
        String a = messageBundle.getMessage("mainmsgAllVariants");

        rwMap.put(messageBundle.getMessage(StandartRegistrationForWork.OUTSTAFING_MSG),
                StandartRegistrationForWork.OUTSTAFING);
        rwMap.put(messageBundle.getMessage(StandartRegistrationForWork.RECRUITING_MSG),
                StandartRegistrationForWork.RECRUITING);
        rwMap.put(messageBundle.getMessage(StandartRegistrationForWork.ALL_MSG),
                StandartRegistrationForWork.ALL);

        registrationForWorkField.setOptionsMap(rwMap);

        priorityField.setOptionsMap(standartMapsService.setPriorityMap());
        radioButtonGroupPaymentsType.setOptionsMap(standartMapsService.setPaymentsTypeMap());
        radioButtonGroupResearcherSalary.setOptionsMap(standartMapsService.setResearcherSalaryMap());
        radioButtonGroupRecrutierSalary.setOptionsMap(standartMapsService.setRecruterSalaryMap());
        remoteWorkField.setOptionsMap(standartMapsService.setRemoteWorkMap());
    }

    private void setHiddeField() {
        // скрыть менеджерские пункты
        if (isUserRoles(userSession.getUser(), MANAGER) || isUserRoles(userSession.getUser(), ADMINISTRATOR)) {
            groupBoxPaymentsDetail.setVisible(true);
            groupBoxPaymentsDetail.setCollapsable(true);
            groupBoxPaymentsResearcher.setVisible(true);
            groupBoxPaymentsRecrutier.setVisible(true);
        } else {
            groupBoxPaymentsDetail.setVisible(false);
            groupBoxPaymentsRecrutier.setVisible(false);
            groupBoxPaymentsResearcher.setVisible(false);

            if (isUserRoles(userSession.getUser(), RESEARCHER)) {
                groupBoxPaymentsResearcher.setVisible(true);
            }

            if (isUserRoles(userSession.getUser(), RECRUITER)) {
                groupBoxPaymentsRecrutier.setVisible(true);
            }
        }
    }

    @Subscribe("radioButtonGroupPaymentsType")
    public void onRadioButtonGroupPaymentsTypeValueChange(HasValue.ValueChangeEvent<Integer> event) {

        switch ((int) radioButtonGroupPaymentsType.getValue()) {
            case 0:
                textFieldPercentOrSum.setCaption("Сумма комиссии");
                textFieldPercentOrSum.setVisible(false);
                textFieldCompanyPayment.setVisible(true);
                textFieldCompanyPayment.setEditable(true);
                break;
            case 1:
                textFieldPercentOrSum.setCaption("Процент, %");
                textFieldCompanyPayment.setVisible(true);
                textFieldPercentOrSum.setVisible(true);
                textFieldCompanyPayment.setEditable(false);
                break;
            case 2:
                textFieldPercentOrSum.setCaption("Процент, %");
                textFieldCompanyPayment.setVisible(true);
                textFieldPercentOrSum.setVisible(true);
                textFieldCompanyPayment.setEditable(false);
                break;
        }

        setCalculateCompanyPercentField();

        calculateRecrutierSalary();
        calculateResearcherSalary();
    }

    @Subscribe("checkBoxUseNDFL")
    public void onCheckBoxUseNDFLValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        setCalculateCompanyPercentField();
        calculateResearcherSalary();
        calculateRecrutierSalary();
    }


    @Subscribe("textFieldPercentOrSum")
    public void onTextFieldPercentOrSumValueChange(HasValue.ValueChangeEvent<String> event) {
        setCalculateCompanyPercentField();
        calculateResearcherSalary();
        calculateRecrutierSalary();
    }

    protected void setCalculateCompanyPercentField() {
        if (textFieldPercentOrSum.getValue() != null) {
            textFieldCompanyPayment.setValue(calculateComission(textFieldPercentOrSum.getValue(),
                    (Integer) radioButtonGroupPaymentsType.getValue(),
                    checkBoxUseNDFL.getValue(),
                    openPositionFieldSalaryMin.getValue(),
                    openPositionFieldSalaryMax.getValue())
            );
        }
    }


    protected BigDecimal minCompanyComission = new BigDecimal(BigInteger.ZERO);
    protected BigDecimal maxCompanyComission = new BigDecimal(BigInteger.ZERO);

    protected String calculateComission(String percent, Integer type, boolean ndflFlag, BigDecimal
            minSalary, BigDecimal maxSalary) {

        String retValue = new String("");
        BigDecimal p = new BigDecimal(percent);
        BigDecimal ndfl = new BigDecimal(1.13);
        BigDecimal mounths = new BigDecimal(12);
        BigDecimal hungred = new BigDecimal(100);

        if (minSalary == null)
            minSalary = BigDecimal.ZERO;

        if (maxSalary == null)
            maxSalary = BigDecimal.ZERO;

        switch (type) {
            case 0:
                retValue = percent;
                minSalary = new BigDecimal(percent);
                maxSalary = new BigDecimal(percent);

                break;
            case 1:
                minSalary = minSalary.multiply(p).multiply(mounths).divide(hungred)
                        .multiply(ndflFlag ? ndfl : BigDecimal.ONE);
                maxSalary = maxSalary.multiply(p).multiply(mounths).divide(hungred)
                        .multiply(ndflFlag ? ndfl : BigDecimal.ONE);

                minSalary = minSalary.setScale(0, RoundingMode.HALF_EVEN);
                maxSalary = maxSalary.setScale(0, RoundingMode.HALF_EVEN);

                retValue = new StringBuilder("От ")
                        .append(minSalary)
                        .append(" до ")
                        .append(maxSalary)
                        .toString();
                break;
            case 2:
                minSalary = minSalary.multiply(p).multiply(ndflFlag ? ndfl : BigDecimal.ONE).divide(hungred);
                maxSalary = maxSalary.multiply(p).multiply(ndflFlag ? ndfl : BigDecimal.ONE).divide(hungred);

                minSalary = minSalary.setScale(0, RoundingMode.HALF_EVEN);
                maxSalary = maxSalary.setScale(0, RoundingMode.HALF_EVEN);

                retValue = new StringBuilder("От ")
                        .append(minSalary)
                        .append(" до ")
                        .append(maxSalary)
                        .toString();
                break;
        }

        minCompanyComission = minSalary;
        maxCompanyComission = maxSalary;

        return retValue;
    }

    @Subscribe("radioButtonGroupResearcherSalary")
    public void onRadioButtonGroupResearcherSalaryValueChange(HasValue.ValueChangeEvent event) {
        calculateResearcherSalary();

        setResearcherSalaryLabel();
    }

    @Subscribe("radioButtonGroupRecrutierSalary")
    public void onRadioButtonGroupRecrutierSalaryValueChange(HasValue.ValueChangeEvent event) {
        calculateRecrutierSalary();

        setRecrutierSalaryLabel();
    }

    protected void calculateResearcherSalary() {
        BigDecimal hungred = new BigDecimal(100);
        BigDecimal minSalary = new BigDecimal(String.valueOf(minCompanyComission));
        BigDecimal maxSalary = new BigDecimal(String.valueOf(maxCompanyComission));
        String textSalaryMessage = null;

        if (radioButtonGroupResearcherSalary.getValue() != null) {
            switch ((int) radioButtonGroupResearcherSalary.getValue()) {
                case 0:
                    textFieldResearcherSalaryPercentOrSum.setCaption("Сумма комиссии");
                    textFieldResearcherSalary.setVisible(false);
                    textFieldResearcherSalaryPercentOrSum.setVisible(true);
                    textFieldResearcherSalary.setEditable(true);

                    textSalaryMessage = textFieldResearcherSalaryPercentOrSum.getValue() + " рублей.";
                    textFieldResearcherSalary.setValue(textSalaryMessage);

                    break;
                case 1:
                    textFieldResearcherSalaryPercentOrSum.setCaption("Процент комиссии, %");
                    textFieldResearcherSalaryPercentOrSum.setVisible(false);
                    textFieldResearcherSalary.setVisible(true);
                    textFieldResearcherSalary.setEditable(false);

                    if (!maxCompanyComission.equals(BigDecimal.ZERO) &&
                            !minCompanyComission.equals(BigDecimal.ZERO)) {

                        BigDecimal percent = new BigDecimal(20);

                        textSalaryMessage = new StringBuilder("От ")
                                .append(minSalary.multiply(percent).divide(hungred).setScale(0, RoundingMode.HALF_EVEN))
                                .append(" до ")
                                .append(maxSalary.multiply(percent).divide(hungred).setScale(0, RoundingMode.HALF_EVEN))
                                .toString();

                        textFieldResearcherSalary.setValue(textSalaryMessage);
                    }

                    break;

                case 2:
                    textFieldResearcherSalaryPercentOrSum.setCaption("Процент комиссии, %");
                    textFieldResearcherSalaryPercentOrSum.setVisible(true);
                    textFieldResearcherSalary.setVisible(true);
                    textFieldResearcherSalary.setEditable(false);
                    textFieldResearcherSalary.setValue(null);

                    if (textFieldPercentOrSum.getValue() != null &&
                            !maxCompanyComission.equals(BigDecimal.ZERO) &&
                            !minCompanyComission.equals(BigDecimal.ZERO)) {

                        if (textFieldResearcherSalaryPercentOrSum.getValue() != null) {

                            BigDecimal percent = new BigDecimal(textFieldResearcherSalaryPercentOrSum.getValue());

                            textSalaryMessage = new StringBuilder("От ")
                                    .append(minSalary.multiply(percent).divide(hungred)
                                            .setScale(0, RoundingMode.HALF_EVEN))
                                    .append(" до ")
                                    .append(maxSalary.multiply(percent).divide(hungred)
                                            .setScale(0, RoundingMode.HALF_EVEN))
                                    .toString();

                            textFieldResearcherSalary.setValue(textSalaryMessage);
                        } else {
                            textFieldResearcherSalary.setValue(null);
                        }
                    } else {
                        textFieldResearcherSalary.setValue(null);
                    }

                    break;
            }
            setResearcherSalaryLabel();
        }
    }

    protected void calculateRecrutierSalary() {
        BigDecimal hungred = new BigDecimal(100);
        BigDecimal minSalary = new BigDecimal(String.valueOf(minCompanyComission));
        BigDecimal maxSalary = new BigDecimal(String.valueOf(maxCompanyComission));
        String textSalaryMessage = null;

        if (radioButtonGroupRecrutierSalary.getValue() != null) {
            switch ((int) radioButtonGroupRecrutierSalary.getValue()) {
                case 0:
                    textFieldRecrutierPercentOrSum.setCaption("Сумма комиссии");
                    textFieldRecrutierSalary.setVisible(false);
                    textFieldRecrutierPercentOrSum.setVisible(true);
                    textFieldRecrutierSalary.setEditable(true);

                    textSalaryMessage = new StringBuilder(textFieldRecrutierPercentOrSum.getValue())
                            .append(" рублей.")
                            .toString();
                    textFieldRecrutierSalary.setValue(textSalaryMessage);

                    break;
                case 1:
                    textFieldRecrutierPercentOrSum.setCaption("Процент комиссии, %");
                    textFieldRecrutierPercentOrSum.setVisible(false);
                    textFieldRecrutierSalary.setVisible(true);
                    textFieldRecrutierSalary.setEditable(false);
                    // textFieldRecrutierSalary.setValue("");

                    if (!maxCompanyComission.equals(BigDecimal.ZERO) &&
                            !minCompanyComission.equals(BigDecimal.ZERO)) {

                        BigDecimal percent = new BigDecimal(10);

                        textSalaryMessage = new StringBuilder("От ")
                                .append(minSalary.multiply(percent).divide(hungred).setScale(0, RoundingMode.HALF_EVEN))
                                .append(" до ")
                                .append(maxSalary.multiply(percent).divide(hungred).setScale(0, RoundingMode.HALF_EVEN))
                                .toString();

                        textFieldRecrutierSalary.setValue(textSalaryMessage);
                    }

                    break;

                case 2:
                    textFieldRecrutierPercentOrSum.setCaption("Процент комиссии, %");
                    textFieldRecrutierPercentOrSum.setVisible(true);
                    textFieldRecrutierSalary.setVisible(true);
                    textFieldRecrutierSalary.setEditable(false);
                    textFieldRecrutierSalary.setValue(null);

                    if (textFieldPercentOrSum.getValue() != null &&
                            !maxCompanyComission.equals(BigDecimal.ZERO) &&
                            !minCompanyComission.equals(BigDecimal.ZERO)) {
                        if (textFieldRecrutierPercentOrSum.getValue() != null) {

                            BigDecimal percent = new BigDecimal(textFieldResearcherSalaryPercentOrSum.getValue());

                            textSalaryMessage = new StringBuilder("От ")
                                    .append(minSalary.multiply(percent).divide(hungred)
                                            .setScale(0, RoundingMode.HALF_EVEN))
                                    .append(" до ")
                                    .append(maxSalary.multiply(percent).divide(hungred)
                                            .setScale(0, RoundingMode.HALF_EVEN))
                                    .toString();

                            textFieldRecrutierSalary.setValue(textSalaryMessage);
                        } else {
                            textFieldRecrutierSalary.setValue(null);
                        }
                    } else {
                        textFieldRecrutierSalary.setValue(null);
                    }

                    break;
            }
            setRecrutierSalaryLabel();
        }
    }

    @Subscribe("textFieldRecrutierPercentOrSum")
    public void onTextFieldRecrutierPercentOrSumValueChange(HasValue.ValueChangeEvent<String> event) {
        calculateRecrutierSalary();
    }

    @Subscribe("textFieldResearcherSalaryPercentOrSum")
    public void onTextFieldResearcherSalaryPercentOrSumValueChange(HasValue.ValueChangeEvent<String> event) {
        calculateResearcherSalary();
    }

    @Subscribe("textFieldRecrutierSalary")
    public void onTextFieldRecrutierSalaryValueChange(HasValue.ValueChangeEvent<String> event) {
        // calculateRecrutierSalary();

        // setRecrutierSalaryLabel();
    }

    @Subscribe("textFieldResearcherSalary")
    public void onTextFieldResearcherSalaryValueChange(HasValue.ValueChangeEvent<String> event) {
        // calculateResearcherSalary();

        // setResearcherSalaryLabel();
    }

    private void setResearcherSalaryLabel() {
        if (radioButtonGroupResearcherSalary.getValue() != null) {
            if ((int) radioButtonGroupResearcherSalary.getValue() == 0) {
                if (textFieldResearcherSalary.getValue() != null) {
                    labelResearcherSalary.setValue(new StringBuilder("Зарплата ресерчера после закрытия вакансии \"<i>")
                            .append(vacansyNameField.getValue())
                            .append("</i>\" составит ")
                            .append(textFieldResearcherSalary.getValue())
                            .append(" рублей.")
                            .toString());

                    groupBoxPaymentsResearcher.setVisible(true);
                } else
                    groupBoxPaymentsResearcher.setVisible(false);
            } else {
                if (textFieldResearcherSalary.getValue() != null) {
                    labelResearcherSalary.setValue(new StringBuilder("Зарплата ресерчера после закрытия вакансии \"<i>")
                            .append(vacansyNameField.getValue())
                            .append("</i>\" составит ")
                            .append(textFieldResearcherSalary.getValue())
                            .append(" рублей.")
                            .toString());
                    groupBoxPaymentsResearcher.setVisible(true);
                    groupBoxPaymentsResearcher.setVisible(true);
                } else
                    groupBoxPaymentsResearcher.setVisible(false);
            }
        }
    }

    private void setRecrutierSalaryLabel() {
        if (radioButtonGroupRecrutierSalary.getValue() != null) {
            if ((int) radioButtonGroupRecrutierSalary.getValue() == 0) {
                if (textFieldRecrutierSalary.getValue() != null) {
                    labelRecrutierSalary.setValue(new StringBuilder("Зарплата рекрутера после закрытия вакансии \"<i>")
                            .append(vacansyNameField.getValue())
                            .append("</i>\" составит ")
                            .append(textFieldRecrutierSalary.getValue())
                            .append(" рублей.")
                            .toString());
                }
            } else {
                if (textFieldRecrutierSalary.getValue() != null) {
                    labelRecrutierSalary.setValue(new StringBuilder("Зарплата рекрутера после закрытия вакансии \"<i>")
                            .append(vacansyNameField.getValue())
                            .append("</i>\" составит ")
                            .append(textFieldRecrutierSalary.getValue())
                            .append(" рублей.")
                            .toString());
                }
            }
        }

        setHiddeField();
    }

    @Subscribe
    public void onBeforeShow1(BeforeShowEvent event) {
        // показываем или нет все строки ввода в оплаты
        if (radioButtonGroupPaymentsType.getValue() == null) {
            textFieldPercentOrSum.setVisible(false);
            textFieldCompanyPayment.setVisible(false);
        }

        if (radioButtonGroupResearcherSalary.getValue() == null) {
            textFieldResearcherSalaryPercentOrSum.setVisible(false);
            textFieldResearcherSalary.setVisible(false);
        }

        if (radioButtonGroupRecrutierSalary.getValue() == null) {
            textFieldRecrutierPercentOrSum.setVisible(false);
            textFieldRecrutierSalary.setVisible(false);
        }

        setTopLabel();
        setIconSomeFileTab();

        if (openPositionRichTextArea.getValue() != null) {
            openPositionRichTextArea.setValue(showKeyCompetition(openPositionRichTextArea.getValue()));
        }

        openClosePositionCheckBox.setValue(openClosePositionCheckBox.getValue() == null ? false : true);

        setInitApprovalProcess();
    }

    private void setIconSomeFileTab() {
        if (someFilesesDc.getItems().size() > 0) {
            tabFiles.setIconFromSet(CubaIcon.FILE_TEXT_O);
        } else {
            tabFiles.setIconFromSet(CubaIcon.FILE_O);
        }
    }

    @Subscribe(id = "someFilesesDc", target = Target.DATA_CONTAINER)
    public void onSomeFilesesDcCollectionChange(CollectionContainer.CollectionChangeEvent<SomeFilesOpenPosition> event) {
        setIconSomeFileTab();
    }

    private void setInitApprovalProcess() {
        UUID entityId = getEditedEntity().getId();
        procAttachmentsDl.setParameter("entityId", entityId);
        procAttachmentsDl.load();
        procActionsFragment.initializer()
                .standard()
                .init(PROCESS_CODE, getEditedEntity());
    }

    private void setTopLabel() {
        try {
            if (vacansyNameField.getValue() != null && projectNameField.getValue() != null) {
                if (projectNameField.getValue() != null) {
                    if (projectNameField.getValue().getProjectDepartment() != null) {
                        if (projectNameField.getValue().getProjectDepartment().getCompanyName() != null) {
                            if (projectNameField.getValue().getProjectDepartment().getCompanyName() != null) {
                                if (projectNameField.getValue().getProjectDepartment().getCompanyName().getComanyName() != null) {
                                    String comanyName = projectNameField.getValue().getProjectDepartment().getCompanyName().getComanyName();

                                    labelOpenPosition.setValue(
                                            new StringBuilder(vacansyNameField.getValue())
                                                    .append(" (")
                                                    .append(comanyName != null ? comanyName : "")
                                                    .append(" : ")
                                                    .append(projectNameField.getValue().getProjectName())
                                                    .append(")").toString());
                                    labelOpenPosition.addStyleName("h3");
                                }

                                // а еще вывести комиссию
                                if (getRoleService.isUserRoles(userSession.getUser(), RESEARCHER)) {
                                    labelTopComissionResearcher.setValue(labelResearcherSalary.getValue());
                                    labelTopComissionResearcher.setVisible(true);

                                    labelTopComissionRecrutier.setVisible(false);
                                } else {
                                    labelTopComissionResearcher.setVisible(false);
                                }

                                if (getRoleService.isUserRoles(userSession.getUser(), RECRUITER)) {
                                    labelTopComissionRecrutier.setValue(labelRecrutierSalary.getValue());
                                    labelTopComissionRecrutier.setVisible(true);

                                    labelTopComissionResearcher.setVisible(false);
                                } else {
                                    labelTopComissionRecrutier.setVisible(false);
                                }

                                if (getRoleService.isUserRoles(userSession.getUser(), MANAGER)) {
                                    labelTopComissionRecrutier.setVisible(false);
                                    labelTopComissionResearcher.setVisible(false);
                                }
                            }
                        }
                    }
                }
            }

        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Subscribe("labelRecrutierSalary")
    public void onLabelRecrutierSalaryValueChange(HasValue.ValueChangeEvent<String> event) {
        setTopLabel();
    }

    @Subscribe("labelResearcherSalary")
    public void onLabelResearcherSalaryValueChange(HasValue.ValueChangeEvent<String> event) {
        setTopLabel();
    }

    @Subscribe
    public void onInit(InitEvent event) {
        setRadioButtons();
        setGroupSubscribeButton();
        setGroupCommandRadioButtin();
        skillImageColumnRenderer();

        setOpenPositionNewsDetailsGenerator();
    }

    private void setOpenPositionNewsDetailsGenerator() {
        openPostionNewsDataGrid.setItemClickAction(new BaseAction("itemClickAction")
                .withHandler(actionPerformedEvent -> openPostionNewsDataGrid
                        .setDetailsVisible(openPostionNewsDataGrid.getSingleSelected(), true)));
    }

    private void setGroupCommandRadioButtin() {
        Map<String, Integer> map = new LinkedHashMap<>();

        map.put("Команда", 1);
        map.put("Вакансия", 0);

        commandOrPosition.setOptionsMap(map);
    }

    @Subscribe("projectNameField")
    public void onProjectNameFieldValueChange1(HasValue.ValueChangeEvent<Project> event) {
        if (event.getValue() != null) {
            if (event.getValue().getProjectIsClosed()) {
                dialogs.createOptionDialog(Dialogs.MessageType.WARNING)
                        .withContentMode(ContentMode.HTML)
                        .withMessage("Вы пытаетесь открыть позицию по закрытому проекту.<br>Открыть проект заново?")
                        .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                            event.getValue().setEndProjectDate(null);
                            event.getValue().setProjectIsClosed(false);
                        }), new DialogAction(DialogAction.Type.NO).withHandler((f -> {
                            projectNameField.setValue(null);
                        })));
            }
        }
    }

    private void setGroupSubscribeButton() {
//        groupSubscribe.setVisible(userSession.getUser().getGroup().getName().equals(MANAGEMENT_GROUP) ||
//                userSession.getUser().getGroup().getName().equals(HUNTING_GROUP));
    }

    @Install(to = "registrationForWorkField", subject = "optionIconProvider")
    private String registrationForWorkFieldOptionImageProvider(Integer integer) {
        String returnIcon = "";

        switch (integer) {
            case 0:
                returnIcon = "font-icon:PLUS_CIRCLE";
                break;
            case 1:
                returnIcon = "font-icon:MINUS_CIRCLE";
                break;
            case 2:
                returnIcon = "font-icon:QUESTION_CIRCLE";
                break;
            default:
                returnIcon = "font-icon:QUESTION_CIRCLE";
        }

        return returnIcon;
    }

    @Install(to = "registrationForWorkField", subject = "optionStyleProvider")
    private String registrationForWorkFieldOptionStyleProvider(Integer integer) {
        String returnIcon = "";

        switch (integer) {
            case 0:
                returnIcon = "open-position-pic-center-large-green";
                break;
            case 1:
                returnIcon = "open-position-pic-center-large-red";
                break;
            case 2:
                returnIcon = "open-position-pic-center-large-orange";
                break;
            default:
                returnIcon = "open-position-pic-center-large-yellow";
        }

        return returnIcon;
    }

    @Install(to = "remoteWorkField", subject = "optionIconProvider")
    private String remoteWorkFieldOptionIconProvider(Integer integer) {
        String returnIcon = "";

        switch (integer) {
            case 1:
                returnIcon = "font-icon:PLUS_CIRCLE";
                break;
            case 0:
                returnIcon = "font-icon:MINUS_CIRCLE";
                break;
            case 2:
                returnIcon = "font-icon:QUESTION_CIRCLE";
                break;
            default:
                returnIcon = "fint-icon:QUESTION_CIRCLE";
                break;
        }

        return returnIcon;
    }

    @Install(to = "priorityField", subject = "optionIconProvider")
    private String priorityFieldOptionIconProvider(Integer integer) {

        String icon = null;

        switch (integer) {
            case -1:
                icon = "icons/traffic-lights_gray.png";
                break;
            case 0: //"Paused"
                icon = "icons/remove.png";
                break;
            case 1: //"Low"
                icon = "icons/traffic-lights_blue.png";
                break;
            case 2: //"Normal"
                icon = "icons/traffic-lights_green.png";
                break;
            case 3: //"High"
                icon = "icons/traffic-lights_yellow.png";
                break;
            case 4: //"Critical"
                icon = "icons/traffic-lights_red.png";
                break;
        }

        return icon;
    }

    @Subscribe(target = Target.DATA_CONTEXT)
    public void onChange(DataContext.ChangeEvent event) {
        entityIsChanged = true;
    }

    public void subscribePosition() {
        Screen opScreen = screenBuilders
                .editor(RecrutiesTasks.class, this)
                .newEntity()
                .withInitializer(data -> {
                    data.setOpenPosition(this.getEditedEntity());
                })
                .newEntity()
                .withParentDataContext(dataContext)
                .withScreenId("itpearls_RecrutiesTasks.edit")
                .withLaunchMode(OpenMode.DIALOG)
                .build();


        opScreen.show();
    }

    public Boolean isUserRoles(User user, String role) {
        Collection<String> s = userSessionSource.getUserSession().getRoles();
        Boolean c = false;
        // установить поле рекрутера
        for (String a : s) {
            if (a.equalsIgnoreCase(role)) {
                c = true;
                break;
            }
        }
        return c;
    }

    @Subscribe("positionTypeField")
    public void onPositionTypeFieldValueChange(HasValue.ValueChangeEvent<Position> event) {
        if (vacansyNameField.getValue() == null || vacansyNameField.getValue().equals("")) {
            vacansyNameField.setValue(generatePositionName());
            if (projectNameField.getValue() != null) {
                vacansyNameField.setValue(generatePositionNameInProject());
                if (cityOpenPositionField.getValue() != null) {
                    vacansyNameField.setValue(generatePositionNameCity());
                }
            }
        }
    }

    @Subscribe("projectNameField")
    public void onProjectNameFieldValueChange(HasValue.ValueChangeEvent<Project> event) {
        vacansyNameField.setValue(generatePositionNameInProject());

        setCompanyDepartmentFromProject();
        setProjectImage(event);
        setProjectOwnerImage(event);
    }

    private void setProjectOwnerImage(HasValue.ValueChangeEvent<Project> event) {
        StringBuilder sb = new StringBuilder();
        sb.append(event.getValue().getProjectOwner().getFirstName())
                .append(" ")
                .append(event.getValue().getProjectOwner().getSecondName());
        if (event.getValue().getProjectOwner().getPersonPosition() != null) {
            sb.append(" / ")
                    .append(event.getValue().getProjectOwner().getPersonPosition().getPositionRuName());
        }
        if (event.getValue().getProjectOwner().getCompanyDepartment() != null) {
            sb.append(" / ")
                    .append(event.getValue().getProjectOwner().getCompanyDepartment().getDepartamentRuName());
            if (event.getValue().getProjectOwner().getCompanyDepartment().getCompanyName() != null) {
                sb.append(" / ")
                        .append(event.getValue().getProjectOwner().getCompanyDepartment().getCompanyName().getComanyName());
            }
        }

        if (event.getValue().getProjectOwner().getCityOfResidence() != null) {
            sb.append(" / ")
                    .append(event.getValue().getProjectOwner().getCityOfResidence().getCityRuName());
        }

        projectOwnerImage.setDescription(sb.toString());

        if (event.getValue() != null) {
            if (event.getValue().getProjectName() != null) {
                if (event.getValue().getProjectOwner().getFileImageFace() != null) {
                    projectOwnerImage.setVisible(true);
                    projectOwnerImage.setValueSource(
                            new ContainerValueSource<>(openPositionDc, "projectName.projectOwner.fileImageFace"));
                } else {
                    projectOwnerImage.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
                }
            } else {
                projectOwnerImage.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
            }
        } else {
            projectOwnerImage.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
        }
    }

    private void setProjectImage(HasValue.ValueChangeEvent<Project> event) {
        if (event.getValue() != null) {
            if (event.getValue().getProjectName() != null) {
                if (event.getValue().getProjectLogo() != null) {
                    projectLogoImage.setValueSource(
                            new ContainerValueSource<>(openPositionDc, "projectName.projectLogo"));
                } else {
                    projectLogoImage.setSource(ThemeResource.class).setPath("icons/no-company.png");
                }
            } else {
                projectLogoImage.setSource(ThemeResource.class).setPath("icons/no-company.png");
            }
        } else {
            projectLogoImage.setSource(ThemeResource.class).setPath("icons/no-company.png");
        }
    }

    private String generatePositionNameInProject() {
        StringBuilder retValue = new StringBuilder(vacansyNameField.getValue());

        if (vacansyNameField.getValue() != null) {
            if (generatePositionName().equals(retValue.toString())) {
                if (projectNameField.getValue() != null) {
                    if (projectNameField.getValue().getProjectName() != null) {
                        retValue.append(" (")
                                .append(projectNameField.getValue().getProjectName())
                                .append(")");
                    }
                }
            }
        }

        return retValue.toString();
    }

    private String generatePositionNameCity() {
        StringBuilder retValue = new StringBuilder(vacansyNameField.getValue());
//        String retValue = vacansyNameField.getValue();

        if (PersistenceHelper.isNew(getEditedEntity())) {
            if (cityOpenPositionField.getValue() != null) {
                if (retValue != null) {
                    if (generatePositionNameInProject().equals(retValue.toString())) {
                        retValue = new StringBuilder(retValue.substring(0, retValue.length() - 1))
                                .append(", ")
                                .append(cityOpenPositionField.getValue().getCityRuName())
                                .append(")");
                    }
                } else {
                    retValue = null;
                }
            }
        }

        return retValue != null ? retValue.toString() : null;
    }

    @Subscribe("cityOpenPositionField")
    public void onCityOpenPositionFieldValueChange(HasValue.ValueChangeEvent<City> event) {
        vacansyNameField.setValue(generatePositionNameCity());
    }

    protected String generatePositionName() {
        String retPosName = "";


        if (positionTypeField.getValue() != null) {
            if (positionTypeField.getValue().getPositionEnName() == null) {
                notifications.create(Notifications.NotificationType.WARNING)
                        .withHideDelayMs(3000)
                        .withDescription("Не заполнено наименование типа позиции")
                        .show();

                screenBuilders.editor(positionTypeField)
                        .editEntity(positionTypeField.getValue())
                        .withScreenClass(PositionEdit.class)
                        .withLaunchMode(OpenMode.DIALOG)
                        .withParentDataContext(dataContext)
                        .build()
                        .show();

                positionTypesLc.load();
            }

            retPosName =
                    (new StringBuilder(positionTypeField.getValue().getPositionRuName() != null ? positionTypeField.getValue().getPositionRuName() : ""))
                            .append(" \\ ")
                            .append(positionTypeField.getValue().getPositionEnName() != null ? positionTypeField.getValue().getPositionEnName() : "")
                            .toString();
        }

        return retPosName;
    }

    public void openClosePosition() {
        String message = "";

        if (!openClosePositionCheckBox.getValue())
            message = "Закрыть";
        else
            message = "Открыть";

        dialogs.createOptionDialog()
                .withMessage(new StringBuilder(message)
                        .append(" позицию \"")
                        .append(vacansyNameField.getValue())
                        .append("\"?")
                        .toString())
                .withCaption(messageBundle.getMessage("msgWarning"))
                .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {

                    if (this.openClosePositionCheckBox.getValue())
                        this.closeWithCommit();
                    return;

                }), new DialogAction(DialogAction.Type.NO))
                .show();
    }

    public void addListCity() {
        SelectCitiesLocation selectCitiesLocation = screens.create(SelectCitiesLocation.class);

        selectCitiesLocation.addAfterShowListener(e -> {
            selectCitiesLocation.setCitiesList(this.getEditedEntity().getCities());
        });
        selectCitiesLocation.addAfterCloseListener(e -> {
            List<City> cities = selectCitiesLocation.getCitiesList();

            openPositionDc.getItem().setCities(cities);
            dataContext.merge(openPositionDc.getItem());

            changeCityListsLabel();
        });

        selectCitiesLocation.show();
    }

    @Subscribe
    public void onBeforeCommitChanges2(BeforeCommitChangesEvent event) {
        if (shortDescriptionTextArea.getValue() != null) {
            if (shortDescriptionTextArea.getValue().length() > 250) {
                notifications
                        .create(Notifications.NotificationType.ERROR)
                        .withCaption("Строка \"Краткое описание ключевых навыков не более 250 символов")
                        .show();
                shortDescriptionTextArea.focus();
                event.preventCommit();
            }
        }
    }

    private void changeCityListsLabel() {
        StringBuilder outStr = new StringBuilder();
        StringBuilder description = new StringBuilder();

        if (getEditedEntity().getCities() != null) {
            for (City s : getEditedEntity().getCities()) {
                if (!outStr.equals("")) {
                    outStr.append(",");
                    description.append("\n");
                }

                outStr.append(s.getCityRuName());
                description.append(s.getCityRuName());
            }

        }

        if (!outStr.equals("")) {
            citiesLabel.setValue(outStr.toString());
            citiesLabel.setDescription(description.toString());
        }
    }

    public void rescanJobDescription() {
        String inputText = Jsoup.parse(openPositionRichTextArea.getValue()).wholeText();
        skillTrees = pdfParserService.parseSkillTree(inputText);
        getEditedEntity().setSkillsList(skillTrees);
    }

    @Subscribe("openPositionRichTextArea")
    public void onOpenPositionRichTextAreaValueChange(HasValue.ValueChangeEvent<String> event) {
        if (openPositionRichTextArea.getValue() != null &&
                !openPositionRichTextArea.getValue().trim().equals(""))
            rescanJobDescription();
    }

    private void skillImageColumnRenderer() {
        openPositionSkillsListTable.addGeneratedColumn("fileImageLogo", entity -> {
            Image image = uiComponents.create(Image.NAME);
            image.setValueSource(new ContainerValueSource<SkillTree, FileDescriptor>(entity.getContainer(),
                    "fileImageLogo"));
            image.setWidth("50px");
            image.setStyleName("image-candidate-face-little-image");
            image.setScaleMode(Image.ScaleMode.CONTAIN);
            image.setAlignment(Component.Alignment.MIDDLE_CENTER);
            return image;
        });
    }

    @Install(to = "openPositionSkillsListTable.isComment", subject = "columnGenerator")
    private Object openPositionSkillsListTableIsCommentColumnGenerator(DataGrid.ColumnGeneratorEvent<SkillTree> event) {
        if (event.getItem().getComment() != null && !event.getItem().equals("")) {
            return CubaIcon.PLUS_CIRCLE;
        } else {
            return CubaIcon.MINUS_CIRCLE;
        }
    }

    @Install(to = "openPositionSkillsListTable.isComment", subject = "styleProvider")
    private String openPositionSkillsListTableIsCommentStyleProvider(SkillTree skillTree) {
        if (skillTree.getComment() != null && !skillTree.equals("")) {
            return "pic-center-large-green";
        } else {
            return "pic-center-large-red";
        }
    }

    @Install(to = "openPositionSkillsListTable", subject = "rowDescriptionProvider")
    private String openPositionSkillsListTableRowDescriptionProvider(SkillTree skillTree) {
        return skillTree.getComment() != null ? Jsoup.parse(skillTree.getComment()).wholeText() : "";
    }

    public void addShortDescription() {
        if (openPositionRichTextArea.getValue() != null) {
            List<SkillTree> skillTrees = pdfParserService
                    .parseSkillTree(Jsoup.parse(openPositionRichTextArea.getValue()).wholeText());
            StringBuilder sb = new StringBuilder();

            for (SkillTree skillTree : skillTrees) {
                if (skillTree.getSkillTree() != null) {
                    sb.insert(0, skillTree.getSkillName()).append(";");
                }
            }

            shortDescriptionTextArea.setValue(sb.toString());
        }
    }

    @Subscribe("positionTypeField")
    public void onPositionTypeFieldValueChange1(HasValue.ValueChangeEvent<Position> event) {
        if (event.getValue() != null) {
            if (event.getValue().getStandartDescription() != null) {
                openPositionStandartDescriptionRichTextArea.setValue(event.getValue().getStandartDescription());
            }

            if (event.getValue().getWhoIsThisGuy() != null) {
                openPositionWhoIsThisGuyRichTextArea.setValue(event.getValue().getWhoIsThisGuy());
            }
        }
    }

    @Subscribe("more10NumberPositionField")
    public void onMore10NumberPositionFieldValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (event.getValue() != null) {
            if (event.getValue()) {
                numberPositionField.setRequired(false);
            } else {
                numberPositionField.setRequired(true);
            }
        }

    }

    public void addOpenPositionNewsButton() {
        screenBuilders.editor(OpenPositionNews.class, this)
                .newEntity()
                .withOpenMode(OpenMode.DIALOG)
                .withInitializer(e -> {
                    e.setOpenPosition(getEditedEntity());
                })
                .show();
    }

    @Subscribe("signDraftCheckBox")
    public void onSignDraftCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (event.getValue() != null) {
            if (event.getValue()) {
                signDraftLabel.setValue("(DRAFT)");
                signDraftCheckBox.setStyleName("h2-gray");
                priorityField.setValue(-1);
            } else {
                signDraftLabel.setValue("");
                signDraftCheckBox.setStyleName("h2");
                priorityField.setValue(null);
            }
        } else {
            signDraftLabel.setValue("");
            signDraftCheckBox.setStyleName("h2");
            priorityField.setValue(null);
        }
    }

    @Subscribe("salaryCandidateRequestCheckBox")
    public void onSalaryCandidateRequestCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (event.getValue()) {
            openPositionFieldSalaryMin.setEnabled(false);
            openPositionFieldSalaryMax.setEnabled(false);
            openPositionFieldSalaryIE.setEnabled(false);
        } else {
            openPositionFieldSalaryMin.setEnabled(true);
            openPositionFieldSalaryMax.setEnabled(true);
            openPositionFieldSalaryIE.setEnabled(true);
        }
    }

    @Subscribe("gradeLookupPickerField")
    public void onGradeLookupPickerFieldValueChange(HasValue.ValueChangeEvent<Grade> event) {
        boolean flag = false;

        for (Grade grade : gradeDc.getItems()) {
            if (vacansyNameField.getValue() != null) {
                if (vacansyNameField.getValue().startsWith(grade.getGradeName())) {
                    vacansyNameField.setValue(new StringBuilder(event.getValue().getGradeName())
                            .append(vacansyNameField.getValue()
                                    .substring(grade.getGradeName().length()))
                            .toString());
                    flag = true;
                    break;
                }
            } else {
                notifications.create(Notifications.NotificationType.WARNING)
                        .withCaption(messageBundle.getMessage("msgWarning"))
                        .withDescription(messageBundle.getMessage("msgNotSetGrade"))
                        .withType(Notifications.NotificationType.WARNING)
                        .show();
                flag = true;
                break;
            }
        }

        if (!flag) {
            vacansyNameField.setValue(new StringBuilder(event.getValue().getGradeName())
                    .append(" ")
                    .append(vacansyNameField.getValue())
                    .toString());
        }
    }

    public void generateNameFieldButton() {
        if (vacansyNameField.getValue() == null) {
            vacansyNameField.setValue(generateVacancyName());
        } else {
            dialogs.createOptionDialog(Dialogs.MessageType.WARNING)
                    .withMessage(messageBundle.getMessage("msgRenameVacancy"))
                    .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                        vacansyNameField.setValue(generateVacancyName());
                    }), new DialogAction(DialogAction.Type.NO))
                    .show();

        }
    }

    private String generateVacancyName() {
//        String retStr = "";
        StringBuilder sb = new StringBuilder();

        if (gradeLookupPickerField.getValue() != null) {
            sb.append(gradeLookupPickerField.getValue().getGradeName())
                    .append(" ");
        } else {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .withContentMode(ContentMode.HTML)
                    .withType(Notifications.NotificationType.HUMANIZED)
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withDescription(new StringBuilder(messageBundle.getMessage("msgGenerateError"))
                            .append(": ")
                            .append(messageBundle.getMessage("msgNotGrade"))
                            .toString())
                    .show();
        }

        if (positionTypeField.getValue() != null) {
            sb.append(positionTypeField.getValue().getPositionRuName())
                    .append(" / ")
                    .append(positionTypeField.getValue().getPositionEnName());
        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .withContentMode(ContentMode.HTML)
                    .withType(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("msgError"))
                    .withDescription(new StringBuilder(messageBundle.getMessage("msgGenerateError"))
                            .append(": ")
                            .append(messageBundle.getMessage("msgNotPositionName"))
                            .toString())
                    .show();

            return "";
        }

        if (projectNameField.getValue() != null) {
            sb.append(" (")
                    .append(projectNameField.getValue().getProjectName());
        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .withContentMode(ContentMode.HTML)
                    .withType(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("msgError"))
                    .withDescription(new StringBuilder(messageBundle.getMessage("msgGenerateError"))
                            .append(": ")
                            .append(messageBundle.getMessage("msgNotProjectName")).toString())
                    .show();

            return "";
        }

        if (cityOpenPositionField.getValue() != null) {
            sb.append(", ")
                    .append(cityOpenPositionField.getValue().getCityRuName());
        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .withContentMode(ContentMode.HTML)
                    .withType(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("msgError"))
                    .withDescription(new StringBuilder(messageBundle.getMessage("msgGenerateError"))
                            .append(": ")
                            .append(messageBundle.getMessage("msgNotCity"))
                            .toString())
                    .show();

            return "";
        }

        if (openPositionDc.getItem() != null) {
            if (openPositionDc.getItem().getCities() != null) {
                if (openPositionDc.getItem().getCities().size() > 0) {
                    for (City city : openPositionDc.getItem().getCities()) {
                        sb.append(", ")
                                .append(city.getCityRuName());
                    }
                }
            }
        }

        sb.append(")");

        return sb.toString();
    }

    public void setSalaryFieldButtonInvoke() {
        OutstaffingRates outstaffingRates = null;

        try {
            outstaffingRates = dataManager.load(OutstaffingRates.class)
                    .query(QUERY_OUTSTAFF_RATES)
                    .parameter("rate", outstaffingCostTextField.getValue())
                    .view("outstaffingRates-view")
                    .one();
        } catch (NoResultException | IllegalStateException e) {
            e.printStackTrace();

            notifications.create(Notifications.NotificationType.ERROR)
                    .withType(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("msgError"))
                    .withDescription(new StringBuilder(messageBundle.getMessage("msgErrorNotCostForSalary"))
                            .append(" ")
                            .append(outstaffingCostTextField.getValue())
                            .toString())
                    .show();
        }

        if (outstaffingRates != null) {
            String commentSalary = new StringBuilder(messageBundle.getMessage("msgMarginalRate"))
                    .append(outstaffingCostTextField.getValue())
                    .append(" \n")
                    .append(messageBundle.getMessage("msgMinSalary"))
                    .append(outstaffingRates.getMinSalary())
                    .append(" \n")
                    .append(messageBundle.getMessage("msgMaxSalary"))
                    .append(outstaffingRates.getMaxSalary())
                    .append(" \n")
                    .append(messageBundle.getMessage("msgSalaryIE"))
                    .append(": ")
                    .append(outstaffingRates.getMaxIESalary())
                    .toString();

            openPositionFieldSalaryMin.setValue(outstaffingRates.getMinSalary());
            openPositionFieldSalaryMax.setValue(outstaffingRates.getMaxSalary());
            openPositionFieldSalaryIE.setValue(outstaffingRates.getMaxIESalary());
            salaryCommentTextFiels.setValue(commentSalary);

            notifications.create(Notifications.NotificationType.SYSTEM)
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withDescription(new StringBuilder(messageBundle.getMessage("msgSetSalaryToForm"))
                            .append(" ")
                            .append(commentSalary)
                            .toString())
                    .show();

        } else {
            /* notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("msgError"))
                    .withDescription(messageBundle.getMessage("msgNotRate")
                            + outstaffingCostTextField.getValue())
                    .show(); */
        }
    }
}
