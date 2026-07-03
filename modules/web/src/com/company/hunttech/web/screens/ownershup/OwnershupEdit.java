package com.company.hunttech.web.screens.ownershup;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.Ownershup;

@UiController("hunttech_Ownershup.edit")
@UiDescriptor("ownershup-edit.xml")
@EditedEntityContainer("ownershupDc")
@LoadDataBeforeShow
public class OwnershupEdit extends StandardEditor<Ownershup> {
}