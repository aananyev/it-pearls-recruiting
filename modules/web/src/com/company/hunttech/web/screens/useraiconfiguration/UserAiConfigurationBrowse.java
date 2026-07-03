package com.company.hunttech.web.screens.useraiconfiguration;

import com.company.hunttech.entity.UserAiConfiguration;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

@UiController("hunttech_UserAiConfiguration.browse")
@UiDescriptor("user-ai-configuration-browse.xml")
@LoadDataBeforeShow
public class UserAiConfigurationBrowse extends StandardLookup<UserAiConfiguration> {
}
