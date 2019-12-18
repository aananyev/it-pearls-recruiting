package com.company.itpearls.web.screens.openposition;

import com.haulmont.cuba.gui.actions.list.CreateAction;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.OpenPosition;

import javax.inject.Inject;
import javax.inject.Named;

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

    @Subscribe
    protected void onInit( InitEvent event ) {
        openPositionsTable.setStyleProvider( ( openPositions, property ) -> {
            if( property == null )
                return "open-position-empty-recrutier";
            else
                return "open-position-empty-recruitier";
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