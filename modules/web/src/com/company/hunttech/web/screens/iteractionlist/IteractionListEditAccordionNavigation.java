package com.company.hunttech.web.screens.iteractionlist;

import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.EditedEntityContainer;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.MessageBundle;
import com.haulmont.cuba.gui.screen.Screen.AfterShowEvent;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.hunttech.hrm.gui.components.OvaFallbackImage;

import javax.inject.Inject;

/**
 * Presentation-расширение основного экрана взаимодействия.
 *
 * Базовый {@link IteractionListEdit} сохраняет бизнес-логику загрузки,
 * валидации и сохранения. Расширение применяет общий визуальный контракт
 * Edit-экранов, строит доступную label-навигацию и дополняет блок частых
 * действий до пяти визуальных позиций.
 */
@UiController("hunttech_IteractionList.edit")
@UiDescriptor("iteraction-list-edit.xml")
@EditedEntityContainer("iteractionListDc")
@LoadDataBeforeShow
public class IteractionListEditAccordionNavigation extends IteractionListEdit {

    private static final int POPULAR_INTERACTION_BUTTONS = 5;
    private static final String NAVIGATION_STYLE = "borderless label-nav-item";
    private static final String ACTIVE_NAVIGATION_STYLE = "label-nav-item-active";

    @Inject
    private HBoxLayout iteractionListMainLayout;
    @Inject
    private VBoxLayout iteractionListNavigation;
    @Inject
    private GroupBoxLayout participantsAccordion;
    @Inject
    private GroupBoxLayout interactionAccordion;
    @Inject
    private GroupBoxLayout resultAccordion;
    @Inject
    private GroupBoxLayout commentAccordion;
    @Inject
    private GroupBoxLayout popularAccordion;
    @Inject
    private SuggestionPickerField candidateField;
    @Inject
    private LookupPickerField iteractionTypeField;
    @Inject
    private LookupField ratingField;
    @Inject
    private TextArea commentField;
    @Inject
    private HBoxLayout mostPopularHbox;
    @Inject
    private OvaFallbackImage projectLogoImage;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private MessageBundle messageBundle;

    private Button participantsNavigationButton;
    private Button interactionNavigationButton;
    private Button resultNavigationButton;
    private Button commentNavigationButton;
    private Button popularNavigationButton;

    // Флаг предотвращает рекурсивные события GroupBox при синхронизации
    // пользовательского клика и штатного ExpandedStateChangeListener CUBA.
    private boolean updatingSharedNavigation;
    private boolean sharedNavigationInitialized;

    /**
     * Запускается после базового BeforeShow: реальные быстрые действия уже
     * созданы, а XML-компоненты доступны для безопасной presentation-адаптации.
     */
    @Subscribe
    protected void onAfterShowApplyEditScreenContract(AfterShowEvent event) {
        applySharedStyles(iteractionListMainLayout);
        restoreProjectLogoRole();
        rebuildSharedNavigation();
        ensureFivePopularButtons();
    }

    /**
     * Добавляет общие semantic stylename поверх локального namespace экрана.
     * Component ID, bindings, loaders и значения сущности не изменяются.
     */
    private void applySharedStyles(Component component) {
        addSharedStyle(component, "iteraction-list-main-layout", "edit-screen-layout");
        addSharedStyle(component, "iteraction-list-sidebar", "edit-sidebar");
        addSharedStyle(component, "iteraction-list-identity-images", "edit-sidebar-visual");
        addSharedStyle(component, "iteraction-list-profile-header", "edit-sidebar-identity");
        addSharedStyle(component, "iteraction-list-profile-title", "edit-sidebar-title");
        addSharedStyle(component, "iteraction-list-profile-subtitle", "edit-sidebar-subtitle");
        addSharedStyle(component, "iteraction-list-sidebar-card", "edit-sidebar-summary");
        addSharedStyle(component, "iteraction-list-sidebar-warning", "edit-sidebar-warning");
        addSharedStyle(component, "iteraction-list-sidebar-spacer", "edit-sidebar-spacer");
        addSharedStyle(component, "iteraction-list-workspace", "edit-workspace");
        addSharedStyle(component, "iteraction-list-toolbar", "edit-toolbar");
        addSharedStyle(component, "iteraction-list-toolbar-title", "edit-toolbar-title");
        addSharedStyle(component, "iteraction-list-toolbar-context", "edit-toolbar-description");
        addSharedStyle(component, "iteraction-list-quick-actions", "edit-card");
        addSharedStyle(component, "iteraction-list-quick-actions-title", "edit-card-title");
        addSharedStyle(component, "iteraction-list-tabs", "edit-tabs");
        addSharedStyle(component, "iteraction-list-scroll", "edit-workspace-scroll");
        addSharedStyle(component, "iteraction-list-content", "edit-workspace-content");
        addSharedStyle(component, "iteraction-list-accordion-section", "edit-accordion-section");
        addSharedStyle(component, "iteraction-list-footer", "edit-footer-actions");
        addSharedStyle(component, "iteraction-list-footer-actions", "edit-toolbar-actions");

        if (component instanceof ComponentContainer) {
            for (Component child : ((ComponentContainer) component).getOwnComponents()) {
                applySharedStyles(child);
            }
        }
    }

    private void addSharedStyle(Component component, String localStyle, String sharedStyle) {
        String styleName = component.getStyleName();
        if (styleName != null && containsStyle(styleName, localStyle)) {
            component.addStyleName(sharedStyle);
        }
    }

    private boolean containsStyle(String styleName, String expectedStyle) {
        for (String token : styleName.split("\\s+")) {
            if (expectedStyle.equals(token)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Возвращает проектному логотипу отдельную визуальную роль: он остаётся
     * информативным, но не конкурирует с основной фотографией кандидата.
     */
    private void restoreProjectLogoRole() {
        projectLogoImage.removeStyleName("iteraction-list-candidate-image");
        projectLogoImage.addStyleName("iteraction-list-project-image");
        projectLogoImage.setWidth("80px");
        projectLogoImage.setHeight("80px");
        projectLogoImage.setOvalWidth("80px");
        projectLogoImage.setOvalHeight("80px");
    }

    /**
     * Заменяет legacy runtime-кнопки базового контроллера общими label-кнопками.
     * Навигация управляет только раскрытием, active-state и keyboard focus.
     */
    private void rebuildSharedNavigation() {
        if (sharedNavigationInitialized) {
            return;
        }

        Component navigationTitle = iteractionListNavigation.getComponent(0);
        iteractionListNavigation.removeAll();
        navigationTitle.removeStyleName("iteraction-list-navigation-title");
        navigationTitle.addStyleName("label-nav-title");
        iteractionListNavigation.add(navigationTitle);

        participantsNavigationButton = createSharedNavigationButton(
                "participantsAccordionNav", "msgAccordionParticipants",
                () -> selectSharedAccordion(participantsAccordion,
                        participantsNavigationButton, candidateField::focus));
        interactionNavigationButton = createSharedNavigationButton(
                "interactionAccordionNav", "msgAccordionInteraction",
                () -> selectSharedAccordion(interactionAccordion,
                        interactionNavigationButton, iteractionTypeField::focus));
        resultNavigationButton = createSharedNavigationButton(
                "resultAccordionNav", "msgAccordionResult",
                () -> selectSharedAccordion(resultAccordion,
                        resultNavigationButton, ratingField::focus));
        commentNavigationButton = createSharedNavigationButton(
                "commentAccordionNav", "msgAccordionComment",
                () -> selectSharedAccordion(commentAccordion,
                        commentNavigationButton, commentField::focus));
        popularNavigationButton = createSharedNavigationButton(
                "popularAccordionNav", "mshMostPopular",
                () -> selectSharedAccordion(popularAccordion,
                        popularNavigationButton, this::focusFirstPopularButton));

        iteractionListNavigation.add(participantsNavigationButton);
        iteractionListNavigation.add(interactionNavigationButton);
        iteractionListNavigation.add(resultNavigationButton);
        iteractionListNavigation.add(commentNavigationButton);
        iteractionListNavigation.add(popularNavigationButton);

        attachSharedAccordionListeners();
        updateSharedNavigationStyles(participantsNavigationButton);
        sharedNavigationInitialized = true;
    }

    private Button createSharedNavigationButton(String id, String messageKey, Runnable handler) {
        Button button = uiComponents.create(Button.class);
        button.setId(id);
        button.setCaption(messageBundle.getMessage(messageKey));
        button.setWidth("100%");
        button.setStyleName(NAVIGATION_STYLE);
        button.addClickListener(clickEvent -> handler.run());
        return button;
    }

    private void attachSharedAccordionListeners() {
        participantsAccordion.addExpandedStateChangeListener(event ->
                synchronizeSharedNavigation(participantsAccordion, participantsNavigationButton));
        interactionAccordion.addExpandedStateChangeListener(event ->
                synchronizeSharedNavigation(interactionAccordion, interactionNavigationButton));
        resultAccordion.addExpandedStateChangeListener(event ->
                synchronizeSharedNavigation(resultAccordion, resultNavigationButton));
        commentAccordion.addExpandedStateChangeListener(event ->
                synchronizeSharedNavigation(commentAccordion, commentNavigationButton));
        popularAccordion.addExpandedStateChangeListener(event ->
                synchronizeSharedNavigation(popularAccordion, popularNavigationButton));
    }

    private void synchronizeSharedNavigation(GroupBoxLayout accordion, Button navigationButton) {
        if (!updatingSharedNavigation && accordion.isExpanded()) {
            updateSharedNavigationStyles(navigationButton);
        }
    }

    private void selectSharedAccordion(GroupBoxLayout selectedAccordion,
                                       Button selectedNavigationButton,
                                       Runnable focusHandler) {
        updatingSharedNavigation = true;
        try {
            participantsAccordion.setExpanded(participantsAccordion == selectedAccordion);
            interactionAccordion.setExpanded(interactionAccordion == selectedAccordion);
            resultAccordion.setExpanded(resultAccordion == selectedAccordion);
            commentAccordion.setExpanded(commentAccordion == selectedAccordion);
            popularAccordion.setExpanded(popularAccordion == selectedAccordion);
            updateSharedNavigationStyles(selectedNavigationButton);
        } finally {
            updatingSharedNavigation = false;
        }
        focusHandler.run();
    }

    /**
     * Базовый `label-nav-item` сохраняется постоянно; меняется только
     * `label-nav-item-active`, как требует общий контракт Edit-экранов.
     */
    private void updateSharedNavigationStyles(Button selectedNavigationButton) {
        setNavigationActive(participantsNavigationButton,
                participantsNavigationButton == selectedNavigationButton);
        setNavigationActive(interactionNavigationButton,
                interactionNavigationButton == selectedNavigationButton);
        setNavigationActive(resultNavigationButton,
                resultNavigationButton == selectedNavigationButton);
        setNavigationActive(commentNavigationButton,
                commentNavigationButton == selectedNavigationButton);
        setNavigationActive(popularNavigationButton,
                popularNavigationButton == selectedNavigationButton);
    }

    private void setNavigationActive(Button button, boolean active) {
        button.addStyleName("label-nav-item");
        if (active) {
            button.addStyleName(ACTIVE_NAVIGATION_STYLE);
        } else {
            button.removeStyleName(ACTIVE_NAVIGATION_STYLE);
        }
    }

    private void focusFirstPopularButton() {
        for (Component component : mostPopularHbox.getOwnComponents()) {
            if (component instanceof Button && component.isEnabled()) {
                ((Button) component).focus();
                return;
            }
        }
    }

    /**
     * Выполняется после базового BeforeShow, когда реальные кнопки уже созданы.
     * Пустые позиции не имеют listener и не могут изменить редактируемую сущность.
     */
    private void ensureFivePopularButtons() {
        int currentButtonCount = mostPopularHbox.getOwnComponents().size();

        while (currentButtonCount < POPULAR_INTERACTION_BUTTONS) {
            Button emptyButton = uiComponents.create(Button.class);
            emptyButton.setWidth("100%");
            emptyButton.setStyleName("iteraction-list-popular-button");
            emptyButton.setCaption("Нет данных");
            emptyButton.setEnabled(false);

            mostPopularHbox.add(emptyButton);
            mostPopularHbox.expand(emptyButton);
            currentButtonCount++;
        }
    }
}
