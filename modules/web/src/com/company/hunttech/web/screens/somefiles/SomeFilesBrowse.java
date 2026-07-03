package com.company.hunttech.web.screens.somefiles;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.SomeFiles;

@UiController("hunttech_SomeFiles.browse")
@UiDescriptor("some-files-browse.xml")
@LookupComponent("someFilesesTable")
@LoadDataBeforeShow
public class SomeFilesBrowse extends StandardLookup<SomeFiles> {
}