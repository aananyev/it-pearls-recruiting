package com.company.hunttech.web.screens.socialnetworkurls;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.SocialNetworkURLs;

@UiController("hunttech_SocialNetworkURLs.browse")
@UiDescriptor("social-network-ur-ls-browse.xml")
@LookupComponent("socialNetworkURLsesTable")
@LoadDataBeforeShow
public class SocialNetworkURLsBrowse extends StandardLookup<SocialNetworkURLs> {
}