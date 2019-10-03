package com.company.itpearls.web.screens.ownershup;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Ownershup;

@UiController("itpearls_Ownershup.browse")
@UiDescriptor("ownershup-browse.xml")
@LookupComponent("ownershupsTable")
@LoadDataBeforeShow
public class OwnershupBrowse extends StandardLookup<Ownershup> {
}