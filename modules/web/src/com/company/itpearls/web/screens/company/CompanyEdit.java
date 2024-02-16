package com.company.itpearls.web.screens.company;

import com.company.itpearls.entity.City;
import com.company.itpearls.entity.Region;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Company;

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
    private TextField<String> comanyNameField;

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setOurClient(false); // установка сразу в "ненашклиент"
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

    // установить регион
    @Subscribe("cityOfCompanyField")
    public void onCityOfCompanyFieldValueChange(HasValue.ValueChangeEvent<City> event) {
        if (!getEditedEntity()
                .getCityOfCompany()
                .equals(null)) {
            getEditedEntity()
                    .setRegionOfCompany(getEditedEntity()
                            .getCityOfCompany()
                            .getCityRegion());
        }
    }

    @Subscribe("regionOfCompanyField")
    public void onRegionOfCompanyFieldValueChange(HasValue.ValueChangeEvent<Region> event) {
        if (!getEditedEntity()
                .getRegionOfCompany()
                .equals(null)) {
            getEditedEntity()
                    .setCountryOfCompany(
                            getEditedEntity()
                                    .getRegionOfCompany()
                                    .getRegionCountry());
        }
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

    public void setCompanyNameField(String comanyName) {
        comanyNameField.setValue(comanyName);
    }
}