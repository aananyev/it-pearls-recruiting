package com.company.hunttech.web.screens.aiprompttemplate;

import com.company.hunttech.entity.AiPromptTemplate;
import com.haulmont.cuba.gui.screen.EditedEntityContainer;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.StandardEditor;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

@UiController("hunttech_AiPromptTemplate.edit")
@UiDescriptor("ai-prompt-template-edit.xml")
@EditedEntityContainer("aiPromptTemplateDc")
@LoadDataBeforeShow
public class AiPromptTemplateEdit extends StandardEditor<AiPromptTemplate> {
}
