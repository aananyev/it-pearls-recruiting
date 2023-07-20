package com.company.itpearls.web.screens.candidatecv;

import com.company.itpearls.core.ParseCVService;
import com.company.itpearls.core.PdfParserService;
import com.company.itpearls.core.WebLoadService;
import com.company.itpearls.entity.*;
import com.company.itpearls.web.screens.SelectedCloseAction;
import com.company.itpearls.web.screens.skilltree.SkillTreeBrowseCheck;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;
import com.haulmont.cuba.security.global.UserSession;
import net.htmlparser.jericho.Source;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.extractor.POITextExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.inject.Inject;
import java.awt.image.*;
import java.io.*;
import java.util.*;

@UiController("itpearls_CandidateCV.edit")
@UiDescriptor("candidate-cv-edit.xml")
@EditedEntityContainer("candidateCVDc")
@LoadDataBeforeShow
public class CandidateCVEdit extends StandardEditor<CandidateCV> {

    private static final String NEED_LETTER_NOTIFICATION = "НЕОБХОДИМО ЗАПОЛНИТЬ ШАБЛОН В СОПРОВОДИТЕЛЬНОМ ПИСЬМЕ " +
            "ПО ТРЕБОВАНИЮ ЗАКАЗЧИКА";
    private static final String WARNING_CAPTION = "ВНИМАНИЕ";
    private static final String EXTENSION_PDF = "pdf";
    private static final String EXTENSION_DOC = "doc";
    private static final String EXTENSION_DOCX = "docx";
    private FileDescriptor fileDescriptor;

    @Inject
    private UserSession userSession;
    @Inject
    private TextField<String> textFieldIOriginalCV;
    @Inject
    private TextField<String> textFieldITPearlsCV;
    @Inject
    private Notifications notifications;
    @Inject
    private LookupPickerField<OpenPosition> candidateCVFieldOpenPosition;
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
    private RichTextArea cvResomandation;
    @Inject
    private RichTextArea letterRecommendation;
    @Inject
    private ParseCVService parseCVService;
    @Inject
    private Label<String> machRegexpFromCV;

    static String referer = "http://www.google.com";
    @Inject
    private Button loadToCVTextArea;
    @Inject
    private WebLoadService webLoadService;
    @Inject
    private Button convertToTextButton;
    @Inject
    private Button showOriginalButon;
    @Inject
    private FileLoader fileLoader;
    @Inject
    private SuggestionPickerField candidateField;
    @Inject
    private Image candidatePic;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private Screens screens;
    @Inject
    private FileUploadField fileImageFaceUpload;
    @Inject
    private FileUploadingAPI fileUploadingAPI;
    @Inject
    private DataManager dataManager;
    @Inject
    private InstanceContainer<CandidateCV> candidateCVDc;
    @Inject
    private Metadata metadata;
    @Inject
    private DataContext dataContext;
    @Inject
    private CheckBox onlyMySubscribeCheckBox;
    @Inject
    private CollectionLoader<OpenPosition> openPositionsDl;
    @Inject
    private CollectionContainer<OpenPosition> openPositionsDc;
    private StringBuffer textResumeStringBuffer = null;
    @Inject
    private Image candidateFaceDefaultImage;

    public FileDescriptor getFileDescriptor() {
        return fileDescriptor;
    }

    @Subscribe
    public void onAfterShow2(AfterShowEvent event) {
        candidateCVFieldOpenPosition.addValueChangeListener(e -> {
            if (e.getValue().getNeedLetter() != null) {
                if (e.getValue().getTemplateLetter() != null) {
                    if (e.getValue().getNeedLetter() && e.getValue().getTemplateLetter() != null) {
                        if (!e.getValue().getTemplateLetter().equals("")) {
                            notifications.create(Notifications.NotificationType.WARNING)
                                    .withDescription(NEED_LETTER_NOTIFICATION)
                                    .withCaption(WARNING_CAPTION)
                                    .withType(Notifications.NotificationType.WARNING)
                                    .show();

                            letterRichTextArea.setValue(letterRichTextArea.getValue()
                                    + "<hr>"
                                    + e.getValue().getTemplateLetter()
                                    + "<hr>");
                        }
                    }
                }
            }
        });
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

    private UUID originalFileId;
    private FileDescriptor originalFileCVDescriptor;

    @Subscribe("fileOriginalCVField")
    public void onFileOriginalCVFieldValueChange(HasValue.ValueChangeEvent<FileDescriptor> event) {
        originalFileCVDescriptor = event.getValue();
        originalFileId = event.getValue().getId();
    }

    public BufferedImage convertRenderedImage(RenderedImage img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        ColorModel cm = img.getColorModel();
        int width = img.getWidth();
        int height = img.getHeight();
        WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        Hashtable properties = new Hashtable();
        String[] keys = img.getPropertyNames();
        if (keys != null) {
            for (int i = 0; i < keys.length; i++) {
                properties.put(keys[i], img.getProperty(keys[i]));
            }
        }
        BufferedImage result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
        img.copyData(raster);
        return result;
    }

    public List<RenderedImage> getImagesFromResources(PDResources resources) throws IOException {
        List<RenderedImage> images = new ArrayList<>();

        for (COSName xObjectName : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(xObjectName);

            if (xObject instanceof PDFormXObject) {
                images.addAll(getImagesFromResources(((PDFormXObject) xObject).getResources()));
            } else if (xObject instanceof PDImageXObject) {
                images.add(((PDImageXObject) xObject).getImage());
            }
        }

        return images;
    }

    @Subscribe
    public void onAfterCommitChanges(AfterCommitChangesEvent event) {
        if(getEditedEntity().getCandidate().getFileImageFace() == null) {
            if (getEditedEntity().getFileImageFace() != null) {
                getEditedEntity().getCandidate().setFileImageFace(getEditedEntity().getFileImageFace());
            }
        }
    }

    @Subscribe("fileOriginalCVField")
    public void onFileOriginalCVFieldFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {
        UUID uuidFile = originalFileId;
        FileDescriptor fileDescriptor = originalFileCVDescriptor;
        String textResume = "";
        List<RenderedImage> images = new ArrayList<>();

        try {
            InputStream inputStream = fileLoader.openStream(fileDescriptor);

            if (fileDescriptor.getExtension().equals(EXTENSION_PDF)) {

                textResume = parsePdfCV(inputStream);
                RandomAccessRead rad = new RandomAccessReadBuffer(fileLoader.openStream(fileDescriptor));

                PDFParser parser = new PDFParser(rad);
                PDFTextStripper pdfStripper = new PDFTextStripper();
                PDDocument pdDoc = parser.parse();

                for (PDPage page : pdDoc.getPages()) {
                    images.addAll(getImagesFromResources(page.getResources()));
                }

                if (images.size() > 0) {
                    SelectRenderedImagesFromList selectRenderedImagesFromList =
                            screens.create(SelectRenderedImagesFromList.class);
                    selectRenderedImagesFromList.setRenderedImages(images);
                    selectRenderedImagesFromList.setCandidateCV(getEditedEntity());

                    selectRenderedImagesFromList.addAfterCloseListener(evnt -> {
                        if (((SelectedCloseAction) evnt.getCloseAction()).getResult()
                                .equals(CandidateCV.SelectedCloseActionType.SELECTED)) {
                            Image selectedImage = selectRenderedImagesFromList
                                    .getSelectedImage();

                            FileDescriptor fd = dataManager
                                    .commit(selectRenderedImagesFromList.getSelectedImageFileDescriptor());

                            try {
                                fileUploadingAPI.putFileIntoStorage(fd.getUuid(), fd);
                            } catch (FileStorageException e) {
                                e.printStackTrace();
                            }

                            FileDescriptorResource resource = selectedImage
                                    .createResource(FileDescriptorResource.class)
                                    .setFileDescriptor(fd);

                            getEditedEntity().setFileImageFace(fd);

                            candidatePic
                                    .setSource(FileDescriptorResource.class)
                                    .setFileDescriptor(fd);
                        }
                    });

                    selectRenderedImagesFromList.show();
                }

//                setVisibleLogo();

            } else if (fileDescriptor.getExtension().equals(EXTENSION_DOC)) {
                /*

                POIFSFileSystem fileSystem = new POIFSFileSystem(inputStream);
                ExtractorFactory.createExtractor(fileSystem);
                POIOLE2TextExtractor oleTextExtractor =
                        ExtractorFactory.createExtractor(fileSystem);
                POITextExtractor[] embeddedExtractors =
                        ExtractorFactory.getEmbededDocsTextExtractors(oleTextExtractor);

                for (POITextExtractor textExtractor : embeddedExtractors) {
                    if (textExtractor instanceof WordExtractor) {
                        WordExtractor wordExtractor = (WordExtractor) textExtractor;
                        String[] paragraphText = wordExtractor.getParagraphText();
                        for (String paragraph : paragraphText) {
                            textResume += paragraph;
                        }
                        // Display the document's header and footer text
                        // System.out.println("Footer text: " + wordExtractor.getFooterText());
                        // System.out.println("Header text: " + wordExtractor.getHeaderText());
                    }
                }

                //    POITextExtractor extractor = createExtractor(inputStream);
                //    textResume = extractor.getText();

                 */
                notifications.create(Notifications.NotificationType.WARNING)
                        .withDescription("Функция загрузки ." + EXTENSION_DOC + " пока не реализована.")
                        .withCaption(WARNING_CAPTION)
                        .show();


            } else if (fileDescriptor.getExtension().equals(EXTENSION_DOCX)) {

                XWPFDocument doc = new XWPFDocument(inputStream);
                POITextExtractor extractor = new XWPFWordExtractor(doc);
                textResume = extractor.getText().replaceAll("\n", breakLine[0]);

                if (textResume != null) {
                    candidateCVRichTextArea.setValue(textResume.replaceAll("\n", breakLine[0]));
                }
            }
        } catch (FileStorageException | IOException | IllegalArgumentException e) {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withDescription("Ошибка распознавания документа " + fileDescriptor.getName())
                    .show();

            throw new RuntimeException(e);
        }

        if (textResume != null) {
            candidateCVRichTextArea.setValue(textResume.replaceAll("\n", breakLine[0]));
        }
    }

    @Subscribe("textFieldIOriginalCV")
    public void onTextFieldIOriginalCVValueChange(HasValue.ValueChangeEvent<String> event) {
        if (textFieldIOriginalCV.getValue() != null) {
            originalCVLink.setUrl(textFieldIOriginalCV.getValue());
            originalCVLink.setVisible(true);
            loadToCVTextArea.setEnabled(true);
        } else {
            originalCVLink.setVisible(false);
            loadToCVTextArea.setEnabled(false);
        }
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

    private void setOnlyMySubscribeCheckBox() {
        onlyMySubscribeCheckBox.setValue(true);
        openPositionsDl.setParameter("subscriber", userSession.getUser());
        openPositionsDl.load();

        if (openPositionsDc.getItems().size() == 0) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withDescription(messageBundle.getMessage("msgNoSubscribeVacansies"))
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .withHideDelayMs(10000)
                    .withType(Notifications.NotificationType.WARNING)
                    .show();
        }

        onlyMySubscribeCheckBox.addValueChangeListener(e -> {
            if (e.getValue()) {
                openPositionsDl.setParameter("subscriber", userSession.getUser());
                openPositionsDl.load();

                if (openPositionsDc.getItems().size() == 0) {
                    notifications.create(Notifications.NotificationType.WARNING)
                            .withCaption(messageBundle.getMessage("msgWarning"))
                            .withDescription(messageBundle.getMessage("msgNoSubscribeVacansies"))
                            .withPosition(Notifications.Position.BOTTOM_RIGHT)
                            .withHideDelayMs(10000)
                            .withType(Notifications.NotificationType.WARNING)
                            .show();
                }
            } else {
                openPositionsDl.removeParameter("subscriber");
                openPositionsDl.load();
            }

        });
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        setOnlyMySubscribeCheckBox();

/*        if (candidateCVRichTextArea.getValue() != null) {
            textResumeStringBuffer = new StringBuffer(candidateCVRichTextArea.getValue());
        } */


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
        candidateCVRichTextArea.setValue(getEditedEntity().getTextCV() != null ?
                getEditedEntity().getTextCV().replaceAll("\n", breakLine[0]) : null);

        candidateCVRichTextArea.addValidator(value -> {
            if (value == null || value.equals("")) {
                convertToTextButton.setEnabled(false);
            } else {
                convertToTextButton.setEnabled(true);
            }
        });

        if (!PersistenceHelper.isNew(getEditedEntity()) &&
                (userSessionSource.getUserSession().getUser().getGroup().getName().equals(StdUserGroup.ACCOUNTING) ||
                        userSessionSource.getUserSession().getUser().getGroup().getName().equals(StdUserGroup.MANAGEMENT))) {
            candidateCVRichTextArea.setEditable(false);
        }

        setCVRecommendation();
        setLetterRecommendation();
//        setVisibleLogo();
        setCandidatePicImage();
    }

    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setTextCV(candidateCVRichTextArea.getValue());
        } else {
            if (candidateCVRichTextArea.getValue() != null) {
                StringBuffer newTextResume = new StringBuffer(candidateCVRichTextArea.getValue());
                if (textResumeStringBuffer.compareTo(newTextResume) != 0) {
                    getEditedEntity().setContactInfoChecked(false);

                    if (candidateCVRichTextArea.getValue() != null) {
                        getEditedEntity().setTextCV(candidateCVRichTextArea.getValue());
                    }
                }
            } else {
                getEditedEntity().setContactInfoChecked(false);
            }
        }
    }

    private String[] breakLine = {"<br>", "<br/>", "<br />", "<p>", "</p>", "</div>"};
    final String brHtml = breakLine[0];
    final String brTemp = "ШbrШ";

    private String setBreakLine(String text) {
        String retText = text;

        for (String s : breakLine) {
            retText = retText.replaceAll(s, brTemp);
        }

        return retText;
    }

    private void setColorHighlightingCompetencies() {
        if (getEditedEntity().getTextCV() != null) {
            String htmlText = getEditedEntity().getTextCV();

            Source source = new Source(setBreakLine(htmlText));

            // Call fullSequentialParse manually as most of the source will be parsed.
            source.fullSequentialParse();
            htmlText = source.getTextExtractor().setIncludeAttributes(true).toString()
                    .replaceAll(brTemp, brHtml);

            htmlText = parseCVService.colorHighlightingCompetencies(htmlText, "brown");

            if (candidateCVFieldOpenPosition.getValue() != null) {
                htmlText = parseCVService.colorHighlightingCompetencies(candidateCVFieldOpenPosition.getValue(),
                        htmlText, "brown", "red");
            } else {
                htmlText = parseCVService.colorHighlightingCompetencies(htmlText, "brown");
            }

            htmlText = parseCVService.colorHighlingCompany(htmlText, "green");
//            htmlText = parseCVService.colorHighlingPositions(htmlText, "blue");

            candidateCVRichTextArea.setValue(htmlText.replaceAll("\n", breakLine[0]));
        }
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

/*    public void setVisibleLogo() {

        if (candidatePic.getValueSource().getValue() == null) {
            //candidatePic.setVisible(false);
            //candidateFaceDefaultImage.setVisible(true);
        } else {
            candidatePic.setVisible(true);
            candidateFaceDefaultImage.setVisible(false);
        }
    } */

    private void setCandidatePicImage() {
        if (getEditedEntity().getFileImageFace() == null) {
            candidateFaceDefaultImage.setVisible(true);
            candidatePic.setVisible(false);
        } else {
            candidateFaceDefaultImage.setVisible(false);
            candidatePic.setVisible(true);
        }
    }

    @Subscribe("candidatePic")
    public void onCandidatePicSourceChange(ResourceView.SourceChangeEvent event) {
        setCandidatePicImage();
    }

    private void setLetterRecommendation() {
        if (candidateCVFieldOpenPosition.getValue() != null) {
            if (candidateCVFieldOpenPosition.getValue().getTemplateLetter() != null) {
                String caption = "Структура письма:";
                final String startText = "<center><h4>Прошу в сопроводительном письме и резюме отразить следующую информацию: </h4></center></br></br>";
                String text = candidateCVFieldOpenPosition.getValue() != null ?
                        (startText + candidateCVFieldOpenPosition.getValue().getTemplateLetter()) : "";

                final String example = "Пример:\n" +
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
                letterRecommendation.setVisible(true);
            } else {
                letterRecommendation.setVisible(false);
            }
        }
    }

    /*    private void setLetterRecommendation() {
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
    } */

    @Subscribe
    public void onAfterShow1(AfterShowEvent event) {
        if (candidateCVRichTextArea.getValue() != null)
            rescanResume();
        setColorHighlightingCompetencies();
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
        setLetterRecommendation();

        if (candidateCVRichTextArea.getValue() != null && !candidateCVRichTextArea.getValue().equals("")) {
            setColorHighlightingCompetencies();
        }
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setDatePost(new Date());

            getEditedEntity().setOwner(userSession.getUser());
        } else {
            if (candidateCVRichTextArea.getValue() != null) {
                textResumeStringBuffer = new StringBuffer(candidateCVRichTextArea.getValue());
            }
        }
    }

    @Subscribe("fileOriginalCVField")
    public void onFileOriginalCVFieldFileUploadError(UploadField.FileUploadErrorEvent event) {
        notifications.create()
                .withType(Notifications.NotificationType.ERROR)
                .withCaption("Ошибка загрузки файла в хранилище.")
                .show();
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

        if (candidateCVFieldOpenPosition.getValue().getComment() != null) {
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

/*    @Install(to = "candidateField", subject = "optionImageProvider")
    private Resource candidateFieldOptionImageProvider(JobCandidate jobCandidate) {

        if (jobCandidate.getFileImageFace() != null) {
            Image image = uiComponents.create(Image.NAME);
            image.setStyleName("round-photo");
            FileDescriptorResource resource = image.createResource(FileDescriptorResource.class)
                    .setFileDescriptor(jobCandidate.getFileImageFace());
            return resource;
        }

        return null;
    }*/

    public void rescanCV() {
        rescanResume();
    }

    public void resumeRecognition() {
        machRegexpFromCV.setValue(parseCVService.parseEmail(candidateCVRichTextArea.getValue())
                + " "
                + parseCVService.parsePhone(candidateCVRichTextArea.getValue()));
    }

    public void loadToCVTextArea() {
        Document doc = null;
        try {
/*            doc = Jsoup.connect(textFieldIOriginalCV.getValue())
                    .userAgent("Chrome/4.0.249.0 Safari/532.5")
                    .referrer(referer)
                    .get();

            Elements elements = doc.select("div#mw-content-text.mw-content-ltr"); */
            String retStr = webLoadService.getCVWebPage(textFieldIOriginalCV.getValue());

            if (candidateCVRichTextArea.getValue() == null) {
                candidateCVRichTextArea.setValue(retStr.replaceAll("\n", breakLine[0])
                        + "<br><br>Ссылка: " + textFieldIOriginalCV.getValue());
            } else {
                dialogs.createOptionDialog(Dialogs.MessageType.WARNING)
                        .withMessage("Заменить старый текст резюме на новый?")
                        .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                            candidateCVRichTextArea.setValue(retStr.replaceAll("\n", breakLine[0]));
                        }), new DialogAction(DialogAction.Type.NO))
                        .show();
            }
        } catch (IOException | NullPointerException e) {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withDescription("Ошибка загрузки страницы: " + textFieldIOriginalCV.getValue())
                    .show();

            e.printStackTrace();
        }
    }

    Boolean flagHTML = true;

    public void convertToText() {
        if (flagHTML) {
            String break_line = "break_line";

            String str = candidateCVRichTextArea.getValue()
                    .replaceAll("<br>", break_line + break_line)
                    .replaceAll("<li>", "<li> - ")
                    .replaceAll("</p>", "</p>" + break_line + break_line)
                    .replaceAll("</li>", "</li>" + break_line)
                    .replaceAll("</dd>", "</dd>" + break_line)
                    .replaceAll("</dt>", "</dt>" + break_line)
                    .replaceAll("</dl>", "</dl>" + break_line)
                    .replaceAll("</div>", "</div>" + break_line + break_line);
            str = Jsoup.parse(str).text().replaceAll(break_line, "<br>");

            candidateCVRichTextArea.setValue(str.replaceAll("\n", breakLine[0]));
            flagHTML = false;
        } else {
            candidateCVRichTextArea.setValue(getEditedEntity().getTextCV().replaceAll("\n", breakLine[0]));
            flagHTML = true;
        }

        setColorHighlightingCompetencies();
    }


    @Subscribe("candidateCVRichTextArea")
    public void onCandidateCVRichTextAreaValueChange(HasValue.ValueChangeEvent<String> event) {
        if (event.getValue() == null || event.getValue().equals("")) {
            convertToTextButton.setEnabled(false);
        } else {
            convertToTextButton.setEnabled(true);
        }
    }

    Boolean flagOriginal = false;

    public void showOriginalText() {
        if (flagOriginal) {
            showOriginalButon.setCaption("Оригинальное");
            flagOriginal = false;

            setColorHighlightingCompetencies();
        } else {
            showOriginalButon.setCaption("Выделение");
            flagOriginal = true;

            candidateCVRichTextArea.setValue(getEditedEntity().getTextCV().replaceAll("\n", breakLine[0]));
        }
    }

    public void setParentDataContext(DataContext parentDataContext) {
        dataContext.setParent(parentDataContext);
    }
}