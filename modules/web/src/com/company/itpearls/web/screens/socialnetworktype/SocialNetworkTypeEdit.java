package com.company.itpearls.web.screens.socialnetworktype;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SocialNetworkType;

@UiController("itpearls_SocialNetworkType.edit")
@UiDescriptor("social-network-type-edit.xml")
@EditedEntityContainer("socialNetworkTypeDc")
@LoadDataBeforeShow
public class SocialNetworkTypeEdit extends StandardEditor<SocialNetworkType> {
}