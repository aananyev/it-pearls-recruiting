package com.company.hunttech.web.screens.openposition;

import com.company.hunttech.entity.OpenPosition;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.ViewBuilder;
import com.haulmont.cuba.gui.Route;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.RadioButtonGroup;
import com.haulmont.cuba.gui.components.TabSheet;
import com.haulmont.cuba.gui.screen.Screen.AfterShowEvent;
import com.haulmont.cuba.gui.screen.Screen.BeforeShowEvent;
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
 * validation, save-процесс, loaders и бизнес-действия. Единственное защитное
 * переопределение lifecycle до вызова базового {@code onBeforeShow} догружает
 * {@code positionType} для URL-маршрута, где CUBA восстанавливает detached
 * экземпляр сущности. Остальная собственная логика ограничена label-навигацией
 * и её presentation-состоянием, поэтому preview не заменяет legacy-экран.</p>
 */
@Route("open-position-edit-preview")
@UiController("hunttech_OpenPosition.editPreview")
@UiDescriptor("open-position-edit-preview.xml")
@EditedEntityContainer("openPositionDc")
@LoadDataBeforeShow
public class OpenPositionEditPreview extends OpenPositionEdit {

    private static final String NAV_STYLE =
            "borderless label-nav-item open-position-preview-nav-item";
    private static final String NAV_ACTIVE_STYLE =
            "borderless label-nav-item label-nav-item-active open-position-preview-nav-item";

    @Inject
    private DataManager dataManager;
    @Inject
    private TabSheet tabSheetOpenPosition;
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
     * lazy-связью сам провоцирует инстанцирование старого valueholder
     * (EclipseLink ValidationException «null Session»). Базовый
     * {@link OpenPositionEdit} продолжает работать без изменений.
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
                        // полный состав openPosition-edit-view (полная версия,
                        // строка 232 views.xml): все поля формы, иначе setItem
                        // вскрывает следующую lazy-связь (grade и др.)
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
     * Синхронизирует active-состояние label-навигации с реальной выбранной
     * вкладкой. Метод не влияет на загрузчики: штатный listener базового
     * OpenPositionEdit по-прежнему выполняет ленивую загрузку содержимого.
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
        setNavigationStyle(previewNavMain, "tabOpenPosition".equals(activeTabId));
        setNavigationStyle(previewNavLaborAgreement, "laborAgreementTab".equals(activeTabId));
        setNavigationStyle(previewNavPayments, "tabPayments".equals(activeTabId));
        setNavigationStyle(previewNavDescription, "tabJobDescription".equals(activeTabId));
        setNavigationStyle(previewNavFiles, "tabFiles".equals(activeTabId));
        setNavigationStyle(previewNavExercise, "tabExercise".equals(activeTabId));
        setNavigationStyle(previewNavMemo, "tabMemoForInterview".equals(activeTabId));
        setNavigationStyle(previewNavTemplateLetter, "tabTemplateLetter".equals(activeTabId));
        setNavigationStyle(previewNavSkills, "tabSkills".equals(activeTabId));
        setNavigationStyle(previewNavNews, "tabOpenPositionNews".equals(activeTabId));
        setNavigationStyle(previewNavApproval, "tabApproval".equals(activeTabId));
        setNavigationStyle(previewNavComments, "commentsTab".equals(activeTabId));
    }

    private void updatePaymentsNavigationVisibility(Integer commandCandidate) {
        previewNavPayments.setVisible(Integer.valueOf(1).equals(commandCandidate));
    }

    private void setNavigationStyle(Button button, boolean active) {
        button.setStyleName(active ? NAV_ACTIVE_STYLE : NAV_STYLE);
    }
}
