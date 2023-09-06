package com.company.itpearls.web.screens.fragments;

import com.company.itpearls.entity.CandidateCV;
import com.company.itpearls.web.screens.SelectedCloseAction;
import com.company.itpearls.web.screens.candidatecv.SelectRenderedImagesFromList;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;
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

import javax.inject.Inject;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@UiController("itpearls_OnlyTextPersonPositionLoadPdf")
@UiDescriptor("onlyTextPersonPositionLoadPdf.xml")
public class OnlyTextPersonPositionLoadPdf extends OnlyTextPersonPosition {
    @Inject
    private Notifications notifications;
    @Inject
    private FileUploadingAPI fileUploadingAPI;
    @Inject
    private FileUploadField uploadField;
    @Inject
    private DataManager dataManager;

    private static final String EXTENSION_PDF = "pdf";
    private static final String EXTENSION_DOC = "doc";
    private static final String EXTENSION_DOCX = "docx";
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private FileLoader fileLoader;
    @Inject
    private Screens screens;

    private Image candidatePic;
    private Boolean downloadStatus = false;
    private FileDescriptor candidatePicFileDescriptot;
    private String textResume;
    private FileDescriptorResource resource;

    @Subscribe("cancelButton")
    public void onCancelButtonClick(Button.ClickEvent event) {
        close(StandardOutcome.CLOSE);
    }

    @Subscribe("uploadField")
    public void onUploadFieldFileUploadError(UploadField.FileUploadErrorEvent event) {
        notifications.create(Notifications.NotificationType.ERROR)
                .withCaption(messageBundle.getMessage("msgError"))
                .withDescription("File upload error")
                .show();
    }

    @Subscribe("uploadField")
    public void onUploadFieldFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) throws FileStorageException, IOException {
//        File file = fileUploadingAPI.getFile(uploadField.getFileId());

/*        if (file != null) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption(messageBundle.getMessage("msgWarning"))
                    .withDescription("File is uploaded to temporary storage at " + file.getAbsolutePath())
                    .show();
        } */

        AtomicReference<FileDescriptor> fd = new AtomicReference<>(uploadField.getFileDescriptor());

/*        try {
            fileUploadingAPI.putFileIntoStorage(uploadField.getFileId(), fd.get());
        } catch (FileStorageException e) {
            throw new RuntimeException("Error saving file to FileStorage", e);
        } */

        notifications.create(Notifications.NotificationType.WARNING)
                .withCaption(messageBundle.getMessage("msgWarning"))
                .withDescription("Uploaded file: " + uploadField.getFileName())
                .show();

        FileDescriptor fdcv = fd.get();

        if (fdcv.getExtension().toLowerCase().equals(EXTENSION_PDF.toLowerCase())) {
//            if (fd.get().getExtension().toLowerCase().equals(EXTENSION_PDF.toLowerCase())) {
            InputStream inputStream = fileLoader.openStream(fdcv);
            List<RenderedImage> images = new ArrayList<>();

            textResume = parsePdfCV(inputStream);
            RandomAccessRead rad = new RandomAccessReadBuffer(fileLoader.openStream(fd.get()));

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

                selectRenderedImagesFromList.addAfterCloseListener(evnt -> {
                    if (((SelectedCloseAction) evnt.getCloseAction()).getResult()
                            .equals(CandidateCV.SelectedCloseActionType.SELECTED)) {
                        Image selectedImage = selectRenderedImagesFromList
                                .getSelectedImage();

                        fd.set(dataManager
                                .commit(selectRenderedImagesFromList.getSelectedImageFileDescriptor()));

                        try {
                            fileUploadingAPI.putFileIntoStorage(fd.get().getUuid(), fd.get());
                        } catch (FileStorageException e) {
                            e.printStackTrace();
                            notifications.create(Notifications.NotificationType.ERROR)
                                    .withCaption(messageBundle.getMessage("msgError"))
                                    .withDescription(messageBundle.getMessage("msgErrorPutFileIntoStorage"))
                                    .withType(Notifications.NotificationType.ERROR)
                                    .show();
                        }

                        resource = selectedImage
                                .createResource(FileDescriptorResource.class)
                                .setFileDescriptor(fd.get());

                        candidatePicFileDescriptot = fd.get();
                        candidatePic
                                .setSource(FileDescriptorResource.class)
                                .setFileDescriptor(fd.get());
                    } else {
                        notifications.create(Notifications.NotificationType.ERROR)
                                .withType(Notifications.NotificationType.ERROR)
                                .withDescription(messageBundle.getMessage("msgNeedLoadPDFFile"))
                                .withCaption(messageBundle.getMessage("msgError"))
                                .show();
                    }
                });
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

    public FileDescriptor getCandidatePicFileDescriptot() {
        return candidatePicFileDescriptot;
    }

    public void setCandidatePicFileDescriptot(FileDescriptor candidatePicFileDescriptot) {
        this.candidatePicFileDescriptot = candidatePicFileDescriptot;
    }

    public String getTextResume() {
        return textResume;
    }

    public void setTextResume(String textResume) {
        this.textResume = textResume;
    }

    public Boolean getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(Boolean downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public void setCandidatePic(Image candidatePic) {
        this.candidatePic = candidatePic;
    }

    public Image getCandidatePic() {
        return candidatePic;
    }

    public FileDescriptorResource getResource() {
        return resource;
    }

    public void setResource(FileDescriptorResource resource) {
        this.resource = resource;
    }
}