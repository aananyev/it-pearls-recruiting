package com.company.itpearls.web.screens.mainlist;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.MainList;

@UiController("itpearls_MainList.edit")
@UiDescriptor("main-list-edit.xml")
@EditedEntityContainer("mainListDc")
@LoadDataBeforeShow
public class MainListEdit extends StandardEditor<MainList> {
}