package com.company.hunttech.web.screens.openposition;

import com.company.hunttech.entity.OpenPosition;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.ViewBuilder;
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
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.Subscribe;
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
 * UI-контракта Edit-экранов, label-навигацией и её active-state.</p>
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

    @Inject
    private DataManager dataManager;
    @Inject
    private TabSheet tabSheetOpenPosition;
    @Inject
    private VBoxLayout openPositionPreviewSidebar;
    @Inject
    private VBoxLayout openPositionPreviewWorkspace;
    @Inject
    private HBoxLayout editActions;
    @Inject
    private Label<String> closedVacancyInfoLabel;
    @Inject
    private Label<String> labelTopComissionRecrutier;
    @Inject
    private Label<String> labelTopComissionResearcher;
    @Inject
    private Label<String> citiesLabel;
    @Inject
    private TextField<String> ownerTextField;
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

    /**
     * После создания XML-компонентов назначает общие edit-* / label-* роли.
     * Метод меняет только stylename, panel-представление и контрактную ширину
     * sidebar; data binding, required, editable, loaders и actions не затрагиваются.
     */
    @Subscribe
    protected void onPreviewInit(InitEvent event) {
        applySharedEditScreenContract();
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
        openPositionPreviewSidebar.setWidth("270px");
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
