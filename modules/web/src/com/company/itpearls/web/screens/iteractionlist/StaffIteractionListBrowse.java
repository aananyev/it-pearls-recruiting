package com.company.itpearls.web.screens.iteractionlist;

import com.company.itpearls.entity.IteractionList;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("staff-itpearls_IteractionList.browse")
@UiDescriptor("staff-iteraction-list-browse.xml")
public class StaffIteractionListBrowse extends IteractionListBrowse {
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private CollectionContainer<IteractionList> iteractionListsDc;
    @Inject
    private DataGrid<IteractionList> iteractionListsTable;

    public void staffCreateBtn() {
        screenBuilders.editor(IteractionList.class, this)
                .withScreenClass(StaffIteractionListEdit.class)
                .withAfterCloseListener(e -> iteractionListsTable.repaint())
                .newEntity()
                .withOpenMode(OpenMode.THIS_TAB)
                .build()
                .show();
    }
}