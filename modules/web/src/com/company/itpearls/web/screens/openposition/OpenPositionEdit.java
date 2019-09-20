package com.company.itpearls.web.screens.openposition;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.OpenPosition;

@UiController("itpearls_OpenPosition.edit")
@UiDescriptor("open-position-edit.xml")
@EditedEntityContainer("openPositionDc")
@LoadDataBeforeShow
public class OpenPositionEdit extends StandardEditor<OpenPosition> {
}