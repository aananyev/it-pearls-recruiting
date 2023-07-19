package com.company.itpearls.web.screens.laboragreement;

import com.company.itpearls.entity.Company;
import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.entity.LaborAgeementType;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.LaborAgreement;

import javax.inject.Inject;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@UiController("itpearls_LaborAgreement.edit")
@UiDescriptor("labor-agreement-edit.xml")
@EditedEntityContainer("laborAgreementDc")
@LoadDataBeforeShow
public class LaborAgreementEdit extends StandardEditor<LaborAgreement> {
    @Inject
    private CheckBox parpetualAgreementCheckBox;
    @Inject
    private DateField<Date> laborAgreementEndDateField;
    @Inject
    private CollectionLoader<LaborAgreement> additionalAgreementDl;
    @Inject
    private SuggestionPickerField<JobCandidate> candidateField;
    @Inject
    private SuggestionPickerField<Company> companyField;
    @Inject
    private LookupPickerField<LaborAgeementType> laborAgreementTypeField;
    @Inject
    private Button commitAndCloseBtn;
    @Inject
    private TextField<String> laborNameTextField;
    @Inject
    private RichTextArea commentField;
    @Inject
    private SuggestionPickerField<Company> companyFromSuggestPickerField;
    @Inject
    private HBoxLayout agreementsHeaderHBox;
    @Inject
    private GroupBoxLayout candidateCompanyGroupBox;
    @Inject
    private GroupBoxLayout legalAgreementGroupBox;
    @Inject
    private GroupBoxLayout legalAgreementCommentGroupBox;
    @Inject
    private CheckBox agreementClosedCheckBox;
    @Inject
    private DateField<Date> laborAgreementDateField;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private DataGrid<LaborAgreement> additionalAgreementDataGrid;
    @Inject
    private UiComponents uiComponents;

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if(parpetualAgreementCheckBox.getValue() != null) {
                laborAgreementEndDateField.setEnabled(!parpetualAgreementCheckBox.getValue());
        }

        setAdditionalLaborAgreementDataGrid();
    }

    private void setAdditionalLaborAgreementDataGrid() {
        additionalAgreementDl.setParameter("additionalLaborAgreement", getEditedEntity());
        additionalAgreementDl.load();
    }

    @Subscribe
    public void onInit(InitEvent event) {
        parpetualAgreementCheckBox.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                laborAgreementEndDateField.setEnabled(!parpetualAgreementCheckBox.getValue());
            }
        });

        laborAgreementEndDateField.addValidator(value -> {
            if (value != null) {
                if (laborAgreementDateField.getValue() != null) {
                    if (laborAgreementDateField.getValue().after(value)) {
                        throw new ValidationException(messageBundle.getMessage("msgAgreementEndDateError"));
                    }
                }
            }

            if (value == null && parpetualAgreementCheckBox.getValue() == false) {
                throw new ValidationException(messageBundle.getMessage("msgNeedAddEndDateAgreement"));
            }
        });

        laborAgreementTypeField.addValueChangeListener(e -> setVisibilityFields());
    }

    @Subscribe("laborAgreementTypeField")
    public void onLaborAgreementTypeFieldValueChange(HasValue.ValueChangeEvent<LaborAgeementType> event) {
        setVisibilityFields();
    }

    private void setVisibilityFields() {
        if (laborAgreementTypeField.getValue() != null) {
            if (laborAgreementTypeField.getValue().getEmployeeOrcompany() != null) {
                if (laborAgreementTypeField.getValue().getEmployeeOrcompany() == AgreementType.EPLOYEE) {
                    candidateCompanyGroupBox.setEnabled(true);
                    candidateField.setVisible(true);
                    candidateField.setRequired(true);
                    companyField.setVisible(false);
                    companyField.setRequired(false);
                    commitAndCloseBtn.setEnabled(true);
                    laborNameTextField.setEnabled(true);
                    commentField.setEnabled(true);
                    commentField.setEnabled(true);
                    companyFromSuggestPickerField.setEnabled(true);
                    agreementsHeaderHBox.setEnabled(true);
                    legalAgreementGroupBox.setEnabled(true);
                    legalAgreementCommentGroupBox.setEnabled(true);
                } else {
                    candidateCompanyGroupBox.setEnabled(true);
                    candidateField.setVisible(false);
                    candidateField.setRequired(false);
                    companyField.setVisible(true);
                    companyField.setRequired(true);
                    commitAndCloseBtn.setEnabled(true);
                    laborNameTextField.setEnabled(true);
                    commentField.setEnabled(true);
                    companyFromSuggestPickerField.setEnabled(true);
                    agreementsHeaderHBox.setEnabled(true);
                    legalAgreementGroupBox.setEnabled(true);
                    legalAgreementCommentGroupBox.setEnabled(true);
                }
            } else {
                candidateCompanyGroupBox.setEnabled(false);
                candidateField.setVisible(true);
                candidateField.setVisible(true);
                candidateField.setRequired(false);
                companyField.setVisible(true);
                companyField.setRequired(false);
                commitAndCloseBtn.setEnabled(false);
                laborNameTextField.setEnabled(false);
                commentField.setEnabled(false);
                companyFromSuggestPickerField.setEnabled(false);
                agreementsHeaderHBox.setEnabled(false);
                legalAgreementGroupBox.setEnabled(false);
                legalAgreementCommentGroupBox.setEnabled(false);
            }
        } else {
            candidateCompanyGroupBox.setEnabled(false);
            candidateField.setVisible(true);
            candidateField.setRequired(false);
            companyField.setVisible(true);
            companyField.setRequired(false);
            commitAndCloseBtn.setEnabled(false);
            laborNameTextField.setEnabled(false);
            commentField.setEnabled(false);
            companyFromSuggestPickerField.setEnabled(false);
            agreementsHeaderHBox.setEnabled(false);
            legalAgreementGroupBox.setEnabled(false);
            legalAgreementCommentGroupBox.setEnabled(false);
        }
    }

    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        if (agreementClosedCheckBox.getValue() == null) {
            agreementClosedCheckBox.setValue(false);
        }

        if (parpetualAgreementCheckBox.getValue() == null) {
            parpetualAgreementCheckBox.setValue(false);
            laborAgreementEndDateField.setValue(null);
        }
    }

    public void newAdditionalAgreement() {
        screenBuilders.editor(LaborAgreement.class, this)
                .withScreenClass(LaborAgreementEdit.class)
                .newEntity()
                .withInitializer(e -> {
                    e.setClosed(false);
                    e.setAgreementName(messageBundle.getMessage("msgAdditionalAgreementFor")
                            + ": "
                            + laborNameTextField.getValue());
                    e.setAdditionalAgreement(true);
                    e.setCustomerCompany(companyField.getValue());
                    e.setLaborAgreementType(laborAgreementTypeField.getValue());
                    e.setEmployeeOrCustomer(laborAgreementTypeField.getValue().getEmployeeOrcompany());
                    e.setJobCandidate(candidateField.getValue());
                    e.setAdditionalLaborAgreement(getEditedEntity());
                    e.setLegalEntityFrom(companyFromSuggestPickerField.getValue());
                    e.setOpenPositions(getEditedEntity().getOpenPositions());
                })
                .withAfterCloseListener(e -> {
                    additionalAgreementDataGrid.repaint();
                })
                .build()
                .show();
    }

    @Install(to = "additionalAgreementDataGrid.active", subject = "columnGenerator")
    private Component additionalAgreementDataGridActiveColumnGenerator(DataGrid.ColumnGeneratorEvent<LaborAgreement> event) {
        return perhapsColumnGenerator(event.getItem());
    }

    private boolean checkAgreementEndDate(LaborAgreement entity) {
        Date currentDate = new Date();

        if (entity.getClosed() != null) {
            if (entity.getClosed()) {
                return false;
            }
        }

        if (currentDate.after(entity.getAgreementDate()) && entity.getPerpetualAgreement()) {
            return true;
        }

        if (currentDate.after(entity.getAgreementDate()) && currentDate.before(entity.getAgreementEndDate())) {
            return true;
        }

        return false;
    }

    public Component perhapsColumnGenerator(LaborAgreement entity) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();
        retHBox.setHeightFull();

        Label retLabel = uiComponents.create(Label.class);

        retLabel.setWidthAuto();
        retLabel.setHeightAuto();
        retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);

        if (checkAgreementEndDate(entity)) {
            retLabel.setIconFromSet(CubaIcon.PLUS_CIRCLE);
            retLabel.setStyleName("open-position-pic-center-large-green");
        } else {
            retLabel.setIconFromSet(CubaIcon.MINUS_CIRCLE);
            retLabel.setStyleName("open-position-pic-center-large-red");
        }

        retHBox.add(retLabel);

        return retHBox;
    }

    @Install(to = "additionalAgreementDataGrid.agreementEndDate", subject = "columnGenerator")
    private Component additionalAgreementDataGridAgreementEndDateColumnGenerator(DataGrid.ColumnGeneratorEvent<LaborAgreement> event) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();
        retHBox.setHeightFull();

        Label retLabel = uiComponents.create(Label.class);

        retLabel.setWidthAuto();
        retLabel.setHeightAuto();
        retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);

        if (event.getItem().getClosed() != null) {
            if (event.getItem().getClosed()) {
                retLabel.setValue(messageBundle.getMessage("msgClosed"));
            } else {
                if (event.getItem().getPerpetualAgreement() != null) {
                    if (event.getItem().getPerpetualAgreement()) {
                        retLabel.setValue(messageBundle.getMessage("msgPerpetualAgreement"));
                    } else {
                        if (event.getItem().getAgreementEndDate() != null) {
                            retLabel.setValue(event.getItem().getAgreementEndDate());
                        } else {
                            retLabel.setValue(messageBundle.getMessage("msgUndefined"));
                        }
                    }
                }
            }
        }

        retHBox.add(retLabel);

        return retHBox;
    }



    /* private void setEmployeeOrCustomerCheckBox() {
        Map<String, Integer> employeeOrCustomerMap = new LinkedHashMap<>();
        employeeOrCustomerMap.put("Сотрудником", 1);
        employeeOrCustomerMap.put("Клиентом", 2);

        employeeOrCustomerRadioButtonGroup.setOptionsMap(employeeOrCustomerMap);
    }

    @Subscribe("employeeOrCustomerRadioButtonGroup")
    public void onEmployeeOrCustomerRadioButtonGroupValueChange(HasValue.ValueChangeEvent<Integer> event) {
        switch (event.getValue()) {
            case 1:
                // компания иили сотрудник
                candidateField.setVisible(true);
                customerCompanyLookupPickerField.setVisible(false);

                legalEntityEmployeeLookupPickerField.setVisible(true);
                break;
            case 2:
                // компания иили сотрудник
                candidateField.setVisible(false);
                customerCompanyLookupPickerField.setVisible(true);

                legalEntityEmployeeLookupPickerField.setVisible(false);

                break;
            default:
                break;
        }
    } */




}