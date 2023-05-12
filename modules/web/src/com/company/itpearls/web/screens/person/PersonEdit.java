package com.company.itpearls.web.screens.person;

import com.haulmont.cuba.gui.components.FileDescriptorResource;
import com.haulmont.cuba.gui.components.FileUploadField;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.components.ResourceView;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Person;

import javax.inject.Inject;

@UiController("itpearls_Person.edit")
@UiDescriptor("person-edit.xml")
@EditedEntityContainer("personDc")
@LoadDataBeforeShow
public class PersonEdit extends StandardEditor<Person> {

    @Inject
    private FileUploadField fileImageFaceUpload;
    @Inject
    private Image peoplePic;
    @Inject
    private Image defaultPeoplePic;

    @Subscribe("fileImageFaceUpload")
    public void onFileImageFaceUploadFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {

        try {
            peoplePic.setVisible(true);
            defaultPeoplePic.setVisible(false);

            FileDescriptorResource fileDescriptorResource =
                    peoplePic.createResource(FileDescriptorResource.class)
                            .setFileDescriptor(fileImageFaceUpload.getFileDescriptor());

            peoplePic.setSource(fileDescriptorResource);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

    }

    private void setPeoplePicImage() {
        if (getEditedEntity().getFileImageFace() == null) {
            defaultPeoplePic.setVisible(true);
            peoplePic.setVisible(false);
        } else {
            defaultPeoplePic.setVisible(false);
            peoplePic.setVisible(true);
        }
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        setPeoplePicImage();
    }

    @Subscribe("defaultPeoplePic")
    public void onDefaultPeoplePicSourceChange(ResourceView.SourceChangeEvent event) {
        setPeoplePicImage();

    }

    @Subscribe("fileImageFaceUpload")
    public void onFileImageFaceUploadBeforeValueClear(FileUploadField.BeforeValueClearEvent event) {
        peoplePic.setVisible(false);
        defaultPeoplePic.setVisible(true);
    }
}