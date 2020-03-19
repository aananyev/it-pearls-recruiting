package com.company.itpearls.web.screens.subscribecandidateaction;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SubscribeCandidateAction;

@UiController("itpearls_SubscribeCandidateAction.browse")
@UiDescriptor("subscribe-candidate-action-browse.xml")
@LookupComponent("subscribeCandidateActionsTable")
@LoadDataBeforeShow
public class SubscribeCandidateActionBrowse extends StandardLookup<SubscribeCandidateAction> {
}