package com.company.itpearls.web.screens.company;

import com.company.itpearls.entity.City;
import com.company.itpearls.entity.Region;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.FileDescriptorResource;
import com.haulmont.cuba.gui.components.FileUploadField;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Company;

import javax.inject.Inject;

@UiController("itpearls_Company.edit")
@UiDescriptor("company-edit.xml")
@EditedEntityContainer("companyDc")
@LoadDataBeforeShow
public class CompanyEdit extends StandardEditor<Company> {

    @Inject
    private Image companyLogoFileImage;
    @Inject
    private FileUploadField companyLogoFileUpload;

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
       if( PersistenceHelper.isNew( getEditedEntity() ) ) {
            getEditedEntity().setOurClient( false ); // установка сразу в "ненашклиент"
       }
    }

    // установить регион
    @Subscribe("cityOfCompanyField")
    public void onCityOfCompanyFieldValueChange(HasValue.ValueChangeEvent<City> event) {
        if(!getEditedEntity().getCityOfCompany().equals(null)) {
            getEditedEntity().setRegionOfCompany(getEditedEntity().getCityOfCompany().getCityRegion());
        }
    }

    @Subscribe("regionOfCompanyField")
    public void onRegionOfCompanyFieldValueChange(HasValue.ValueChangeEvent<Region> event) {
       if(!getEditedEntity().getRegionOfCompany().equals(null)) {
           getEditedEntity().setCountryOfCompany(getEditedEntity().getRegionOfCompany().getRegionCountry());
       }
    }


    @Subscribe("companyLogoFileUpload")
    public void onCompanyLogoFileUploadFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {
        FileDescriptorResource fileDescriptorResource = companyLogoFileImage.createResource(FileDescriptorResource.class)
                .setFileDescriptor(companyLogoFileUpload.getFileDescriptor());

        companyLogoFileImage.setSource(fileDescriptorResource);
    }
    
}