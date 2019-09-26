package com.company.itpearls.web.screens.requiredparameters;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.RequiredParameters;

@UiController("itpearls_RequiredParameters.edit")
@UiDescriptor("required-parameters-edit.xml")
@EditedEntityContainer("requiredParametersDc")
@LoadDataBeforeShow
public class RequiredParametersEdit extends StandardEditor<RequiredParameters> {
}