package com.company.hunttech.web.screens.region;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.Region;

@UiController("hunttech_Region.browse")
@UiDescriptor("region-browse.xml")
@LookupComponent("regionsTable")
@LoadDataBeforeShow
public class RegionBrowse extends StandardLookup<Region> {
}