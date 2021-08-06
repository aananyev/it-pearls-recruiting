package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.entity.Company;
import com.company.itpearls.entity.Position;
import com.company.itpearls.entity.Project;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.OpenPosition;

import javax.inject.Inject;

@UiController("itpearls_OpenPositionMaster.browse")
@UiDescriptor("open-position-master-browse.xml")
@LookupComponent("openPositionsTable")
@LoadDataBeforeShow
public class OpenPositionMasterBrowse extends StandardLookup<OpenPosition> {
    @Inject
    private CollectionLoader<Project> projectNameDl;
    @Inject
    private CollectionLoader<Position> openPositionsDl;
    @Inject
    private DataGrid<Position> openPositionsTable;
    @Inject
    private CollectionLoader<OpenPosition> vacansyNameDl;
    @Inject
    private CollectionLoader<Company> companyDl;
    @Inject
    private DataGrid<Project> projectNameTable;

    @Subscribe("openPositionsTable")
    public void onOpenPositionsTableSelection(DataGrid.SelectionEvent<Position> event) {
        if(openPositionsTable.getSingleSelected() != null) {
            projectNameDl.setParameter("positionType", openPositionsTable.getSingleSelected());
            vacansyNameDl.setParameter("positionType", openPositionsTable.getSingleSelected());
            companyDl.setParameter("positionType", openPositionsTable.getSingleSelected());
        }

        projectNameDl.load();
        vacansyNameDl.load();
        companyDl.load();
    }

}