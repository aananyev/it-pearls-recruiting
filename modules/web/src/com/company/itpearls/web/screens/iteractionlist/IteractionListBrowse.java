package com.company.itpearls.web.screens.iteractionlist;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.IteractionList;

@UiController("itpearls_IteractionList.browse")
@UiDescriptor("iteraction-list-browse.xml")
@LookupComponent("iteractionListsTable")
@LoadDataBeforeShow
public class IteractionListBrowse extends StandardLookup<IteractionList> {
}