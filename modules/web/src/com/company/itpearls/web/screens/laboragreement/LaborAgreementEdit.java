package com.company.itpearls.web.screens.laboragreement;

import com.company.itpearls.entity.*;
import com.company.itpearls.web.screens.SelectedCloseAction;
import com.company.itpearls.web.screens.candidatecv.SelectRenderedImagesFromList;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.extractor.POITextExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import javax.inject.Inject;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

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
    @Inject
    private FileLoader fileLoader;
    @Inject
    private Notifications notifications;
    @Inject
    private RichTextArea agreementTextRichTextArea;
    @Inject
    private Dialogs dialogs;

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if (parpetualAgreementCheckBox.getValue() != null) {
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
        getEditedEntity().setEmployeeOrCustomer(event.getValue().getEmployeeOrcompany());
    }


    private UUID originalFileId;
    private FileDescriptor originalFileCVDescriptor;
    private FileDescriptor fileDescriptor;
    private static final String EXTENSION_PDF = "pdf";
    private static final String EXTENSION_DOC = "doc";
    private static final String EXTENSION_DOCX = "docx";
    private String[] breakLine = {"<br>", "<br/>", "<br />", "<p>", "</p>", "</div>"};
    final String brHtml = breakLine[0];
    final String brTemp = "ШbrШ";


    public FileDescriptor getFileDescriptor() {
        return fileDescriptor;
    }

    @Subscribe("fileAgreementFileUpload")
    public void onFileAgreementFileUploadValueChange(HasValue.ValueChangeEvent<FileDescriptor> event) {
        originalFileCVDescriptor = event.getValue();
        originalFileId = event.getValue().getId();
    }

    @Subscribe("fileAgreementFileUpload")
    public void onFileAgreementFileUploadFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {
        UUID uuidFile = originalFileId;
        FileDescriptor fileDescriptor = originalFileCVDescriptor;
        String textAgreement = "";

        try {
            InputStream inputStream = fileLoader.openStream(fileDescriptor);

            if (fileDescriptor.getExtension().equals(EXTENSION_PDF)) {
                textAgreement = parsePdfCV(inputStream);
            } else if (fileDescriptor.getExtension().equals(EXTENSION_DOC)) {
                notifications.create(Notifications.NotificationType.WARNING)
                        .withDescription("Функция загрузки ." + EXTENSION_DOC + " пока не реализована.")
                        .withCaption(messageBundle.getMessage("msgWarning"))
                        .show();


            } else if (fileDescriptor.getExtension().equals(EXTENSION_DOCX)) {

                XWPFDocument doc = new XWPFDocument(inputStream);
                POITextExtractor extractor = new XWPFWordExtractor(doc);
                textAgreement = extractor.getText().replaceAll("\n", breakLine[0]);
            }
        } catch (FileStorageException | IOException | IllegalArgumentException e) {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withDescription("Ошибка распознавания документа " + fileDescriptor.getName())
                    .show();

            throw new RuntimeException(e);
        }

        if (textAgreement != null) {
            if (agreementTextRichTextArea.getValue() != null) {
                String finalTextAgreement = textAgreement;
                dialogs.createOptionDialog(Dialogs.MessageType.CONFIRMATION)
                        .withMessage(messageBundle.getMessage("msgReplaceTextAgreement"))
                        .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY)
                                        .withHandler(e -> {
                                            agreementTextRichTextArea.setValue(finalTextAgreement.replaceAll("\n", breakLine[0]));
                                        }),
                                new DialogAction(DialogAction.Type.NO))
                        .show();
            }
        }
    }

    private String parsePdfCV(InputStream inputStream) {
        String parsedText = "";

        try {
            RandomAccessRead rad = new RandomAccessReadBuffer(inputStream);

            PDFParser parser = new PDFParser(rad);
            PDFTextStripper pdfStripper = new PDFTextStripper();
            PDDocument pdDoc = parser.parse();
            parsedText = pdfStripper.getText(pdDoc).replace("\n", "<br>");
        } catch (IOException e) {
            e.printStackTrace();

            return null;
        }

        return parsedText;
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