package com.company.hunttech.web.screens.ownershup;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.Ownershup;

@UiController("hunttech_Ownershup.browse")
@UiDescriptor("ownershup-browse.xml")
@LookupComponent("ownershupsTable")
@LoadDataBeforeShow
public class OwnershupBrowse extends StandardLookup<Ownershup> {
}