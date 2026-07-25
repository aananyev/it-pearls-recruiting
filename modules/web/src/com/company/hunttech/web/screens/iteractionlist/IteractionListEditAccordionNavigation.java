package com.company.hunttech.web.screens.iteractionlist;

import com.company.hunttech.entity.Iteraction;
import com.company.hunttech.entity.JobCandidate;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.GroupBoxLayout;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.components.SuggestionPickerField;
import com.haulmont.cuba.gui.components.TextArea;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.screen.EditedEntityContainer;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.MessageBundle;
import com.haulmont.cuba.gui.screen.Screen.InitEvent;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

/**
 * Добавляет к базовому редактору IteractionList только presentation-навигацию:
 * взаимоисключающие аккордеоны и кликабельный индекс в левой панели.
 *
 * Data containers, loaders, JPQL, lifecycle и бизнес-обработчики остаются
 * в {@link IteractionListEdit} без изменений.
 */
@UiController("hunttech_IteractionList.edit.accordion")
@UiDescriptor("iteraction-list-edit-accordion-navigation.xml")
@EditedEntityContainer("iteractionListDc")
@LoadDataBeforeShow
public class IteractionListEditAccordionNavigation extends IteractionListEdit {

    private static final String NAVIGATION_STYLE = "borderless iteraction-list-nav-item";
    private static final String ACTIVE_NAVIGATION_STYLE =
            "borderless iteraction-list-nav-item iteraction-list-nav-item-active";

    @Inject
    private UiComponents uiComponents;
    @Inject
    private MessageBundle messageBundle;
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
    private SuggestionPickerField<JobCandidate> candidateField;
    @Inject
    private LookupPickerField<Iteraction> iteractionTypeField;
    @Inject
    private LookupField ratingField;
    @Inject
    private TextArea<String> commentField;

    private Button participantsAccordionNav;
    private Button interactionAccordionNav;
    private Button resultAccordionNav;
    private Button commentAccordionNav;
    private Button popularAccordionNav;
    private boolean updatingAccordionState;

    @Subscribe
    protected void onInitAccordionNavigation(InitEvent event) {
        initAccordionNavigation();
        initAccordionHeaderSynchronization();
    }

    /**
     * Сохраняет заголовок существующего XML-индекса и заменяет пять fallback LABEL
     * на визуально идентичные borderless-кнопки CUBA по паттерну SettingsWindow.
     */
    private void initAccordionNavigation() {
        Component navigationTitle = iteractionListNavigation.getComponent(0);
        iteractionListNavigation.removeAll();
        iteractionListNavigation.add(navigationTitle);

        participantsAccordionNav = createNavigationButton(
                "participantsAccordionNav", "msgAccordionParticipants",
                () -> selectAccordion(participantsAccordion, participantsAccordionNav, candidateField::focus));
        interactionAccordionNav = createNavigationButton(
                "interactionAccordionNav", "msgAccordionInteraction",
                () -> selectAccordion(interactionAccordion, interactionAccordionNav, iteractionTypeField::focus));
        resultAccordionNav = createNavigationButton(
                "resultAccordionNav", "msgAccordionResult",
                () -> selectAccordion(resultAccordion, resultAccordionNav, ratingField::focus));
        commentAccordionNav = createNavigationButton(
                "commentAccordionNav", "msgAccordionComment",
                () -> selectAccordion(commentAccordion, commentAccordionNav, commentField::focus));
        popularAccordionNav = createNavigationButton(
                "popularAccordionNav", "mshMostPopular",
                () -> selectAccordion(popularAccordion, popularAccordionNav, () -> { }));

        iteractionListNavigation.add(participantsAccordionNav);
        iteractionListNavigation.add(interactionAccordionNav);
        iteractionListNavigation.add(resultAccordionNav);
        iteractionListNavigation.add(commentAccordionNav);
        iteractionListNavigation.add(popularAccordionNav);

        selectAccordion(participantsAccordion, participantsAccordionNav, () -> { });
    }

    /**
     * Синхронизирует клики по штатным заголовкам GroupBox с индексом слева.
     * При раскрытии секции заголовком она становится единственной открытой секцией.
     */
    private void initAccordionHeaderSynchronization() {
        participantsAccordion.addExpandedStateChangeListener(event ->
                synchronizeExpandedAccordion(participantsAccordion, participantsAccordionNav));
        interactionAccordion.addExpandedStateChangeListener(event ->
                synchronizeExpandedAccordion(interactionAccordion, interactionAccordionNav));
        resultAccordion.addExpandedStateChangeListener(event ->
                synchronizeExpandedAccordion(resultAccordion, resultAccordionNav));
        commentAccordion.addExpandedStateChangeListener(event ->
                synchronizeExpandedAccordion(commentAccordion, commentAccordionNav));
        popularAccordion.addExpandedStateChangeListener(event ->
                synchronizeExpandedAccordion(popularAccordion, popularAccordionNav));
    }

    private Button createNavigationButton(String id, String messageKey, Runnable handler) {
        Button button = uiComponents.create(Button.class);
        button.setId(id);
        button.setCaption(messageBundle.getMessage(messageKey));
        button.setWidth("100%");
        button.setStyleName(NAVIGATION_STYLE);
        button.addClickListener(clickEvent -> handler.run());
        return button;
    }

    private void synchronizeExpandedAccordion(GroupBoxLayout accordion, Button navigationButton) {
        if (!updatingAccordionState && accordion.isExpanded()) {
            selectAccordion(accordion, navigationButton, () -> { });
        }
    }

    /**
     * Раскрывает ровно один раздел, синхронизирует активный пункт и переводит
     * фокус в первое штатное поле. Значения entity при этом не изменяются.
     */
    private void selectAccordion(GroupBoxLayout selectedAccordion,
                                 Button selectedNavigationButton,
                                 Runnable focusHandler) {
        updatingAccordionState = true;
        try {
            participantsAccordion.setExpanded(participantsAccordion == selectedAccordion);
            interactionAccordion.setExpanded(interactionAccordion == selectedAccordion);
            resultAccordion.setExpanded(resultAccordion == selectedAccordion);
            commentAccordion.setExpanded(commentAccordion == selectedAccordion);
            popularAccordion.setExpanded(popularAccordion == selectedAccordion);
            updateNavigationStyles(selectedNavigationButton);
        } finally {
            updatingAccordionState = false;
        }
        focusHandler.run();
    }

    private void updateNavigationStyles(Button selectedNavigationButton) {
        participantsAccordionNav.setStyleName(
                participantsAccordionNav == selectedNavigationButton ? ACTIVE_NAVIGATION_STYLE : NAVIGATION_STYLE);
        interactionAccordionNav.setStyleName(
                interactionAccordionNav == selectedNavigationButton ? ACTIVE_NAVIGATION_STYLE : NAVIGATION_STYLE);
        resultAccordionNav.setStyleName(
                resultAccordionNav == selectedNavigationButton ? ACTIVE_NAVIGATION_STYLE : NAVIGATION_STYLE);
        commentAccordionNav.setStyleName(
                commentAccordionNav == selectedNavigationButton ? ACTIVE_NAVIGATION_STYLE : NAVIGATION_STYLE);
        popularAccordionNav.setStyleName(
                popularAccordionNav == selectedNavigationButton ? ACTIVE_NAVIGATION_STYLE : NAVIGATION_STYLE);
    }
}
