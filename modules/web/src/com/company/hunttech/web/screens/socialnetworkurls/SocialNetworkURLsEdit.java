package com.company.hunttech.web.screens.socialnetworkurls;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.SocialNetworkURLs;

@UiController("hunttech_SocialNetworkURLs.edit")
@UiDescriptor("social-network-ur-ls-edit.xml")
@EditedEntityContainer("socialNetworkURLsDc")
@LoadDataBeforeShow
public class SocialNetworkURLsEdit extends StandardEditor<SocialNetworkURLs> {
}