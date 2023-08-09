package com.company.itpearls.web.screens.somefiles;

import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.FileUploadField;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.company.itpearls.web.screens.somefiles.SomeFilesEdit;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;

import javax.inject.Inject;
import java.io.File;

@UiController("itpearls_SomeFilesOpenPosition.edit")
@UiDescriptor("some-files-open-position-edit.xml")
public class SomeFilesOpenPositionEdit extends SomeFilesEdit {
}