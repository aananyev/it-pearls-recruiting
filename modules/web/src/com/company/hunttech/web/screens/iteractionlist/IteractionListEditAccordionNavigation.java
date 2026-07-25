package com.company.hunttech.web.screens.iteractionlist;

import com.haulmont.cuba.gui.screen.EditedEntityContainer;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

/**
 * Совместимый alias для временного screen ID аккордеонной версии.
 * Основной legacy ID и вся presentation-логика находятся в IteractionListEdit.
 */
@UiController("hunttech_IteractionList.edit.accordion")
@UiDescriptor("iteraction-list-edit.xml")
@EditedEntityContainer("iteractionListDc")
@LoadDataBeforeShow
public class IteractionListEditAccordionNavigation extends IteractionListEdit {
}
