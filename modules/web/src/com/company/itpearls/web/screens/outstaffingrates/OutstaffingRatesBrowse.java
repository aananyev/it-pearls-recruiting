package com.company.itpearls.web.screens.outstaffingrates;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.OutstaffingRates;

@UiController("itpearls_OutstaffingRates.browse")
@UiDescriptor("outstaffing-rates-browse.xml")
@LookupComponent("outstaffingRatesesTable")
@LoadDataBeforeShow
public class OutstaffingRatesBrowse extends StandardLookup<OutstaffingRates> {
}