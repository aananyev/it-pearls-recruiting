package com.company.hunttech.web.screens.grade;

import com.company.hunttech.entity.Grade;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;

@UiController("hunttech_Grade.edit")
@UiDescriptor("grade-edit.xml")
@EditedEntityContainer("gradeDc")
@LoadDataBeforeShow
public class GradeEdit extends StandardEditor<Grade> {

    @Inject
    private TextField<String> gradeNameField;
    @Inject
    private Button mainNav;

    /**
     * Презентационная навигация: переводит фокус к первому полю «Основных данных»
     * и подсвечивает активный пункт sidebar. Entity, loaders и lifecycle не затрагиваются.
     */
    public void focusMainSection() {
        gradeNameField.focus();
        setActiveNavigation(mainNav);
    }

    private void setActiveNavigation(Button activeButton) {
        activeButton.addStyleName("label-nav-item-active");
    }
}
