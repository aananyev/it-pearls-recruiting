package com.company.itpearls.web.screens.socialnetworktype;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SocialNetworkType;

@UiController("itpearls_SocialNetworkType.browse")
@UiDescriptor("social-network-type-browse.xml")
@LookupComponent("socialNetworkTypesTable")
@LoadDataBeforeShow
public class SocialNetworkTypeBrowse extends StandardLookup<SocialNetworkType> {
}