package com.company.itpearls.web.screens.region;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Region;

@UiController("itpearls_Region.browse")
@UiDescriptor("region-browse.xml")
@LookupComponent("regionsTable")
@LoadDataBeforeShow
public class RegionBrowse extends StandardLookup<Region> {
}