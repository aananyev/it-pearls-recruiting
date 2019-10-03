package com.company.itpearls.web.screens.iteractionlist;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.IteractionList;

@UiController("itpearls_IteractionList.edit")
@UiDescriptor("iteraction-list-edit.xml")
@EditedEntityContainer("iteractionListDc")
@LoadDataBeforeShow
public class IteractionListEdit extends StandardEditor<IteractionList> {
}