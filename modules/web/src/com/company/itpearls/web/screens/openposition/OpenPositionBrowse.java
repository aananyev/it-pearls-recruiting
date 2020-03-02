package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.entity.RecrutiesTasks;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.ValueLoadContext;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.actions.list.CreateAction;
import com.haulmont.cuba.gui.builders.ScreenBuilderProcessor;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.Null;
import java.awt.*;
import java.util.Date;
import java.util.List;

@UiController("itpearls_OpenPosition.browse")
@UiDescriptor("open-position-browse.xml")
@LookupComponent("openPositionsTable")
@LoadDataBeforeShow
public class OpenPositionBrowse extends StandardLookup<OpenPosition> {
    @Inject
    private CollectionLoader<OpenPosition> openPositionsDl;
    @Inject
    private CheckBox checkBoxOnlyOpenedPosition;
    @Inject
    private Table<OpenPosition> openPositionsTable;
    @Inject
    private DataManager dataManager;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private CheckBox checkBoxOnlyMySubscribe;
    @Inject
    private UserSession userSession;

    @Subscribe
    protected void onInit(InitEvent event) {
        openPositionsTable.setStyleProvider((openPositions, property) -> {
            Integer s = dataManager.loadValue("select count(e.reacrutier) " +
                    "from itpearls_RecrutiesTasks e " +
                    "where e.openPosition = :openPos and " +
                    "e.endDate >= :currentDate", Integer.class)
                    .parameter("openPos", openPositions)
                    .parameter("currentDate", new Date())
                    .one();

            if (property == null) {
                if (s == 0)
                    return "open-position-empty-recrutier";
                else
                    return "open-position-job-recruitier";
            } else {
                if (s == 0)
                    return "open-position-empty-recrutier";
                else
                    return "open-position-job-recruitier";
            }
        });
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        checkBoxOnlyOpenedPosition.setValue(true); // только открытые позиции

        setSubcribersFilter();

    }

    private void setSubcribersFilter() {
       if( checkBoxOnlyMySubscribe.getValue() ) {
           openPositionsDl.setParameter( "recrutier", userSession.getUser() );
       } else {
           openPositionsDl.removeParameter( "recrutier" );
       }
       
       openPositionsDl.load();
    }

    @Subscribe("checkBoxOnlyMySubscribe")
    public void onCheckBoxOnlyMySubscribeValueChange(HasValue.ValueChangeEvent<Boolean> event) {
       setSubcribersFilter();
    }

    @Subscribe("checkBoxOnlyOpenedPosition")
    public void onCheckBoxOnlyOpenedPositionValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (checkBoxOnlyOpenedPosition.getValue()) {
            openPositionsDl.setParameter("openClosePos", false);
        } else {
            openPositionsDl.removeParameter("openClosePos");
        }

        openPositionsDl.load();
    }

    @Install(to = "openPositionsTable", subject = "iconProvider")
    private String openPositionsTableIconProvider(OpenPosition openPosition) {

        String icon = null;

        Integer priority = openPosition.getPriority();

        if (priority != null) {

            switch (priority) {
                case 0: //"Paused"
                    icon = "icons/remove.png";
                    break;
                case 1: //"Low"
                    icon = "icons/traffic-lights_blue.png";
                    break;
                case 2: //"Normal"
                    icon = "icons/traffic-lights_green.png";
                    break;
                case 3: //"High"
                    icon = "icons/traffic-lights_yellow.png";
                    break;
                case 4: //"Critical"
                    icon = "icons/traffic-lights_red.png";
                    break;
            }
        } else {
            icon = "icons/question-white.png";
        }

        return icon;
    }

    public void subscribePosition() {
       Screen opScreen = screenBuilders
                .editor(RecrutiesTasks.class, this)
                .newEntity()
                .withScreenId("itpearls_RecrutiesTasks.edit")
                .withLaunchMode(OpenMode.DIALOG)
                .build();

       opScreen.show();
    }
}

