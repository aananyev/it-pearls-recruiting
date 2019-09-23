package com.company.itpearls.web.screens.iteraction;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Iteraction;

@UiController("itpearls_Iteraction.edit")
@UiDescriptor("iteraction-edit.xml")
@EditedEntityContainer("iteractionDc")
@LoadDataBeforeShow
public class IteractionEdit extends StandardEditor<Iteraction> {
}