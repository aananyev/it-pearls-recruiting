package com.company.itpearls.web.screens.position;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Position;

@UiController("itpearls_Position.edit")
@UiDescriptor("position-edit.xml")
@EditedEntityContainer("positionDc")
@LoadDataBeforeShow
public class PositionEdit extends StandardEditor<Position> {
}