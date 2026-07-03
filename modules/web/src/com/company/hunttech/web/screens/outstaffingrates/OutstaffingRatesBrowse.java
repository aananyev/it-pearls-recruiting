package com.company.hunttech.web.screens.outstaffingrates;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.OutstaffingRates;

@UiController("hunttech_OutstaffingRates.browse")
@UiDescriptor("outstaffing-rates-browse.xml")
@LookupComponent("outstaffingRatesesTable")
@LoadDataBeforeShow
public class OutstaffingRatesBrowse extends StandardLookup<OutstaffingRates> {
}