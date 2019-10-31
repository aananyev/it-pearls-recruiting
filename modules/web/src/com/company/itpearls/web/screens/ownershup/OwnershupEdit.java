package com.company.itpearls.web.screens.ownershup;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Ownershup;

@UiController("itpearls_Ownershup.edit")
@UiDescriptor("ownershup-edit.xml")
@EditedEntityContainer("ownershupDc")
@LoadDataBeforeShow
public class OwnershupEdit extends StandardEditor<Ownershup> {
}