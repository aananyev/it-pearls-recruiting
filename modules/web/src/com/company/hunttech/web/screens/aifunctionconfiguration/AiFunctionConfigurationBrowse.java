package com.company.hunttech.web.screens.aifunctionconfiguration;

import com.company.hunttech.entity.ai.AiFunctionConfiguration;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

@UiController("hunttech_AiFunctionConfiguration.browse")
@UiDescriptor("ai-function-configuration-browse.xml")
@LookupComponent("aiFunctionsTable")
@LoadDataBeforeShow
public class AiFunctionConfigurationBrowse extends StandardLookup<AiFunctionConfiguration> {
}
