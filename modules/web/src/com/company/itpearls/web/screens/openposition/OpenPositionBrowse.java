package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.entity.RecrutiesTasks;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.ValueLoadContext;
import com.haulmont.cuba.gui.actions.list.CreateAction;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.OpenPosition;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.Null;
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

    @Subscribe
    protected void onInit( InitEvent event ) {
        openPositionsTable.setStyleProvider( ( openPositions, property ) -> {
            Integer s = dataManager.loadValue( "select count(e.reacrutier) " +
                    "from itpearls_RecrutiesTasks e " +
                    "where e.openPosition = :openPos and " +
                    "e.endDate >= :currentDate", Integer.class)
                    .parameter( "openPos", openPositions )
                    .parameter( "currentDate", new Date() )
                    .one();

            if( property == null ) {
                if( s == 0 )
                    return "open-position-empty-recrutier";
                else
                    return "open-position-job-recruitier";
            }
            else {
                if (s == 0)
                    return "open-position-empty-recrutier";
                else
                    return "open-position-job-recruitier";
            }
        });
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        checkBoxOnlyOpenedPosition.setValue( true ); // только открытые позиции
    }

    @Subscribe("checkBoxOnlyOpenedPosition")
    public void onCheckBoxOnlyOpenedPositionValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if( checkBoxOnlyOpenedPosition.getValue() ) {
            openPositionsDl.setParameter("openClosePos", false );
        } else {
            openPositionsDl.removeParameter("openClosePos");
        }

        openPositionsDl.load();
    }
}