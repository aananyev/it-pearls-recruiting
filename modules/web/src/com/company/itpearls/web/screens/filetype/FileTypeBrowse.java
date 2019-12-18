package com.company.itpearls.web.screens.filetype;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.FileType;

@UiController("itpearls_FileType.browse")
@UiDescriptor("file-type-browse.xml")
@LookupComponent("fileTypesTable")
@LoadDataBeforeShow
public class FileTypeBrowse extends StandardLookup<FileType> {
}