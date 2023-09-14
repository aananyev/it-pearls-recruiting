package com.company.itpearls.web.screens.internalemailtemplate;

import com.company.itpearls.core.EmailGenerationService;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.InternalEmailTemplate;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

@UiController("itpearls_InternalEmailTemplate.edit")
@UiDescriptor("internal-email-template-edit.xml")
@EditedEntityContainer("internalEmailTemplateDc")
@LoadDataBeforeShow
public class InternalEmailTemplateEdit extends StandardEditor<InternalEmailTemplate> {
    @Inject
    private EmailGenerationService emailGenerationService;
//    @Inject
//    private RichTextArea commentKeysRichTextArea;
    @Inject
    private LookupPickerField<User> templateAuthorField;
    @Inject
    private UserSession userSession;
    @Inject
    private CheckBox visibleOtherCheckBox;
    @Inject
    private OptionsList commentKeysOptionList;
    @Inject
    private RichTextArea templateTextField;

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

        commentKeysOptionList.setOptionsMap(emailKeys);
//        commentKeysRichTextArea.setValue(retStr);
    }

    @Subscribe("templateTextField")
    public void onTemplateTextFieldValueChange(HasValue.ValueChangeEvent<String> event) {
    }

    @Subscribe("commentKeysOptionList")
    public void onCommentKeysOptionListDoubleClick(OptionsList.DoubleClickEvent event) {

        String text = event.getItem() + " " +
                templateTextField.getValue();

        templateTextField.setValue(text);
    }

    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        if (visibleOtherCheckBox.getValue() == null) {
            visibleOtherCheckBox.setValue(false);
        }
    }
}