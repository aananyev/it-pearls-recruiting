package com.company.hunttech.web.screens.openposition;

import com.company.hunttech.UiNotificationEvent;
import com.company.hunttech.core.*;
import com.company.hunttech.entity.*;
import com.company.hunttech.service.GetRoleService;
import com.company.hunttech.web.StandartRegistrationForWork;
import com.company.hunttech.web.screens.position.PositionEdit;
import com.company.hunttech.web.util.FileDescriptorImageHelper;
import com.hunttech.hrm.gui.components.OvaFallbackImage;
import com.haulmont.bpm.entity.ProcAttachment;
import com.haulmont.bpm.gui.procactionsfragment.ProcActionsFragment;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.core.global.ViewBuilder;
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


@UiController("hunttech_OpenPosition.edit")
@UiDescriptor("open-position-edit.xml")
@EditedEntityContainer("openPositionDc")
@LoadDataBeforeShow
/**
 * Контроллер формы редактирования позиции (вакансии) HRM HuntTech
 * ({@code hunttech_OpenPosition.edit}).
 *
 * <p>Форма содержит 12 вкладок: «О вакансии» (идентификаторы, проект/компания/город,
 * настройки команды, приоритет, зарплата, количество персонала, аккордеон описаний),
 * «Трудовые соглашения», «Оплата» (схемы комиссий ресурсера/рекрутера), «Описание
 * должности» (опыт, RU/EN/стандартное описание, «кто этот парень»), «Файлы»,
 * «Тестовое задание», «Памятка к собеседованию», «Шаблон письма», «Навыки»,
 * «Новости», «Согласование» (BPM) и «Комментарии».</p>
 *
 * <p>Тяжёлые LOB-поля (comment, commentEn, exercise, templateLetter, memoForInterview)
 * и коллекции вкладок не входят в edit-view: они догружаются lazy при первом открытии
 * вкладки (флаги {@code *Loaded}), чтобы не тянуть большие тексты при открытии формы.</p>
 *
 * <p>Перед коммитом контроллер проверяет уникальность {@code vacansyID}, собирает списки
 * подписчиков и формирует уведомления (email/Telegram) об открытии/закрытии позиции;
 * после коммита синхронизирует статус дочерних вакансий ({@code openCloseChildVacancy}).
 * Зарплатные вилки проходят валидацию (мин ≤ макс), комиссии пересчитываются при изменении
 * схем оплаты; таймер {@code closedVacancyTimer} обеспечивает автозакрытие по closingDate.</p>
 *
 * @see OpenPositionBrowse
 * @see OpenPositionServiceBean
 */
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
    private ApplicationSetupService applicationSetupService;
    @Inject
    private TextManipulationService textManipulationService;

    @Subscribe("closedVacancyTimer")
    /** Тик таймера (60 с): обновление обратного отсчёта и автозакрытие вакансии при наступлении closingDate. */
    public void onClosedVacancyTimerTimerAction(Timer.TimerActionEvent event) {
        updateClosingVacancyLabel();
    }

    @Subscribe("closingDateDateField")
    /** Изменение даты закрытия → пересчёт метки обратного отсчёта. */
    public void onClosingDateDateFieldValueChange(HasValue.ValueChangeEvent<Date> event) {
        initClosedVacancyTimerFacet();
    }

    /** Обновление предупреждения о скором автоматическом закрытии вакансии. */
    private void updateClosingVacancyLabel() {
        closedVacancyInfoLabel.setValue(new StringBuilder()
                .append(messageBundle.getMessage("msgClosingVacancyAfter"))
                .append(" ")
                .append(getTimerClosingVacancyValue(closingDateDateField.getValue()))
                .toString());
    }

    /** Текст обратного отсчёта до автоматического закрытия («Вакансия будет закрыта автоматически через N»). */
    private String getTimerClosingVacancyValue(Date closingDate) {
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

    private static final String QUERY_OUTSTAFF_RATES = "select e from hunttech_OutstaffingRates e where e.rate = :rate";
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
    private static final String MANAGEMENT_GROUP = "Менеджмент";
    private static final String HUNTING_GROUP = "Хантинг";

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
    private FileLoader fileLoader;
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
    static String QUERY_SELECT_COMMAND = "select e from hunttech_OpenPosition e where e.parentOpenPosition = :parentOpenPosition and e.openClose = false";
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
    private CollectionLoader<SkillTree> openPositionSkillsListsDl;
    @Inject
    private CollectionContainer<SkillTree> openPositionSkillsListsDc;
    @Inject
    private CollectionLoader<LaborAgreement> laborAgreementDl;
    @Inject
    private CollectionContainer<LaborAgreement> laborAgreementDc;
    @Inject
    private CollectionLoader<OpenPositionComment> commentsOpenPositionDl;
    @Inject
    private CollectionContainer<OpenPositionComment> commentsOpenPositionDc;
    @Inject
    private CollectionLoader<SomeFilesOpenPosition> someFilesesDl;
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
    private CollectionContainer<SomeFilesOpenPosition> someFilesesDc;
    @Inject
    private OpenPositionService openPositionService;
    @Inject
    private CheckBox onlyOpenProjectCheckBox;
    @Inject
    private CheckBox withOpenPositionCheckBox;
    @Inject
    private OvaFallbackImage projectLogoImage;
    @Inject
    private OvaFallbackImage projectOwnerImage;
    @Inject
    private TabSheet tabSheetOpenPosition;

    private boolean mainTabLobsLoaded;
    private boolean exerciseLoaded;
    private boolean memoLoaded;
    private boolean templateLetterLoaded;
    private boolean skillsLoaded;
    private boolean filesLoaded;
    private boolean commentsTabLoaded;
    private boolean laborAgreementLoaded;
    private boolean applyingPositionTypeFromHandler;
    /** false until onAfterShow completes — blocks RichTextArea ValueChange during @LoadDataBeforeShow bind */
    private boolean screenFullyLoaded;
    private boolean skillsRescanned;

    /** Блокировка авто-загрузки коллекций до установки параметра позиции. */
    private <E extends Entity> void preventAutoLoadUntilParameterSet(CollectionLoader<E> loader,
                                                                     String parameterName) {
        loader.addPreLoadListener(e -> {
            if (loader.getParameter(parameterName) == null) {
                e.preventLoad();
            }
        });
    }

    @Subscribe
    /** Перед показом формы: инициализация полей типа позиции, проекта, таймера, новостей, блокировок, иконок и BPM-процесса. */
    public void onBeforeShow(BeforeShowEvent event) {

        if (!PersistenceHelper.isNew(getEditedEntity())) {
            loadMainTabLobs();
            ensurePositionLobsLoaded();
            mainTabLobsLoaded = true;
        }

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
        initPositionTypeDescriptionFields();
        setOpenPositionNews(event);
        setOpenCloseStart();

        initProjectNameField();
    }

    @Subscribe("tabSheetOpenPosition")
    /** Переключение вкладки → lazy-загрузка LOB/коллекций вкладки при первом открытии. */
    public void onTabSheetOpenPositionSelectedTabChange(TabSheet.SelectedTabChangeEvent event) {
        if (event.getSelectedTab() == null || PersistenceHelper.isNew(getEditedEntity())) {
            return;
        }
        String tabName = event.getSelectedTab().getName();
        if ("tabExercise".equals(tabName) && !exerciseLoaded) {
            loadExerciseLob();
            exerciseLoaded = true;
        }
        if ("tabMemoForInterview".equals(tabName) && !memoLoaded) {
            loadMemoForInterviewLob();
            memoLoaded = true;
        }
        if ("tabTemplateLetter".equals(tabName) && !templateLetterLoaded) {
            loadTemplateLetterLob();
            templateLetterLoaded = true;
        }
        if ("tabSkills".equals(tabName) && !skillsLoaded) {
            loadSkillsList();
            skillsLoaded = true;
        }
        if ("tabFiles".equals(tabName) && !filesLoaded) {
            loadSomeFiles();
            filesLoaded = true;
            setIconSomeFileTab();
        }
        if ("commentsTab".equals(tabName) && !commentsTabLoaded) {
            loadCommentsTab();
            commentsTabLoaded = true;
        }
        if ("laborAgreementTab".equals(tabName) && !laborAgreementLoaded) {
            loadLaborAgreement();
            laborAgreementLoaded = true;
        }
    }

    /** Lazy-загрузка LOB-описаний основной вкладки (RU/EN описания типа позиции). */
    private void loadMainTabLobs() {
        OpenPosition reloaded = dataManager.reload(getEditedEntity(), ViewBuilder.of(OpenPosition.class)
                .add("comment")
                .add("commentEn")
                .build());
        getEditedEntity().setComment(reloaded.getComment());
        getEditedEntity().setCommentEn(reloaded.getCommentEn());
    }

    /** Догрузка описаний типа позиции, если они ещё не загружены. */
    private void ensurePositionLobsLoaded() {
        Position position = getEditedEntity().getPositionType();
        if (position == null || position.getId() == null) {
            return;
        }
        if (positionTypeDescriptionLobsLoaded(position)) {
            return;
        }
        setPositionTypeOnEntity(loadPositionWithDescriptionLobs(position.getId()));
    }

    /** Проверка: загружены ли описания типа позиции (RU/EN, «кто этот парень»). */
    private boolean positionTypeDescriptionLobsLoaded(Position position) {
        return position != null
                && PersistenceHelper.isLoaded(position, "standartDescription")
                && PersistenceHelper.isLoaded(position, "whoIsThisGuy");
    }

    /** Перезагрузка позиции с LOB-полями описаний через dataManager. */
    private Position loadPositionWithDescriptionLobs(UUID positionId) {
        return dataManager.load(Position.class)
                .id(positionId)
                .view(ViewBuilder.of(Position.class)
                        .add("positionRuName")
                        .add("positionEnName")
                        .add("standartDescription")
                        .add("whoIsThisGuy")
                        .build())
                .one();
    }

    /** Установка выбранного типа позиции на редактируемую сущность. */
    private void setPositionTypeOnEntity(Position position) {
        Position current = getEditedEntity().getPositionType();
        if (current != null && position != null && Objects.equals(current.getId(), position.getId())) {
            if (positionTypeDescriptionLobsLoaded(current)) {
                return;
            }
        }
        applyingPositionTypeFromHandler = true;
        try {
            getEditedEntity().setPositionType(position);
        } finally {
            applyingPositionTypeFromHandler = false;
        }
    }

    /** Применение описаний выбранного типа позиции к UI-полям (описания, «кто этот парень», навыки). */
    private void applyPositionTypeDescriptionUi(Position position) {
        if (position == null) {
            return;
        }
        if (position.getStandartDescription() != null) {
            openPositionStandartDescriptionRichTextArea.setValue(position.getStandartDescription());
            openPositionStandartDescriptionAccorden.setVisible(true);
            openPositionStandartDescriptionRichTextArea.setEnabled(true);
        } else {
            openPositionStandartDescriptionAccorden.setVisible(false);
            openPositionStandartDescriptionRichTextArea.setEnabled(false);
        }
        if (position.getWhoIsThisGuy() != null) {
            openPositionWhoIsThisGuyRichTextArea.setValue(position.getWhoIsThisGuy());
            openPositionWhoIsThisGuyAccorden.setVisible(true);
            openPositionWhoIsThisGuyRichTextArea.setEnabled(true);
        } else {
            openPositionWhoIsThisGuyAccorden.setVisible(false);
            openPositionWhoIsThisGuyRichTextArea.setEnabled(false);
        }
    }

    /** Lazy-загрузка текста тестового задания при первом открытии вкладки. */
    private void loadExerciseLob() {
        OpenPosition reloaded = dataManager.reload(getEditedEntity(), ViewBuilder.of(OpenPosition.class)
                .add("exercise")
                .build());
        getEditedEntity().setExercise(reloaded.getExercise());
    }

    /** Lazy-загрузка памятки к собеседованию при первом открытии вкладки. */
    private void loadMemoForInterviewLob() {
        OpenPosition reloaded = dataManager.reload(getEditedEntity(), ViewBuilder.of(OpenPosition.class)
                .add("memoForInterview")
                .build());
        getEditedEntity().setMemoForInterview(reloaded.getMemoForInterview());
    }

    /** Lazy-загрузка шаблона сопроводительного письма при первом открытии вкладки. */
    private void loadTemplateLetterLob() {
        OpenPosition reloaded = dataManager.reload(getEditedEntity(), ViewBuilder.of(OpenPosition.class)
                .add("templateLetter")
                .build());
        getEditedEntity().setTemplateLetter(reloaded.getTemplateLetter());
    }

    /** Lazy-загрузка дерева навыков позиции при первом открытии вкладки. */
    private void loadSkillsList() {
        openPositionSkillsListsDl.setParameter("openPosition", getEditedEntity());
        openPositionSkillsListsDl.load();
        skillTrees = new ArrayList<>(openPositionSkillsListsDc.getItems());
    }

    /** Lazy-загрузка таблицы файлов позиции при первом открытии вкладки. */
    private void loadSomeFiles() {
        someFilesesDl.setParameter("openPosition", getEditedEntity());
        someFilesesDl.load();
    }

    /** Lazy-загрузка комментариев позиции при первом открытии вкладки. */
    private void loadCommentsTab() {
        commentsOpenPositionDl.setParameter("openPosition", getEditedEntity());
        commentsOpenPositionDl.load();
        commentsScrollBox.removeAll();
        setCommentsOpenPositionScroll(commentsScrollBox);
        setCommentOpenPositionScrollIteractionList(getEditedEntity(), commentsScrollBox);
    }

    /** Lazy-загрузка трудовых соглашений при первом открытии вкладки. */
    private void loadLaborAgreement() {
        laborAgreementDl.setParameter("openPosition", getEditedEntity());
        laborAgreementDl.load();
    }

    /** Инициализация полей описаний типа позиции (RU/EN, стандартное, «кто этот парень»). */
    private void initPositionTypeDescriptionFields() {
        Position position = getEditedEntity().getPositionType();
        if (position == null) {
            return;
        }
        if (position.getStandartDescription() != null) {
            openPositionStandartDescriptionRichTextArea.setValue(position.getStandartDescription());
        }
        if (position.getWhoIsThisGuy() != null) {
            openPositionWhoIsThisGuyRichTextArea.setValue(position.getWhoIsThisGuy());
        }
    }

    /** Гарантия загрузки комментариев перед отрисовкой ленты. */
    private void ensureOpenPositionCommentsLoaded() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            return;
        }
        if (!commentsTabLoaded) {
            loadCommentsTab();
            commentsTabLoaded = true;
        }
    }

    /** Настройка таймера автозакрытия: запуск при заданной closingDate. */
    private void initClosedVacancyTimerFacet() {
        if (closingDateDateField.getValue() != null) {
            updateClosingVacancyLabel();
            closedVacancyTimer.start();
        } else {
            closedVacancyInfoLabel.setValue("");
            closedVacancyTimer.stop();
        }
    }

    /** Инициализация picker-поля проекта: установка текущего проекта и фильтров. */
    private void initProjectNameField() {
        projectNameField.setOptionImageProvider(this::projectFielsImageProvider);
        companyDepartamentField.setOptionImageProvider(this::companyDepartamentFieldImageProvider);
        companyNameField.setOptionImageProvider(this::companyNameFieldImageProvider);
        withOpenPositionCheckBox.setValue(true);
    }

    /** Иконка опции компании (логотип). */
    private Resource companyNameFieldImageProvider(Company company) {
        Image retImage = uiComponents.create(Image.class);
        retImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        retImage.setWidth("30px");

        if (company.getFileCompanyLogo() != null) {
            return FileDescriptorImageHelper.createCompanyLogoResource(retImage, fileLoader,
                    company.getFileCompanyLogo());
        } else {
            return retImage.createResource(ThemeResource.class).setPath("icons/no-company.png");
        }
    }

    /** Иконка опции департамента (логотип компании). */
    private Resource companyDepartamentFieldImageProvider(CompanyDepartament companyDepartament) {
        Image retImage = uiComponents.create(Image.class);
        retImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        retImage.setWidth("30px");

        if (companyDepartament.getCompanyName().getFileCompanyLogo() != null) {
            return FileDescriptorImageHelper.createCompanyLogoResource(retImage, fileLoader,
                    companyDepartament.getCompanyName().getFileCompanyLogo());
        } else {
            return retImage.createResource(ThemeResource.class).setPath("icons/no-company.png");
        }
    }

    /** Иконка опции проекта (логотип проекта). */
    protected Resource projectFielsImageProvider(Project project) {
        Image retImage = uiComponents.create(Image.class);
        retImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        retImage.setWidth("30px");

        if (project.getProjectLogo() != null) {
            return FileDescriptorImageHelper.createCompanyLogoResource(retImage, fileLoader,
                    project.getProjectLogo());
        } else {
            return retImage.createResource(ThemeResource.class).setPath("icons/no-company.png");
        }
    }

    /** Отрисовка одной строки комментария в ленте (с автором, рейтингом и ответом). */
    private void setCommentOpenPositionScrollIteractionList(OpenPosition editedEntity, ScrollBoxLayout commentsScrollBox) {
        final String QUERY_OPEN_POSITION_INTERACTIONS =
                "select e from hunttech_IteractionList e " +
                        "where e.vacancy = :vacancy and e.iteractionType.signFeedback = true";
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

    /** Отрисовка всей ленты комментариев позиции. */
    public void setCommentsOpenPositionScroll(ScrollBoxLayout commentsScrollBox) {
        for (OpenPositionComment openPositionComment : commentsOpenPositionDc.getItems()) {
            if (openPositionComment.getComment() != null) {
                VBoxLayout commentBox = getCommentBox(openPositionComment);
                commentsScrollBox.add(commentBox);
            }
        }
    }

    /** Перенос изменений таблицы соглашений на сущность перед коммитом. */
    private void syncLaborAgreementToEntity() {
        if (!laborAgreementLoaded) {
            return;
        }
        ensureLaborAgreementLoadedOnEntity();
        getEditedEntity().setLaborAgreement(new ArrayList<>(laborAgreementDc.getItems()));
    }

    /** Гарантия наличия коллекции соглашений на сущности. */
    private void ensureLaborAgreementLoadedOnEntity() {
        if (!PersistenceHelper.isLoaded(getEditedEntity(), "laborAgreement")) {
            OpenPosition reloaded = dataManager.reload(getEditedEntity(),
                    ViewBuilder.of(OpenPosition.class)
                            .add("laborAgreement", "laborAgreement-openPosition-tab-view")
                            .build());
            dataContext.merge(reloaded);
        }
    }

    /** Перенос изменений таблицы навыков на сущность. */
    private void syncSkillsListToEntity() {
        List<SkillTree> items = null;
        if (skillsLoaded) {
            items = new ArrayList<>(openPositionSkillsListsDc.getItems());
        } else if (skillsRescanned && skillTrees != null) {
            items = new ArrayList<>(skillTrees);
        }
        if (items == null) {
            return;
        }
        ensureSkillsListLoadedOnEntity();
        getEditedEntity().setSkillsList(items);
    }

    /** Гарантия наличия коллекции навыков на сущности. */
    private void ensureSkillsListLoadedOnEntity() {
        if (!PersistenceHelper.isLoaded(getEditedEntity(), "skillsList")) {
            OpenPosition reloaded = dataManager.reload(getEditedEntity(),
                    ViewBuilder.of(OpenPosition.class)
                            .add("skillsList", "skillTree-openPosition-tab-view")
                            .build());
            dataContext.merge(reloaded);
        }
    }

    @Install(to = "someFilesTable.create", subject = "newEntitySupplier")
    /** Создание новой записи файла позиции (с привязкой к позиции и текущему пользователю). */
    private SomeFilesOpenPosition someFilesTableCreateEntitySupplier() {
        SomeFilesOpenPosition file = metadata.create(SomeFilesOpenPosition.class);
        file.setOpenPosition(getEditedEntity());
        return file;
    }

    /** Применение фильтров списка проектов (только открытые / с открытыми позициями). */
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
    /** Смена чекбокса «только открытые проекты» → перезагрузка списка проектов. */
    public void onOnlyOpenProjectCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        setProjectClosedFilter();
    }

    @Subscribe("withOpenPositionCheckBox")
    /** Смена чекбокса «с открытыми позициями» → перезагрузка списка проектов. */
    public void onWithOpenPositionCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (event.getValue()) {
            projectNamesLc.setParameter("withOpenPosition", true);
            onlyOpenProjectCheckBox.setValue(false);
        } else {
            projectNamesLc.removeParameter("withOpenPosition");
        }

        projectNamesLc.load();
    }

    /** Создание контейнера строки комментария (автор, дата, текст, рейтинг, ответ). */
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
            candidateName.setValue(iteractionList.getCandidate().getFullName()
                    + " / "
                    + iteractionList.getCandidate().getPersonPosition().getPositionRuName());

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
                FileDescriptorImageHelper.setUserProfilePhoto(image, fileLoader,
                        (ExtUser) iteractionList.getRecrutier());
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
                                replyButtonInvoke(e, "("
                                        + name.getValue()
                                        + ") Re:"
                                        + (String) closeEvent.getValue("comment"));
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
                FileDescriptorImageHelper.setUserProfilePhoto(image, fileLoader,
                        (ExtUser) openPositionComment.getUser());
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
                                replyButtonInvoke(e, "("
                                        + name.getValue()
                                        + ") Re:"
                                        + (String) closeEvent.getValue("comment"));
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


    /** Ответ на комментарий: вставка reply-строки в ленту. */
    private void replyButtonInvoke(Button.ClickEvent e, String replyStr) {
        createComment(replyStr);

        events.publish(new UiNotificationEvent(this,
                messageBundle.getMessage("msgPublishOpenPositionComment")
                        + ":"
                        + getEditedEntity().getVacansyName()));
    }


    /** Создание комментария-рейтинга позиции и обновление ленты. */
    private void createComment(String commentStr) {

        OpenPositionComment comment = metadata.create(OpenPositionComment.class);
        comment.setOpenPosition(getEditedEntity());
        comment.setDateComment(new Date());
        comment.setUser((ExtUser) userSession.getUser());

        if (commentStr != null) {
            comment.setComment(commentStr);

            ensureOpenPositionCommentsLoaded();
            dataContext.merge(comment);
            commentsOpenPositionDc.getMutableItems().add(comment);
            commentsScrollBox.removeAll();
            setCommentsOpenPositionScroll(commentsScrollBox);
            setCommentOpenPositionScrollIteractionList(getEditedEntity(), commentsScrollBox);

        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("msgError"))
                    .withDescription(messageBundle.getMessage("msgDoNotCommentMessage"))
                    .withType(Notifications.NotificationType.ERROR)
                    .show();
        }
    }

    /** Привязка созданного комментария к позиции. */
    private void setCommentToVacancy() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            String defComment = "<i>" +
                    "<!-- НЕ ДЛЯ КАНДИДАТА:<br><br>" +
                    "-->" +
                    "</i>";

            openPositionRichTextArea.setValue(defComment);
        }
    }

    /** Подготовка статуса открытия/закрытия позиции перед сохранением. */
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
    /** Генератор детальной панели строки новости (дата, тема, автор, кандидат, текст). */
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

    /** Содержимое деталей новости. */
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

    /** Кнопка закрытия детальной панели новости. */
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

    /** Заполнение таблицы новостей позиции (обычные и приоритетные). */
    private void setOpenPositionNews(BeforeShowEvent event) {
        if (getEditedEntity() != null) {
            openPositionNewsLc.setParameter("openPosition", getEditedEntity());
        } else {
            openPositionNewsLc.removeParameter("openPosition");
        }

        openPositionNewsLc.load();
    }

    /** Блокировка вкладки «Стандартное описание» до генерации. */
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

    /** Блокировка вкладки «Кто этот парень» до генерации. */
    private void whiIsThisGuyDisable(BeforeShowEvent event) {
        if (getEditedEntity().getPositionType() != null) {
            if (getEditedEntity().getPositionType().getWhoIsThisGuy() == null) {
                openPositionWhoIsThisGuyAccorden.setVisible(false);
                openPositionWhoIsThisGuyRichTextArea.setEnabled(false);
            } else {
                openPositionWhoIsThisGuyAccorden.setVisible(true);
                openPositionWhoIsThisGuyRichTextArea.setEnabled(true);
            }
        }
    }


    /** Установка признака внутреннего проекта (по выбранному проекту). */
    private void setInternalProject() {
        if (getRoleService.isUserRoles(userSession.getUser(), MANAGER) ||
                getRoleService.isUserRoles(userSession.getUser(), ADMINISTRATOR)) {
            internalProjectCheckBox.setVisible(true);
        } else {
            internalProjectCheckBox.setVisible(false);
        }
    }

    /** Инициализация радио-группы командного опыта. */
    private void setCommandExperienceRadioButton() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("Нет требований", 0);
        map.put("Без опыта", 1);
        map.put("1 год", 2);
        map.put("3 года", 3);
        map.put("5 лет и более", 4);
        map.put("Управление командой", 5);

        commanExperienceRadioButton.setOptionsMap(map);
    }

    /** Инициализация радио-группы опыта коммерческой разработки. */
    private void setWorkExperienceRadioButton() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("Нет требований", 0);
        map.put("Без опыта", 1);
        map.put("1 год", 2);
        map.put("2 года", 3);
        map.put("3 года", 4);
        map.put("5 лет и более", 5);

        workExperienceRadioButton.setOptionsMap(map);
    }

    Boolean onNeedExercise;

    @Subscribe("needExerciseCheckBox")
    /** Смена чекбокса «нужно тестовое» → блокировка/разблокировка редактора. */
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
    /** Изменение минимальной зарплаты → валидация вилки. */
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
    /** Валидация максимальной зарплаты: не меньше минимальной. */
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
    /** Валидация минимальной зарплаты: не больше максимальной. */
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
    /** Изменение максимальной зарплаты → валидация вилки. */
    public void onOpenPositionFieldSalaryMaxValueChange1(HasValue.ValueChangeEvent<BigDecimal> event) {
        if (openPositionFieldSalaryMax.getValue() != null) {
            if (startSalaryMaxValue != null) {
                if (!startSalaryMaxValue.equals(openPositionFieldSalaryMax.getValue())) {

                    openPositionService.setOpenPositionNewsAutomatedMessage(getEditedEntity(),
                            "Изменены зарплатные предложение (MAX): старое "
                                    + (startSalaryMaxValue.toString().length() >= 3
                                    ? startSalaryMaxValue.toString().substring(0, startSalaryMaxValue.toString().length() - 3)
                                    : "НЕ ОПРЕДЕЛЕНО")
                                    + " на новое "
                                    + openPositionFieldSalaryMax.getValue(),
                            "Изменены зарплатные предложение (MAX): старое "
                                    + (startSalaryMaxValue.toString().length() >= 3
                                    ? startSalaryMaxValue.toString().substring(0, startSalaryMaxValue.toString().length() - 3)
                                    : "НЕ ОПРЕДЕЛЕНО")
                                    + " на новое "
                                    + openPositionFieldSalaryMax.getValue(),
                            new Date(),
                            (ExtUser) userSession.getUser());
                    startSalaryMaxValue = openPositionFieldSalaryMin.getValue();
                }
            }
        }
    }

    String openPositionText;

    @Subscribe("openPositionRichTextArea")
    /** Изменение русского описания → пересборка стандартного описания (после полной загрузки экрана). */
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
    /** Изменение шаблона сопроводительного письма. */
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


    /** Блокировка пары взаимосвязанных полей. */
    private void setDisableTwoField() {
    }

    @Subscribe("vacansyNameField")
    /** Изменение названия вакансии. */
    public void onVacansyNameFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        setTopLabel();
    }

    @Subscribe("openPositionFieldSalaryMin")
    /** Изменение минимальной зарплаты → пересчёт комиссий. */
    public void onOpenPositionFieldSalaryMinValueChange(HasValue.ValueChangeEvent<BigDecimal> event) {
        setCalculateCompanyPercentField();
        calculateResearcherSalary();
        calculateRecrutierSalary();
    }

    @Subscribe("openPositionFieldSalaryMax")
    /** Изменение максимальной зарплаты → пересчёт комиссий. */
    public void onOpenPositionFieldSalaryMaxValueChange(HasValue.ValueChangeEvent<BigDecimal> event) {
        setCalculateCompanyPercentField();
        calculateResearcherSalary();
        calculateRecrutierSalary();
    }

    /** Подстановка департамента компании из выбранного проекта. */
    private void setCompanyDepartmentFromProject() {
        if (projectNameField.getValue() != null) {
            companyDepartamentField.setValue(projectNameField.getValue().getProjectDepartment());
        }
    }

    /** Подстановка компании из выбранного департамента. */
    private void setCompanyNameFromDepartment() {
        if (companyDepartamentField.getValue() != null) {
            companyNameField.setValue(companyDepartamentField.getValue().getCompanyName());
        }
    }

    @Subscribe("companyDepartamentField")
    /** Смена департамента → подстановка компании. */
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

    /** Очистка связанной таблицы персонала. */
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

    /** Подстановка города компании в поле «город позиции». */
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
    /** Смена компании → обновление списка департаментов и города. */
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
    /** Смена типа «команда/вакансия» → блокировки и перезагрузка родительских позиций. */
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
    /** Смена родительской позиции → подстановка проекта и описаний. */
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
    /** После коммита: Telegram-уведомление об изменении позиции. */
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
                    "Открыта новая вакансия",
                    vacansyNameField.getValue() + "\n"
                            + positionTypeField.getValue().getPositionEnName() + " \\ "
                            + positionTypeField.getValue().getPositionRuName() + "\n\n"
                            + "Salary MIN: "
                            + openPositionFieldSalaryMin.getValue() + "\n"
                            + "Salary MAX: "
                            + openPositionFieldSalaryMax.getValue() + "\n\n"
                            + (shortDescriptionTextArea.getValue() != null ?
                            Jsoup.parse(shortDescriptionTextArea.getValue()).wholeText() : ""),
                    new Date(),
                    (ExtUser) userSession.getUser());
        }

        if (!PersistenceHelper.isNew(getEditedEntity())) {
            if (!vacansyNameField.getValue().equals(startVacansyName)) {
                openPositionService.setOpenPositionNewsAutomatedMessage(getEditedEntity(),
                        userSession.getUser().getName()
                                + " изменил наименование вакансии",
                        "Старое: " + startVacansyName
                                + "<br>Новое: "
                                + vacansyNameField.getValue(),
                        new Date(),
                        (ExtUser) userSession.getUser());
            }
        }
    }

    @Subscribe
    /** Перед коммитом: подготовка статусов и коллекций. */
    public void onBeforeCommitChanges4(BeforeCommitChangesEvent event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            StringBuilder sb = new StringBuilder()
                    .append("Новая вакансия: ")
                    .append(vacansyNameField.getValue())
                    .append("\n\n")
                    .append(openPositionRichTextArea.getValue())
                    .append("\n\nЗарплатное предложение: от ")
                    .append(openPositionFieldSalaryMin.getValue())
                    .append(" до ")
                    .append(openPositionFieldSalaryMax.getValue())
                    .append("\n\n(")
                    .append(salaryCommentTextFiels.getValue())
                    .append(")");
            notifyTelegramOpenPositionChange(textManipulationService.formattedHtml2text(sb.toString()));
        } else {

            Boolean flag = getEditedEntity().getOpenClose() != null ? getEditedEntity().getOpenClose() : false;

            if (!flag) {
                notifyTelegramOpenPositionChange(textManipulationService
                        .formattedHtml2text(new StringBuilder("Изменена вакансия: ")
                                .append(vacansyNameField.getValue()).toString()));
            }
        }
    }

    /** Отправка Telegram-уведомления об изменении позиции (текст из OpenPositionServiceBean). */
    private void notifyTelegramOpenPositionChange(String message) {
        TelegramSendResult result = telegramService.sendMessageToChatResult(
                applicationSetupService.getTelegramChatOpenPosition(), message);
        if (!result.isSuccess()) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withDescription(messageBundle.formatMessage(
                            "msgTelegramNotificationFailedWithReason", result.getFailureReason()))
                    .withHideDelayMs(8000)
                    .withPosition(Notifications.Position.DEFAULT)
                    .show();
        }
    }

    @Subscribe("priorityNewsCheckBox")
    /** Смена чекбокса «приоритетные новости» → перезагрузка новостей. */
    public void onPriorityNewsCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (event.getValue()) {
            openPositionNewsLc.setParameter("priorityNews", true);
        } else {
            openPositionNewsLc.removeParameter("priorityNews");
        }

        openPositionNewsLc.load();
    }


    @Subscribe("openClosePositionCheckBox")
    /** Смена чекбокса открытости → блокировка/разблокировка полей. */
    public void onOpenClosePositionCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        openCloseCurrentStatus = openClosePositionCheckBox.getValue();

        disableEnableFields(event);

        if (!event.getValue()) {
            if (getEditedEntity().getProjectName() != null) {
                getEditedEntity().getProjectName().setProjectIsClosed(false);
            }
        }
    }

    /** Блокировка/разблокировка полей формы по статусу позиции (открыта/закрыта/черновик). */
    private void disableEnableFields(HasValue.ValueChangeEvent<Boolean> event) {
        if (getEditedEntity().getOpenClose()) {
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
        }
    }

    @Subscribe
    /** После коммита: синхронизация статуса дочерних позиций. */
    public void onAfterCommitChanges1(AfterCommitChangesEvent event) {
        openCloseChildVacancy(event);

    }

    /** Закрытие/открытие дочерних позиций вместе с родительской. */
    private void openCloseChildVacancy(AfterCommitChangesEvent event) {
        List<OpenPosition> openPositions = dataManager.load(OpenPosition.class)
                .query(QUERY_SELECT_COMMAND)
                .parameter("parentOpenPosition", getEditedEntity())
                .list();

        String magPos = "";

        if (openPositions.size() != 0) {
            for (OpenPosition a : openPositions) {
                magPos = magPos + "<li><i>" + a.getVacansyName() + "</i></li>";
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
    /** После показа: загрузка LOB основной вкладки, логотипов, новостей, настроек и approval-процесса. */
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
        screenFullyLoaded = true;
        initClosedVacancyTimerFacet();
    }

    /** Форматирование ключевых компетенций для отображения. */
    private String showKeyCompetition(String value) {
        if (skillTrees == null) {
            return value;
        }
        for (SkillTree skillTree : skillTrees) {
            String keyWithStyle = "<b><font color=\"brown>\" face=\"serif\">"
                    + skillTree.getSkillName()
                    + "</font></b>";
            if (!value.contains(keyWithStyle)) {
                value = value.replaceAll("(?i)" + skillTree.getSkillName(), keyWithStyle);
            }
        }

        return value;
    }

    /** Отправка уведомления о позиции (email/Telegram). */
    private void sendMessage() {
        events.publish(new UiNotificationEvent(this, "Открыта новая позиция: " +
                getEditedEntity().getVacansyName()));
    }

    /** Формирование текста уведомления об открытии/закрытии. */
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
    /** Изменение памятки к собеседованию. */
    public void onMemoForInterviewRichTextAreaValueChange(HasValue.ValueChangeEvent<String> event) {
        if (event.getValue() != null && !event.getValue().equals("")) {
            needMemoCheckBox.setValue(true);
        } else {
            needMemoCheckBox.setValue(false);
        }
    }

    @Subscribe
    /** Перед коммитом: проверка дубля vacansyID и сборка уведомлений. */
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        syncLaborAgreementToEntity();
        syncSkillsListToEntity();

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

        }

        if (checkDuplicatePositionId()) {
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

    /** Проверка уникальности vacansyID; при дубле — диалог подтверждения продолжения. */
    private boolean checkDuplicatePositionId() {
        String vacancyID = vacansyIDTextField.getValue();
        if (vacancyID == null) {
            return false;
        }
        if (!PersistenceHelper.isNew(getEditedEntity())) {
            return dataManager.loadValue(
                    "select count(e) from hunttech_OpenPosition e " +
                    "where e.vacansyID = :vacancyID and e.id <> :currentId", Long.class)
                    .parameter("vacancyID", vacancyID)
                    .parameter("currentId", getEditedEntity().getId())
                    .one() > 0;
        }
        return dataManager.loadValue(
                "select count(e) from hunttech_OpenPosition e " +
                "where e.vacansyID = :vacancyID", Long.class)
                .parameter("vacancyID", vacancyID)
                .one() > 0;
    }

    /** Публикация события изменения позиции для подписчиков. */
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
    /** Перед коммитом: формирование сообщений открытия/закрытия. */
    public void onBeforeCommitChanges3(BeforeCommitChangesEvent event) {
        publishEventMessage(event);

    }

    /** Формирование и рассылка уведомления о закрытии позиции. */
    private void sendClosePositionMessage() {
        events.publish(new UiNotificationEvent(this, "Закрыта вакансия: " +
                getEditedEntity().getVacansyName()));
    }

    /** Формирование и рассылка уведомления об открытии позиции. */
    private void sendOpenPositionMessage() {
        events.publish(new UiNotificationEvent(this, "Открыта новая вакансия: " +
                getEditedEntity().getVacansyName()));
    }

    @Subscribe
    /** Перед коммитом: сборка списков подписчиков и комментариев. */
    public void onBeforeCommitChanges1(BeforeCommitChangesEvent event) {
        if (openClosePositionCheckBox.getValue() == null)
            openClosePositionCheckBox.setValue(false);

        if (internalProjectCheckBox.getValue() == null)
            internalProjectCheckBox.setValue(false);

        if (PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setLastOpenDate(new Date());
        }
    }

    /** Проверка дублей позиции (вспомогательная проверка перед коммитом). */
    private OpenPosition checkDublicateOpenPosition(BeforeCommitChangesEvent event) {
        // StringIndexOutOfBoundsException: begin 0, end -1, length 2
        List<OpenPosition> openPositions = new ArrayList<>();

        try {
            openPositions = dataManager.load(OpenPosition.class)
                    .query("select e from hunttech_OpenPosition e " +
                            "where e.positionType = :positionType " +
                            "and e.vacansyName like :vacansyName " +
                            "and e.projectName = :projectName " +
                            "and e.parentOpenPosition = :parentOpenPosition " +
                            "and e.vacansyName = :vacansyName " +
                            "and e.remoteWork = :remoteWork " +
                            "and e.cityPosition = :cityPosition")
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

    /** Сбор email всех подписчиков позиции для рассылки. */
    private String getAllSubscibers() {
        LoadContext<User> loadContext = LoadContext.create(User.class)
                .setQuery(LoadContext.createQuery("select e from sec$User e"));

        List<User> listManagers = dataManager.loadList(loadContext);

        String maillist = "";

        for (User user : listManagers) {
            if (user.getEmail() != null && user.getActive())
                maillist = maillist + user.getEmail() + ";";
        }

        return maillist;
    }

    /** Email-список подписчиков сущности (по активным подпискам и ролям). */
    private String getSubscriberMaillist(Entity entity) {
        List<RecrutiesTasks> listResearchers = dataManager.load(RecrutiesTasks.class)
                .query("select e " +
                        "from hunttech_RecrutiesTasks e " +
                        "where e.endDate  >= :currentDate and " +
                        "e.openPosition = :openPosition")
                .parameter("currentDate", new Date())
                .parameter("openPosition", entity)
                .view("recrutiesTasks-view")
                .list();


        String maillist = "";
        Boolean subs = false;

        for (RecrutiesTasks address : listResearchers) {
            String email = address.getReacrutier().getEmail();

            if (address.getSubscribe() == null ? false : address.getSubscribe())
                // if( address.getSubscribe() )
                if (email != null)
                    maillist = maillist + email + ";";
        }

        return maillist;
    }

    /** Email-список всех рекрутеров. */
    private String getRecrutiersMaillist() {
        return "alan@hunttech.ru";
    }

    Boolean flagPriority = true;
    Integer startPriorityStatus;

    @Subscribe("priorityField")
    /** Смена приоритета → авто-установка даты закрытия через неделю. */
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
                                    "Изменен приоритет вакансии на " + result.get(),
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

    /** Установка closingDate = сегодня + 7 дней. */
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

    /** Инициализация радио-групп оплаты (ресурсер/рекрутер). */
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

        priorityMap.put("Draft", OpenPositionPriority.DRAFT.getId());
        priorityMap.put("Paused", OpenPositionPriority.PAUSED.getId());
        priorityMap.put("Low", OpenPositionPriority.LOW.getId());
        priorityMap.put("Normal", OpenPositionPriority.NORMAL.getId());
        priorityMap.put("High", OpenPositionPriority.HIGH.getId());
        priorityMap.put("Critical", OpenPositionPriority.CRITICAL.getId());

        priorityField.setOptionsMap(priorityMap);

        Map<String, Integer> paymentsType = new LinkedHashMap<>();
        paymentsType.put("Фиксированная оплата", 0);
        paymentsType.put("Процент от годового оклада", 1);
        paymentsType.put("Процент от месячной зарплаты", 2);

        radioButtonGroupPaymentsType.setOptionsMap(paymentsType);

        Map<String, Integer> researcherSalary = new LinkedHashMap<>();
        researcherSalary.put("Фиксированная комиссия", 0);
        researcherSalary.put("Процент комиссии компании, 20%", 1);
        researcherSalary.put("Процент комиссии компании", 2);

        radioButtonGroupResearcherSalary.setOptionsMap(researcherSalary);

        Map<String, Integer> recrutierSalary = new LinkedHashMap<>();
        recrutierSalary.put("Фиксированная комиссия", 0);
        recrutierSalary.put("Процент комиссии компании, 10%", 1);
        recrutierSalary.put("Процент комиссии компании", 2);

        radioButtonGroupRecrutierSalary.setOptionsMap(recrutierSalary);

        Map<String, Integer> remoteWork = new LinkedHashMap<>();
        remoteWork.put("Нет", 0);
        remoteWork.put("Удаленная работа", 1);
        remoteWork.put("Частично 50/50", 2);

        remoteWorkField.setOptionsMap(remoteWork);
    }

    /** Скрытие/показ полей в зависимости от статуса позиции. */
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
    /** Смена типа комиссии компании → пересчёт. */
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
    /** Смена флага НДФЛ → пересчёт комиссий. */
    public void onCheckBoxUseNDFLValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        setCalculateCompanyPercentField();
        calculateResearcherSalary();
        calculateRecrutierSalary();
    }


    @Subscribe("textFieldPercentOrSum")
    /** Смена процента/суммы комиссии компании → пересчёт. */
    public void onTextFieldPercentOrSumValueChange(HasValue.ValueChangeEvent<String> event) {
        setCalculateCompanyPercentField();
        calculateResearcherSalary();
        calculateRecrutierSalary();
    }

    /** Настройка расчётного поля комиссии компании. */
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

    /** Расчёт комиссий (компания, ресурсер, рекрутер) по текущим параметрам. */
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

                retValue = "От " +
                        minSalary.toString() +
                        " до " +
                        maxSalary.toString();
                break;
            case 2:
                minSalary = minSalary.multiply(p).multiply(ndflFlag ? ndfl : BigDecimal.ONE).divide(hungred);
                maxSalary = maxSalary.multiply(p).multiply(ndflFlag ? ndfl : BigDecimal.ONE).divide(hungred);

                minSalary = minSalary.setScale(0, RoundingMode.HALF_EVEN);
                maxSalary = maxSalary.setScale(0, RoundingMode.HALF_EVEN);

                retValue = "От " +
                        minSalary.toString() +
                        " до " +
                        maxSalary.toString();
                break;
        }

        minCompanyComission = minSalary;
        maxCompanyComission = maxSalary;

        return retValue;
    }

    @Subscribe("radioButtonGroupResearcherSalary")
    /** Смена схемы оплаты ресурсера → пересчёт. */
    public void onRadioButtonGroupResearcherSalaryValueChange(HasValue.ValueChangeEvent event) {
        calculateResearcherSalary();

        setResearcherSalaryLabel();
    }

    @Subscribe("radioButtonGroupRecrutierSalary")
    /** Смена схемы оплаты рекрутера → пересчёт. */
    public void onRadioButtonGroupRecrutierSalaryValueChange(HasValue.ValueChangeEvent event) {
        calculateRecrutierSalary();

        setRecrutierSalaryLabel();
    }

    /** Расчёт выплаты ресурсеру. */
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

                        textSalaryMessage = "От " +
                                minSalary.multiply(percent).divide(hungred).setScale(0, RoundingMode.HALF_EVEN) +
                                " до " +
                                maxSalary.multiply(percent).divide(hungred).setScale(0, RoundingMode.HALF_EVEN);

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

                            textSalaryMessage = "От " +
                                    minSalary.multiply(percent).divide(hungred)
                                            .setScale(0, RoundingMode.HALF_EVEN) +
                                    " до " +
                                    maxSalary.multiply(percent).divide(hungred)
                                            .setScale(0, RoundingMode.HALF_EVEN);

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

    /** Расчёт выплаты рекрутеру. */
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

                    textSalaryMessage = textFieldRecrutierPercentOrSum.getValue() + " рублей.";
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

                        textSalaryMessage = "От " +
                                minSalary.multiply(percent).divide(hungred).setScale(0, RoundingMode.HALF_EVEN) +
                                " до " +
                                maxSalary.multiply(percent).divide(hungred).setScale(0, RoundingMode.HALF_EVEN);

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

                            textSalaryMessage = "От " +
                                    minSalary.multiply(percent).divide(hungred)
                                            .setScale(0, RoundingMode.HALF_EVEN) +
                                    " до " +
                                    maxSalary.multiply(percent).divide(hungred)
                                            .setScale(0, RoundingMode.HALF_EVEN);

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
    /** Смена процента/суммы рекрутера → пересчёт. */
    public void onTextFieldRecrutierPercentOrSumValueChange(HasValue.ValueChangeEvent<String> event) {
        calculateRecrutierSalary();
    }

    @Subscribe("textFieldResearcherSalaryPercentOrSum")
    /** Смена процента/суммы ресурсера → пересчёт. */
    public void onTextFieldResearcherSalaryPercentOrSumValueChange(HasValue.ValueChangeEvent<String> event) {
        calculateResearcherSalary();
    }

    @Subscribe("textFieldRecrutierSalary")
    /** Смена суммы рекрутера → пересчёт. */
    public void onTextFieldRecrutierSalaryValueChange(HasValue.ValueChangeEvent<String> event) {
        // calculateRecrutierSalary();

        // setRecrutierSalaryLabel();
    }

    @Subscribe("textFieldResearcherSalary")
    /** Смена суммы ресурсера → пересчёт. */
    public void onTextFieldResearcherSalaryValueChange(HasValue.ValueChangeEvent<String> event) {
        // calculateResearcherSalary();

        // setResearcherSalaryLabel();
    }

    /** Обновление итоговой подписи оплаты ресурсера. */
    private void setResearcherSalaryLabel() {
        if (radioButtonGroupResearcherSalary.getValue() != null) {
            if ((int) radioButtonGroupResearcherSalary.getValue() == 0) {
                if (textFieldResearcherSalary.getValue() != null) {
                    labelResearcherSalary.setValue("Зарплата ресерчера после закрытия вакансии \"<i>" +
                            vacansyNameField.getValue() + "</i>\" составит " +
                            textFieldResearcherSalary.getValue() + " рублей.");

                    groupBoxPaymentsResearcher.setVisible(true);
                } else
                    groupBoxPaymentsResearcher.setVisible(false);
            } else {
                if (textFieldResearcherSalary.getValue() != null) {
                    labelResearcherSalary.setValue("Зарплата ресерчера после закрытия вакансии \"<i>" +
                            vacansyNameField.getValue() + "</i>\" составит " +
                            textFieldResearcherSalary.getValue() + " рублей.");
                    groupBoxPaymentsResearcher.setVisible(true);
                    groupBoxPaymentsResearcher.setVisible(true);
                } else
                    groupBoxPaymentsResearcher.setVisible(false);
            }
        }
    }

    /** Обновление итоговой подписи оплаты рекрутера. */
    private void setRecrutierSalaryLabel() {
        if (radioButtonGroupRecrutierSalary.getValue() != null) {
            if ((int) radioButtonGroupRecrutierSalary.getValue() == 0) {
                if (textFieldRecrutierSalary.getValue() != null) {
                    labelRecrutierSalary.setValue("Зарплата рекрутера после закрытия вакансии \"<i>" +
                            vacansyNameField.getValue() + "</i>\" составит " +
                            textFieldRecrutierSalary.getValue() + " рублей.");
                }
            } else {
                if (textFieldRecrutierSalary.getValue() != null) {
                    labelRecrutierSalary.setValue("Зарплата рекрутера после закрытия вакансии \"<i>" +
                            vacansyNameField.getValue() + "</i>\" составит " +
                            textFieldRecrutierSalary.getValue() + " рублей.");
                }
            }
        }

        setHiddeField();
    }

    @Subscribe
    /** Перед показом: дополнительные настройки полей и иконок. */
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
        initProjectImagesOnOpen();

        if (openPositionRichTextArea.getValue() != null) {
            openPositionRichTextArea.setValue(showKeyCompetition(openPositionRichTextArea.getValue()));
        }

        openClosePositionCheckBox.setValue(openClosePositionCheckBox.getValue() == null ? false : true);

        setInitApprovalProcess();
    }

    /** Иконка вкладки «Файлы» с признаком наличия файлов. */
    private void setIconSomeFileTab() {
        if (!filesLoaded && !PersistenceHelper.isNew(getEditedEntity())) {
            tabFiles.setIconFromSet(CubaIcon.FILE_O);
            return;
        }
        if (someFilesesDc.getItems().size() > 0) {
            tabFiles.setIconFromSet(CubaIcon.FILE_TEXT_O);
        } else {
            tabFiles.setIconFromSet(CubaIcon.FILE_O);
        }
    }

    /** Загрузка логотипа проекта и аватара владельца при открытии формы. */
    private void initProjectImagesOnOpen() {
        Project project = getEditedEntity().getProjectName();
        if (project == null) {
            updateProjectLogoImage(null);
            updateProjectOwnerImage(null);
            return;
        }
        updateProjectLogoImage(project.getProjectLogo());
        updateProjectOwnerImage(project.getProjectOwner());
    }

    /** Обновление изображения логотипа проекта в шапке. */
    private void updateProjectLogoImage(FileDescriptor projectLogo) {
        projectLogoImage.setValueSource(null);
        if (FileDescriptorImageHelper.fileExists(fileLoader, projectLogo)) {
            projectLogoImage.setSource(FileDescriptorResource.class).setFileDescriptor(projectLogo);
        } else {
            projectLogoImage.applyFallback();
        }
    }

    /** Обновление изображения владельца проекта в шапке. */
    private void updateProjectOwnerImage(Person projectOwner) {
        projectOwnerImage.setDescription(buildProjectOwnerDescription(projectOwner));
        projectOwnerImage.setValueSource(null);
        FileDescriptor face = projectOwner != null ? projectOwner.getFileImageFace() : null;
        if (FileDescriptorImageHelper.fileExists(fileLoader, face)) {
            projectOwnerImage.setSource(FileDescriptorResource.class).setFileDescriptor(face);
        } else {
            projectOwnerImage.applyFallback();
        }
    }

    /** HTML-подсказка владельца проекта (должность, город, компания, департамент). */
    private String buildProjectOwnerDescription(Person projectOwner) {
        if (projectOwner == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(projectOwner.getFirstName() != null ? projectOwner.getFirstName() : "")
                .append(" ")
                .append(projectOwner.getSecondName() != null ? projectOwner.getSecondName() : "");
        if (projectOwner.getPersonPosition() != null) {
            sb.append(" / ")
                    .append(projectOwner.getPersonPosition().getPositionRuName());
        }
        if (projectOwner.getCompanyDepartment() != null) {
            sb.append(" / ")
                    .append(projectOwner.getCompanyDepartment().getDepartamentRuName());
            if (projectOwner.getCompanyDepartment().getCompanyName() != null) {
                sb.append(" / ")
                        .append(projectOwner.getCompanyDepartment().getCompanyName().getComanyName());
            }
        }
        if (projectOwner.getCityOfResidence() != null) {
            sb.append(" / ")
                    .append(projectOwner.getCityOfResidence().getCityRuName());
        }
        return sb.toString();
    }

    @Subscribe(id = "someFilesesDc", target = Target.DATA_CONTAINER)
    /** Смена коллекции файлов → обновление иконки вкладки. */
    public void onSomeFilesesDcCollectionChange(CollectionContainer.CollectionChangeEvent<SomeFilesOpenPosition> event) {
        setIconSomeFileTab();
    }

    /** Инициализация BPM-процесса согласования позиции. */
    private void setInitApprovalProcess() {
        UUID entityId = getEditedEntity().getId();
        procAttachmentsDl.setParameter("entityId", entityId);
        procAttachmentsDl.load();
        procActionsFragment.initializer()
                .standard()
                .init(PROCESS_CODE, getEditedEntity());
    }

    /** Формирование шапки формы: название, комиссии, статус и логотипы. */
    private void setTopLabel() {
        try {
            if (vacansyNameField.getValue() != null && projectNameField.getValue() != null) {
                if (projectNameField.getValue() != null) {
                    if (projectNameField.getValue().getProjectDepartment() != null) {
                        if (projectNameField.getValue().getProjectDepartment().getCompanyName() != null) {
                            if (projectNameField.getValue().getProjectDepartment().getCompanyName() != null) {
                                if (projectNameField.getValue().getProjectDepartment().getCompanyName().getComanyName() != null) {
                                    String comanyName = projectNameField.getValue().getProjectDepartment().getCompanyName().getComanyName();

                                    labelOpenPosition.setValue(vacansyNameField.getValue() +
                                            " (" +
                                            (comanyName != null ? comanyName : "") +
                                            " : " +
                                            projectNameField.getValue().getProjectName() +
                                            ")");
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
    /** Изменение метки комиссии рекрутера. */
    public void onLabelRecrutierSalaryValueChange(HasValue.ValueChangeEvent<String> event) {
        setTopLabel();
    }

    @Subscribe("labelResearcherSalary")
    /** Изменение метки комиссии ресурсера. */
    public void onLabelResearcherSalaryValueChange(HasValue.ValueChangeEvent<String> event) {
        setTopLabel();
    }

    @Subscribe
    /** Инициализация экрана: карты options и начальные настройки. */
    public void onInit(InitEvent event) {
        preventAutoLoadUntilParameterSet(laborAgreementDl, "openPosition");
        preventAutoLoadUntilParameterSet(commentsOpenPositionDl, "openPosition");
        preventAutoLoadUntilParameterSet(someFilesesDl, "openPosition");
        preventAutoLoadUntilParameterSet(openPositionSkillsListsDl, "openPosition");
        preventAutoLoadUntilParameterSet(procAttachmentsDl, "entityId");

        setRadioButtons();
        setGroupSubscribeButton();
        setGroupCommandRadioButtin();
        skillImageColumnRenderer();

        setOpenPositionNewsDetailsGenerator();
    }

    /** Настройка генератора деталей строк новостей. */
    private void setOpenPositionNewsDetailsGenerator() {
        openPostionNewsDataGrid.setItemClickAction(new BaseAction("itemClickAction")
                .withHandler(actionPerformedEvent -> openPostionNewsDataGrid
                        .setDetailsVisible(openPostionNewsDataGrid.getSingleSelected(), true)));
    }

    /** Настройка радио-группы «команда/вакансия». */
    private void setGroupCommandRadioButtin() {
        Map<String, Integer> map = new LinkedHashMap<>();

        map.put("Команда", 1);
        map.put("Вакансия", 0);

        commandOrPosition.setOptionsMap(map);
    }

    @Subscribe("projectNameField")
    /** Смена проекта → подстановка компании, департамента, позиции и описаний. */
    public void onProjectNameFieldValueChange1(HasValue.ValueChangeEvent<Project> event) {
        if (event.getValue() != null) {
            if (event.getValue().getProjectIsClosed()) {
                dialogs.createOptionDialog(Dialogs.MessageType.WARNING)
                        .withContentMode(ContentMode.HTML)
                        .withMessage("Вы пытаетесь открыть позицию по закрытому проекту.<br>" +
                                "Открыть проект заново?")
                        .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                            event.getValue().setEndProjectDate(null);
                            event.getValue().setProjectIsClosed(false);
                        }), new DialogAction(DialogAction.Type.NO).withHandler((f -> {
                            projectNameField.setValue(null);
                        })));
            }
        }
    }

    /** Настройка кнопки групповой подписки по роли. */
    private void setGroupSubscribeButton() {
//        groupSubscribe.setVisible(userSession.getUser().getGroup().getName().equals(MANAGEMENT_GROUP) ||
//                userSession.getUser().getGroup().getName().equals(HUNTING_GROUP));
    }

    @Install(to = "registrationForWorkField", subject = "optionIconProvider")
    /** Иконка опции регистрации для работы. */
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
    /** Стиль опции регистрации для работы. */
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
    /** Иконка опции формата удалённой работы. */
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
    /** Иконка опции приоритета. */
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
    /** Обработчик изменения значения (общий). */
    public void onChange(DataContext.ChangeEvent event) {
        entityIsChanged = true;
    }

    /** Подписка текущего рекрутёра на позицию (RecrutiesTasks). */
    public void subscribePosition() {
        Screen opScreen = screenBuilders
                .editor(RecrutiesTasks.class, this)
                .newEntity()
                .withInitializer(data -> {
                    data.setOpenPosition(this.getEditedEntity());
                })
                .newEntity()
                .withParentDataContext(dataContext)
                .withScreenId("hunttech_RecrutiesTasks.edit")
                .withLaunchMode(OpenMode.DIALOG)
                .build();


        opScreen.show();
    }

    /** Проверка наличия у пользователя заданной роли. */
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
    /** Смена типа позиции → подстановка описаний и названия. */
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
    /** Смена проекта → пересборка названия вакансии. */
    public void onProjectNameFieldValueChange(HasValue.ValueChangeEvent<Project> event) {
        vacansyNameField.setValue(generatePositionNameInProject());

        setCompanyDepartmentFromProject();
        Project project = event.getValue();
        updateProjectLogoImage(project != null ? project.getProjectLogo() : null);
        updateProjectOwnerImage(project != null ? project.getProjectOwner() : null);
    }

    /** Генерация названия вакансии с учётом проекта. */
    private String generatePositionNameInProject() {
        String retValue = vacansyNameField.getValue();

        if (vacansyNameField.getValue() != null) {
            if (generatePositionName().equals(retValue)) {
                if (projectNameField.getValue() != null) {
                    if (projectNameField.getValue().getProjectName() != null) {
                        retValue = retValue + " (" + projectNameField.getValue().getProjectName() + ")";
                    }
                }
            }
        }

        return retValue;
    }

    /** Генерация названия вакансии с учётом города. */
    private String generatePositionNameCity() {
        String retValue = vacansyNameField.getValue();

        if (PersistenceHelper.isNew(getEditedEntity())) {
            if (cityOpenPositionField.getValue() != null) {
                if (retValue != null) {
                    if (generatePositionNameInProject().equals(retValue)) {
                        retValue = retValue.substring(0, retValue.length() - 1) + ", " + cityOpenPositionField.getValue().getCityRuName() + ")";
                    }
                } else {
                    retValue = null;
                }
            }
        }

        return retValue;
    }

    @Subscribe("cityOpenPositionField")
    /** Смена города → генерация названия вакансии. */
    public void onCityOpenPositionFieldValueChange(HasValue.ValueChangeEvent<City> event) {
        vacansyNameField.setValue(generatePositionNameCity());
    }

    /** Полная генерация названия вакансии по типу/проекту/городу. */
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
                    (positionTypeField.getValue().getPositionRuName() != null ? positionTypeField.getValue().getPositionRuName() : "")
                            + " \\ "
                            + (positionTypeField.getValue().getPositionEnName() != null ? positionTypeField.getValue().getPositionEnName() : "");
        }

        return retPosName;
    }

    /** Переключение статуса открытости позиции. */
    public void openClosePosition() {
        String message = "";

        if (!openClosePositionCheckBox.getValue())
            message = "Закрыть";
        else
            message = "Открыть";

        dialogs.createOptionDialog()
                .withMessage(message + " позицию \"" + vacansyNameField.getValue() + "\"?")
                .withCaption("ВНИМАНИЕ!")
                .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {

                    if (this.openClosePositionCheckBox.getValue())
                        this.closeWithCommit();
                    return;

                }), new DialogAction(DialogAction.Type.NO))
                .show();
    }

    /** Открытие окна массового выбора городов позиции (SelectCitiesLocation). */
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
    /** Перед коммитом: подготовка городов и подписей. */
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

    /** Обновление сводной подписи списка городов. */
    private void changeCityListsLabel() {
        String outStr = "";
        String description = "";

        if (getEditedEntity().getCities() != null) {
            for (City s : getEditedEntity().getCities()) {
                if (!outStr.equals("")) {
                    outStr = outStr + ",";
                    description = description + "\n";
                }

                outStr = outStr + s.getCityRuName();
                description = description + s.getCityRuName();
            }

        }

        if (!outStr.equals("")) {
            citiesLabel.setValue(outStr);
            citiesLabel.setDescription(description);
        }
    }

    /**
     * Parses skills from job description text. Never reads/writes {@code OpenPosition.skillsList}
     * on the edited entity — only {@link #skillTrees} and {@link #openPositionSkillsListsDc}.
     * Entity sync happens in {@link #syncSkillsListToEntity()} on commit.
     */
    /** Пересканирование описания должности → пересборка дерева навыков. */
    public void rescanJobDescription() {
        if (openPositionRichTextArea.getValue() == null) {
            return;
        }
        String inputText = Jsoup.parse(openPositionRichTextArea.getValue()).wholeText();
        if (inputText.trim().isEmpty()) {
            return;
        }
        skillTrees = reloadSkillsForOpenPositionTab(pdfParserService.parseSkillTree(inputText));
        skillsRescanned = true;
        if (skillsLoaded) {
            openPositionSkillsListsDc.setItems(skillTrees);
        }
    }

    /** Перезагрузка коллекции навыков для вкладки. */
    private List<SkillTree> reloadSkillsForOpenPositionTab(List<SkillTree> skills) {
        if (skills == null || skills.isEmpty()) {
            return skills;
        }
        return dataManager.load(SkillTree.class)
                .ids(skills.stream().map(SkillTree::getId).collect(java.util.stream.Collectors.toList()))
                .view("skillTree-openPosition-tab-view")
                .list();
    }

    @Subscribe("openPositionRichTextArea")
    /** Изменение описания → пересборка навыков (после загрузки). */
    public void onOpenPositionRichTextAreaValueChange(HasValue.ValueChangeEvent<String> event) {
        if (!screenFullyLoaded) {
            return;
        }
        if (openPositionRichTextArea.getValue() != null &&
                !openPositionRichTextArea.getValue().trim().equals("")) {
            rescanJobDescription();
        }
    }

    /** Рендер логотипа навыка в таблице навыков. */
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
    /** Генератор колонки «комментарий» навыка. */
    private Object openPositionSkillsListTableIsCommentColumnGenerator(DataGrid.ColumnGeneratorEvent<SkillTree> event) {
        if (event.getItem().getComment() != null && !event.getItem().equals("")) {
            return CubaIcon.PLUS_CIRCLE;
        } else {
            return CubaIcon.MINUS_CIRCLE;
        }
    }

    @Install(to = "openPositionSkillsListTable.isComment", subject = "styleProvider")
    /** Стиль колонки «комментарий» навыка. */
    private String openPositionSkillsListTableIsCommentStyleProvider(SkillTree skillTree) {
        if (skillTree.getComment() != null && !skillTree.equals("")) {
            return "pic-center-large-green";
        } else {
            return "pic-center-large-red";
        }
    }

    @Install(to = "openPositionSkillsListTable", subject = "rowDescriptionProvider")
    /** Подсказка строки таблицы навыков. */
    private String openPositionSkillsListTableRowDescriptionProvider(SkillTree skillTree) {
        return skillTree.getComment() != null ? Jsoup.parse(skillTree.getComment()).wholeText() : "";
    }

    /** Извлечение короткого описания и навыков из описания должности (сканирование). */
    public void addShortDescription() {
        if (openPositionRichTextArea.getValue() != null) {
            List<SkillTree> skillTrees = pdfParserService
                    .parseSkillTree(Jsoup.parse(openPositionRichTextArea.getValue()).wholeText());
//            String retStr = "";
            StringBuilder sb = new StringBuilder();

            for (SkillTree skillTree : skillTrees) {
                if (skillTree.getSkillTree() != null) {
                    sb.insert(0, skillTree.getSkillName()).append(";");
//                    retStr = skillTree.getSkillName() + ";" + retStr;
                }
            }

            shortDescriptionTextArea.setValue(sb.toString());
        }
    }

    @Subscribe("positionTypeField")
    /** Смена типа позиции (второй обработчик) → обновление названия и описаний. */
    public void onPositionTypeFieldValueChange1(HasValue.ValueChangeEvent<Position> event) {
        if (applyingPositionTypeFromHandler) {
            return;
        }
        if (event.getValue() == null || event.getValue().getId() == null) {
            return;
        }
        Position current = getEditedEntity().getPositionType();
        UUID positionId = event.getValue().getId();
        if (current != null && positionId.equals(current.getId()) && positionTypeDescriptionLobsLoaded(current)) {
            applyPositionTypeDescriptionUi(current);
            return;
        }
        Position reloaded = loadPositionWithDescriptionLobs(positionId);
        setPositionTypeOnEntity(reloaded);
        applyPositionTypeDescriptionUi(reloaded);
    }

    @Subscribe("more10NumberPositionField")
    /** Смена чекбокса «более 10 позиций». */
    public void onMore10NumberPositionFieldValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (event.getValue() != null) {
            if (event.getValue()) {
                numberPositionField.setRequired(false);
            } else {
                numberPositionField.setRequired(true);
            }
        }

    }

    /** Создание новости позиции (OpenPositionNews). */
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
    /** Смена чекбокса «черновик» → блокировка полей. */
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
    /** Смена чекбокса «запрос зарплаты у кандидата». */
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
    /** Смена грейда → генерация названия вакансии. */
    public void onGradeLookupPickerFieldValueChange(HasValue.ValueChangeEvent<Grade> event) {
        boolean flag = false;

        for (Grade grade : gradeDc.getItems()) {
            if (vacansyNameField.getValue() != null) {
                if (vacansyNameField.getValue().startsWith(grade.getGradeName())) {
                    vacansyNameField.setValue(event.getValue().getGradeName()
                            + vacansyNameField.getValue()
                            .substring(grade.getGradeName().length()));
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
            vacansyNameField.setValue(event.getValue().getGradeName() + " " + vacansyNameField.getValue());
        }
    }

    /** Обработчик кнопки автогенерации названия вакансии. */
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

    /** Генерация названия вакансии по типу позиции и проекту. */
    private String generateVacancyName() {
//        String retStr = "";
        StringBuilder sb = new StringBuilder();

        if (gradeLookupPickerField.getValue() != null) {
            sb.append(gradeLookupPickerField.getValue().getGradeName())
                    .append(" ");
//            retStr = gradeLookupPickerField.getValue().getGradeName() + " ";
        } else {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .withContentMode(ContentMode.HTML)
                    .withType(Notifications.NotificationType.HUMANIZED)
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withDescription(messageBundle.getMessage("msgGenerateError")
                            + ": "
                            + messageBundle.getMessage("msgNotGrade"))
                    .show();
        }

        if (positionTypeField.getValue() != null) {
            sb.append(positionTypeField.getValue().getPositionRuName())
                    .append(" / ")
                    .append(positionTypeField.getValue().getPositionEnName());
/*            retStr += positionTypeField.getValue().getPositionRuName()
                    + " / "
                    + positionTypeField.getValue().getPositionEnName(); */
        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .withContentMode(ContentMode.HTML)
                    .withType(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("msgError"))
                    .withDescription(messageBundle.getMessage("msgGenerateError")
                            + ": "
                            + messageBundle.getMessage("msgNotPositionName"))
                    .show();

            return "";
        }

        if (projectNameField.getValue() != null) {
            sb.append(" (")
                    .append(projectNameField.getValue().getProjectName());
//            retStr += " (" + projectNameField.getValue().getProjectName();
        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .withContentMode(ContentMode.HTML)
                    .withType(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("msgError"))
                    .withDescription(messageBundle.getMessage("msgGenerateError")
                            + ": "
                            + messageBundle.getMessage("msgNotProjectName"))
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
                    .withDescription(messageBundle.getMessage("msgGenerateError")
                            + ": "
                            + messageBundle.getMessage("msgNotCity"))
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

    /** Подстановка зарплаты из рейтов аутстафа (OutstaffingRates). */
    public void setSalaryFieldButtonInvoke() {
        OutstaffingRates outstaffingRates = null;

        try {
            outstaffingRates = dataManager.load(OutstaffingRates.class)
                    .query(QUERY_OUTSTAFF_RATES)
                    .parameter("rate", outstaffingCostTextField.getValue())
                    .one();
        } catch (NoResultException | IllegalStateException e) {
            e.printStackTrace();

            notifications.create(Notifications.NotificationType.ERROR)
                    .withType(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("msgError"))
                    .withDescription(messageBundle.getMessage("msgErrorNotCostForSalary")
                            + " "
                            + outstaffingCostTextField.getValue())
                    .show();
        }

        if (outstaffingRates != null) {
            String commentSalary = messageBundle.getMessage("msgMarginalRate")
                    + outstaffingCostTextField.getValue() + " \n"
                    + messageBundle.getMessage("msgMinSalary")
                    + outstaffingRates.getMinSalary() + " \n"
                    + messageBundle.getMessage("msgMaxSalary")
                    + outstaffingRates.getMaxSalary() + " \n"
                    + messageBundle.getMessage("msgSalaryIE") + ": "
                    + outstaffingRates.getMaxIESalary();

            openPositionFieldSalaryMin.setValue(outstaffingRates.getMinSalary());
            openPositionFieldSalaryMax.setValue(outstaffingRates.getMaxSalary());
            openPositionFieldSalaryIE.setValue(outstaffingRates.getMaxIESalary());
            salaryCommentTextFiels.setValue(commentSalary);

            notifications.create(Notifications.NotificationType.SYSTEM)
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withDescription(messageBundle.getMessage("msgSetSalaryToForm")
                            + " "
                            + commentSalary)
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
