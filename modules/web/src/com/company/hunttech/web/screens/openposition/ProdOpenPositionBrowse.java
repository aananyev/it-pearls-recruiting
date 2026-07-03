package com.company.hunttech.web.screens.openposition;

import com.company.hunttech.entity.OpenPosition;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.screen.Install;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.company.hunttech.web.screens.openposition.OpenPositionBrowse;

import javax.inject.Inject;

@UiController("hunttech_ProdOpenPosition.browse")
@UiDescriptor("prod-open-position-browse.xml")
public class ProdOpenPositionBrowse extends OpenPositionBrowse {
    @Inject
    private UiComponents uiComponents;

    @Install(to = "openPositionsTable.vacansyName", subject = "columnGenerator")
    private Object openPositionsTableVacansyNameColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        Label openPositionNameLabel = uiComponents.create(Label.class);
        Label projectNameLabel = uiComponents.create(Label.class);

        VBoxLayout vbl = uiComponents.create(VBoxLayout.class);
        vbl.setWidthFull();
        vbl.setHeightAuto();
        vbl.setSpacing(true);

        openPositionNameLabel.setValue(event.getItem().getVacansyName());
        openPositionNameLabel.setStyleName("h4");

        projectNameLabel.setValue(event.getItem().getProjectName().getProjectName());

        vbl.add(openPositionNameLabel);
        vbl.add(projectNameLabel);

        return vbl;
    }
}