package com.company.itpearls.web.screens.personelreserve;

import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.PersonelReserve;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;

@UiController("itpearls_PersonelReserve.browse")
@UiDescriptor("personel-reserve-browse.xml")
@LookupComponent("personelReservesTable")
@LoadDataBeforeShow
public class PersonelReserveBrowse extends StandardLookup<PersonelReserve> {
    @Inject
    private CheckBox allCandidatesCheckBox;
    @Inject
    private CollectionLoader<PersonelReserve> personelReservesDl;
    @Inject
    private UserSession userSession;
    @Inject
    private CheckBox activesCheckBox;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        allCandidatesCheckBox.setValue(true);
        activesCheckBox.setValue(true);
    }

    @Subscribe("allCandidatesCheckBox")
    public void onAllCandidatesCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (event.getValue()) {
            personelReservesDl.setParameter("recruter", userSession.getUser());
        } else {
            personelReservesDl.removeParameter("recruter");
        }

        personelReservesDl.load();
    }

    @Subscribe("activesCheckBox")
    public void onActivesCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
       if (event.getValue()) {
           personelReservesDl.setParameter("actives", true);
       } else {
           personelReservesDl.removeParameter("actives");
       }

       personelReservesDl.load();
    }
}