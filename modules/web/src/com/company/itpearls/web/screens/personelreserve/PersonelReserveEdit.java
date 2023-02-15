package com.company.itpearls.web.screens.personelreserve;

import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.DateField;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.PersonelReserve;

import javax.inject.Inject;
import java.util.Date;

@UiController("itpearls_PersonelReserve.edit")
@UiDescriptor("personel-reserve-edit.xml")
@EditedEntityContainer("personelReserveDc")
@LoadDataBeforeShow
public class PersonelReserveEdit extends StandardEditor<PersonelReserve> {
    @Inject
    private DateField<Date> dateField;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            dateField.setValue(new Date());
        }
    }

}