package com.company.hunttech.web.screens.aiprompttemplate;

import com.company.hunttech.entity.AiPromptTemplate;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

@UiController("hunttech_AiPromptTemplate.browse")
@UiDescriptor("ai-prompt-template-browse.xml")
@LoadDataBeforeShow
public class AiPromptTemplateBrowse extends StandardLookup<AiPromptTemplate> {
}
