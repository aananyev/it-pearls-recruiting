package com.company.itpearls.web.screens.signicons;

import com.company.itpearls.core.ParseCVService;
import com.company.itpearls.core.StarsAndOtherService;
import com.company.itpearls.entity.ExtUser;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.components.ColorPicker;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SignIcons;
import com.haulmont.cuba.security.global.UserSession;
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
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private TextField<String> titleEndField;
    @Inject
    private StarsAndOtherService starsAndOtherService;
    @Inject
    private ParseCVService parseCVService;
    @Inject
    private UserSession userSession;

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

    public void selectIconButtonInvoke() {
        SignIconsSelecter selectIcon = (SignIconsSelecter) screenBuilders.screen(this)
                .withScreenClass(SignIconsSelecter.class)
                .withOpenMode(OpenMode.DIALOG)
                .build();

        selectIcon.addAfterCloseListener(e -> {
            iconNameField.setValue(selectIcon.getIconSelected());
            iconSampleLabel.setIcon(iconNameField.getValue());
            titleEndField.setValue(iconNameField.getValue());
        });

        selectIcon.show();
    }

    @Subscribe("titleRuField")
    public void onTitleRuFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        if (titleRuField.getValue() != null) {
            String outString =
                    starsAndOtherService.deleteSystemChar(
                            starsAndOtherService.cyrillicToLatin(
                            titleRuField.getValue().replaceAll(" ", "_")));
            titleEndField.setValue(outString);
        }
    }

    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        getEditedEntity().setUser((ExtUser) userSession.getUser());
    }
}