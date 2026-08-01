package com.company.hunttech.web.screens.openposition;

import com.company.hunttech.entity.OpenPosition;
import com.haulmont.cuba.gui.Route;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.RadioButtonGroup;
import com.haulmont.cuba.gui.components.TabSheet;
import com.haulmont.cuba.gui.screen.AfterShowEvent;
import com.haulmont.cuba.gui.screen.EditedEntityContainer;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

/**
 * Изолированный предварительный вариант OpenPositionEdit.
 *
 * <p>Контроллер наследует исходный {@link OpenPositionEdit} без переопределения
 * lifecycle-обработчиков, loaders, validation, save-процесса и бизнес-действий.
 * Собственная логика ограничена переключением вкладок из label-навигации и
 * визуальным состоянием активного пункта. Поэтому preview использует тот же
 * контракт сущности {@link OpenPosition}, но не заменяет legacy-экран.</p>
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
