package com.company.itpearls.web.screens.mainlist;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.MainList;

@UiController("itpearls_MainList.browse")
@UiDescriptor("main-list-browse.xml")
@LookupComponent("mainListsTable")
@LoadDataBeforeShow
public class MainListBrowse extends StandardLookup<MainList> {
}