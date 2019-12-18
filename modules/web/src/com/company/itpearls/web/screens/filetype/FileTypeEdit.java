package com.company.itpearls.web.screens.filetype;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.FileType;

@UiController("itpearls_FileType.edit")
@UiDescriptor("file-type-edit.xml")
@EditedEntityContainer("fileTypeDc")
@LoadDataBeforeShow
public class FileTypeEdit extends StandardEditor<FileType> {
}