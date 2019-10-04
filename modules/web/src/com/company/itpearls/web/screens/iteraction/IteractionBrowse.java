package com.company.itpearls.web.screens.iteraction;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Iteraction;

import java.awt.*;

@UiController("itpearls_Iteraction.browse")
@UiDescriptor("iteraction-browse.xml")
@LookupComponent("iteractionsTable")
@LoadDataBeforeShow
public class IteractionBrowse extends StandardLookup<Iteraction> {
    public Component generateNumberCell(Iteraction entity) {
        return null;
    }
}