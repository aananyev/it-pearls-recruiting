package com.company.itpearls.web.screens.fragments;

import com.company.itpearls.entity.Position;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.components.SuggestionField;
import com.haulmont.cuba.gui.components.SuggestionPickerField;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.screen.*;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@UiController("itpearls_OnlyTextPersonPosition")
@UiDescriptor("onlyTextPersonPosition.xml")
public class OnlyTextPersonPosition extends Onlytext {
    @Inject
    private Notifications notifications;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private SuggestionPickerField<Position> personPositionField;
    @Inject
    private MetadataTools metadataTools;
    @Inject
    private CollectionContainer<Position> personPositionsDc;

    public Position getPersonPosition() {
        return personPositionField.getValue();
    }

    @Subscribe
    public void onBeforeClose(BeforeCloseEvent event) {
        if (!event.closedWith(StandardOutcome.CLOSE)) {
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

    @Subscribe
    public void onInit(InitEvent event) {
        List<Position> position = new ArrayList<>(personPositionsDc.getItems());

        personPositionField.setSearchExecutor((searchString, searchParams) ->
                position.stream()
                        .filter(pos ->
                                StringUtils.containsIgnoreCase(metadataTools.getInstanceName(pos),
                                                searchString))
                        .collect(Collectors.toList()));

    }

    @Subscribe("cancelButton")
    public void onCancelButtonClick(Button.ClickEvent event) {
        close(StandardOutcome.CLOSE);
    }
}