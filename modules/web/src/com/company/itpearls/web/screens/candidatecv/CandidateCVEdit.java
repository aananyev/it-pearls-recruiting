package com.company.itpearls.web.screens.candidatecv;

import com.company.itpearls.entity.CandidateCV;
import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.SomeFiles;
import com.company.itpearls.web.screens.somefiles.SomeFilesEdit;
import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.WebBrowserTools;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.web.AppUI;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Date;

@UiController("itpearls_CandidateCV.edit")
@UiDescriptor("candidate-cv-edit.xml")
@EditedEntityContainer("candidateCVDc")
@LoadDataBeforeShow
public class CandidateCVEdit extends StandardEditor<CandidateCV> {
    @Inject
    private UserSession userSession;
    @Inject
    private LinkButton linkOriginalCV;
    @Inject
    private LinkButton linkITPearlsCV;
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

    @Subscribe
    public void onInit(InitEvent event) {
        AppUI ui = AppBeans.get(AppUI.class);
        webBrowserTools = ui.getWebBrowserTools();

        fileOriginalCVField.addFileUploadSucceedListener(uploadSucceedEvent -> {
            if (fileOriginalCVField.getFileId() != null) {
                File file = fileUploadingAPI.getFile(fileOriginalCVField.getFileId());

                commitChanges();

                notifications.create(Notifications.NotificationType.TRAY)
                        .withCaption("Commit changes of " + candidateField.getValue().getFullName())
                        .withDescription("INFO")
                        .show();

                if (file != null) {
                    notifications.create()
                            .withCaption("File is uploaded to temporary storage at " + file.getAbsolutePath())
                            .show();
                }

                FileDescriptor fd = fileOriginalCVField.getFileDescriptor();

                try {
                    fileUploadingAPI.putFileIntoStorage(fileOriginalCVField.getFileId(), fd);
                } catch (FileStorageException e) {
                    throw new RuntimeException("Error saving file to FileStorage", e);
                }

                dataManager.commit(fd);

                notifications.create()
                        .withCaption("Uploaded file: " + fileOriginalCVField.getFileName())
                        .show();
            }
        });

        fileOriginalCVField.addFileUploadErrorListener(uploadErrorEvent ->
                notifications.create()
                        .withCaption("File upload error")
                        .show());

        /*
        // а вдруг этот класс вызван из ItercationListEdit кнопкой?
         ExchangeBean exchange = getEditedEntity().getValue( "exchange" );

        if( exchange != null) {
            candidateField.setValue( exchange.getCandidate() );
            candidateCVFieldOpenPosition.setValue( exchange.getOpenPosition() );
        } */
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

    @Subscribe("linkOriginalCV")
    public void onLinkOriginalCVClick(Button.ClickEvent event) {
//        openURL(textFieldIOriginalCV.getValue());

        webBrowserTools.showWebPage(textFieldIOriginalCV.getValue(), null);
    }

    @Subscribe("linkITPearlsCV")
    public void onLinkITPearlsCVClick(Button.ClickEvent event) {
//        openURL(textFieldITPearlsCV.getValue());
        webBrowserTools.showWebPage(textFieldITPearlsCV.getValue(), null);
    }

    @Subscribe("textFieldIOriginalCV")
    public void onTextFieldIOriginalCVValueChange(HasValue.ValueChangeEvent<String> event) {
        if (textFieldIOriginalCV.getValue() != null)
            linkOriginalCV.setVisible(true);
        else
            linkOriginalCV.setVisible(false);
    }

    @Subscribe("textFieldITPearlsCV")
    public void onTextFieldITPearlsCVValueChange(HasValue.ValueChangeEvent<String> event) {
        if (textFieldITPearlsCV.getValue() != null) {
            linkITPearlsCV.setVisible((true));
            itpearlsCVLink.setUrl(textFieldITPearlsCV.getValue());
        } else {
            linkITPearlsCV.setVisible(false);
        }
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (textFieldIOriginalCV.getValue() != null) {
            linkOriginalCV.setVisible(true);
            originalCVLink.setUrl(textFieldIOriginalCV.getValue());
        } else {
            linkOriginalCV.setVisible(false);
        }

        if (textFieldITPearlsCV.getValue() != null) {
            linkITPearlsCV.setVisible(true);
        } else {
            linkITPearlsCV.setVisible(false);
        }
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
                    templateLetter = templateLetter + candidateCVFieldOpenPosition.getValue().getProjectName().getProjectDepartment().getTemplateLetter();
                }
            }

            if (candidateCVFieldOpenPosition.getValue() != null) {
                if (candidateCVFieldOpenPosition.getValue().getProjectName().getTemplateLetter() != null) {
                    templateLetter = templateLetter + candidateCVFieldOpenPosition.getValue().getProjectName().getTemplateLetter();
                }
            }

            if (candidateCVFieldOpenPosition.getValue() != null) {
                if (candidateCVFieldOpenPosition.getValue().getTemplateLetter() != null) {
                    templateLetter = templateLetter + candidateCVFieldOpenPosition.getValue().getTemplateLetter();
                }
            }

            letterRichTextArea.setValue(templateLetter);
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

    @Subscribe("linkOriginalCV")
    public void onLinkOriginalCVClick1(Button.ClickEvent event) {
        setUrlOriginalCV();
    }

    public void setUrlOriginalCV() {
        String value = textFieldIOriginalCV.getValue();

        if (value == null)
            return;
        if (!value.startsWith("http://") && !value.startsWith("https://"))
            value = "http://" + value;

//        webBrowserTools.showWebPage(value, null);
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
}