package com.company.itpearls.web.screens.company;

import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Company;

import javax.inject.Inject;

@UiController("itpearls_Company.browse")
@UiDescriptor("company-browse.xml")
@LookupComponent("companiesTable")
@LoadDataBeforeShow
public class CompanyBrowse extends StandardLookup<Company> {
    @Inject
    private CheckBox checkBoxOnlyOurClient;
    @Inject
    private CollectionLoader<Company> companiesDl;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        filterOurClients();
    }

    @Subscribe("checkBoxOnlyOurClient")
    public void onCheckBoxOnlyOurClientValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        filterOurClients();
    }

    private void filterOurClients() {
        if (checkBoxOnlyOurClient.getValue()) {
            companiesDl.setParameter("setOurClient", true);
        } else {
            companiesDl.removeParameter("setOurClient");
        }

        companiesDl.load();
    }
}
