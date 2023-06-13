package com.company.itpearls.web.screens.outstaffingrates;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.OutstaffingRates;

@UiController("itpearls_OutstaffingRates.edit")
@UiDescriptor("outstaffing-rates-edit.xml")
@EditedEntityContainer("outstaffingRatesDc")
@LoadDataBeforeShow
public class OutstaffingRatesEdit extends StandardEditor<OutstaffingRates> {
}