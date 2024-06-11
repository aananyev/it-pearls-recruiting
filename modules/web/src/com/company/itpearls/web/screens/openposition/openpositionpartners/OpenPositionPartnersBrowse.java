package com.company.itpearls.web.screens.openposition.openpositionpartners;

import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.company.itpearls.web.screens.openposition.OpenPositionBrowse;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;

@UiController("itpearls_OpenPositionPartners.browse")
@UiDescriptor("open-position-partners-browse.xml")
public class OpenPositionPartnersBrowse extends OpenPositionBrowse {
    @Inject
    private CollectionLoader<OpenPosition> openPositionsDl;
    @Inject
    private UserSession userSession;

    @Subscribe
    public void onAfterShow3(AfterShowEvent event) {
        openPositionsDl.setParameter("partnersPerson", userSession.getUser());
        openPositionsDl.load();
    }
}