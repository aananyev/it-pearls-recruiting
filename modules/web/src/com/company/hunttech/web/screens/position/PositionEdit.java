package com.company.hunttech.web.screens.position;

import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.HasValue;
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

    /**
     * Презентационная навигация: переводит фокус к русскому наименованию должности
     * и подсвечивает активный пункт sidebar. Entity, loaders и lifecycle не затрагиваются.
     */
    public void focusMainSection() {
        positionRuNameField.focus();
        setActiveNavigation(mainNav);
    }

    /**
     * Презентационная навигация: переводит фокус к редактору общего описания
     * и подсвечивает активный пункт sidebar. Entity, loaders и lifecycle не затрагиваются.
     */
    public void focusDescriptionSection() {
        standartDescriptionTextArea.focus();
        setActiveNavigation(descriptionNav);
    }

    private void setActiveNavigation(Button activeButton) {
        activeButton.addStyleName("label-nav-item-active");
    }
}
