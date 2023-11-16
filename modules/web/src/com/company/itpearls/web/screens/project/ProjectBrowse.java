package com.company.itpearls.web.screens.project;

import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.Person;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Project;
import com.haulmont.cuba.gui.screen.LookupComponent;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import java.util.*;
import java.util.Calendar;
import java.util.stream.Collectors;

@UiController("itpearls_Project.browse")
@UiDescriptor("project-browse.xml")
@LookupComponent("projectsTable")
@LoadDataBeforeShow
public class ProjectBrowse extends StandardLookup<Project> {
    @Inject
    private UiComponents uiComponents;
    @Inject
    private CheckBox onlyOpenProjectCheckBox;
    @Inject
    private CollectionLoader<Project> projectsDl;
    @Inject
    private TreeDataGrid<Project> projectsTable;
    @Inject
    private MessageTools messageTools;
    @Inject
    private LookupField columnSelector;
    @Inject
    private CheckBox withOpenPositionCheckBox;
    @Inject
    private MessageBundle messageBundle;

    private final static String width_30px = "30px";
    private final static String width_50px = "50px";
    private final static String style_circle_30px = "circle-30px";
    private final static String style_table_wordwrap = "table-wordwrap";


    @Install(to = "projectsTable.projectLogoColumn", subject = "columnGenerator")
    private Object projectsTableProjectLogoColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<Project> event) {
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        retBox.setWidthFull();
        retBox.setHeightFull();
        retBox.setAlignment(Component.Alignment.MIDDLE_CENTER);

        Image image = uiComponents.create(Image.class);
        image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        image.setWidth("60px");
        image.setHeight("60px");
        image.setStyleName("icon-no-border-50px");
        image.setAlignment(Component.Alignment.MIDDLE_CENTER);

        if (event.getItem().getProjectLogo() != null) {
            image.setSource(FileDescriptorResource.class)
                    .setFileDescriptor(event
                            .getItem()
                            .getProjectLogo());
        } else {
            image.setSource(ThemeResource.class).setPath("icons/no-company.png");
        }

        retBox.add(image);
        return retBox;
    }

    @Subscribe
    public void onInit(InitEvent event) {
        initColumnSelector();
    }

    private void initColumnSelector() {
        List<DataGrid.Column<Project>> columns = projectsTable.getColumns();
        Map<String, String> columnsMap = columns.stream()
                .collect(Collectors.toMap(
                        column -> {
                            MetaPropertyPath propertyPath = column.getPropertyPath();
                            return propertyPath != null
                                    ? messageTools.getPropertyCaption(propertyPath.getMetaProperty())
                                    : column.getId();
                        },
                        DataGrid.Column::getId,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new));
        columnSelector.setOptionsMap(columnsMap);

        columnSelector.setValue(columns.get(0).getId());
    }

    @Subscribe("columnSelector")
    protected void onColumnSelectorValueChange(HasValue.ValueChangeEvent<String> event) {
        projectsTable.setHierarchyColumn(event.getValue());
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        onlyOpenProjectCheckBox.setValue(false);
        setProjectClosedFilter();

        withOpenPositionCheckBox.setValue(true);
    }

    @Install(to = "projectsTable", subject = "rowDescriptionProvider")
    private String projectsTableRowDescriptionProvider(Project project) {
        return Jsoup.parse(project.getProjectDescription() != null ? project.getProjectDescription() : "").text();
    }

    private void setProjectClosedFilter() {
        if (onlyOpenProjectCheckBox.getValue()) {
            projectsDl.removeParameter("projectClosed");
            withOpenPositionCheckBox.setValue(false);
        } else {
            projectsDl.setParameter("projectClosed", false);
        }

        projectsDl.load();
    }

    @Subscribe("onlyOpenProjectCheckBox")
    public void onOnlyOpenProjectCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        setProjectClosedFilter();
    }

    @Install(to = "projectsTable.iconProjectDesc", subject = "columnGenerator")
    private Icons.Icon projectsTableIconProjectDescColumnGenerator(DataGrid.ColumnGeneratorEvent<Project> event) {
        if (event.getItem().getProjectDescription() != null) {
            return CubaIcon.PLUS_CIRCLE;
        } else {
            return CubaIcon.MINUS_CIRCLE;
        }
    }

    @Install(to = "projectsTable.iconProjectDesc", subject = "styleProvider")
    private String projectsTableIconProjectDescStyleProvider(Project project) {
        if (project.getProjectDescription() != null) {
            return "pic-center-large-green";
        } else {
            return "pic-center-large-red";
        }
    }

    @Install(to = "projectsTable.iconProjectDesc", subject = "descriptionProvider")
    private String projectsTableIconProjectDescDescriptionProvider(Project project) {
        if (project.getProjectDescription() != null) {
            return Jsoup.parse(project.getProjectDescription()).text();
        } else {
            return null;
        }
    }

    @Subscribe("withOpenPositionCheckBox")
    public void onWithOpenPositionCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (event.getValue()) {
            projectsDl.setParameter("withOpenPosition", true);
            onlyOpenProjectCheckBox.setValue(false);
        } else {
            projectsDl.removeParameter("withOpenPosition");
        }

        projectsDl.load();
    }

/*    @Install(to = "projectsTable.projectOwner", subject = "columnGenerator")
    private Object projectsTableProjectOwnerColumnGenerator(DataGrid.ColumnGeneratorEvent<Project> event) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();
        retHBox.setAlignment(Component.Alignment.MIDDLE_RIGHT);
        retHBox.setHeightFull();
        retHBox.setSpacing(true);

        Label newProjectLabel = uiComponents.create(Label.class);
        newProjectLabel.setValue(messageBundle.getMessage("msgNewReserve"));
        newProjectLabel.setStyleName("button_table_red");
        newProjectLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);
        newProjectLabel.setWidthAuto();
        newProjectLabel.setHeightAuto();

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(new Date());
        gregorianCalendar.add(Calendar.DAY_OF_MONTH, -3);
        if (event.getItem().getStartProjectDate() != null) {
            if (event.getItem().getStartProjectDate().before(gregorianCalendar.getTime())) {
                newProjectLabel.setVisible(false);
            } else {
                newProjectLabel.setVisible(true);
            }
        }

        HBoxLayout retObject = setComponentsToOpenPositionsTable(event, event.getItem().getProjectName());
        retObject.setWidthFull();

        Image image = setProjectOwnerImage(event.getItem().getProjectOwner());
        retHBox.add(newProjectLabel);
        retHBox.add(retObject);
        retHBox.add(image);
        retHBox.expand(retObject);

        return retHBox;
    }*/

    private Image setProjectOwnerImage(Person projectOwner) {
        StringBuilder sb = new StringBuilder();
        sb.append(projectOwner.getFirstName())
                .append(" ")
                .append(projectOwner.getSecondName());
        if (projectOwner.getPersonPosition() != null) {
            sb.append(" / ")
                    .append(projectOwner.getPersonPosition().getPositionRuName());
        }
        if (projectOwner.getCompanyDepartment() != null) {
            sb.append(" / ")
                    .append(projectOwner.getCompanyDepartment().getDepartamentRuName());
            if (projectOwner.getCompanyDepartment().getCompanyName() != null) {
                sb.append(" / ")
                        .append(projectOwner.getCompanyDepartment().getCompanyName().getComanyName());
            }
        }

        if (projectOwner.getCityOfResidence() != null) {
            sb.append(" / ")
                    .append(projectOwner.getCityOfResidence().getCityRuName());
        }

        Image retImage = uiComponents.create(Image.class);
        retImage.setWidth(width_30px);
        retImage.setHeight(width_30px);
        retImage.setStyleName(style_circle_30px);
        retImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        retImage.setAlignment(Component.Alignment.MIDDLE_LEFT);
        retImage.setDescription(sb.toString());

        if (projectOwner.getFileImageFace() != null) {
            retImage
                    .setSource(FileDescriptorResource.class)
                    .setFileDescriptor(projectOwner.getFileImageFace());
        } else {
            retImage.setVisible(false);
        }

        return retImage;
    }

    private HBoxLayout setComponentsToOpenPositionsTable(DataGrid.ColumnGeneratorEvent<Project> event,
                                                         String dataStr) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        Label label = uiComponents.create(Label.class);

        retHBox.setWidthFull();
        retHBox.setHeightFull();
        retHBox.setAlignment(Component.Alignment.MIDDLE_CENTER);
        retHBox.setStyleName(style_table_wordwrap);

        label.setHeightAuto();
        label.setAlignment(Component.Alignment.MIDDLE_LEFT);
        label.setStyleName(style_table_wordwrap);

        label.setValue(dataStr);

        retHBox.add(label);

        return retHBox;
    }

    @Install(to = "projectsTable.projectName", subject = "columnGenerator")
    private Object projectsTableProjectNameColumnGenerator(DataGrid.ColumnGeneratorEvent<Project> event) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();
        retHBox.setAlignment(Component.Alignment.MIDDLE_RIGHT);
        retHBox.setHeightFull();
        retHBox.setSpacing(true);

        Label newVacancyLabel = uiComponents.create(Label.class);
        newVacancyLabel.setValue(messageBundle.getMessage("msgNewReserve"));
        newVacancyLabel.setStyleName("button_table_red");
        newVacancyLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);
        newVacancyLabel.setWidthAuto();
        newVacancyLabel.setHeightAuto();

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(new Date());
        gregorianCalendar.add(Calendar.DAY_OF_MONTH, -14);
        if (event.getItem().getStartProjectDate() != null) {
            if (event.getItem().getStartProjectDate().before(gregorianCalendar.getTime())) {
                newVacancyLabel.setVisible(false);
            } else {
                newVacancyLabel.setVisible(true);
            }
        }

        HBoxLayout retObject = setComponentsToOpenPositionsTable(event, event.getItem().getProjectName());
        retObject.setWidthFull();

        Image image = setProjectOwnerImage(event.getItem().getProjectOwner());
        retHBox.add(newVacancyLabel);
        retHBox.add(retObject);
        retHBox.add(image);
        retHBox.expand(retObject);

        return retHBox;
    }
}