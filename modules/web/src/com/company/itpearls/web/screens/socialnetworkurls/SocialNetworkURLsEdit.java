package com.company.itpearls.web.screens.socialnetworkurls;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SocialNetworkURLs;

@UiController("itpearls_SocialNetworkURLs.edit")
@UiDescriptor("social-network-ur-ls-edit.xml")
@EditedEntityContainer("socialNetworkURLsDc")
@LoadDataBeforeShow
public class SocialNetworkURLsEdit extends StandardEditor<SocialNetworkURLs> {
}