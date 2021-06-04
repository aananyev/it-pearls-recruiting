package com.company.itpearls.web.screens.candidatecv;

import com.company.itpearls.core.PdfParserService;
import com.company.itpearls.entity.*;
import com.company.itpearls.web.screens.skilltree.SkillTreeBrowseCheck;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.web.AppUI;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.extractor.POITextExtractor;
import org.apache.poi.ooxml.extractor.ExtractorFactory;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.xmlbeans.XmlException;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import java.io.*;
import java.util.*;

@UiController("itpearls_CandidateCV.edit")
@UiDescriptor("candidate-cv-edit.xml")
@EditedEntityContainer("candidateCVDc")
@LoadDataBeforeShow
public class CandidateCVEdit extends StandardEditor<CandidateCV> {

    @Inject
    private UserSession userSession;
    @Inject
    private TextField<String> textFieldIOriginalCV;

    private WebBrowserTools webBrowserTools;
    @Inject
    private TextField<String> textFieldITPearlsCV;
    @Inject
    private FileUploadField fileOriginalCVField;
    @Inject
    private Notifications notifications;
    @Inject
    private LookupPickerField<OpenPosition> candidateCVFieldOpenPosition;
    @Inject
    private LookupPickerField<JobCandidate> candidateField;
    @Inject
    private Dialogs dialogs;
    @Inject
    private RichTextArea letterRichTextArea;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private Link itpearlsCVLink;
    @Inject
    private Link originalCVLink;
    @Inject
    private RichTextArea questionLetterRichTextArea;
    @Inject
    private RichTextArea candidateCVRichTextArea;
    @Inject
    private PdfParserService pdfParserService;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private TextArea<String> quoteTextArea;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private FileUploadField fileCVField;

    @Subscribe
    public void onInit(InitEvent event) {
        AppUI ui = AppBeans.get(AppUI.class);
        webBrowserTools = ui.getWebBrowserTools();

/*        fileOriginalCVField.addFileUploadSucceedListener(uploadSucceedEvent -> {
            File loadFile = fileUploadingAPI.getFile(fileOriginalCVField.getFileId());
            String textResume = "";

            textResume = parsePdfCV(loadFile);
            candidateCVRichTextArea.setValue(textResume);

            FileDescriptor fd = fileOriginalCVField.getFileDescriptor();

            try {
                fileUploadingAPI.putFileIntoStorage(fileOriginalCVField.getFileId(), fd);
                dataManager.commit(fd);

                notifications.create()
                        .withType(Notifications.NotificationType.TRAY)
                        .withCaption("Uploaded file: " + fileOriginalCVField.getFileName())
                        .show();
            } catch (FileStorageException e) {
                throw new RuntimeException("Error saving file to FileStorage", e);
            }

            rescanResume();

        }); */

        fileOriginalCVField.addFileUploadErrorListener(uploadErrorEvent ->
                notifications.create()
                        .withCaption("Ошибка загрузки файла " + fileOriginalCVField.getFileName())
                        .show());


        fileCVField.addFileUploadErrorListener(uploadErrorEvent ->
                notifications.create()
                        .withCaption("Ошибка загрузки файла " + fileCVField.getFileName())
                        .show());
    }

    @Subscribe("textFieldIOriginalCV")
    public void onTextFieldIOriginalCVValueChange(HasValue.ValueChangeEvent<String> event) {
        if (textFieldIOriginalCV.getValue() != null) {
            originalCVLink.setUrl(textFieldIOriginalCV.getValue());
            originalCVLink.setVisible(true);
        } else
            originalCVLink.setVisible(false);
    }

    @Subscribe("textFieldITPearlsCV")
    public void onTextFieldITPearlsCVValueChange(HasValue.ValueChangeEvent<String> event) {
        if (textFieldITPearlsCV.getValue() != null) {
            itpearlsCVLink.setUrl(textFieldITPearlsCV.getValue());
            itpearlsCVLink.setVisible(true);
        } else {
            itpearlsCVLink.setVisible(false);
        }
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (textFieldIOriginalCV.getValue() != null) {
            originalCVLink.setUrl(textFieldIOriginalCV.getValue());
            originalCVLink.setVisible(true);
        } else {
            originalCVLink.setVisible(false);
        }

        if (textFieldITPearlsCV.getValue() != null) {
            itpearlsCVLink.setVisible(true);
        } else {
            itpearlsCVLink.setVisible(false);
        }

        quoteTextArea.setValue(messageBundle.getMessage("msgSalesCV"));
    }

    @Subscribe
    public void onAfterShow1(AfterShowEvent event) {
        if (candidateCVRichTextArea.getValue() != null)
            rescanResume();
    }

    private void setTemplateLetter() {
        String templateLetter = "";

        if (getEditedEntity().getLetter() == null) {
            if (candidateCVFieldOpenPosition.getValue() != null) {
                if(candidateCVFieldOpenPosition.getValue().getProjectName() != null) {
                    if(candidateCVFieldOpenPosition.getValue().getProjectName().getProjectDepartment() != null) {
                        if (candidateCVFieldOpenPosition
                                .getValue()
                                .getProjectName()
                                .getProjectDepartment()
                                .getTemplateLetter() != null) {

                            templateLetter = templateLetter
                                    + candidateCVFieldOpenPosition.getValue().getProjectName().getProjectDepartment().getTemplateLetter()
                                    + "\n<br>";
                        }
                    }
                }
            }

            if (candidateCVFieldOpenPosition.getValue() != null) {
                if (candidateCVFieldOpenPosition.getValue().getProjectName().getTemplateLetter() != null) {
                    templateLetter = templateLetter
                            + candidateCVFieldOpenPosition.getValue().getProjectName().getTemplateLetter()
                            + "\n<br>";
                }
            }

            if (candidateCVFieldOpenPosition.getValue() != null) {
                if (candidateCVFieldOpenPosition.getValue().getTemplateLetter() != null) {
                    templateLetter = templateLetter
                            + candidateCVFieldOpenPosition.getValue().getTemplateLetter()
                            + "\n<br>";
                }
            }

            letterRichTextArea.setValue(templateLetter);

            if (!templateLetter.equals("")) {
                questionLetterRichTextArea.setVisible(true);
                questionLetterRichTextArea.setValue(templateLetter);
            } else {
                questionLetterRichTextArea.setVisible(false);
            }
        }
    }

    @Subscribe("candidateCVFieldOpenPosition")
    public void onCandidateCVFieldOpenPositionValueChange(HasValue.ValueChangeEvent<OpenPosition> event) {
        setTemplateLetter();
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setDatePost(new Date());

            getEditedEntity().setOwner(userSession.getUser());
        }
    }

    @Subscribe("tabFiles")
    public void onTabFilesLayoutClick(LayoutClickNotifier.LayoutClickEvent event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            dialogs.createOptionDialog()
                    .withCaption("Warning")
                    .withMessage("Сохранить резюме кандидата?")
                    .withActions(
                            new DialogAction(DialogAction.Type.YES,
                                    Action.Status.PRIMARY).withHandler(y -> {
                                this.commitChanges();
                            }),
                            new DialogAction(DialogAction.Type.NO)
                    )
                    .show();
        }
    }



/*
    @Subscribe("fileOriginalCVField")
    public void onFileOriginalCVFieldFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {
        File parseFile = fileUploadingAPI.getFile(fileOriginalCVField.getFileId());
        String textResume = "";

        textResume = parsePdfCV(parseFile);
        candidateCVRichTextArea.setValue(textResume);

        File loadFile = fileUploadingAPI.getFile(fileOriginalCVField.getFileId());
        FileDescriptor fd = fileOriginalCVField.getFileDescriptor();

        CommitContext commitContext = new CommitContext();
        commitContext.addInstanceToCommit(getEditedEntity());
        commitContext.addInstanceToCommit(fd);


        try {
            fileUploadingAPI.putFileIntoStorage(fileOriginalCVField.getFileId(), fd);
            dataManager.commit(commitContext);

            notifications.create()
                    .withType(Notifications.NotificationType.TRAY)
                    .withCaption("Uploaded file: " + fileOriginalCVField.getFileName())
                    .show();
        } catch (FileStorageException e) {
            throw new RuntimeException("Error saving file to FileStorage", e);
        }

        rescanResume();
    } */

    @Subscribe("fileOriginalCVField")
    public void onFileOriginalCVFieldFileUploadError(UploadField.FileUploadErrorEvent event) {
        notifications.create()
                .withCaption("Ошибка загрузки файла в хранилище.")
                .show();
    }

    private String parsePdfCV(File fileName) {
        String parsedText = "";

        if (fileOriginalCVField.getFileName().endsWith(".pdf")) {

            try {
                PDFParser parser = new PDFParser(new RandomAccessFile(fileName, "r"));
                parser.parse();

                COSDocument cosDoc = parser.getDocument();
                PDFTextStripper pdfStripper = new PDFTextStripper();
                PDDocument pdDoc = new PDDocument(cosDoc);
                parsedText = pdfStripper.getText(pdDoc).replace("\n", "<br>");

            } catch (IOException e) {
                notifications.create()
                        .withType(Notifications.NotificationType.WARNING)
                        .withCaption("ВНИМАНИЕ!")
                        .withDescription("Ошибка расшифровки PDF резюме.\nЗагрузите PDF/DOC/DOCX для расшифровки.")
                        .show();

                e.printStackTrace();
            }
        } else if (fileOriginalCVField.getFileName().endsWith(".docx")) {
            try {
                InputStream fis = new FileInputStream(fileName);

                XWPFDocument doc = new XWPFDocument(fis);
                POITextExtractor extractor = new XWPFWordExtractor(doc);
                parsedText = extractor.getText();
            } catch (IOException | IncompatibleClassChangeError e) {
                notifications.create()
                        .withType(Notifications.NotificationType.WARNING)
                        .withCaption("ВНИМАНИЕ!")
                        .withDescription("Ошибка расшифровки DOCX резюме.\nЗагрузите PDF для расшифровки.")
                        .show();

                e.printStackTrace();
            }

        } else if (fileOriginalCVField.getFileName().endsWith(".doc")) {

            try {
                POITextExtractor extractor = ExtractorFactory.createExtractor(fileName);
                parsedText = extractor.getText();
            } catch (IOException | NoClassDefFoundError | XmlException | OpenXML4JException e) {
                notifications.create()
                        .withType(Notifications.NotificationType.WARNING)
                        .withCaption("ВНИМАНИЕ!")
                        .withDescription("Ошибка расшифровки DOC резюме.\nЗагрузите PDF для расшифровки.")
                        .show();

                e.printStackTrace();
            }
        }


        return parsedText.replace("\n", "<br>");
    }

    @Install(to = "skillTreesTable.wikiPage", subject = "columnGenerator")
    private Object skillTreesTableWikiPageColumnGenerator(DataGrid.ColumnGeneratorEvent<SkillTree> event) {
        Link retLink = uiComponents.create(Link.NAME);

        if (event.getItem().getWikiPage() != null) {
            retLink.setUrl(event.getItem().getWikiPage());
            retLink.setTarget("_blank");
            retLink.setHeightAuto();
            retLink.setWidthAuto();
            retLink.setAlignment(Component.Alignment.BOTTOM_LEFT);
            retLink.setCaption(event.getItem().getWikiPage());
            retLink.setVisible(true);
        }

        return retLink;
    }

    public void setParameter(CandidateCV entity) {
        candidateCVFieldOpenPosition.setValue(entity.getToVacancy());
        candidateField.setValue(entity.getCandidate());
    }

    public List<SkillTree> rescanResume() {

        if (candidateCVRichTextArea.getValue() != null) {
            String inputText = Jsoup.parse(candidateCVRichTextArea.getValue()).text();
            List<SkillTree> skillTrees = pdfParserService.parseSkillTree(inputText);

            Set<SkillTree> st = new HashSet<>(skillTrees);
            skillTrees.clear();
            skillTrees.addAll(st);

            getEditedEntity().setSkillTree(skillTrees);

            return skillTrees;
        } else {
            return null;
        }
    }

    public void checkSkillFromJD() {
        List<SkillTree> skillTrees = rescanResume();
        String inputText = Jsoup.parse(candidateCVFieldOpenPosition.getValue().getComment()).text();
        List<SkillTree> skillTreesFromJD = pdfParserService.parseSkillTree(inputText);

        if (candidateCVFieldOpenPosition.getValue() != null) {
            SkillTreeBrowseCheck s = screenBuilders.screen(this)
                    .withScreenClass(SkillTreeBrowseCheck.class)
                    .build();
            s.setCandidateCVSkills(skillTrees);
            s.setOpenPositionSkills(skillTreesFromJD);
            s.setTitle(candidateCVFieldOpenPosition.getValue().getVacansyName());

            s.show();
        } else {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("ВНИМАНИЕ!")
                    .withDescription("Для проверки навыков кандидата по резюме " +
                            "\nнеобходимозаполнить поле \"Вакансия\".")
                    .show();
        }
    }

    @Install(to = "skillTreesTable.isComment", subject = "columnGenerator")
    private Object skillTreesTableIsCommentColumnGenerator(DataGrid.ColumnGeneratorEvent<SkillTree> event) {
        if (event.getItem().getComment() != null && !event.getItem().equals("")) {
            return CubaIcon.PLUS_CIRCLE;
        } else {
            return CubaIcon.MINUS_CIRCLE;
        }
    }

    @Install(to = "skillTreesTable.isComment", subject = "styleProvider")
    private String skillTreesTableIsCommentStyleProvider(SkillTree skillTree) {
        if (skillTree.getComment() != null && !skillTree.equals("")) {
            return "pic-center-large-green";
        } else {
            return "pic-center-large-red";
        }
    }

    @Install(to = "candidateField", subject = "optionImageProvider")
    private Resource candidateFieldOptionImageProvider(JobCandidate jobCandidate) {

        if (jobCandidate.getFileImageFace() != null) {
            Image image = uiComponents.create(Image.NAME);
            image.setStyleName("round-photo");
            FileDescriptorResource resource = image.createResource(FileDescriptorResource.class)
                    .setFileDescriptor(jobCandidate.getFileImageFace());
            return resource;
        }

        return null;
    }

    public void rescanCV() {
        rescanResume();
    }
}