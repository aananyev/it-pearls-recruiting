package com.company.hunttech.web.screens.vacancyprompttemplate;

import com.company.hunttech.entity.VacancyPromptTemplate;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.TextArea;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.screen.EditedEntityContainer;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.StandardEditor;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("hunttech_VacancyPromptTemplate.edit")
@UiDescriptor("vacancy-prompt-template-edit.xml")
@EditedEntityContainer("vacancyPromptTemplateDc")
@LoadDataBeforeShow
public class VacancyPromptTemplateEdit extends StandardEditor<VacancyPromptTemplate> {

    @Inject
    private TextField<String> codeField;
    @Inject
    private TextArea<String> systemContextField;
    @Inject
    private Button vacancyPromptTemplateMainNav;
    @Inject
    private Button vacancyPromptTemplatePromptNav;

    /**
     * Переводит фокус к основным реквизитам шаблона, не затрагивая entity и lifecycle сохранения.
     */
    public void focusMainSection() {
        codeField.focus();
        setActiveNavigation(vacancyPromptTemplateMainNav, vacancyPromptTemplatePromptNav);
    }

    /**
     * Переводит фокус к полю системного контекста, сохраняя исходные bindings формы.
     */
    public void focusPromptSection() {
        systemContextField.focus();
        setActiveNavigation(vacancyPromptTemplatePromptNav, vacancyPromptTemplateMainNav);
    }

    private void setActiveNavigation(Button activeButton, Button inactiveButton) {
        activeButton.addStyleName("label-nav-item-active");
        inactiveButton.removeStyleName("label-nav-item-active");
    }
}
