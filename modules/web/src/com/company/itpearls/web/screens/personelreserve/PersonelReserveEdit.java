package com.company.itpearls.web.screens.personelreserve;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.PersonelReserve;

@UiController("itpearls_PersonelReserve.edit")
@UiDescriptor("personel-reserve-edit.xml")
@EditedEntityContainer("personelReserveDc")
@LoadDataBeforeShow
public class PersonelReserveEdit extends StandardEditor<PersonelReserve> {
}