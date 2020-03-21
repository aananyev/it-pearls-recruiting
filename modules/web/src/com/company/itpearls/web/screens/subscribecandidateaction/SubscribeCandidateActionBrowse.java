package com.company.itpearls.web.screens.subscribecandidateaction;

import com.company.itpearls.service.GetRoleService;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SubscribeCandidateAction;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;

@UiController("itpearls_SubscribeCandidateAction.browse")
@UiDescriptor("subscribe-candidate-action-browse.xml")
@LookupComponent("subscribeCandidateActionsTable")
@LoadDataBeforeShow
public class SubscribeCandidateActionBrowse extends StandardLookup<SubscribeCandidateAction> {
    @Inject
    private GetRoleService getRoleService;
    @Inject
    private UserSession userSession;
    @Inject
    private CollectionLoader<SubscribeCandidateAction> subscribeCandidateActionsDl;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
       if( !getRoleService.isUserRoles( userSession.getUser(), "Manager" ) ) {
           subscribeCandidateActionsDl.setParameter("subscriber", userSession.getUser() );
       } else {
           subscribeCandidateActionsDl.removeParameter( "subscriber" );
       }

       subscribeCandidateActionsDl.load();
    }
}