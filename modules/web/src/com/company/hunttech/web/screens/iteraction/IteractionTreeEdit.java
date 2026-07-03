package com.company.hunttech.web.screens.iteraction;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.Iteraction;

@UiController("hunttech_Iteraction_tree.edit")
@UiDescriptor("iteraction-tree-edit.xml")
@EditedEntityContainer("iteractionDc")
@LoadDataBeforeShow
public class IteractionTreeEdit extends StandardEditor<Iteraction> {
}