package com.company.hunttech.web.screens.region;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.Region;

@UiController("hunttech_Region.edit")
@UiDescriptor("region-edit.xml")
@EditedEntityContainer("regionDc")
@LoadDataBeforeShow
public class RegionEdit extends StandardEditor<Region> {
}