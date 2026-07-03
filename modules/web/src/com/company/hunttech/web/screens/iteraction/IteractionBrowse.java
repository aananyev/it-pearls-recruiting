package com.company.hunttech.web.screens.iteraction;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.Iteraction;

@UiController("hunttech_Iteraction.browse")
@UiDescriptor("iteraction-browse.xml")
@LookupComponent("iteractionsTable")
@LoadDataBeforeShow
public class IteractionBrowse extends StandardLookup<Iteraction> {
}