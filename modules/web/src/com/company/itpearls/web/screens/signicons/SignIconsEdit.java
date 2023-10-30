package com.company.itpearls.web.screens.signicons;

import com.haulmont.cuba.gui.components.ColorPicker;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SignIcons;
import com.vaadin.server.Page;

import javax.inject.Inject;

@UiController("itpearls_SignIcons.edit")
@UiDescriptor("sign-icons-edit.xml")
@EditedEntityContainer("signIconsDc")
@LoadDataBeforeShow
public class SignIconsEdit extends StandardEditor<SignIcons> {
    @Inject
    private TextField<String> iconNameField;
    @Inject
    private Label<String> iconSampleLabel;
    @Inject
    private ColorPicker iconColorColorPicker;
    @Inject
    private TextField<String> titleRuField;
    @Inject
    private InstanceContainer<SignIcons> signIconsDc;
    @Inject
    private Label<String> labelNameLabel;

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        setIconSampleLabel();
    }

    private void setIconSampleLabel() {
        if (iconNameField.getValue() != null) {
            iconSampleLabel.setIcon(iconNameField.getValue());
        }

        if (iconColorColorPicker.getValue() != null) {
            iconSampleLabel.setStyleName("pic-center-large-" + iconColorColorPicker.getValue());
        }

        if (titleRuField.getValue() != null) {
            labelNameLabel.setValue(titleRuField.getValue());
        }
    }

    protected void injectColorCss(String color) {
        Page.Styles styles = Page.getCurrent().getStyles();
        styles.add(String.format(
                ".pic-center-large-%s {" +
                        "color: #%s;" +
                        "text-align: center;" +
                        "text-color: gray;" +
                        "font-size: large;" +
                        "margin: 0 auto;" +
                        "}",
                color, color));
    }

    @Subscribe("iconColorColorPicker")
    public void onIconColorColorPickerValueChange(HasValue.ValueChangeEvent<String> event) {
        if (event.getValue() != null) {
            injectColorCss((String) event.getValue());
            iconSampleLabel.setStyleName("pic-center-large-" + event.getValue());
        }
    }
}