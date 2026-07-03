package com.company.hunttech.web.screens.filetype;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.FileType;

@UiController("hunttech_FileType.browse")
@UiDescriptor("file-type-browse.xml")
@LookupComponent("fileTypesTable")
@LoadDataBeforeShow
public class FileTypeBrowse extends StandardLookup<FileType> {
}