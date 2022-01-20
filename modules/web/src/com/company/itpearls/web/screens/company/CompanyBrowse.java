package com.company.itpearls.web.screens.company;

import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
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
    @Inject
    private CheckBox checkBoxOnlyLegalEntity;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        filterOurClients();
    }

    @Subscribe("checkBoxOnlyOurClient")
    public void onCheckBoxOnlyOurClientValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        filterOurClients();
    }

    @Subscribe("checkBoxOnlyLegalEntity")
    public void onCheckBoxOnlyLegalEntityValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        filterOurLegalEntity();
    }

    private void filterOurLegalEntity() {
        if (checkBoxOnlyLegalEntity.getValue()) {
            companiesDl.setParameter("setOurLegalEntity", true);
        } else {
            companiesDl.removeParameter("setOurLegalEntity");
        }

        companiesDl.load();
    }

    private void filterOurClients() {
        if (checkBoxOnlyOurClient.getValue()) {
            companiesDl.setParameter("setOurClient", true);
        } else {
            companiesDl.removeParameter("setOurClient");
        }

        companiesDl.load();
    }

    @Install(to = "companiesTable.ourCompanyIconColumn", subject = "columnGenerator")
    private Icons.Icon companiesTableOurCompanyIconColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<Company> event) {
        String returnIcon = "";

        if(event.getItem().getOurLegalEntity() != null) {
            if (event.getItem().getOurLegalEntity()) {
                returnIcon = "PLUS_CIRCLE";
            } else {
                returnIcon = "MINUS_CIRCLE";
            }
        } else {
            returnIcon = "MINUS_CIRCLE";
        }

        return CubaIcon.valueOf(returnIcon);
    }

    @Install(to = "companiesTable.ourClientIconColumn", subject = "columnGenerator")
    private Icons.Icon companiesTableOurClientIconColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<Company> event) {
        String returnIcon = "";

        if( event.getItem().getOurClient() != null) {
            if (event.getItem().getOurClient()) {
                returnIcon = "PLUS_CIRCLE";
            } else {
                returnIcon = "MINUS_CIRCLE";
            }
        } else {
            returnIcon = "MINUS_CIRCLE";
        }

        return CubaIcon.valueOf(returnIcon);
    }

    @Install(to = "companiesTable.ourCompanyIconColumn", subject = "styleProvider")
    private String companiesTableOurCompanyIconColumnStyleProvider(Company company) {
        String style = "";

        if(company.getOurLegalEntity() != null) {
            if (company.getOurLegalEntity()) {
                style = "open-position-pic-center-large-green";
            } else {
                style = "open-position-pic-center-large-red";
            }
        } else {
            style = "open-position-pic-center-large-red";
        }

        return style;
    }

    @Install(to = "companiesTable.ourClientIconColumn", subject = "styleProvider")
    private String companiesTableOurClientIconColumnStyleProvider(Company company) {
        String style = "";

        if(company.getOurClient() != null) {
            if (company.getOurClient()) {
                style = "open-position-pic-center-large-green";
            } else {
                style = "open-position-pic-center-large-red";
            }
        } else {
            style = "open-position-pic-center-large-red";
        }

        return style;
    }
}
