package com.company.itpearls.web.screens.partnerssubscribeopenposition;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.PartnersSubscribeOpenPosition;

@UiController("itpearls_PartnersSubscribeOpenPosition.browse")
@UiDescriptor("partners-subscribe-open-position-browse.xml")
@LookupComponent("partnersSubscribeOpenPositionsTable")
@LoadDataBeforeShow
public class PartnersSubscribeOpenPositionBrowse extends StandardLookup<PartnersSubscribeOpenPosition> {
}