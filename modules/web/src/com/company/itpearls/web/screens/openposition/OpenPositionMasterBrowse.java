package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.entity.Company;
import com.company.itpearls.entity.Position;
import com.company.itpearls.entity.Project;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.components.RichTextArea;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.OpenPosition;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

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
    @Inject
    private DataGrid<Company> companyTable;
    @Inject
    private RichTextArea companyInfoRichTextArea;
    @Inject
    private RichTextArea projectInfoRichTextArea;
    @Inject
    private DataGrid<OpenPosition> vacansyNameTable;
    @Inject
    private RichTextArea vacansyInfoRictTextArea;

    private Map<String, Integer> priorityMap = new LinkedHashMap<>();
    @Inject
    private LookupField notLowerRatingLookupField;

    @Subscribe("openPositionsTable")
    public void onOpenPositionsTableSelection(DataGrid.SelectionEvent<Position> event) {
        if (openPositionsTable.getSingleSelected() != null) {
            projectNameDl.setParameter("positionType", openPositionsTable.getSingleSelected());
            vacansyNameDl.setParameter("positionType", openPositionsTable.getSingleSelected());
            companyDl.setParameter("positionType", openPositionsTable.getSingleSelected());
        }

        projectNameDl.load();
        vacansyNameDl.load();
        companyDl.load();
    }

    @Subscribe("companyTable")
    public void onCompanyTableSelection(DataGrid.SelectionEvent<Company> event) {
        if (event.getSelected() != null) {
            if (companyTable.getSingleSelected() != null) {
                if (companyTable.getSingleSelected().getCompanyDescription() != null) {
                    companyInfoRichTextArea.setValue(companyTable.getSingleSelected().getCompanyDescription());
                } else {
                    companyInfoRichTextArea.setValue("");
                }
            }
        }
    }

    @Subscribe("projectNameTable")
    public void onProjectNameTableSelection(DataGrid.SelectionEvent<Project> event) {
        if (event.getSelected() != null) {
            if (projectNameTable.getSingleSelected() != null) {
                if (projectNameTable.getSingleSelected().getProjectDescription() != null) {
                    projectInfoRichTextArea.setValue(projectNameTable.getSingleSelected().getProjectDescription());
                } else {
                    projectInfoRichTextArea.setValue("");
                }
            }
        }
    }

    @Subscribe("vacansyNameTable")
    public void onVacansyNameTableSelection(DataGrid.SelectionEvent<OpenPosition> event) {
        if (event.getSelected() != null) {
            if (vacansyNameTable.getSingleSelected() != null) {
                if (vacansyNameTable.getSingleSelected().getComment() != null) {
                    vacansyInfoRictTextArea.setValue(vacansyNameTable.getSingleSelected().getComment());
                } else {
                    vacansyInfoRictTextArea.setValue("");
                }
            }
        }
    }

    @Install(to = "vacansyNameTable.priorityColumn", subject = "columnGenerator")
    private Icons.Icon vacansyNameTablePriorityColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {

        switch ((int) event.getItem().getPriority()) {
            case 0:
                return CubaIcon.ANGLE_DOUBLE_DOWN;
            case 1:
                return CubaIcon.ANGLE_DOWN;
            case 2:
                return CubaIcon.CIRCLE;
            case 3:
                return CubaIcon.ANGLE_UP;
            case 4:
                return CubaIcon.ANGLE_DOUBLE_UP;
        }

        return null;
    }

    @Install(to = "vacansyNameTable.priorityColumn", subject = "styleProvider")
    private String vacansyNameTablePriorityColumnStyleProvider(OpenPosition openPosition) {
        switch ((int) openPosition.getPriority()) {
            case 0:
                return "pic-center-large-grey";
            case 1:
                return "pic-center-large-blue";
            case 2:
                return "pic-center-large-green";
            case 3:
                return "pic-center-large-yellow";
            case 4:
                return "pic-center-large-red";
        }

        return null;
    }

    @Install(to = "notLowerRatingLookupField", subject = "optionIconProvider")
    private String notLowerRatingLookupFieldOptionIconProvider(Object object) {
        String icon = null;

        switch ((int) object) {
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

        return icon;
    }


    private void setMapOfPriority() {
        priorityMap.put("Paused", 0);
        priorityMap.put("Low", 1);
        priorityMap.put("Normal", 2);
        priorityMap.put("High", 3);
        priorityMap.put("Critical", 4);
    }

    @Subscribe
    public void onInit(InitEvent event) {
        setMapOfPriority();
    }

    private void setStatusNotLower() {
        notLowerRatingLookupField.setOptionsMap(priorityMap);

        notLowerRatingLookupField.addValueChangeListener(e -> {
            if(notLowerRatingLookupField.getValue() != null) {
                openPositionsDl.setParameter("priority", notLowerRatingLookupField.getValue());
                vacansyNameDl.setParameter("priority", notLowerRatingLookupField.getValue());
            } else {
                openPositionsDl.setParameter("priority", 1);
                vacansyNameDl.setParameter("priority", 1);
            }

            vacansyNameDl.load();
            openPositionsDl.load();
        });
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        setStatusNotLower();

        openPositionsDl.setParameter("priority", 1);
        vacansyNameDl.setParameter("priority", 1);

        openPositionsDl.load();
    }
}