package com.company.hunttech.web.screens.iteractionlist;

import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.HBoxLayout;
import com.haulmont.cuba.gui.screen.Screen.AfterShowEvent;
import com.haulmont.cuba.gui.screen.EditedEntityContainer;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

/**
 * Presentation-расширение основного экрана взаимодействия.
 *
 * Базовый {@link IteractionListEdit} сохраняет всю бизнес-логику: получает
 * ранжированные типы через InteractionService и назначает точный Iteraction.
 * Расширение только дополняет отсутствующие визуальные позиции до пяти.
 */
@UiController("hunttech_IteractionList.edit")
@UiDescriptor("iteraction-list-edit.xml")
@EditedEntityContainer("iteractionListDc")
@LoadDataBeforeShow
public class IteractionListEditAccordionNavigation extends IteractionListEdit {

    private static final int POPULAR_INTERACTION_BUTTONS = 5;

    @Inject
    private HBoxLayout mostPopularHbox;
    @Inject
    private UiComponents uiComponents;

    /**
     * Выполняется после базового BeforeShow, когда реальные кнопки уже созданы.
     * Пустые позиции не имеют listener и не могут изменить редактируемую сущность.
     */
    @Subscribe
    protected void onAfterShowEnsureFivePopularButtons(AfterShowEvent event) {
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
