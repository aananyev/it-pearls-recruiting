package com.company.itpearls.web.screens.internalemailtemplate;

import com.company.itpearls.core.EmailGenerationService;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.components.RichTextArea;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.InternalEmailTemplate;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@UiController("itpearls_InternalEmailTemplate.edit")
@UiDescriptor("internal-email-template-edit.xml")
@EditedEntityContainer("internalEmailTemplateDc")
@LoadDataBeforeShow
public class InternalEmailTemplateEdit extends StandardEditor<InternalEmailTemplate> {
    @Inject
    private EmailGenerationService emailGenerationService;
    @Inject
    private RichTextArea commentKeysRichTextArea;
    @Inject
    private LookupPickerField<User> templateAuthorField;
    @Inject
    private UserSession userSession;
    @Inject
    private CheckBox visibleOtherCheckBox;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        setEmailKeys();

        templateAuthorField.setValue(userSession.getUser());
    }

    private void setEmailKeys() {
        HashMap<String, String> emailKeys = emailGenerationService.generateKeys();
        String retStr = "";

        for (Map.Entry<String, String> entry : emailKeys.entrySet()) {
            retStr = retStr + entry.getKey() + " - " + entry.getValue() + "<br>";
        }

        commentKeysRichTextArea.setValue(retStr);
    }

    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        if (visibleOtherCheckBox.getValue() == null) {
            visibleOtherCheckBox.setValue(false);
        }
    }
}