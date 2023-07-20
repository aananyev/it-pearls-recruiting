package com.company.itpearls.web.screens.somefiles;

import com.company.itpearls.entity.CandidateCV;
import com.company.itpearls.entity.FileType;
import com.company.itpearls.entity.SomeFiles;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;

import javax.inject.Inject;
import java.io.File;

@UiController("itpearls_SomeFiles.edit")
@UiDescriptor("some-files-edit.xml")
@EditedEntityContainer("someFilesDc")
@LoadDataBeforeShow
public class SomeFilesEdit extends StandardEditor<SomeFiles> {
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private TextField<String> fileDescriptionField;
    @Inject
    private LookupPickerField<FileType> fileTypeField;
    @Inject
    private DataContext dataContext;
    @Inject
    private FileUploadField fileDescriptorField;
    @Inject
    private FileUploadingAPI fileUploadingAPI;
    @Inject
    private Notifications notifications;
    @Inject
    private DataManager dataManager;

    @Subscribe("fileDescriptorField")
    public void onFileDescriptorFieldFileUploadSucceed(FileUploadField.FileUploadSucceedEvent event) {
        File file = fileUploadingAPI.getFile(fileDescriptorField.getFileId());

        if (file != null) {
            notifications.create()
                    .withCaption("Файл загружен как " + file.getAbsolutePath())
                    .show();
        }

        FileDescriptor fd = fileDescriptorField.getFileDescriptor();

        try {
            fileUploadingAPI.putFileIntoStorage(fileDescriptorField.getFileId(), fd);
        } catch (FileStorageException e) {
            throw new RuntimeException("Error saving file to FileStorage", e);
        }

        dataManager.commit(fd);

        notifications.create()
                .withCaption("Загружен файл: " + fileDescriptorField.getFileName())
                .show();

    }

    @Subscribe("fileDescriptorField")
    public void onFileDescriptorFieldFileUploadError(UploadField.FileUploadErrorEvent event) {
        notifications.create()
                .withCaption("Ошибка загрузки файла!")
                .show();
    }


    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setFileOwner(userSessionSource.getUserSession().getUser());
        }
    }



    public void setParentDataContext(DataContext parentDataContext) {
        dataContext.setParent(parentDataContext);
    }
}