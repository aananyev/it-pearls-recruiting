package com.company.hunttech.web.screens.openposition;

import com.company.hunttech.entity.OpenPosition;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.PopupButton;
import com.haulmont.cuba.gui.components.TreeDataGrid;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import java.util.Set;

/**
 * Контроллер Split-View реестра открытых вакансий ({@code hunttech_OpenPositionReestr.browse}).
 *
 * <p>Наследует полную бизнес-логику работы с открытыми позициями и профильным сайдбаром (312px)
 * из {@link OpenPositionBrowse}, дополняя ее специализированным тулбаром быстрых фильтров и действий.</p>
 */
@UiController("hunttech_OpenPositionReestr.browse")
@UiDescriptor("open-position-reestr-browse.xml")
@com.haulmont.cuba.gui.screen.LookupComponent("openPositionsTable")
@LoadDataBeforeShow
public class OpenPositionReestrBrowse extends OpenPositionBrowse {

    @Inject
    private Notifications notifications;

    @Inject
    private TreeDataGrid<OpenPosition> openPositionsTable;
    @Inject
    private CollectionLoader<OpenPosition> openPositionsDl;

    // Кнопки тулбара и быстрых действий реестра
    @Inject
    private Button createPositionBtn;
    @Inject
    private Button smartUploadBtn;
    @Inject
    private Button editPositionToolbarBtn;
    @Inject
    private Button removePositionToolbarBtn;

    @Inject
    private PopupButton vacanciesFilterPopupButton;
    @Inject
    private PopupButton priorityFilterPopupButton;
    @Inject
    private PopupButton actionsWithPositionButton;

    @Subscribe
    public void onInit(Screen.InitEvent event) {
        initToolbarActions();
        initFilterPopupActions();
    }

    @Subscribe
    public void onAfterShow(Screen.AfterShowEvent event) {
        initTableSelectionListener();
        updateSidebarWithPosition(null);
    }

    private void initTableSelectionListener() {
        if (openPositionsTable != null) {
            openPositionsTable.addSelectionListener(e -> {
                Set<OpenPosition> selected = e.getSelected();
                OpenPosition position = selected.isEmpty() ? null : selected.iterator().next();
                updateSidebarWithPosition(position);
                updateToolbarButtonsState(position != null);
            });
        }
    }

    private void updateToolbarButtonsState(boolean hasSelection) {
        if (editPositionToolbarBtn != null) {
            editPositionToolbarBtn.setEnabled(hasSelection);
        }
        if (removePositionToolbarBtn != null) {
            removePositionToolbarBtn.setEnabled(hasSelection);
        }
        if (openEditCardBtn != null) {
            openEditCardBtn.setEnabled(hasSelection);
        }
        if (suggestCandidatesBtn != null) {
            suggestCandidatesBtn.setEnabled(hasSelection);
        }
        if (subscribeBtn != null) {
            subscribeBtn.setEnabled(hasSelection);
        }
    }

    private void initToolbarActions() {
        if (createPositionBtn != null) {
            createPositionBtn.addClickListener(e -> {
                screenBuilders.editor(OpenPosition.class, this)
                        .newEntity()
                        .withOpenMode(OpenMode.NEW_TAB)
                        .show();
            });
        }
        if (smartUploadBtn != null) {
            smartUploadBtn.addClickListener(e -> {
                notifications.create(Notifications.NotificationType.TRAY)
                        .withCaption("Умная загрузка")
                        .withDescription("Мастер умного создания вакансии открывается...")
                        .show();
            });
        }
        if (editPositionToolbarBtn != null) {
            editPositionToolbarBtn.addClickListener(e -> openSelectedForEdit());
        }
        if (removePositionToolbarBtn != null) {
            removePositionToolbarBtn.addClickListener(e -> {
                Action removeAction = openPositionsTable.getAction("remove");
                if (removeAction != null) {
                    removeAction.actionPerform(openPositionsTable);
                }
            });
        }
    }

    private void openSelectedForEdit() {
        OpenPosition selected = openPositionsTable.getSingleSelected();
        if (selected != null) {
            screenBuilders.editor(OpenPosition.class, this)
                    .editEntity(selected)
                    .withOpenMode(OpenMode.NEW_TAB)
                    .show();
        }
    }

    private void initFilterPopupActions() {
        if (vacanciesFilterPopupButton != null) {
            vacanciesFilterPopupButton.addAction(new BaseAction("filterAll")
                    .withCaption("Все открытые вакансии")
                    .withIcon("COMPASS")
                    .withHandler(e -> {
                        openPositionsDl.removeParameter("openClosePos");
                        openPositionsDl.removeParameter("subscriber");
                        openPositionsDl.removeParameter("newOpenPosition");
                        openPositionsDl.setParameter("openClosePos", false);
                        openPositionsDl.load();
                        vacanciesFilterPopupButton.setCaption("Все открытые");
                    }));

            vacanciesFilterPopupButton.addAction(new BaseAction("filterMySubscriptions")
                    .withCaption("Мои подписки")
                    .withIcon("USER")
                    .withHandler(e -> {
                        openPositionsDl.removeParameter("newOpenPosition");
                        openPositionsDl.setParameter("subscriber", userSession.getUser());
                        openPositionsDl.setParameter("openClosePos", false);
                        openPositionsDl.load();
                        vacanciesFilterPopupButton.setCaption("Мои подписки");
                    }));

            vacanciesFilterPopupButton.addAction(new BaseAction("filterNew")
                    .withCaption("Новые (3 дня)")
                    .withIcon("CLOCK_O")
                    .withHandler(e -> {
                        openPositionsDl.removeParameter("subscriber");
                        openPositionsDl.setParameter("newOpenPosition", 3);
                        openPositionsDl.setParameter("openClosePos", false);
                        openPositionsDl.load();
                        vacanciesFilterPopupButton.setCaption("Новые (3 дня)");
                    }));

            vacanciesFilterPopupButton.addAction(new BaseAction("filterAllWithArchive")
                    .withCaption("Все, включая архивные")
                    .withIcon("ARCHIVE")
                    .withHandler(e -> {
                        openPositionsDl.removeParameter("openClosePos");
                        openPositionsDl.removeParameter("subscriber");
                        openPositionsDl.removeParameter("newOpenPosition");
                        openPositionsDl.load();
                        vacanciesFilterPopupButton.setCaption("Все (с архивом)");
                    }));
        }

        if (priorityFilterPopupButton != null) {
            priorityFilterPopupButton.addAction(new BaseAction("priorityAll")
                    .withCaption("Все приоритеты")
                    .withIcon("LIST")
                    .withHandler(e -> {
                        openPositionsDl.removeParameter("priority");
                        openPositionsDl.load();
                        priorityFilterPopupButton.setCaption("Приоритет");
                    }));

            priorityFilterPopupButton.addAction(new BaseAction("priorityHigh")
                    .withCaption("Высокий приоритет")
                    .withIcon("CIRCLE")
                    .withHandler(e -> {
                        openPositionsDl.setParameter("priority", 1);
                        openPositionsDl.load();
                        priorityFilterPopupButton.setCaption("Высокий");
                    }));

            priorityFilterPopupButton.addAction(new BaseAction("priorityNormal")
                    .withCaption("Обычный приоритет")
                    .withIcon("CIRCLE_O")
                    .withHandler(e -> {
                        openPositionsDl.setParameter("priority", 2);
                        openPositionsDl.load();
                        priorityFilterPopupButton.setCaption("Обычный");
                    }));
        }
    }
}
