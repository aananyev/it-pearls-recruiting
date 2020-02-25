package com.company.itpearls.web.screens.iteraction;

import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Iteraction;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

@UiController("itpearls_Iteraction.edit")
@UiDescriptor("iteraction-edit.xml")
@EditedEntityContainer("iteractionDc")
@LoadDataBeforeShow
public class IteractionEdit extends StandardEditor<Iteraction> {
    @Inject
    private Embedded embeddedPict;
    @Inject
    private TextField<String> numberField;
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
    private TextField textFieldDBFieldName;

    @Subscribe
    public void onInit(InitEvent event) {
        Map<String, Integer> mapAddType = new LinkedHashMap<>();
        mapAddType.put("Data", 1);
        mapAddType.put("String", 2);
        mapAddType.put("Integer", 3);

        radioButtonAddType.setOptionsMap(mapAddType);
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if( !PersistenceHelper.isNew( getEditedEntity() )) {
            embeddedPict.setIcon(getEditedEntity().getPic());
        }

        disablePicAndButton();

        setDisableElements();
    }

    @Subscribe("checkBoxFlag")
    public void onCheckBoxFlagValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        setDisableElements();
    }

    protected void setDisableElements() {
        textFieldCaption.setEditable( checkBoxFlag.getValue() );
        textFieldDBFieldName.setEditable( checkBoxFlag.getValue() );
        radioButtonAddType.setEditable( checkBoxFlag.getValue() );
    }

    

    @Subscribe("checkBoxCallDialog")
    public void onCheckBoxCallDialogValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        disablePicAndButton();
    }

    private void disablePicAndButton() {
        if( !checkBoxCallDialog.getValue() ) {
            textFieldCallButtonText.setEditable( false );
            textFieldCallForm.setEditable( false );
            iteractionFieldPic.setEditable( false );
        } else {
            textFieldCallButtonText.setEditable( true );
            textFieldCallForm.setEditable( true );
            iteractionFieldPic.setEditable( true );
        }
    }

    @Subscribe("iteractionFieldPic")
    public void onIteractionFieldPicValueChange(HasValue.ValueChangeEvent<String> event) {
        embeddedPict.setIcon( getEditedEntity().getPic() );
    }

    @Inject
    private LookupPickerField<Iteraction> iteractionTreeField;
    @Inject
    private TextField<String> iterationNameField;

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
       if(PersistenceHelper.isNew(getEditedEntity())) {
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