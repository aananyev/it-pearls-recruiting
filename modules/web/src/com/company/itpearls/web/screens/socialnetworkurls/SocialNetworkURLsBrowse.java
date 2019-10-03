package com.company.itpearls.web.screens.socialnetworkurls;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SocialNetworkURLs;

@UiController("itpearls_SocialNetworkURLs.browse")
@UiDescriptor("social-network-ur-ls-browse.xml")
@LookupComponent("socialNetworkURLsesTable")
@LoadDataBeforeShow
public class SocialNetworkURLsBrowse extends StandardLookup<SocialNetworkURLs> {
}