package com.company.hunttech.web.screens.outstaffingrates;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.OutstaffingRates;

@UiController("hunttech_OutstaffingRates.edit")
@UiDescriptor("outstaffing-rates-edit.xml")
@EditedEntityContainer("outstaffingRatesDc")
@LoadDataBeforeShow
public class OutstaffingRatesEdit extends StandardEditor<OutstaffingRates> {
}