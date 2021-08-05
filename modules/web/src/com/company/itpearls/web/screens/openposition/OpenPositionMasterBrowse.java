package com.company.itpearls.web.screens.openposition;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.OpenPosition;

@UiController("itpearls_OpenPositionMaster.browse")
@UiDescriptor("open-position-master-browse.xml")
@LookupComponent("openPositionsTable")
@LoadDataBeforeShow
public class OpenPositionMasterBrowse extends StandardLookup<OpenPosition> {
}