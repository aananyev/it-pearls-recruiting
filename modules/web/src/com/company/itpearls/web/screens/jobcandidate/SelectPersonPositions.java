package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.entity.Position;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.TwinColumn;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@UiController("itpearls_SelectPersonPositions")
@UiDescriptor("select-person-positions.xml")
public class SelectPersonPositions extends Screen {
    @Inject
    private DataManager dataManager;
    @Inject
    private TwinColumn<Position> positionTwinColumn;

    private List<Position> positions = new ArrayList<>();
    private List<Position> positionList = new ArrayList<>();

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        positions = dataManager.load(Position.class)
                .query("select e from itpearls_Position e " +
                        "where e.positionRuName not like '%(не использовать)%' " +
                        "order by e.positionRuName")
                .list();

        positionTwinColumn.setOptionsList(positions);
    }

    @Subscribe("closeBtn")
    public void onCloseBtnClick(Button.ClickEvent event) {
        closeWithDefaultAction();
    }

    public void setPositionsList(List<Position> positionsList) {
        this.positionList = positionsList;
        positionTwinColumn.setValue(this.positionList);
    }

    public List<Position> getPositionsList() {
        positionList = (List<Position>) positionTwinColumn.getValue();
        return this.positionList;
    }
}