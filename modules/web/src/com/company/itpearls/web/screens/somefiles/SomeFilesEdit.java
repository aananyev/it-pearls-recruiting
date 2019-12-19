package com.company.itpearls.web.screens.somefiles;

import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SomeFiles;

import javax.inject.Inject;

@UiController("itpearls_SomeFiles.edit")
@UiDescriptor("some-files-edit.xml")
@EditedEntityContainer("someFilesDc")
@LoadDataBeforeShow
public class SomeFilesEdit extends StandardEditor<SomeFiles> {
    @Inject
    private UserSessionSource userSessionSource;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if( PersistenceHelper.isNew( getEditedEntity() ) ) {
            getEditedEntity().setFileOwner( userSessionSource.getUserSession().getUser() );
        }
    }

/*    @Subscribe
    public void onInit(InitEvent event) {
        if( PersistenceHelper.isNew( getEditedEntity() ) ) {
//            getEditedEntity().setFileOwner( userSessionSource.getUserSession().getUser() );
        }
    } */
}