package com.company.itpearls.web.screens.region;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Region;

@UiController("itpearls_Region.edit")
@UiDescriptor("region-edit.xml")
@EditedEntityContainer("regionDc")
@LoadDataBeforeShow
public class RegionEdit extends StandardEditor<Region> {
}