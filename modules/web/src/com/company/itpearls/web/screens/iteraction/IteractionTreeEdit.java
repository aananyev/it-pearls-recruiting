package com.company.itpearls.web.screens.iteraction;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Iteraction;

@UiController("itpearls_Iteraction_tree.edit")
@UiDescriptor("iteraction-tree-edit.xml")
@EditedEntityContainer("iteractionDc")
@LoadDataBeforeShow
public class IteractionTreeEdit extends StandardEditor<Iteraction> {
}