package com.company.hunttech.web.screens.openposition;

import com.company.hunttech.entity.OpenPosition;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.ViewBuilder;
import com.company.hunttech.web.StandartRegistrationForWork;
import com.haulmont.cuba.gui.screen.MessageBundle;
import com.haulmont.cuba.gui.Route;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.ComponentContainer;
import com.haulmont.cuba.gui.components.DateField;
import com.haulmont.cuba.gui.components.GroupBoxLayout;
import com.haulmont.cuba.gui.components.HBoxLayout;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.components.RadioButtonGroup;
import com.haulmont.cuba.gui.components.RichTextArea;
import com.haulmont.cuba.gui.components.ScrollBoxLayout;
import com.haulmont.cuba.gui.components.SuggestionPickerField;
import com.haulmont.cuba.gui.components.TabSheet;
import com.haulmont.cuba.gui.components.TextArea;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.screen.Screen.AfterShowEvent;
import com.haulmont.cuba.gui.screen.Screen.BeforeShowEvent;
import com.haulmont.cuba.gui.screen.Screen.InitEvent;
import com.haulmont.cuba.gui.screen.EditedEntityContainer;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.Target;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

/**
 * Изолированный предварительный вариант OpenPositionEdit.
 *
 * <p>Контроллер наследует исходный {@link OpenPositionEdit} и делегирует ему
 * validation, save-процесс, loaders и бизнес-действия. Защитное переопределение
 * lifecycle до вызова базового {@code onBeforeShow} догружает {@code positionType}
 * для URL-маршрута, где CUBA восстанавливает detached-экземпляр сущности.
 * Остальная собственная логика ограничена presentation-слоем: применением общего
 * UI-контракта Edit-экранов, компоновкой существующих компонентов, label-навигацией
 * и её active-state.</p>
 */
@Route("open-position-edit-preview")
@UiController("hunttech_OpenPosition.editPreview")
@UiDescriptor("open-position-edit-preview.xml")
@EditedEntityContainer("openPositionDc")
@LoadDataBeforeShow
public class OpenPositionEditPreview extends OpenPositionEdit {

    private static final String BASE_NAV_STYLE = "label-nav-item";
    private static final String ACTIVE_NAV_STYLE = "label-nav-item-active";
    private static final String ACCORDION_STYLE = "edit-accordion-section";
    private static final String FORM_CONTROL_STYLE = "edit-form-control";
    private static final String WORKSPACE_SCROLL_STYLE = "edit-workspace-scroll";
    private static final String WORKSPACE_CONTENT_STYLE = "edit-workspace-content";
    private static final String FIELD_ROW_STYLE = "open-position-preview-field-row";
    private static final String PRIMARY_SECTION_STYLE = "open-position-preview-primary-section";

    @Inject
    private DataManager dataManager;
    @Inject
    private TabSheet tabSheetOpenPosition;
    @Inject
    private VBoxLayout openPositionPreviewSidebar;
    @Inject
    private VBoxLayout openPositionPreviewIdentity;
    @Inject
    private VBoxLayout openPositionPreviewNavigation;
    @Inject
    private VBoxLayout openPositionPreviewSummary;
    @Inject
    private VBoxLayout openPositionPreviewSidebarSpacer;
    @Inject
    private VBoxLayout openPositionPreviewWorkspace;
    @Inject
    private HBoxLayout openPositionPreviewToolbar;
    @Inject
    private ScrollBoxLayout mainTabScrollBox;
    @Inject
    private HBoxLayout editActions;
    @Inject
    private Component projectLogoImage;
    @Inject
    private Component projectOwnerImage;
    @Inject
    private Label<String> labelOpenPosition;
    @Inject
    private Label<String> signDraftLabel;
    @Inject
    private Label<String> closedVacancyInfoLabel;
    @Inject
    private Label<String> labelTopComissionRecrutier;
    @Inject
    private Label<String> labelTopComissionResearcher;
    @Inject
    private Label<String> citiesLabel;
    @Inject
    private Label<String> summaryVacansyIDLabel;
    @Inject
    private Label<String> summaryPositionTypeLabel;
    @Inject
    private Label<String> summaryGradeLabel;
    @Inject
    private Label<String> summaryProjectNameLabel;
    @Inject
    private Label<String> summaryCityPositionLabel;
    @Inject
    private Label<String> summaryNumberPositionLabel;
    @Inject
    private Label<String> summaryRegistrationForWorkLabel;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private TextField<String> ownerTextField;
    @Inject
    private Button windowCommitAndCloseButton;
    @Inject
    private Button windowCloseButton;
    @Inject
    private Button previewNavMain;
    @Inject
    private Button previewNavLaborAgreement;
    @Inject
    private Button previewNavPayments;
    @Inject
    private Button previewNavDescription;
    @Inject
    private Button previewNavFiles;
    @Inject
    private Button previewNavExercise;
    @Inject
    private Button previewNavMemo;
    @Inject
    private Button previewNavTemplateLetter;
    @Inject
    private Button previewNavSkills;
    @Inject
    private Button previewNavNews;
    @Inject
    private Button previewNavApproval;
    @Inject
    private Button previewNavComments;
    @Inject
    private RadioButtonGroup<Integer> commandOrPosition;

    @Inject
    private GroupBoxLayout identityStatusAccordion;
    @Inject
    private GroupBoxLayout commandFieldHBox;
    @Inject
    private GroupBoxLayout commandVacancyAccordion;
    @Inject
    private GroupBoxLayout projectLocationAccordion;
    @Inject
    private GroupBoxLayout positionCountAccordion;
    @Inject
    private GroupBoxLayout salaryAccordion;

    @Inject
    private HBoxLayout vacancyNameHBox;
    @Inject
    private HBoxLayout vacancyTitleSpacerHBox;
    @Inject
    private HBoxLayout hboxProject1;
    @Inject
    private HBoxLayout hboxVacansy;
    @Inject
    private HBoxLayout hboxProject;
    @Inject
    private HBoxLayout hboxCompany;
    @Inject
    private HBoxLayout hboxSalary;
    @Inject
    private HBoxLayout space2Box;

    /**
     * После создания XML-компонентов назначает общие edit-* / label-* роли и
     * локальные layout-классы. Метод меняет только stylename, размеры визуальных
     * образов, panel-представление и пустой spacer; data binding, required,
     * editable, loaders, listeners и actions не затрагиваются.
     */
    @Subscribe
    protected void onPreviewInit(InitEvent event) {
        applySharedEditScreenContract();
        applyPreviewLayoutPolish();
    }

    /**
     * Перед штатной инициализацией legacy-контроллера подготавливает lazy-связь
     * {@code positionType}. Это предотвращает EclipseLink ValidationException
     * при прямом URL-маршруте, не меняя порядок и содержание базового lifecycle.
     */
    @Override
    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        ensureRoutePositionTypeLoaded();
        super.onBeforeShow(event);
    }

    /**
     * Догружает только связь {@code positionType} и её LOB-описания, если CUBA
     * восстановила editor entity после URL-навигации без активной persistence
     * session. Экземпляр в контейнере заменяется reload-нутым целиком: прямой
     * сеттер {@code setPositionType} на detached entity с неинициализированной
     * lazy-связью сам провоцирует инстанцирование старого valueholder.
     */
    private void ensureRoutePositionTypeLoaded() {
        OpenPosition editedPosition = getEditedEntity();
        if (PersistenceHelper.isNew(editedPosition)
                || PersistenceHelper.isLoaded(editedPosition, "positionType")) {
            return;
        }

        OpenPosition reloadedPosition = dataManager.load(OpenPosition.class)
                .id(editedPosition.getId())
                .view(ViewBuilder.of(OpenPosition.class)
                        // Полный состав openPosition-edit-view: setItem не должен
                        // вскрывать следующую незагруженную lazy-связь формы.
                        .add("vacansyID").add("vacansyName").add("openClose")
                        .add("signDraft").add("priority").add("rating")
                        .add("lastOpenDate").add("closingDate").add("remoteWork")
                        .add("registrationForWork").add("remoteComment")
                        .add("needExercise").add("needLetter").add("needMemoForInterview")
                        .add("salaryMin").add("salaryMax").add("salaryIE")
                        .add("salaryFixLimit").add("salaryCandidateRequest")
                        .add("salaryComment").add("outstaffingCost")
                        .add("numberPosition").add("more10NumberPosition")
                        .add("workExperience").add("commandCandidate")
                        .add("commandExperience").add("internalProject")
                        .add("shortDescription").add("rawDescription")
                        .add("interviewChecklist").add("searchMap").add("interviewPlan")
                        .add("paymentsType").add("typeCompanyComission")
                        .add("typeSalaryOfResearcher").add("typeSalaryOfRecrutier")
                        .add("useTaxNDFL").add("percentComissionOfCompany")
                        .add("percentSalaryOfResearcher").add("percentSalaryOfRecrutier")
                        .add("priorityComment")
                        .add("grade", "grade-picker-view")
                        .add("cityPosition", "city-picker-view")
                        .add("cities", "city-picker-view")
                        .add("positionType", positionType -> positionType
                                .add("positionRuName")
                                .add("positionEnName")
                                .add("standartDescription")
                                .add("whoIsThisGuy"))
                        .add("projectName", "project-edit-view")
                        .add("parentOpenPosition", "openPosition-parent-picker-view")
                        .add("owner", "extUser-picker-view")
                        .build())
                .one();
        getEditedEntityContainer().setItem(reloadedPosition);
    }

    /**
     * Применяет общий UI API HRM HuntTech поверх уже утверждённой компоновки.
     * Shared SCSS подключён во всех семи темах, поэтому здесь не создаются
     * копии CSS и не изменяются theme-файлы других экранов.
     */
    private void applySharedEditScreenContract() {
        openPositionPreviewSidebar.setWidth("264px");
        openPositionPreviewSidebar.addStyleName("edit-sidebar");
        openPositionPreviewWorkspace.addStyleName("edit-workspace");

        closedVacancyInfoLabel.addStyleName("edit-sidebar-warning");
        labelTopComissionRecrutier.addStyleName("edit-sidebar-hint");
        labelTopComissionResearcher.addStyleName("edit-sidebar-hint");
        citiesLabel.addStyleName("edit-help");
        ownerTextField.addStyleName(FORM_CONTROL_STYLE);

        editActions.removeStyleName("edit-actions");
        editActions.addStyleName("edit-footer-actions");

        applySharedWorkspaceStyles(openPositionPreviewWorkspace);
    }

    /**
     * Улучшает визуальную иерархию без перестановки бизнес-полей: сокращает
     * перегруженный профиль вакансии, закрепляет семантические роли основных
     * секций и переводит существующие HBox-строки в локальную responsive-сетку.
     */
    private void applyPreviewLayoutPolish() {
        addStyles(openPositionPreviewSidebar,
                "open-position-preview-sidebar-compact");
        addStyles(openPositionPreviewIdentity,
                "open-position-preview-identity-card");
        addStyles(openPositionPreviewNavigation,
                "open-position-preview-navigation");
        addStyles(openPositionPreviewSummary,
                "open-position-preview-summary-card");
        addStyles(openPositionPreviewSidebarSpacer,
                "open-position-preview-sidebar-flex");
        addStyles(openPositionPreviewWorkspace,
                "open-position-preview-workspace-polished");
        addStyles(openPositionPreviewToolbar,
                "open-position-preview-toolbar");
        addStyles(tabSheetOpenPosition,
                "open-position-preview-tabs");
        // Legacy-обработчик сохраняет доступ к tabPayments, но в утверждённой
        // компоновке его header скрыт, поскольку поля показаны в договорах.
        tabSheetOpenPosition.getTab("tabPayments")
                .setStyleName("open-position-preview-payments-tab");
        addStyles(mainTabScrollBox,
                "open-position-preview-main-scroll");
        addStyles(editActions,
                "open-position-preview-footer");

        projectLogoImage.setWidth("88px");
        projectLogoImage.setHeight("88px");
        projectLogoImage.addStyleName("open-position-preview-logo");
        projectOwnerImage.setWidth("70px");
        projectOwnerImage.setHeight("70px");
        projectOwnerImage.addStyleName("open-position-preview-owner-image");

        addStyles(labelOpenPosition,
                "open-position-preview-title-clamp");
        addStyles(signDraftLabel,
                "open-position-preview-status-line");
        addStyles(windowCommitAndCloseButton,
                "open-position-preview-primary-action");
        addStyles(windowCloseButton,
                "open-position-preview-secondary-action");

        addPrimarySection(identityStatusAccordion);
        addStyles(commandFieldHBox,
                "open-position-preview-subsection");
        addPrimarySection(commandVacancyAccordion);
        addStyles(projectLocationAccordion,
                "open-position-preview-project-section");
        addPrimarySection(positionCountAccordion);
        addPrimarySection(salaryAccordion);

        addFieldRow(vacancyNameHBox,
                "open-position-preview-row-title");
        addFieldRow(hboxProject1,
                "open-position-preview-row-three");
        addFieldRow(hboxVacansy,
                "open-position-preview-row-position");
        addStyles(hboxVacansy,
                "open-position-preview-project-type-row");
        addFieldRow(hboxProject,
                "open-position-preview-row-half");
        addStyles(hboxProject,
                "open-position-preview-project-name-row");
        addFieldRow(hboxCompany,
                "open-position-preview-row-half");
        addStyles(hboxCompany,
                "open-position-preview-project-company-row");
        addFieldRow(hboxSalary,
                "open-position-preview-row-salary");
        addFieldRow(space2Box,
                "open-position-preview-row-wide");

        // Пустой legacy-spacer создавал заметный разрыв между реквизитами и
        // настройками команды; его скрытие не меняет данные или события формы.
        vacancyTitleSpacerHBox.setVisible(false);
    }

    private void addPrimarySection(GroupBoxLayout section) {
        section.addStyleName(PRIMARY_SECTION_STYLE);
    }

    private void addFieldRow(HBoxLayout row, String specificStyle) {
        row.addStyleName(FIELD_ROW_STYLE);
        row.addStyleName(specificStyle);
    }

    private void addStyles(Component component, String... styleNames) {
        for (String styleName : styleNames) {
            component.addStyleName(styleName);
        }
    }

    /**
     * Проходит по фактическому дереву компонентов CUBA и нормализует только
     * визуальные роли. GroupBoxLayout становится общей accordion-секцией,
     * ScrollBoxLayout получает общий scroll-контракт, а типовые поля —
     * непосредственный {@code edit-form-control}, требуемый UI-контрактом.
     */
    private void applySharedWorkspaceStyles(Component component) {
        if (isSharedFormControl(component)) {
            component.addStyleName(FORM_CONTROL_STYLE);
        }

        if (component instanceof GroupBoxLayout) {
            GroupBoxLayout section = (GroupBoxLayout) component;
            section.removeStyleName("light");
            section.removeStyleName("edit-card");
            section.addStyleName(ACCORDION_STYLE);
            section.setShowAsPanel(true);
        }

        if (component instanceof ScrollBoxLayout) {
            component.addStyleName(WORKSPACE_SCROLL_STYLE);
            for (Component child : ((ScrollBoxLayout) component).getOwnComponents()) {
                if (child instanceof VBoxLayout) {
                    child.addStyleName(WORKSPACE_CONTENT_STYLE);
                }
            }
        }

        if (component instanceof ComponentContainer) {
            for (Component child : ((ComponentContainer) component).getOwnComponents()) {
                applySharedWorkspaceStyles(child);
            }
        }
    }

    /**
     * Ограничивает общий field-style типовыми компонентами из контракта и не
     * меняет геометрию CheckBox, RadioButtonGroup, таблиц и action-кнопок.
     */
    private boolean isSharedFormControl(Component component) {
        return component instanceof TextField
                || component instanceof TextArea
                || component instanceof LookupField
                || component instanceof LookupPickerField
                || component instanceof SuggestionPickerField
                || component instanceof DateField
                || component instanceof RichTextArea;
    }

    /**
     * После завершения штатной инициализации базового editor синхронизирует
     * presentation-состояние навигации. Вкладка оплат остаётся доступной только
     * для карточки команды — ровно по условию legacy OpenPositionEdit.
     */
    @Subscribe
    protected void onPreviewAfterShow(AfterShowEvent event) {
        updatePaymentsNavigationVisibility(commandOrPosition.getValue());
        if (tabSheetOpenPosition.getSelectedTab() != null) {
            updateNavigationState(tabSheetOpenPosition.getSelectedTab().getName());
        }
        updateSidebarTitleTooltip();
        refreshSummary();
    }

    /**
     * Синхронизирует сводку «Ключевые параметры» в sidebar с текущими
     * значениями редактируемой вакансии: обновляется при показе экрана и при
     * каждом изменении атрибута — сводка всегда совпадает с вкладкой
     * «Основное» (грейд, проект, город и т.д.).
     */
    private void refreshSummary() {
        OpenPosition op = getEditedEntity();
        summaryVacansyIDLabel.setValue(op.getVacansyID() != null ? op.getVacansyID() : "");
        summaryPositionTypeLabel.setValue(instanceName(op.getPositionType()));
        summaryGradeLabel.setValue(instanceName(op.getGrade()));
        summaryProjectNameLabel.setValue(instanceName(op.getProjectName()));
        summaryCityPositionLabel.setValue(instanceName(op.getCityPosition()));
        summaryNumberPositionLabel.setValue(op.getNumberPosition() != null
                ? String.valueOf(op.getNumberPosition()) : "");
        summaryRegistrationForWorkLabel.setValue(registrationForWorkName(op.getRegistrationForWork()));
    }

    /**
     * Возвращает человекочитаемое имя типа оформления кандидата по коду
     * {@link StandartRegistrationForWork}; для неизвестного кода — пустая
     * строка (сводка не показывает «null»).
     */
    private String registrationForWorkName(Integer code) {
        if (code == null) {
            return "";
        }
        if (StandartRegistrationForWork.OUTSTAFING.equals(code)) {
            return messageBundle.getMessage("mainmsgOutstaffing");
        }
        if (StandartRegistrationForWork.RECRUITING.equals(code)) {
            return messageBundle.getMessage("mainmsgRecruiting");
        }
        if (StandartRegistrationForWork.ALL.equals(code)) {
            return messageBundle.getMessage("mainmsgAllVariants");
        }
        return "";
    }

    /**
     * Возвращает instanceName сущности (как у property-биндинга CUBA),
     * либо пустую строку для {@code null} — сводка не показывает «null».
     */
    private String instanceName(Object entity) {
        return entity != null ? entity.toString() : "";
    }

    /**
     * Любое изменение полей вакансии во вкладке «Основное» немедленно
     * отражается в сводке sidebar.
     */
    @Subscribe(id = "openPositionDc", target = Target.DATA_CONTAINER)
    protected void onPreviewItemPropertyChanged(InstanceContainer.ItemPropertyChangeEvent<OpenPosition> event) {
        refreshSummary();
    }

    /**
     * Полное название вакансии остаётся доступным как tooltip, хотя CSS
     * ограничивает его высоту в sidebar для сохранения навигации на экране.
     */
    private void updateSidebarTitleTooltip() {
        String title = labelOpenPosition.getValue();
        if (title != null && !title.trim().isEmpty()) {
            labelOpenPosition.setDescription(title);
        }
    }

    /**
     * Повторяет в label-навигации уже существующую видимость вкладки оплат,
     * не изменяя значение сущности и не вмешиваясь в обработчик базового экрана.
     */
    @Subscribe("commandOrPosition")
    protected void onPreviewCommandOrPositionChanged(HasValue.ValueChangeEvent<Integer> event) {
        updatePaymentsNavigationVisibility(event.getValue());
    }

    /**
     * Синхронизирует active-state label-навигации с реальной выбранной вкладкой.
     * Метод не влияет на загрузчики: штатный listener базового OpenPositionEdit
     * по-прежнему выполняет ленивую загрузку содержимого.
     */
    @Subscribe("tabSheetOpenPosition")
    protected void onPreviewTabChanged(TabSheet.SelectedTabChangeEvent event) {
        if (event.getSelectedTab() != null) {
            updateNavigationState(event.getSelectedTab().getName());
        }
    }

    public void previewOpenMain() {
        selectTab("tabOpenPosition");
    }

    public void previewOpenLaborAgreement() {
        selectTab("laborAgreementTab");
    }

    public void previewOpenPayments() {
        selectTab("tabPayments");
    }

    public void previewOpenDescription() {
        selectTab("tabJobDescription");
    }

    public void previewOpenFiles() {
        selectTab("tabFiles");
    }

    public void previewOpenExercise() {
        selectTab("tabExercise");
    }

    public void previewOpenMemo() {
        selectTab("tabMemoForInterview");
    }

    public void previewOpenTemplateLetter() {
        selectTab("tabTemplateLetter");
    }

    public void previewOpenSkills() {
        selectTab("tabSkills");
    }

    public void previewOpenNews() {
        selectTab("tabOpenPositionNews");
    }

    public void previewOpenApproval() {
        selectTab("tabApproval");
    }

    public void previewOpenComments() {
        selectTab("commentsTab");
    }

    /** Переключает только presentation-состояние TabSheet. */
    private void selectTab(String tabId) {
        tabSheetOpenPosition.setSelectedTab(tabId);
    }

    private void updateNavigationState(String activeTabId) {
        setNavigationActive(previewNavMain, "tabOpenPosition".equals(activeTabId));
        setNavigationActive(previewNavLaborAgreement, "laborAgreementTab".equals(activeTabId));
        setNavigationActive(previewNavPayments, "tabPayments".equals(activeTabId));
        setNavigationActive(previewNavDescription, "tabJobDescription".equals(activeTabId));
        setNavigationActive(previewNavFiles, "tabFiles".equals(activeTabId));
        setNavigationActive(previewNavExercise, "tabExercise".equals(activeTabId));
        setNavigationActive(previewNavMemo, "tabMemoForInterview".equals(activeTabId));
        setNavigationActive(previewNavTemplateLetter, "tabTemplateLetter".equals(activeTabId));
        setNavigationActive(previewNavSkills, "tabSkills".equals(activeTabId));
        setNavigationActive(previewNavNews, "tabOpenPositionNews".equals(activeTabId));
        setNavigationActive(previewNavApproval, "tabApproval".equals(activeTabId));
        setNavigationActive(previewNavComments, "commentsTab".equals(activeTabId));
    }

    private void updatePaymentsNavigationVisibility(Integer commandCandidate) {
        previewNavPayments.setVisible(Integer.valueOf(1).equals(commandCandidate));
    }

    /**
     * Базовый label-nav-item остаётся на компоненте постоянно; изменяется только
     * общий state-класс label-nav-item-active, как требует UI-контракт.
     */
    private void setNavigationActive(Button button, boolean active) {
        button.addStyleName(BASE_NAV_STYLE);
        button.removeStyleName(ACTIVE_NAV_STYLE);
        if (active) {
            button.addStyleName(ACTIVE_NAV_STYLE);
        }
    }
}
