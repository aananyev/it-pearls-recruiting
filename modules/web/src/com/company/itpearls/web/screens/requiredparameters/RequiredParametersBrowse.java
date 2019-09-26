package com.company.itpearls.web.screens.requiredparameters;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.RequiredParameters;

@UiController("itpearls_RequiredParameters.browse")
@UiDescriptor("required-parameters-browse.xml")
@LookupComponent("requiredParametersesTable")
@LoadDataBeforeShow
public class RequiredParametersBrowse extends StandardLookup<RequiredParameters> {
}