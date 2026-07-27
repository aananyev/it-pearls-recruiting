package com.company.hunttech.web.screens.iteractionlist;

import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.GroupBoxLayout;
import com.haulmont.cuba.gui.components.HBoxLayout;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.components.SuggestionPickerField;
import com.haulmont.cuba.gui.components.TextArea;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.screen.EditedEntityContainer;
import com.haulmont.cuba.gui.screen.MessageBundle;
import com.haulmont.cuba.gui.screen.Screen.AfterShowEvent;
import com.haulmont.cuba.gui.screen.Screen.InitEvent;
import com.haulmont.cuba.gui.screen.Subscribe;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Presentation-контроллер заново спроектированного IteractionListEdit.
 *
 * Базовый {@link IteractionListEdit} остаётся единственным владельцем загрузки,
 * валидации, динамических полей, подписок, сохранения и исторического сервиса
 * быстрых взаимодействий. Этот класс управляет только label-навигацией,
 * состояниями аккордеонов и пятью стабильными визуальными позициями.
 */
public class IteractionListEditAccordionNavigation extends IteractionListEdit {

    private static final int POPULAR_INTERACTION_BUTTONS = 5;
    private static final String NAVIGATION_STYLE = "borderless label-nav-item";
    private static final String ACTIVE_NAVIGATION_STYLE = "label-nav-item-active";
    private static final String POPULAR_BUTTON_STYLE = "iteraction-list-popular-button";
    private static final String EMPTY_POPULAR_CAPTION = "Нет данных";

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
    private UiComponents uiComponents;
    @Inject
    private MessageBundle messageBundle;

    private Button participantsNavigationButton;
    private Button interactionNavigationButton;
    private Button resultNavigationButton;
    private Button commentNavigationButton;

    // Флаг исключает рекурсивные ExpandedStateChangeEvent при синхронизации
    // пользовательского клика, ручного раскрытия GroupBox и active-state.
    private boolean updatingAccordionState;
    private boolean navigationInitialized;

    /**
     * Отключает legacy presentation-инициализацию базового контроллера.
     * Бизнес lifecycle не затрагивается: переопределён только обработчик,
     * который ранее создавал пять навигационных пунктов, включая быстрые кнопки.
     */
    @Override
    @Subscribe
    protected void onInitIteractionNavigation(InitEvent event) {
        // Новая label-навигация строится после создания XML-компонентов.
    }

    /**
     * Выполняется после базового BeforeShow: сервис уже создал кнопки для точных
     * Iteraction, поэтому presentation-слой может безопасно оформить позиции
     * и дополнить отсутствующие disabled-заглушками без бизнес-listener.
     */
    @Subscribe
    protected void onAfterShowInitializePresentation(AfterShowEvent event) {
        initializeNavigation();
        normalizePopularButtons();
    }

    /**
     * Создаёт пункты только для четырёх реальных рабочих аккордеонов.
     * Быстрые действия постоянно видимы над scroll-area и не являются разделом.
     */
    private void initializeNavigation() {
        if (navigationInitialized) {
            return;
        }

        Component navigationTitle = iteractionListNavigation.getComponent(0);
        iteractionListNavigation.removeAll();
        navigationTitle.addStyleName("label-nav-title");
        iteractionListNavigation.add(navigationTitle);

        participantsNavigationButton = createNavigationButton(
                "participantsAccordionNav",
                "msgAccordionParticipants",
                () -> selectAccordion(
                        participantsAccordion,
                        participantsNavigationButton,
                        candidateField::focus));
        interactionNavigationButton = createNavigationButton(
                "interactionAccordionNav",
                "msgAccordionInteraction",
                () -> selectAccordion(
                        interactionAccordion,
                        interactionNavigationButton,
                        iteractionTypeField::focus));
        resultNavigationButton = createNavigationButton(
                "resultAccordionNav",
                "msgAccordionResult",
                () -> selectAccordion(
                        resultAccordion,
                        resultNavigationButton,
                        ratingField::focus));
        commentNavigationButton = createNavigationButton(
                "commentAccordionNav",
                "msgAccordionComment",
                () -> selectAccordion(
                        commentAccordion,
                        commentNavigationButton,
                        commentField::focus));

        iteractionListNavigation.add(participantsNavigationButton);
        iteractionListNavigation.add(interactionNavigationButton);
        iteractionListNavigation.add(resultNavigationButton);
        iteractionListNavigation.add(commentNavigationButton);

        attachAccordionListeners();
        selectAccordion(participantsAccordion, participantsNavigationButton, () -> {
        });
        navigationInitialized = true;
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

    /**
     * Ручное раскрытие заголовком выбирает тот же раздел, сворачивает остальные
     * и синхронизирует active-state. Флаг предотвращает повторный вход listener.
     */
    private void attachAccordionListeners() {
        participantsAccordion.addExpandedStateChangeListener(event ->
                synchronizeExpandedAccordion(participantsAccordion, participantsNavigationButton));
        interactionAccordion.addExpandedStateChangeListener(event ->
                synchronizeExpandedAccordion(interactionAccordion, interactionNavigationButton));
        resultAccordion.addExpandedStateChangeListener(event ->
                synchronizeExpandedAccordion(resultAccordion, resultNavigationButton));
        commentAccordion.addExpandedStateChangeListener(event ->
                synchronizeExpandedAccordion(commentAccordion, commentNavigationButton));
    }

    private void synchronizeExpandedAccordion(GroupBoxLayout accordion, Button navigationButton) {
        if (!updatingAccordionState && accordion.isExpanded()) {
            selectAccordion(accordion, navigationButton, () -> {
            });
        }
    }

    /**
     * Меняет только presentation-state. Метод не пишет entity, не запускает
     * loader, не обращается к сервисам и не выполняет commit.
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
            updateNavigationStyles(selectedNavigationButton);
        } finally {
            updatingAccordionState = false;
        }
        focusHandler.run();
    }

    /**
     * Базовый `label-nav-item` сохраняется постоянно; добавляется или удаляется
     * только состояние `label-nav-item-active`.
     */
    private void updateNavigationStyles(Button selectedNavigationButton) {
        setNavigationActive(
                participantsNavigationButton,
                participantsNavigationButton == selectedNavigationButton);
        setNavigationActive(
                interactionNavigationButton,
                interactionNavigationButton == selectedNavigationButton);
        setNavigationActive(
                resultNavigationButton,
                resultNavigationButton == selectedNavigationButton);
        setNavigationActive(
                commentNavigationButton,
                commentNavigationButton == selectedNavigationButton);
    }

    private void setNavigationActive(Button button, boolean active) {
        button.addStyleName("label-nav-item");
        if (active) {
            button.addStyleName(ACTIVE_NAVIGATION_STYLE);
        } else {
            button.removeStyleName(ACTIVE_NAVIGATION_STYLE);
        }
    }

    /**
     * Сохраняет точные Iteraction и listeners кнопок, созданных базовым
     * контроллером. Недостающие позиции не имеют click listener, disabled
     * и потому не могут изменить field value или DataContext.
     */
    private void normalizePopularButtons() {
        List<Component> currentComponents =
                new ArrayList<>(mostPopularHbox.getOwnComponents());

        int visiblePosition = 0;
        for (Component component : currentComponents) {
            if (!(component instanceof Button)) {
                mostPopularHbox.remove(component);
                continue;
            }
            if (visiblePosition >= POPULAR_INTERACTION_BUTTONS) {
                mostPopularHbox.remove(component);
                continue;
            }

            Button button = (Button) component;
            button.addStyleName(POPULAR_BUTTON_STYLE);
            button.setWidth("100%");
            button.setDescription(button.getCaption());
            mostPopularHbox.expand(button);
            visiblePosition++;
        }

        while (visiblePosition < POPULAR_INTERACTION_BUTTONS) {
            Button emptyButton = uiComponents.create(Button.class);
            emptyButton.setWidth("100%");
            emptyButton.setStyleName(POPULAR_BUTTON_STYLE);
            emptyButton.setCaption(EMPTY_POPULAR_CAPTION);
            emptyButton.setDescription(EMPTY_POPULAR_CAPTION);
            emptyButton.setEnabled(false);

            mostPopularHbox.add(emptyButton);
            mostPopularHbox.expand(emptyButton);
            visiblePosition++;
        }
    }
}
