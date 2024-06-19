package com.company.itpearls.web.screens.iteractionlist.iteractionlistpartners;

import com.company.itpearls.core.PartnerPersonService;
import com.company.itpearls.entity.IteractionListPartners;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.company.itpearls.web.screens.iteractionlist.IteractionListBrowse;

import javax.inject.Inject;

@UiController("itpearls_IteractionListPartners.browse")
@UiDescriptor("iteraction-list-partners-browse.xml")
public class IteractionListPartnersBrowse extends IteractionListBrowse {
    @Inject
    private CollectionLoader<IteractionListPartners> iteractionListsDl;
    @Inject
    private PartnerPersonService partnerPersonService;
    @Inject
    private Button buttonExcel;

    @Subscribe
    public void onBeforeShow1(BeforeShowEvent event) {
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        setIteractionListsDc();
        buttonExcel.setVisible(false);
    }

    private void setIteractionListsDc() {
        iteractionListsDl.setParameter("partner", partnerPersonService.getMyPartner());
        iteractionListsDl.load();
    }
}