package com.company.itpearls.web.screens.candidatecv;

import com.company.itpearls.core.ParseCVService;
import com.company.itpearls.core.PdfParserService;
import com.company.itpearls.entity.*;
import com.company.itpearls.web.screens.skilltree.SkillTreeBrowseCheck;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;
import com.haulmont.cuba.security.global.UserSession;
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
import org.slf4j.Logger;

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
    @Inject
    private FileUploadingAPI fileUploadingAPI;
    @Inject
    private RichTextArea cvResomandation;
    @Inject
    private RichTextArea letterRecommendation;
    @Inject
    private InstanceContainer<CandidateCV> candidateCVDc;
    @Inject
    private ParseCVService parseCVService;
    @Inject
    private Label<String> machRegexpFromCV;
    @Inject
    private Logger log;

    @Subscribe
    public void onInit(InitEvent event) {
/*        fileOriginalCVField.addFileUploadSucceedListener(uploadSucceedEvent -> {
            File loadFile = fileUploadingAPI.getFile(fileOriginalCVField.getFileId());
            String textResume = "";

            textResume = parsePdfCV(loadFile);
            candidateCVRichTextArea.setValue(textResume);

            FileDescriptor fd = fileOriginalCVField.getFileDescriptor();
            candidateCVDc.getItem().setOriginalFileCV(fd);

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

        });

        fileOriginalCVField.addFileUploadErrorListener(uploadErrorEvent ->
                notifications.create()
                        .withCaption("Ошибка загрузки файла " + fileOriginalCVField.getFileName())
                        .show());


        fileCVField.addFileUploadErrorListener(uploadErrorEvent ->
                notifications.create()
                        .withCaption("Ошибка загрузки файла " + fileCVField.getFileName())
                        .show());*/
    }

    @Install(to = "candidateCVFieldOpenPosition", subject = "optionIconProvider")
    private String candidateCVFieldOpenPositionOptionIconProvider(OpenPosition openPosition) {
        if (!openPosition.getOpenClose()) {
            return CubaIcon.PLUS_CIRCLE.source();
        } else {
            return CubaIcon.MINUS_CIRCLE.source();
        }
    }

    @Install(to = "candidateCVFieldOpenPosition", subject = "optionStyleProvider")
    private String candidateCVFieldOpenPositionOptionStyleProvider(OpenPosition openPosition) {
        if (!openPosition.getOpenClose()) {
            return "open-position-lookup-field-black";
        } else {
            return "open-position-lookup-field-gray";
        }
    }

    @Subscribe("fileOriginalCVField")
    public void onFileOriginalCVFieldFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {
        File file = fileUploadingAPI.getFile(getEditedEntity().getOriginalFileCV().getId());

        String textResume = "";

        textResume = parsePdfCV(file);
        candidateCVRichTextArea.setValue(textResume);

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

        setCVRecommendation();
        setLetterRecommendation();
    }

    private void setCVRecommendation() {
        String text = "<ol>" +
                "<li>Резюме должно быть информативным и кратким, даже если за плечами 15+ лет опыта. Сфокусируйтесь на трех последних местах работы, это интересует работодателя в первую очередь.</li>" +
                "<li>Включите в резюме только самые ключевые задачи, функциональные обязанности и достижения. Не используйте закрученных словооборотов, составляйте описание тезисно и лаконично.</li>" +
                "<li>Обязательно указывайте ваши успехи (достижения) для каждого места работы. Они должны быть конкретны, измеримы и соответствовать должности.</li>" +
                "<li>Если вы работали на разных проектах, объедините информацию под одним названием «Проектная деятельность», а в описании можете расписать проекты подробнее.</li>" +
                "<li>Указанные в резюме профессиональные и личностные компетенции будут являться словами-маркерами, по которым будущий работодатель сможет вас быстро идентифицировать и соотнести с должностью.</li>" +
                "</ol>";

        cvResomandation.setValue(text);
    }

    private void setLetterRecommendation() {
        String caption = "Структура письма:";
        String text = "<ol>\n" +
                "<li>Представление кандидата, его ФИО, текущей позиции, позиции на которую он претендует в компании партнера или проекте. </li>" +
                "<li>Опыт работы кандидата и значимые компании или проекты, которые должны быть значимы для нашего заказчика. 2-3 предложения не более.</li>" +
                "<li>Управленческие скилы кандидата, такие как навык управления людьми или командами разработчиков, навыки управления проектами и владением методологиями (RAP, Waterfall, Scrum, Agile и т.п.). Или отсутствие скилов ввиду, например, направленостью кандидата (не хочет быть менеджером, хочет быть разработчиком)</li>" +
                "<li>Общая структура мотивации кандидата, а также мотивация смены работы.</li>" +
                "<li>Зарплатные ожидания кандидата, текущий уровень зарплаты. Если зарплатные ожидания завышены, то можно этот пункт либо опустить, либо обосновать почему кандидат хочет повысить планку.</li>" +
                "<li>Личностные качества, которые кандидат показал на собеседовании. Умение и желание работать в команде, активная жизненная позиция, обучаемость и самообучаемость, дополнительное образование, речь, навыки ведения переговоров или общения с заказчиком.</li>" +
                "<li>Матрица компетенций кандидата из резюме.</li>" +
                "</ol>";

        String example = "Пример:\n" +
                "\n" +
                "Уважаемые коллеги, хочу представить Вам Александра Катаева Devops инженера в компании Грид Динамикс в саратовском офисе. Мы предложили Алексею поговорить о сотрудничестве с Вашей командой на позиции DevOps инженера.\n" +
                "\n" +
                "Алексей имеет общий опыт работы более 5 лет на позиции IT Engineer в компании Нет Крекер и с февраля 2018 года DevOps Engineer в компании Grid Dynamics.\n" +
                "\n" +
                "Алексей в настоящее время не находится в активном поиске работы, но у него есть интерес в программировании “железа” и он согласился пообщаться с Вами. Алексей рассматривает возможность своей релокации за рубеж в будущем.\n" +
                "\n" +
                "Алексей в процессе беседы показал себя как сдержанный и вежливый человек. Алексей - надежный сотрудник и не склонен к частой смене работы (время работы в компании Нет Крекер - 5 лет). Алексей командный игрок и комфортно чувствует себя в коллективе единомышленников.\n";

        letterRecommendation.setValue(text);
        letterRecommendation.setCaption(caption);
        letterRecommendation.setDescription(example);
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
                if (candidateCVFieldOpenPosition.getValue().getProjectName() != null) {
                    if (candidateCVFieldOpenPosition.getValue().getProjectName().getProjectDepartment() != null) {
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
                .withType(Notifications.NotificationType.ERROR)
                .withCaption("Ошибка загрузки файла в хранилище.")
                .show();
    }

    private String parsePdfCV(File fileName) {
        String parsedText = "";

        if (fileOriginalCVField.getFileName().endsWith(".pdf")) {

            try {
                try {
                    PDFParser parser = new PDFParser(new RandomAccessFile(fileName, "r"));
                    parser.parse();

                    COSDocument cosDoc = parser.getDocument();
                    PDFTextStripper pdfStripper = new PDFTextStripper();
                    PDDocument pdDoc = new PDDocument(cosDoc);
                    parsedText = pdfStripper.getText(pdDoc).replace("\n", "<br>");
                } catch (NullPointerException e) {
                    notifications.create(Notifications.NotificationType.ERROR)
                            .withCaption("Ошибка загрузки файла " + fileName)
                            .show();

                    log.error("Error", e);
                }

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
            } catch (IOException | IncompatibleClassChangeError | NullPointerException e) {
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
        String inputText = "";

        if(candidateCVFieldOpenPosition.getValue().getComment() != null) {
            inputText = Jsoup.parse(candidateCVFieldOpenPosition.getValue().getComment()).text();
        }

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

    public void resumeRecognition() { /*
        Screen resumeRecognition = screenBuilders.screen(this)
                .withScreenClass(ResumeRecognition.class)
                .build()
                .show(); */

        machRegexpFromCV.setValue(parseCVService.parseEmail(candidateCVRichTextArea.getValue())
                + " "
                + parseCVService.parsePhone(candidateCVRichTextArea.getValue()));

    }
}