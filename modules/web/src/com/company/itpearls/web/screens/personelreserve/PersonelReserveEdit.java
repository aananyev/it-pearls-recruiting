package com.company.itpearls.web.screens.personelreserve;

import com.company.itpearls.entity.OpenPosition;
import com.google.gson.GsonBuilder;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.PersonelReserve;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

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
    @Inject
    private LookupPickerField<OpenPosition> openPositionField;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private CheckBox onlyMySubscribeCheckBox;
    @Inject
    private CollectionLoader<OpenPosition> openPositionsDl;
    @Inject
    private UserSession userSession;
    @Inject
    private CollectionContainer<OpenPosition> openPositionsDc;
    @Inject
    private Notifications notifications;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private DateField<Date> endDateField;

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
        initOpenPositionField();
        setOnlyMySubscribeCheckBox();
        setEndDate();
    }

    private void setEndDate() {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            if (endDateField.getValue() == null) {
                if (dateField.getValue() != null) {
                    GregorianCalendar dateCalendar = new GregorianCalendar();
                    dateCalendar.setTime(dateField.getValue());
                    dateCalendar.add(1, Calendar.MONTH);

                    endDateField.setValue(dateCalendar.getTime());
                }
            }
        }
    }

    private void setCandidatePicImage() {
        if (getEditedEntity().getJobCandidate() != null) {
            if (getEditedEntity().getJobCandidate().getFileImageFace() == null) {
                candidateDefaultPic.setVisible(true);
                candidatePic.setVisible(false);
            } else {
                candidateDefaultPic.setVisible(false);
                candidatePic.setVisible(true);
            }
        }
    }

    private void initOpenPositionField() {
        openPositionField.setOptionImageProvider(this::openPositionFieldImageProvider);
    }

    protected Resource openPositionFieldImageProvider(OpenPosition openPosition) {
        Image retImage = uiComponents.create(Image.class);
        retImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        retImage.setWidth("30px");

        if (openPosition.getProjectName() != null) {
            if (openPosition.getProjectName().getProjectLogo() != null) {
                return retImage.createResource(FileDescriptorResource.class)
                        .setFileDescriptor(
                                openPosition
                                        .getProjectName()
                                        .getProjectLogo());
            } else {
                return retImage.createResource(ThemeResource.class).setPath("icons/no-company.png");
            }
        } else {
            return retImage.createResource(ThemeResource.class).setPath("icons/no-company.png");
        }
    }

    private void setOnlyMySubscribeCheckBox() {
        onlyMySubscribeCheckBox.setValue(true);
        openPositionsDl.setParameter("subscriber", userSession.getUser());
        openPositionsDl.load();

        if (openPositionsDc.getItems().size() == 0) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withDescription(messageBundle.getMessage("msgNoSubscribeVacansies"))
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .withHideDelayMs(10000)
                    .withType(Notifications.NotificationType.WARNING)
                    .show();
        }

        onlyMySubscribeCheckBox.addValueChangeListener(e -> {
            if (e.getValue()) {
                openPositionsDl.setParameter("subscriber", userSession.getUser());
                openPositionsDl.load();

                if (openPositionsDc.getItems().size() == 0) {
                    notifications.create(Notifications.NotificationType.WARNING)
                            .withCaption(messageBundle.getMessage("msgWarning"))
                            .withDescription(messageBundle.getMessage("msgNoSubscribeVacansies"))
                            .withPosition(Notifications.Position.BOTTOM_RIGHT)
                            .withHideDelayMs(10000)
                            .withType(Notifications.NotificationType.WARNING)
                            .show();
                }
            } else {
                openPositionsDl.removeParameter("subscriber");
                openPositionsDl.load();
            }
        });
    }

}