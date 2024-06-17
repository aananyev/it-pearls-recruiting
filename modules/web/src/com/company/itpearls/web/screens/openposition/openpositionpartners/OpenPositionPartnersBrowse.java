package com.company.itpearls.web.screens.openposition.openpositionpartners;

import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.company.itpearls.web.screens.openposition.OpenPositionBrowse;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.Date;

@UiController("itpearls_OpenPositionPartners.browse")
@UiDescriptor("open-position-partners-browse.xml")
public class OpenPositionPartnersBrowse extends OpenPositionBrowse {
    @Inject
    private CollectionLoader<OpenPosition> openPositionsDl;
    @Inject
    private UserSession userSession;
    @Inject
    private Button createBtn;
    @Inject
    private Button removeBtn;

    @Subscribe
    public void onAfterShow3(AfterShowEvent event) {
        setOpenPositionDl();

        createBtn.setVisible(false);
        removeBtn.setVisible(false);
    }

    private void setOpenPositionDl() {
        openPositionsDl.setParameter("login", userSession.getUser().getLogin());
        openPositionsDl.setParameter("currentDate", new Date());
        openPositionsDl.load();
    }
}