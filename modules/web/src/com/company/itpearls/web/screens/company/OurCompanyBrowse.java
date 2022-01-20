package com.company.itpearls.web.screens.company;

import com.company.itpearls.entity.Company;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.company.itpearls.web.screens.company.CompanyBrowse;

import javax.inject.Inject;

@UiController("itpearls_OurCompany.browse")
@UiDescriptor("our-company-browse.xml")
public class OurCompanyBrowse extends CompanyBrowse {
    @Inject
    private CheckBox checkBoxOnlyLegalEntity;
    @Inject
    private CheckBox checkBoxOnlyOurClient;
    @Inject
    private CollectionLoader<Company> companiesDl;

    @Subscribe
    public void onBeforeShow1(BeforeShowEvent event) {
        checkBoxOnlyLegalEntity.setVisible(false);
        checkBoxOnlyOurClient.setValue(false);

        companiesDl.setParameter("setOurLegalEntity", true);
        companiesDl.load();
    }
}