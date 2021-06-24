package com.company.itpearls.web.screens.iteraction;

import com.company.itpearls.core.EmailGenerationService;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Iteraction;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@UiController("itpearls_Iteraction.edit")
@UiDescriptor("iteraction-edit.xml")
@EditedEntityContainer("iteractionDc")
@LoadDataBeforeShow
public class IteractionEdit extends StandardEditor<Iteraction> {
    @Inject
    private CheckBox checkBoxCalendar;
    @Inject
    private TextField<String> textFieldCalendarItemStyle;
    @Inject
    private Image embeddedPict;
    @Inject
    private CheckBox checkBoxCallDialog;
    @Inject
    private TextField<String> textFieldCallButtonText;
    @Inject
    private TextField<String> textFieldCallForm;
    @Inject
    private TextField<String> iteractionFieldPic;
    @Inject
    private RadioButtonGroup<Integer> radioButtonAddType;
    @Inject
    private CheckBox checkBoxFlag;
    @Inject
    private TextField textFieldCaption;
    @Inject
    private Label<String> labelItercationName;
    @Inject
    private Label<String> labelWarning;
    @Inject
    private Messages messages;
    @Inject
    private RadioButtonGroup radioButtonTypeNotifications;
    @Inject
    private TextField<String> lookupFieldEmails;
    @Inject
    private CheckBox checkBoxSetDefaultDateTime;

    private Map<String, Integer> mapAddType = new LinkedHashMap<>();
    private Map<String, Integer> mapTypeNotifications = new LinkedHashMap<>();
    private Map<String, Integer> mapCheckTrace = new LinkedHashMap<>();
    @Inject
    private RadioButtonGroup typeTraceRadioButtons;
    @Inject
    private TwinColumn checkTraceTwinColumn;
    @Inject
    private CheckBox neetToSendEmailCheckBox;
    @Inject
    private RichTextArea textEmailToSendRichTextArea;
    @Inject
    private EmailGenerationService emailGenerationService;
    @Inject
    private RichTextArea commentKeysRichTextArea;

    @Subscribe("checkBoxCalendar")
    public void onCheckBoxCalendarValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        textFieldCalendarItemStyle.setRequired(checkBoxCalendar.getValue());
    }

    @Subscribe
    public void onInit(InitEvent event) {
        addRadioButtonAddType();
        addTypeNotifications();
        addCheckTrace();
    }

    private void addCheckTrace() {
        mapCheckTrace.put("Нет отслеживания", 1);
        mapCheckTrace.put("Отслеживание до появления события по октрытой позиции", 2);
        mapCheckTrace.put("Отслеживание по всем позициям", 3);

        typeTraceRadioButtons.setOptionsMap(mapCheckTrace);
    }

    @Subscribe("typeTraceRadioButtons")
    public void onTypeTraceRadioButtonsValueChange(HasValue.ValueChangeEvent<Integer> event) {
        switch (event.getValue()) {
            case 1:
                checkTraceTwinColumn.setEnabled(false);
                break;
            case 2:
                checkTraceTwinColumn.setEnabled(true);
                break;
            case 3:
                checkTraceTwinColumn.setEnabled(true);
                break;
            default:
                checkTraceTwinColumn.setEnabled(false);
                break;
        }
    }

    private void addTypeNotifications() {
        mapTypeNotifications.put("Нет", 1);
        mapTypeNotifications.put("Только менеджеру", 2);
        mapTypeNotifications.put("Подписчику вакансии", 3);
        mapTypeNotifications.put("Подписчику кандидата", 4);
        mapTypeNotifications.put("Определенным адресам (список)", 5);
        mapTypeNotifications.put("Всем", 6);

        radioButtonTypeNotifications.setOptionsMap(mapTypeNotifications);
    }

    private void addRadioButtonAddType() {
        mapAddType.put("Data", 1);
        mapAddType.put("String", 2);
        mapAddType.put("Integer", 3);

        radioButtonAddType.setOptionsMap(mapAddType);
    }

    @Subscribe("radioButtonAddType")
    public void onRadioButtonAddTypeValueChange(HasValue.ValueChangeEvent<Integer> event) {
        if (radioButtonAddType.getValue() == 1) {
            checkBoxSetDefaultDateTime.setEnabled(true);
        } else {
            checkBoxSetDefaultDateTime.setEnabled(false);
        }
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
/*        if( !PersistenceHelper.isNew( getEditedEntity() )) {
            // embeddedPict.setIcon(getEditedEntity().getPic());
        } */

        if (!PersistenceHelper.isNew(getEditedEntity()))
            labelItercationName.setValue(getEditedEntity().getIterationName());

        labelWarning.setValue(messages.getMessage(getClass(), "msgForAdmin"));

        disablePicAndButton();

        setDisableElements();

        if (radioButtonTypeNotifications.getValue() != null)
            lookupFieldEmails.setEditable(radioButtonTypeNotifications.getValue().equals(5));
        else
            lookupFieldEmails.setEditable(false);

        needSendEmail();
    }

    private void needSendEmail() {
        neetToSendEmailCheckBox.addValueChangeListener(event -> {
            textEmailToSendRichTextArea.setEnabled(false);

            if(event.getValue() != null) {
                if(event.getValue()) {
                    textEmailToSendRichTextArea.setEnabled(true);
                } else {
                    textEmailToSendRichTextArea.setEnabled(false);
                }
            }
        });

        HashMap<String, String> emailKeys = emailGenerationService.generateKeys();
        String retStr = "";

        for(Map.Entry<String, String> entry : emailKeys.entrySet()) {
            retStr = retStr + entry.getKey() + " - " + entry.getValue() + "<br>";
        }

        commentKeysRichTextArea.setValue(retStr);

    }

    @Subscribe("radioButtonTypeNotifications")
    public void onRadioButtonTypeNotificationsValueChange(HasValue.ValueChangeEvent event) {
        lookupFieldEmails.setEditable(radioButtonTypeNotifications.getValue().equals(5));
    }

    @Subscribe("checkBoxFlag")
    public void onCheckBoxFlagValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (checkBoxFlag.getValue()) {
            checkBoxCallDialog.setValue(false);
        }

        radioButtonAddType.setRequired(checkBoxFlag.getValue());
        textFieldCaption.setRequired(checkBoxFlag.getValue());

        setDisableElements();
    }

    protected void setDisableElements() {
        textFieldCaption.setEditable(checkBoxFlag.getValue());
//        textFieldDBFieldName.setEditable( checkBoxFlag.getValue() );
        radioButtonAddType.setEditable(checkBoxFlag.getValue());
    }

    @Subscribe("checkBoxCallDialog")
    public void onCheckBoxCallDialogValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (checkBoxCallDialog.getValue())
            checkBoxFlag.setValue(false);

        disablePicAndButton();
    }

    private void disablePicAndButton() {
        if (!checkBoxCallDialog.getValue()) {
            textFieldCallButtonText.setEditable(false);
            textFieldCallForm.setEditable(false);
            // iteractionFieldPic.setEditable( false );
        } else {
            textFieldCallButtonText.setEditable(true);
            textFieldCallForm.setEditable(true);
            // iteractionFieldPic.setEditable( true );
        }
    }

    @Subscribe("iteractionFieldPic")
    public void onIteractionFieldPicValueChange(HasValue.ValueChangeEvent<String> event) {
        String iconURL = iteractionFieldPic.getValue();
        embeddedPict.setSource(ThemeResource.class).setPath(iconURL);
    }

    @Inject
    private LookupPickerField<Iteraction> iteractionTreeField;
    @Inject
    private TextField<String> iterationNameField;

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setMandatoryIteraction(false);
        }
    }

    @Subscribe("iteractionCheckBoxMandatory")
    public void onIteractionCheckBoxMandatoryValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (getEditedEntity().getMandatoryIteraction()) {
            iteractionTreeField.setEditable(false);
            iterationNameField.setEditable(false);
        } else {
            iteractionTreeField.setEditable(true);
            iterationNameField.setEditable(true);
        }
    }
}