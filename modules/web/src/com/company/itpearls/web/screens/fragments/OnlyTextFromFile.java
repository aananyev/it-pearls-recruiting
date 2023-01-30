package com.company.itpearls.web.screens.fragments;

import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.FileUploadField;
import com.haulmont.cuba.gui.components.RichTextArea;
import com.haulmont.cuba.gui.components.UploadField;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.company.itpearls.web.screens.fragments.Onlytext;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@UiController("itpearls_OnlyTextFromFile")
@UiDescriptor("onlyTextFromFile.xml")
public class OnlyTextFromFile extends Onlytext {
    @Inject
    private FileUploadingAPI fileUploadingAPI;
    @Inject
    private Notifications notifications;
    @Inject
    private DataManager dataManager;
    @Inject
    private FileLoader fileLoader;
    @Inject
    private RichTextArea textRichTextArea;
    @Inject
    private FileUploadField uploadField;

    private static final String EXTENSION_PDF = "pdf";
    private static final String EXTENSION_DOC = "doc";
    private static final String EXTENSION_DOCX = "docx";
    private static final String EXTENSION_TXT = "txt";

    @Subscribe("uploadField")
    public void onUploadFieldFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) throws FileStorageException {
        File file = fileUploadingAPI.getFile(uploadField.getFileId());
        String textResume = "";

        if (file != null) {
            notifications.create()
                    .withCaption("File is uploaded to temporary storage at " + file.getAbsolutePath())
                    .show();
        }

        FileDescriptor fd = uploadField.getFileDescriptor();

        try {
            fileUploadingAPI.putFileIntoStorage(uploadField.getFileId(), fd);
        } catch (FileStorageException e) {
            throw new RuntimeException("Error saving file to FileStorage", e);
        }

        dataManager.commit(fd);

        notifications.create()
                .withCaption("Uploaded file: " + uploadField.getFileName())
                .show();

        InputStream inputStream = fileLoader.openStream(fd);

        if(fd.getExtension().equals(EXTENSION_PDF)) {
            textResume = parsePdfCV(inputStream);
        } else {
            if (fd.getExtension().equals(EXTENSION_TXT)) {
                textResume = inputStream.toString();
            }
        }

        textRichTextArea.setValue(textResume);
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

    @Subscribe("uploadField")
    public void onUploadFieldFileUploadError(UploadField.FileUploadErrorEvent event) {
        notifications.create()
                .withCaption("File upload error")
                .show();
    }
}