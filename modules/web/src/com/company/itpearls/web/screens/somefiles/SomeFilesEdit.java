package com.company.itpearls.web.screens.somefiles;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SomeFiles;

@UiController("itpearls_SomeFiles.edit")
@UiDescriptor("some-files-edit.xml")
@EditedEntityContainer("someFilesDc")
@LoadDataBeforeShow
public class SomeFilesEdit extends StandardEditor<SomeFiles> {
}