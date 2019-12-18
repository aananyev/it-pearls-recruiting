package com.company.itpearls.web.screens.somefiles;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SomeFiles;

@UiController("itpearls_SomeFiles.browse")
@UiDescriptor("some-files-browse.xml")
@LookupComponent("someFilesesTable")
@LoadDataBeforeShow
public class SomeFilesBrowse extends StandardLookup<SomeFiles> {
}