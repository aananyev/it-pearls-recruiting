package com.company.itpearls.web.screens.candidatecv;

import com.company.itpearls.BeanNotificationEvent;
import com.company.itpearls.UiNotificationEvent;
import com.company.itpearls.entity.JobCandidate;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.SomeFiles;
import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.CandidateCV;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.gui.WebBrowserTools;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.web.AppUI;
import org.springframework.context.event.EventListener;

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
    private CollectionLoader<SomeFiles> someFilesesDl;
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

    @Subscribe
    public void onInit(InitEvent event) {
        AppUI ui = AppBeans.get(AppUI.class);
        webBrowserTools = ui.getWebBrowserTools();

        fileOriginalCVField.addFileUploadSucceedListener( uploadSucceedEvent -> {
            File file = fileUploadingAPI.getFile(fileOriginalCVField.getFileId());

            commitChanges();

            notifications.create( Notifications.NotificationType.TRAY )
                    .withCaption("Commit changes of " + candidateField.getValue().getFullName() )
                    .withDescription( "INFO" )
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
    
    void openURL( String url) {
        
        String mylaunch = url;
        String os = System.getProperty("os.name").toLowerCase();

        if( os.indexOf( "win" ) >= 0 ) {
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
                    cmd.append((i == 0 ? "" : " || ") + browsers[i] +" \"" + mylaunch + "\" ");

                rt.exec(new String[]{"sh", "-c", cmd.toString()});
            } catch (IOException e) {
                //System.out.println("THROW::: make sure we handle browser error");
                e.printStackTrace();
            }
        }
    }

    @Subscribe("linkOriginalCV")
    public void onLinkOriginalCVClick(Button.ClickEvent event) {
        openURL( textFieldIOriginalCV.getValue() );
    }

    @Subscribe("linkITPearlsCV")
    public void onLinkITPearlsCVClick(Button.ClickEvent event) {
            openURL( textFieldITPearlsCV.getValue() );
    }

    @Subscribe("textFieldIOriginalCV")
    public void onTextFieldIOriginalCVValueChange(HasValue.ValueChangeEvent<String> event) {
        if( textFieldIOriginalCV.getValue() != null )
            linkOriginalCV.setVisible( true );
        else
            linkOriginalCV.setVisible( false );
    }

    @Subscribe("textFieldITPearlsCV")
    public void onTextFieldITPearlsCVValueChange(HasValue.ValueChangeEvent<String> event) {
        if( textFieldITPearlsCV.getValue() != null )
            linkITPearlsCV.setVisible(( true ));
        else
            linkITPearlsCV.setVisible( false );
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if( textFieldIOriginalCV.getValue() != null ) {
                linkOriginalCV.setVisible( true );
        } else {
                linkOriginalCV.setVisible( false );
        }

        if( textFieldITPearlsCV.getValue() != null ) {
                linkITPearlsCV.setVisible( true );
            } else {
                linkITPearlsCV.setVisible( false );
            }
        // отфильтровать файлы кандидата только
        if( !PersistenceHelper.isNew( getEditedEntity() ) )
            someFilesesDl.setParameter( "candidate", candidateField.getValue() );
        else
//           someFilesesDl.setParameter( "candidate", null );

         someFilesesDl.load();
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if(PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setDatePost( new Date() );

            getEditedEntity().setOwner(userSession.getUser());
        }
    }

    public void setUrlOriginalCV() {
         String value = textFieldIOriginalCV.getValue();

         if (value == null)
             return;
         if (!value.startsWith("http://"))
             value = "http://" + value;

         webBrowserTools.showWebPage(value, ParamsMap.of("target", "_blank"));
    }

    @Subscribe("tabFiles")
    public void onTabFilesLayoutClick(LayoutClickNotifier.LayoutClickEvent event) {
        if( PersistenceHelper.isNew( getEditedEntity() ) ) {
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

    public void setParameter( CandidateCV entity ) {
        candidateCVFieldOpenPosition.setValue( entity.getToVacancy() );
        candidateField.setValue( entity.getCandidate() );
    }

    @Subscribe("candidateField")
    public void onCandidateFieldValueChange(HasValue.ValueChangeEvent<JobCandidate> event) {
//        someFilesesDl.setParameter( "candidate", candidateField.getValue() );

//        someFilesesDl.load();
    }
}