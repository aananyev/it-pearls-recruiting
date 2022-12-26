package com.company.itpearls.web.screens.company;

import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Company;
import com.haulmont.cuba.gui.screen.LookupComponent;

import javax.inject.Inject;

@UiController("itpearls_Company.browse")
@UiDescriptor("company-browse.xml")
@LookupComponent("companiesTable")
@LoadDataBeforeShow
public class CompanyBrowse extends StandardLookup<Company> {
    @Inject
    private UiComponents uiComponents;

    @Install(to = "companiesTable.companyLogoColumn", subject = "columnGenerator")
    private Component companiesTableCompanyLogoColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<Company> event) {
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        retBox.setWidthFull();
        retBox.setHeightFull();

        Image image = uiComponents.create(Image.class);
        image.setDescriptionAsHtml(true);
        image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        image.setWidth("20px");
        image.setHeight("20px");
        image.setStyleName("circle-no-border-20px");
        image.setAlignment(Component.Alignment.MIDDLE_CENTER);
        image.setDescription("<h4>"
                + event.getItem().getComanyName()
                + "</h4><br><br>"
                + event.getItem().getCompanyDescription());

        if (event.getItem().getFileCompanyLogo() != null) {
            image.setSource(FileDescriptorResource.class)
                    .setFileDescriptor(event
                            .getItem()
                            .getFileCompanyLogo());
        } else {
            image.setSource(ThemeResource.class).setPath("icons/no-company.png");
        }

        retBox.add(image);
        return retBox;
    }

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
