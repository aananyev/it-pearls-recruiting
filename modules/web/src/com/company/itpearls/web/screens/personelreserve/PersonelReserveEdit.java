package com.company.itpearls.web.screens.personelreserve;

import com.company.itpearls.entity.*;
import com.google.gson.GsonBuilder;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
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
    @Inject
    private Dialogs dialogs;
    @Inject
    private LookupPickerField<ExtUser> reactutierField;
    @Inject
    private LookupPickerField<Position> personPositionField;

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
            getEditedEntity().setInProcess(true);
            getEditedEntity().setRemovedFromReserve(false);
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
                    dateCalendar.add(Calendar.MONTH, 1);

                    endDateField.setValue(dateCalendar.getTime());
                }
            }

            if (reactutierField.getValue() == null) {
                reactutierField.setValue((ExtUser) userSession.getUser());
            }
        }

        dateField.addValidator(value -> {
            if (value != null) {
                if (value != null && endDateField.getValue() != null && value.after(endDateField.getValue())) {
                    throw new ValidationException(messageBundle.getMessage("msgStartDateAfterEndDate"));
                }
            } else {
                throw new ValidationException(messageBundle.getMessage("msgStartDateIsNull"));
            }
        });

        endDateField.addValidator(value -> {
            if (value != null) {
                if (dateField.getValue() != null && dateField.getValue().after(value)) {
                    throw new ValidationException(messageBundle.getMessage("msgStartDateAfterEndDate"));
                }
            } else {
                throw new ValidationException(messageBundle.getMessage("msgEndDateIsNull"));
            }
        });
    }

    @Subscribe("jobCandidateField")
    public void onJobCandidateFieldValueChange(HasValue.ValueChangeEvent<JobCandidate> event) {
       if (event.getValue() != null) {
           if (event.getValue().getPersonPosition() != null) {
               personPositionField.setValue(event.getValue().getPersonPosition());
           }
       }
    }


    @Subscribe("dateField")
    public void onDateFieldValueChange(HasValue.ValueChangeEvent<Date> event) {
        if (endDateField.getValue() == null) {
            dialogs.createOptionDialog(Dialogs.MessageType.CONFIRMATION)
                    .withType(Dialogs.MessageType.WARNING)
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withMessage(messageBundle.getMessage("msgSetEndDate"))
                    .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                        GregorianCalendar calendar = new GregorianCalendar();

                        calendar.setTime(dateField.getValue());
                        calendar.add(Calendar.MONTH, 1);
                        endDateField.setValue(calendar.getTime());

                    }), new DialogAction(DialogAction.Type.NO))
                    .show();
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