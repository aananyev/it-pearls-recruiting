package com.company.itpearls.web.screens.position;

import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Position;

import javax.inject.Inject;

@UiController("itpearls_Position.edit")
@UiDescriptor("position-edit.xml")
@EditedEntityContainer("positionDc")
@LoadDataBeforeShow
public class PositionEdit extends StandardEditor<Position> {
    @Inject
    private Label<String> textPositionName;
    @Inject
    private TextField<String> positionEnNameField;
    @Inject
    private TextField<String> positionRuNameField;
    @Inject
    private Image positionLogoPic;
    @Inject
    private Image logoDefaultPic;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        setLabel();
        setLogoPicImage();
    }

    @Subscribe("positionEnNameField")
    public void onPositionEnNameFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        setLabel();
    }

    @Subscribe("positionRuNameField")
    public void onPositionRuNameFieldTextChange1(TextInputField.TextChangeEvent event) {
        setLabel();
    }

    @Subscribe("positionEnNameField")
    public void onPositionEnNameFieldTextChange(TextInputField.TextChangeEvent event) {
        setLabel();

    }

    @Subscribe("positionRuNameField")
    public void onPositionRuNameFieldTextChange(TextInputField.TextChangeEvent event) {
        setLabel();
    }

    private void setLabel() {
        String a = positionEnNameField.getValue() + " - " + positionRuNameField.getValue();

        textPositionName.setValue( a );
    }

    private void setLogoPicImage() {
        if (getEditedEntity().getLogo() == null) {
            logoDefaultPic.setVisible(true);
            positionLogoPic.setVisible(false);
        } else {
            logoDefaultPic.setVisible(false);
            positionLogoPic.setVisible(true);
        }
    }

    @Subscribe("positionLogoPic")
    public void onPositionLogoPicSourceChange(ResourceView.SourceChangeEvent event) {
        setLogoPicImage();

    }

}