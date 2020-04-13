package com.company.itpearls.web.screens.company;

import com.company.itpearls.entity.City;
import com.company.itpearls.entity.Region;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Company;

@UiController("itpearls_Company.edit")
@UiDescriptor("company-edit.xml")
@EditedEntityContainer("companyDc")
@LoadDataBeforeShow
public class CompanyEdit extends StandardEditor<Company> {

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
    
    
}