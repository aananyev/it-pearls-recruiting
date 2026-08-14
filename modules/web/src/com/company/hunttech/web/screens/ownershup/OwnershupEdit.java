package com.company.hunttech.web.screens.ownershup;

import com.company.hunttech.entity.Ownershup;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;

@UiController("hunttech_Ownershup.edit")
@UiDescriptor("ownershup-edit.xml")
@EditedEntityContainer("ownershupDc")
@LoadDataBeforeShow
public class OwnershupEdit extends StandardEditor<Ownershup> {

    @Inject
    private TextField<String> shortTypeField;
    @Inject
    private Button mainNav;

    /**
     * Презентационная навигация: переводит фокус к первому полю «Основных данных»
     * и подсвечивает активный пункт sidebar. Entity, loaders и lifecycle не затрагиваются.
     */
    public void focusMainSection() {
        shortTypeField.focus();
        setActiveNavigation(mainNav);
    }

    private void setActiveNavigation(Button activeButton) {
        activeButton.addStyleName("label-nav-item-active");
    }
}
