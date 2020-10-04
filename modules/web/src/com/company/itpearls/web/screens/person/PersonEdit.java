package com.company.itpearls.web.screens.person;

import com.haulmont.cuba.gui.components.FileDescriptorResource;
import com.haulmont.cuba.gui.components.FileUploadField;
import com.haulmont.cuba.gui.components.Image;
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

    @Subscribe("fileImageFaceUpload")
    public void onFileImageFaceUploadFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {
        FileDescriptorResource fileDescriptorResource = peoplePic.createResource(FileDescriptorResource.class)
                .setFileDescriptor(fileImageFaceUpload.getFileDescriptor());

        peoplePic.setSource(fileDescriptorResource);
    }
}