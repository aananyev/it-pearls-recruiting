package com.company.hunttech.web.screens.filetype;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.FileType;

@UiController("hunttech_FileType.edit")
@UiDescriptor("file-type-edit.xml")
@EditedEntityContainer("fileTypeDc")
@LoadDataBeforeShow
public class FileTypeEdit extends StandardEditor<FileType> {
}