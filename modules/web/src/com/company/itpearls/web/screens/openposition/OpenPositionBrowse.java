package com.company.itpearls.web.screens.openposition;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.OpenPosition;

@UiController("itpearls_OpenPosition.browse")
@UiDescriptor("open-position-browse.xml")
@LookupComponent("openPositionsTable")
@LoadDataBeforeShow
public class OpenPositionBrowse extends StandardLookup<OpenPosition> {
}