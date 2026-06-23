package com.company.itpearls.web.screens.company;

import com.company.itpearls.entity.City;
import com.company.itpearls.entity.Company;
import com.company.itpearls.entity.Region;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.ViewBuilder;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;

@UiController("itpearls_Company.edit")
@UiDescriptor("company-edit.xml")
@EditedEntityContainer("companyDc")
@LoadDataBeforeShow
public class CompanyEdit extends StandardEditor<Company> {
    @Inject
    private Image companyDefaultLogoFileImage;
    @Inject
    private Image companyLogoFileImage;
    @Inject
    private FileUploadField companyLogoFileUpload;
    @Inject
    private DataManager dataManager;
    @Inject
    private TabSheet mainTab;

    private boolean addressLoaded;
    private boolean companyDescriptionLoaded;
    private boolean departmentsLoaded;

    @Subscribe("mainTab")
    public void onMainTabSelectedTabChange(TabSheet.SelectedTabChangeEvent event) {
        if (event.getSelectedTab() == null || PersistenceHelper.isNew(getEditedEntity())) {
            return;
        }
        String tabName = event.getSelectedTab().getName();
        if ("tabConpanyDetails".equals(tabName) && !addressLoaded) {
            loadAddress();
            addressLoaded = true;
        }
        if ("companyDescriptionTab".equals(tabName) && !companyDescriptionLoaded) {
            loadCompanyDescriptions();
            companyDescriptionLoaded = true;
        }
        if ("tabCompanyDepartament".equals(tabName) && !departmentsLoaded) {
            loadDepartments();
            departmentsLoaded = true;
        }
    }

    private void loadAddress() {
        Company reloaded = dataManager.reload(getEditedEntity(), ViewBuilder.of(Company.class)
                .add("addressOfCompany")
                .build());
        getEditedEntity().setAddressOfCompany(reloaded.getAddressOfCompany());
    }

    private void loadCompanyDescriptions() {
        Company reloaded = dataManager.reload(getEditedEntity(), ViewBuilder.of(Company.class)
                .add("companyDescription")
                .add("workingConditions")
                .build());
        getEditedEntity().setCompanyDescription(reloaded.getCompanyDescription());
        getEditedEntity().setWorkingConditions(reloaded.getWorkingConditions());
    }

    private void loadDepartments() {
        Company reloaded = dataManager.reload(getEditedEntity(), ViewBuilder.of(Company.class)
                .add("departmentOfCompany", "companyDepartament-department-child-view")
                .build());
        getEditedEntity().setDepartmentOfCompany(reloaded.getDepartmentOfCompany());
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setOurClient(false);
        } else if (!addressLoaded) {
            loadAddress();
            addressLoaded = true;
        }
    }

    @Subscribe("companyLogoFileUpload")
    public void onCompanyLogoFileUploadBeforeValueClear(FileUploadField.BeforeValueClearEvent event) {
        setCompanyPicImage();
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
       setCompanyPicImage();
    }

    private void setCompanyPicImage() {
        if (getEditedEntity().getFileCompanyLogo() == null) {
            companyDefaultLogoFileImage.setVisible(true);
            companyLogoFileImage.setVisible(false);
        } else {
            companyDefaultLogoFileImage.setVisible(false);
            companyLogoFileImage.setVisible(true);
        }
    }

    @Subscribe("cityOfCompanyField")
    public void onCityOfCompanyFieldValueChange(HasValue.ValueChangeEvent<City> event) {
        City city = event.getValue();
        if (city == null) {
            return;
        }
        City cityLoaded = dataManager.reload(city, "city-location-view");
        Region region = cityLoaded.getCityRegion();
        getEditedEntity().setRegionOfCompany(region);
        if (region != null) {
            Region regionLoaded = dataManager.reload(region, "region-browse-view");
            getEditedEntity().setCountryOfCompany(regionLoaded.getRegionCountry());
        }
    }

    @Subscribe("regionOfCompanyField")
    public void onRegionOfCompanyFieldValueChange(HasValue.ValueChangeEvent<Region> event) {
        Region region = event.getValue();
        if (region == null) {
            return;
        }
        Region regionLoaded = dataManager.reload(region, "region-browse-view");
        getEditedEntity().setCountryOfCompany(regionLoaded.getRegionCountry());
    }

    @Subscribe("companyLogoFileUpload")
    public void onCompanyLogoFileUploadFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {
        try {
            companyLogoFileImage.setVisible(true);
            companyDefaultLogoFileImage.setVisible(false);

            FileDescriptorResource fileDescriptorResource =
                    companyLogoFileImage.createResource(FileDescriptorResource.class)
                            .setFileDescriptor(
                                    companyLogoFileUpload.getFileDescriptor());

            companyLogoFileImage.setSource(fileDescriptorResource);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Subscribe("companyLogoFileImage")
    public void onCompanyLogoFileImageSourceChange(ResourceView.SourceChangeEvent event) {
        setCompanyPicImage();
    }
}