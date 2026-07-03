package com.company.hunttech.web.screens.recrutiestasks;

import com.company.hunttech.entity.Position;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.RecrutiesTasks;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@UiController("hunttech_RecrutiesGroupSubscribeTasks.browse")
@UiDescriptor("recruties-tasks-group-subscribe-browse.xml")
@LookupComponent("recrutiesTasksesTable")
@LoadDataBeforeShow
public class RecrutiesTasksGroupSubscribeBrowse extends StandardLookup<RecrutiesTasks> {
    @Inject
    private Metadata metadata;

    private List<KeyValueEntity> positions = new ArrayList<>();
    @Inject
    private DataManager dataManager;
    @Inject
    private DataGrid recrutiesTasksesTable;

//    private CollectionContainer<String> openPositions;

    @Subscribe
    public void onInit(InitEvent event) {
    }
}