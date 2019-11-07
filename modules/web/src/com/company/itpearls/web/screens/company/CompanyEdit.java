package com.company.itpearls.web.screens.company;

import com.haulmont.cuba.core.global.PersistenceHelper;
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
}