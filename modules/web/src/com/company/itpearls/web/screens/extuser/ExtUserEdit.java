package com.company.itpearls.web.screens.extuser;

import com.company.itpearls.entity.ExtUser;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;

@UiController("itpearls_ExtUserEdit")
@UiDescriptor("ext-user-edit.xml")
public class ExtUserEdit extends Screen {
    @Inject
    private Field smtpPassword;
    @Inject
    private Image defaultPic;
    @Inject
    private FileUploadField fileImageFaceUpload;
    @Inject
    private Image userPic;

    @Install(to = "emailFieldPasswordRequired.smtpPasswordRequired", subject = "validator")
    private void emailFieldPasswordRequiredSmtpPasswordRequiredValidator(Boolean aBoolean) {
        smtpPassword.setRequired(aBoolean);
    }

    @Subscribe("fileImageFaceUpload")
    public void onFileImageFaceUploadFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {
        try {

            defaultPic.setVisible(false);
            userPic.setVisible(true);

            FileDescriptorResource fileDescriptorResource = userPic.createResource(FileDescriptorResource.class)
                    .setFileDescriptor(fileImageFaceUpload.getFileDescriptor());

            userPic.setSource(fileDescriptorResource);

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }


    @Subscribe("userPic")
    public void onUserPicSourceChange(ResourceView.SourceChangeEvent event) {
        setCandidatePicImage();
    }

    private void setCandidatePicImage() {
        if (userPic.getValueSource() == null) {
            defaultPic.setVisible(true);
            userPic.setVisible(false);
        } else {
            defaultPic.setVisible(false);
            userPic.setVisible(true);
        }
    }
}