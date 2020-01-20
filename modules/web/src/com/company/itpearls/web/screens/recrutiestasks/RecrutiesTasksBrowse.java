package com.company.itpearls.web.screens.recrutiestasks;

import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.components.GroupTable;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.RecrutiesTasks;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Date;

@UiController("itpearls_RecrutiesTasks.browse")
@UiDescriptor("recruties-tasks-browse.xml")
@LookupComponent("recrutiesTasksesTable")
@LoadDataBeforeShow
public class RecrutiesTasksBrowse extends StandardLookup<RecrutiesTasks> {
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private CollectionLoader<RecrutiesTasks> recrutiesTasksesDl;

    @Subscribe
    public void onInit(InitEvent event) {
       String role = "Researcher";

        // если роль - ресерчер, то автоматически вставить себя
        Collection<String> s = userSessionSource.getUserSession().getRoles();
        // установить поле рекрутера
        if( s.contains(role) ) {
            // если ресерчер то ограничить просмотр других рекрутеров
            recrutiesTasksesDl.setParameter( "recrutier", userSessionSource.getUserSession().getUser() );
        } else {
            recrutiesTasksesDl.removeParameter( "recrutier" );
        }

        recrutiesTasksesDl.load();

    }
}