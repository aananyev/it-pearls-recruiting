package com.company.itpearls.web.screens.position;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Position;

@UiController("itpearls_Position.browse")
@UiDescriptor("position-browse.xml")
@LookupComponent("positionsTable")
@LoadDataBeforeShow
public class PositionBrowse extends StandardLookup<Position> {
}