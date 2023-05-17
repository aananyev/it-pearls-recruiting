package com.company.itpearls.web.screens.fragments;

import com.company.itpearls.entity.Position;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.components.SuggestionField;
import com.haulmont.cuba.gui.components.SuggestionPickerField;
import com.haulmont.cuba.gui.screen.MessageBundle;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("itpearls_OnlyTextPersonPosition")
@UiDescriptor("onlyTextPersonPosition.xml")
public class OnlyTextPersonPosition extends Onlytext {
    @Inject
    private Notifications notifications;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private SuggestionPickerField<Position> personPositionField;

    public Position getPersonPosition() {
        return personPositionField.getValue();
    }

    @Subscribe
    public void onBeforeClose(BeforeCloseEvent event) {
        if (personPositionField.getValue() == null) {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withType(Notifications.NotificationType.ERROR)
                    .withPosition(Notifications.Position.MIDDLE_CENTER)
                    .withCaption(messageBundle.getMessage("msg://msgError"))
                    .withDescription(messageBundle.getMessage("msg://msgErrorOpenPosition"))
                    .show();
            event.getScreen().getWindow().setFocusComponent("personPositionField");

            event.preventWindowClose();
        }
    }
}