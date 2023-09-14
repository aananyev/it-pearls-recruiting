package com.company.itpearls.web.screens.personelreserve;

import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.DateField;
import com.haulmont.cuba.gui.components.Image;
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
    @Inject
    private Image candidateDefaultPic;
    @Inject
    private Image candidatePic;

    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        getEditedEntity().setSelectedForAction(getEditedEntity().getRemovedFromReserve() != null
                ? getEditedEntity().getRemovedFromReserve() : false);
        getEditedEntity().setRemovedFromReserve(getEditedEntity().getRemovedFromReserve() != null
                ? getEditedEntity().getRemovedFromReserve() : false);
        getEditedEntity().setInProcess(getEditedEntity().getInProcess() != null
                ? getEditedEntity().getInProcess() : false);
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            dateField.setValue(new Date());
        }

        setCandidatePicImage();
    }

    private void setCandidatePicImage() {
        if (getEditedEntity().getJobCandidate().getFileImageFace() == null) {
            candidateDefaultPic.setVisible(true);
            candidatePic.setVisible(false);
        } else {
            candidateDefaultPic.setVisible(false);
            candidatePic.setVisible(true);
        }
    }

}