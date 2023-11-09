package com.company.itpearls.web.screens.socialnetworktype;

import com.haulmont.cuba.gui.components.FileDescriptorResource;
import com.haulmont.cuba.gui.components.FileUploadField;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SocialNetworkType;

import javax.inject.Inject;

@UiController("itpearls_SocialNetworkType.edit")
@UiDescriptor("social-network-type-edit.xml")
@EditedEntityContainer("socialNetworkTypeDc")
@LoadDataBeforeShow
public class SocialNetworkTypeEdit extends StandardEditor<SocialNetworkType> {
    @Inject
    private Image snLogoFileImage;
    @Inject
    private Image snDefaultLogoFileImage;
    @Inject
    private FileUploadField snLogoFileUpload;

    @Subscribe("snLogoFileUpload")
    public void onSnLogoFileUploadFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {
        try {
            snLogoFileImage.setVisible(true);
            snDefaultLogoFileImage.setVisible(false);

            FileDescriptorResource fileDescriptorResource =
                    snLogoFileImage.createResource(FileDescriptorResource.class)
                            .setFileDescriptor(
                                    snLogoFileUpload.getFileDescriptor());

            snLogoFileImage.setSource(fileDescriptorResource);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        setSNPicImage();
    }

    private void setSNPicImage() {
        if (getEditedEntity().getLogo() == null) {
            snDefaultLogoFileImage.setVisible(true);
            snLogoFileImage.setVisible(false);
        } else {
            snDefaultLogoFileImage.setVisible(false);
            snLogoFileImage.setVisible(true);
        }
    }
}