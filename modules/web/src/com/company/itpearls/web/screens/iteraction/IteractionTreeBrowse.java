package com.company.itpearls.web.screens.iteraction;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Iteraction;

@UiController("itpearls_Iteraction._tree.browse")
@UiDescriptor("iteraction-tree-browse.xml")
@LookupComponent("iteractionsTable")
@LoadDataBeforeShow
public class IteractionTreeBrowse extends StandardLookup<Iteraction> {
}