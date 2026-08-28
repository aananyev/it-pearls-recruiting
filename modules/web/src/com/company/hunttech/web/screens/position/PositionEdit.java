package com.company.hunttech.web.screens.position;

import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.RichTextArea;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.components.TextInputField;
import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.Position;

import javax.inject.Inject;

@UiController("hunttech_Position.edit")
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
    private RichTextArea standartDescriptionTextArea;
    @Inject
    private Button mainNav;
    @Inject
    private Button descriptionNav;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        setLabel();
    }

    @Subscribe("positionEnNameField")
    public void onPositionEnNameFieldTextChange(TextInputField.TextChangeEvent event) {
        String en = event.getText();
        String ru = positionRuNameField.getValue();
        updateLabelText(en, ru);
    }

    @Subscribe("positionRuNameField")
    public void onPositionRuNameFieldTextChange(TextInputField.TextChangeEvent event) {
        String ru = event.getText();
        String en = positionEnNameField.getValue();
        updateLabelText(en, ru);
    }

    private void setLabel() {
        String en = positionEnNameField.getValue();
        String ru = positionRuNameField.getValue();
        updateLabelText(en, ru);
    }

    private void updateLabelText(String en, String ru) {
        if (textPositionName == null) {
            return;
        }
        if (en != null && !en.trim().isEmpty() && ru != null && !ru.trim().isEmpty()) {
            textPositionName.setValue(en.trim() + " — " + ru.trim());
        } else if (ru != null && !ru.trim().isEmpty()) {
            textPositionName.setValue(ru.trim());
        } else if (en != null && !en.trim().isEmpty()) {
            textPositionName.setValue(en.trim());
        } else {
            textPositionName.setValue("");
        }
    }

    /**
     * Презентационная навигация: переводит фокус к русскому наименованию должности
     * и подсвечивает активный пункт sidebar.
     */
    public void focusMainSection() {
        positionRuNameField.focus();
        setActiveNavigation(mainNav);
    }

    /**
     * Презентационная навигация: переводит фокус к редактору общего описания
     * и подсвечивает активный пункт sidebar.
     */
    public void focusDescriptionSection() {
        standartDescriptionTextArea.focus();
        setActiveNavigation(descriptionNav);
    }

    private void setActiveNavigation(Button activeButton) {
        if (mainNav != null) {
            mainNav.removeStyleName("label-nav-item-active");
        }
        if (descriptionNav != null) {
            descriptionNav.removeStyleName("label-nav-item-active");
        }
        if (activeButton != null) {
            activeButton.addStyleName("label-nav-item-active");
        }
    }
}
