package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.entity.Company;
import com.company.itpearls.entity.Position;
import com.company.itpearls.entity.Project;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.gui.screen.LookupComponent;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
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
    @Inject
    private CollectionLoader<Company> companyDl;
    @Inject
    private TextField<String> headerTextField;
    @Inject
    private DataManager dataManager;
    @Inject
    private Label<String> minSalaryLabel;
    @Inject
    private Label<String> maxSalaryLabel;

    @Subscribe("openPositionsTable")
    public void onOpenPositionsTableSelection(DataGrid.SelectionEvent<Position> event) {
        if (openPositionsTable.getSingleSelected() != null) {
            vacansyNameDl.setParameter("posTypeVacancy", openPositionsTable.getSingleSelected());
            projectNameDl.setParameter("posType", openPositionsTable.getSingleSelected());
            companyDl.setParameter("posTypeCompany", openPositionsTable.getSingleSelected());
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

        if (vacansyNameTable.getSelected() != null) {
            projectNameDl.setParameter("openPosition", vacansyNameTable.getSingleSelected());
            companyDl.setParameter("openPosition", vacansyNameTable.getSingleSelected());
        } else {
            projectNameDl.removeParameter("openPosition");
            companyDl.removeParameter("openPosition");
        }

        projectNameDl.load();
        companyDl.load();
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
            if (notLowerRatingLookupField.getValue() != null) {
                openPositionsDl.setParameter("priorityfield", notLowerRatingLookupField.getValue());
                vacansyNameDl.setParameter("priorityfield", notLowerRatingLookupField.getValue());
                companyDl.setParameter("priorityfield", notLowerRatingLookupField.getValue());
                projectNameDl.setParameter("priorityfield", notLowerRatingLookupField.getValue());
            } else {
                openPositionsDl.setParameter("priorityfield", 1);
                vacansyNameDl.setParameter("priorityfield", 1);
                openPositionsDl.removeParameter("priorityfield");
                vacansyNameDl.removeParameter("priorityfield");
            }

            vacansyNameDl.load();
            openPositionsDl.load();
        });
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        setStatusNotLower();
        setHeaderBox();
        setMinMaxSalaryLabel();
        setCompanyTable();

        openPositionsDl.removeParameter("priorityfield");
        vacansyNameDl.removeParameter("priorityfield");
        projectNameDl.removeParameter("priorityfield");
        companyDl.removeParameter("priorityfield");

        openPositionsDl.load();
        vacansyNameDl.load();
    }

    private void setCompanyTable() {
        companyTable.addSelectionListener(e -> {
            if(companyTable.getSingleSelected() != null) {
                projectNameDl.setParameter("company", companyTable.getSingleSelected());
            } else {
                projectNameDl.removeParameter("company");
            }
        });
    }

    private void setMinMaxSalaryLabel() {
        openPositionsTable.addSelectionListener(e -> {
            String QUERY_MIN_MAX_SALARY = "select e " +
                    "from itpearls_OpenPosition e " +
                    "where e.positionType = :position " +
                    "and (not e.openClose = true)";

            if (openPositionsTable.getSingleSelected() != null) {
                List<OpenPosition> openPositionList = dataManager.load(OpenPosition.class)
                        .query(QUERY_MIN_MAX_SALARY)
                        .parameter("position", openPositionsTable.getSingleSelected())
                        .list();

                if (openPositionList != null) {
                    BigDecimal min = openPositionList.get(0).getSalaryMin(),
                            max = openPositionList.get(0).getSalaryMax();

                    for (OpenPosition op : openPositionList) {
                        if (op.getSalaryMin() != null) {
                            if (min.compareTo(op.getSalaryMin()) > 0) {
                                min = op.getSalaryMin();
                            }
                        }

                        if (op.getSalaryMax() != null) {
                            if (max.compareTo(op.getSalaryMax()) < 0) {
                                max = op.getSalaryMax();
                            }
                        }
                    }

                    minSalaryLabel.setValue(min.toString());
                    maxSalaryLabel.setValue(max.toString());
                }
            }
        });
    }

    @Install(to = "vacansyNameTable.salaryMinMaxColumn", subject = "columnGenerator")
    private String vacansyNameTableSalaryMinMaxColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        String retStr = "";

        try {
            retStr = getSalaryString(event.getItem());
        } catch (NullPointerException e) {
            retStr = "";
        }

        return retStr;
    }

    private void setHeaderBox() {
        headerTextField.setValue("Мастер собеседования: ");
    }

    private String getSalaryString(OpenPosition openPosition) {
        int minLength = openPosition.getSalaryMin().toString().length();
        int maxLength = openPosition.getSalaryMax().toString().length();

        BigDecimal salaryMin = openPosition.getSalaryMin().divide(BigDecimal.valueOf(1000));
        BigDecimal salaryMax = openPosition.getSalaryMax().divide(BigDecimal.valueOf(1000));

        String retStr = "";

        try {
            int salMin = salaryMin.divide(BigDecimal.valueOf(1000)).intValue();
            if (salMin != 0) {
                retStr = salaryMin.toString().substring(0, salaryMin.toString().length() - 3)
                        + " т.р./"
                        + salaryMax.toString().substring(0, salaryMax.toString().length() - 3)
                        + " т.р.";
            } else {
                retStr = "До "
                        + salaryMax.toString().substring(0, salaryMax.toString().length() - 3)
                        + " т.р.";
            }
        } catch (NullPointerException | StringIndexOutOfBoundsException e) {
            retStr = "";
        }

        if (salaryMax.intValue() == 0) {
            retStr = "неопределено";
        }

        return retStr;
    }

    @Install(to = "vacansyNameTable.salaryMinMaxColumn", subject = "descriptionProvider")
    private String vacansyNameTableSalaryMinMaxColumnDescriptionProvider(OpenPosition openPosition) {
        String retStr = "";

        if (openPosition.getSalaryFixLimit() != null) {
            if (openPosition.getSalaryFixLimit()) {
                retStr = "Фиксированное запрлатное предложение\n";
            }
        }

        try {
            retStr = retStr + getSalaryStringCaption(openPosition);
        } catch (NullPointerException e) {
            retStr = "";
        }

        return retStr;
    }

    private String getSalaryStringCaption(OpenPosition openPosition) {
        int minLength = openPosition.getSalaryMin().toString().length();
        int maxLength = openPosition.getSalaryMax().toString().length();

        BigDecimal salaryMin = openPosition.getSalaryMin().divide(BigDecimal.valueOf(1000));
        BigDecimal salaryMax = openPosition.getSalaryMax().divide(BigDecimal.valueOf(1000));

        String retStr = "";

        try {
            retStr = salaryMin.toString().substring(0, salaryMin.toString().length() - 3)
                    + " т.р./"
                    + salaryMax.toString().substring(0, salaryMax.toString().length() - 3)
                    + " т.р.";
        } catch (NullPointerException e) {
            retStr = "";
        }

        if (salaryMin.intValue() == 0) {
            retStr = "неопределено";
        }

        return retStr;
    }

    @Install(to = "vacansyNameTable.salaryMinMaxColumn", subject = "styleProvider")
    private String vacansyNameTableSalaryMinMaxColumnStyleProvider(OpenPosition openPosition) {
        String retStr = "";

        if (openPosition.getSalaryFixLimit() != null) {
            if (openPosition.getSalaryFixLimit()) {
                retStr = "salary-fix-limit";
            }
        }

        return retStr;
    }

    @Install(to = "vacansyNameTable.vacansyName", subject = "descriptionProvider")
    private String vacansyNameTableVacansyNameDescriptionProvider(OpenPosition openPosition) {
        return openPosition.getVacansyName();
    }

    public void clearFilters() {
        vacansyNameDl.removeParameter("priorityfield");
        openPositionsDl.removeParameter("priorityfield");
        projectNameDl.removeParameter("priorityfield");
        companyDl.removeParameter("priorityfield");

        vacansyNameDl.load();
        openPositionsDl.load();
        projectNameDl.load();
        companyDl.load();

        notLowerRatingLookupField.setValue(null);


        openPositionsTable.deselectAll();
        vacansyNameTable.deselectAll();
        companyTable.deselectAll();
        projectNameTable.deselectAll();
    }
}