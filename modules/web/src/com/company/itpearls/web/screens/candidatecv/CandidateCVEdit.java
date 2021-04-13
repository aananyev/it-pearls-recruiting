package com.company.itpearls.web.screens.candidatecv;

import com.company.itpearls.core.PdfParserService;
import com.company.itpearls.entity.*;
import com.company.itpearls.web.screens.skilltree.SkillTreeBrowseCheck;
import com.company.itpearls.web.screens.somefiles.SomeFilesEdit;
import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.web.AppUI;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

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
    private FileUploadingAPI fileUploadingAPI;
    @Inject
    private Notifications notifications;
    @Inject
    private DataManager dataManager;
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
    private DataContext dataContext;
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
    private CollectionContainer<JobCandidate> candidatesDc;
    @Inject
    private Image candidatePic;
    @Inject
    private Screens screens;

    @Subscribe
    public void onInit(InitEvent event) {
        AppUI ui = AppBeans.get(AppUI.class);
        webBrowserTools = ui.getWebBrowserTools();

        fileOriginalCVField.addFileUploadSucceedListener(uploadSucceedEvent -> {
            if (fileOriginalCVField.getFileId() != null) {
                File file = fileUploadingAPI.getFile(fileOriginalCVField.getFileId());

                commitChanges();

                FileDescriptor fd = fileOriginalCVField.getFileDescriptor();

                try {
                    fileUploadingAPI.putFileIntoStorage(fileOriginalCVField.getFileId(), fd);
                } catch (FileStorageException e) {
                    throw new RuntimeException("Error saving file to FileStorage", e);
                }

                dataManager.commit(fd);

                notifications.create()
                        .withType(Notifications.NotificationType.TRAY)
                        .withCaption("Uploaded file: " + fileOriginalCVField.getFileName())
                        .show();
            }
        });

        fileOriginalCVField.addFileUploadErrorListener(uploadErrorEvent ->
                notifications.create()
                        .withCaption("File upload error")
                        .show());
    }

    void openURL(String url) {

        String mylaunch = url;
        String os = System.getProperty("os.name").toLowerCase();

        if (os.indexOf("win") >= 0) {
            try {
                Runtime rt = Runtime.getRuntime();
                rt.exec("rundll32 url.dll,FileProtocolHandler " + mylaunch);
            } catch (IOException e) {
                //System.out.println("THROW::: make sure we handle browser error");
                e.printStackTrace();
            }
        }

        if (os.indexOf("mac") >= 0) {
            try {
                Runtime rt = Runtime.getRuntime();
                rt.exec("open " + mylaunch);
            } catch (IOException e) {
                //System.out.println("THROW::: make sure we handle browser error");
                e.printStackTrace();
            }
        }

        if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0) {
            try {
                Runtime rt = Runtime.getRuntime();
                String[] browsers = {"epiphany", "firefox", "mozilla", "konqueror",
                        "netscape", "opera", "links", "lynx"};
                StringBuffer cmd = new StringBuffer();

                for (int i = 0; i < browsers.length; i++)
                    cmd.append((i == 0 ? "" : " || ") + browsers[i] + " \"" + mylaunch + "\" ");

                rt.exec(new String[]{"sh", "-c", cmd.toString()});
            } catch (IOException e) {
                //System.out.println("THROW::: make sure we handle browser error");
                e.printStackTrace();
            }
        }
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

    }

    @Subscribe
    public void onAfterShow1(AfterShowEvent event) {
        if(candidateCVRichTextArea.getValue() != null)
            rescanResume();
    }

    public void onLinkButtonClick(String mylaunch) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("win") >= 0) {
            try {
                Runtime rt = Runtime.getRuntime();
                rt.exec("rundll32 url.dll,FileProtocolHandler " + mylaunch);
            } catch (IOException e) {
                //System.out.println("THROW::: make sure we handle browser error");
                e.printStackTrace();
            }
            if (os.indexOf("mac") >= 0) {
                try {
                    Runtime rt = Runtime.getRuntime();
                    rt.exec("open" + mylaunch);
                } catch (IOException e) {
                    //System.out.println("THROW::: make sure we handle browser error");
                    e.printStackTrace();
                }
            }
            if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0) {
                try {
                    Runtime rt = Runtime.getRuntime();
                    String[] browsers = {"epiphany", "firefox", "mozilla", "konqueror",
                            "netscape", "opera", "links", "lynx"};
                    StringBuffer cmd = new StringBuffer();
                    for (int i = 0; i < browsers.length; i++)
                        cmd.append((i == 0 ? "" : " || ") + browsers[i] + " \"" + mylaunch + "\" ");
                    rt.exec(new String[]{"sh", "-c", cmd.toString()});
                } catch (IOException e) {
                    //System.out.println("THROW::: make sure we handle browser error");
                    e.printStackTrace();
                }
            }
        }
    }

    private void setTemplateLetter() {
        String templateLetter = "";

        if (getEditedEntity().getLetter() == null) {
            if (candidateCVFieldOpenPosition.getValue() != null) {
                if (candidateCVFieldOpenPosition.getValue().getProjectName().getProjectDepartment().getTemplateLetter() != null) {
                    templateLetter = templateLetter
                            + candidateCVFieldOpenPosition.getValue().getProjectName().getProjectDepartment().getTemplateLetter()
                            + "\n<br>";
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

            if(!templateLetter.equals("")) {
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

    public void setUrlOriginalCV() {
        String value = textFieldIOriginalCV.getValue();

        if (value == null)
            return;
        if (!value.startsWith("http://") && !value.startsWith("https://"))
            value = "http://" + value;
        onLinkButtonClick(value);
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

    public void setUrlITPearlsCV() {
        String value = textFieldITPearlsCV.getValue();

        if (value == null)
            return;
        if (!value.startsWith("http://"))
            value = "http://" + value;

        webBrowserTools.showWebPage(value, ParamsMap.of("target", "_blank"));
    }

    @Subscribe("fileOriginalCVField")
    public void onFileOriginalCVFieldFileUploadSucceed1(FileUploadField.FileUploadSucceedEvent event) {
        File loadFile = fileUploadingAPI.getFile(fileOriginalCVField.getFileId());
        String textResume = "";
        File imageFace = null;

        try {
            textResume = parsePdfCV(loadFile);
//            imageFace = pdfParserService.getImageFromPDF(loadFile);
//            candidatePic.setSource(ClasspathResource.class)
//                    .setPath(imageFace.getAbsolutePath());

            candidateCVRichTextArea.setValue(textResume.replace("\n", "<br>"));
        } catch (IOException e) {
            notifications.create()
                    .withType(Notifications.NotificationType.WARNING)
                    .withDescription("ВНИМАНИЕ!")
                    .withCaption("Ошибка расшифровки резюме.\nЗагрузите PDF для расшифровки.")
                    .show();
        }

        rescanResume();
    }

    @Subscribe("fileOriginalCVField")
    public void onFileOriginalCVFieldFileUploadError(UploadField.FileUploadErrorEvent event) {
        notifications.create()
                .withCaption("Ошибка загрузки файла в хранилище.")
                .show();
    }

    private void getPhotofromPDF(String fileName) throws IOException {
        File file = new File(fileName);
        PDDocument pdDocument = PDDocument.load(file);

        PDFRenderer renderer = new PDFRenderer(pdDocument);

        BufferedImage image = renderer.renderImage(0);
        String TMP_IMAGE = "tmp_image.jpg";
        ImageIO.write(image, "JPEG", new File(TMP_IMAGE));

        pdDocument.close();
    }

    private String parsePdfCV(File fileName) throws IOException {
        String parsedText = "";

        if(fileOriginalCVField.getFileName().contains("pdf")) {

            PDFParser parser = new PDFParser(new RandomAccessFile(fileName, "r"));
            parser.parse();

            COSDocument cosDoc = parser.getDocument();
            PDFTextStripper pdfStripper = new PDFTextStripper();
            PDDocument pdDoc = new PDDocument(cosDoc);
            parsedText = pdfStripper.getText(pdDoc);

            candidateCVRichTextArea.setValue(parsedText.replace("\n", "<br>"));
        } else {
            notifications.create()
                    .withDescription("ОШИБКА")
                    .withCaption("Файл не является PDF")
                    .withType(Notifications.NotificationType.WARNING)
                    .show();
        }

        return parsedText;
    }

    public void setParameter(CandidateCV entity) {
        candidateCVFieldOpenPosition.setValue(entity.getToVacancy());
        candidateField.setValue(entity.getCandidate());
    }

    @Subscribe("candidateField")
    public void onCandidateFieldValueChange(HasValue.ValueChangeEvent<JobCandidate> event) {
//        someFilesesDl.setParameter("candidate", candidateField.getValue());
//        someFilesesDl.load();
    }

    public void createSomeFileButtonAction() {
        screenBuilders.editor(SomeFiles.class, this)
                .newEntity()
                .withScreenClass(SomeFilesEdit.class)
                .withParentDataContext(dataContext)
                .withInitializer(someFiles -> {
                    someFiles.setCandidateCV(this.getEditedEntity());
                })
                .build()
                .show();
    }

/*    public void rescanResume() {
        String inputText = Jsoup.parse(candidateCVRichTextArea.getValue()).text();
        List<SkillTree> skillTrees = pdfParserService.parseSkillTree(inputText);

        getEditedEntity().setSkillTree(skillTrees);
    }*/


    public List<SkillTree> rescanResume() {
        String inputText = Jsoup.parse(candidateCVRichTextArea.getValue()).text();
        List<SkillTree> skillTrees = pdfParserService.parseSkillTree(inputText);

        getEditedEntity().setSkillTree(skillTrees);

        return skillTrees;
    }

    public void checkSkillFromJD() {
        List<SkillTree> skillTrees = rescanResume();
        String inputText = Jsoup.parse(candidateCVFieldOpenPosition.getValue().getComment()).text();
        List<SkillTree> skillTreesFromJD = pdfParserService.parseSkillTree(inputText);

        if(candidateCVFieldOpenPosition.getValue() != null) {
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
}