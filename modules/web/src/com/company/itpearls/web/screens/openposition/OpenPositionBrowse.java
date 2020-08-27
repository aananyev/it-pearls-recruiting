package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.entity.RecrutiesTasks;
import com.company.itpearls.service.GetRoleService;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.global.UserSession;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import javax.swing.*;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private DataManager dataManager;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private CheckBox checkBoxOnlyMySubscribe;
    @Inject
    private UserSession userSession;
    @Inject
    private Button buttonExcel;
    @Inject
    private GetRoleService getRoleService;
    @Inject
    private DataGrid<OpenPosition> openPositionsTable;

    private String ROLE_MANAGER = "Manager";
    private String ROLE_ADMINISTRATOR = "Administrators";
    Map<String, Integer> remoteWork = new LinkedHashMap<>();
    @Inject
    private UiComponents uiComponents;

    @Subscribe
    protected void onInit(InitEvent event) {
        addIconColumn();
        // addIconRemoteWork();
        initRemoteWorkMap();

        initTableGenerator();
    }

    private void initTableGenerator() {
        openPositionsTable.setItemClickAction(new BaseAction("itemClickAction")
                .withHandler(actionPerformedEvent ->
                        openPositionsTable.setDetailsVisible(openPositionsTable.getSingleSelected(), true)));

    }

    @Install(to = "openPositionsTable.remoteWork", subject = "descriptionProvider")
    private String openPositionsTableRemoteWorkDescriptionProvider(OpenPosition openPosition) {
        return String.valueOf(remoteWork.get(openPosition.getRemoteWork()));
    }

    private void initRemoteWorkMap() {
        remoteWork.put("Нет", 0);
        remoteWork.put("Удаленная работа", 1);
        remoteWork.put("Частично 50/50", 2);
    }

    @Install(to = "openPositionsTable.remoteWork", subject = "columnGenerator")
    private Icons.Icon openPositionsTableRemoteWorkColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        String  returnIcon = "";

        switch (event.getItem().getRemoteWork()) {
            case 1:
                returnIcon = "PLUS_CIRCLE";
                break;
            case 0:
                returnIcon = "MINUS_CIRCLE";
                break;
            case 2:
                returnIcon = "QUESTION_CIRCLE";
                break;
            default:
                returnIcon = "QUESTION_CIRCLE";
                break;
        }

        return CubaIcon.valueOf(returnIcon);
    }

    @Install(to = "openPositionsTable.icon", subject = "styleProvider")
    private String openPositionsTableIconStyleProvider(OpenPosition openPosition) {
        return "open-position-pic-center";
    }

    @Install(to = "openPositionsTable.remoteWork", subject = "styleProvider")
    private String openPositionsTableRemoteWorkStyleProvider(OpenPosition openPosition) {
        String style = "";

        switch (openPosition.getRemoteWork()) {
            case 1:
                style = "open-position-pic-center-large-green";
                break;
            case 2:
                style = "open-position-pic-center-large-red";
                break;
            case 0:
                style = "open-position-pic-center-large-yellow";
                break;
        }

        return style;
    }

    @Install(to = "openPositionsTable.openClose", subject = "styleProvider")
    private String openPositionsTableOpenCloseStyleProvider(OpenPosition openPosition) {
        return "open-position-pic-center";
    }

    @Install(to = "openPositionsTable.numberPosition", subject = "styleProvider")
    private String openPositionsTableNumberPositionStyleProvider(OpenPosition openPosition) {
        return "open-position-pic-center";
    }

    private void addIconColumn() {
        //  обавление светофорчика
        DataGrid.Column iconColumn = openPositionsTable.addGeneratedColumn("icon",
                new DataGrid.ColumnGenerator<OpenPosition, String>() {
                    @Override
                    public String getValue(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
                        return getIcon(event.getItem());
                    }

                    @Override
                    public Class<String> getType() {
                        return String.class;
                    }
                });

        iconColumn.setRenderer(openPositionsTable.createRenderer(DataGrid.ImageRenderer.class));
    }

    private void addIconRemoteWork() {
        // добавление удаленнной работы
        DataGrid.Column iconRemoteWork = openPositionsTable.addGeneratedColumn("remoteWork",
                new DataGrid.ColumnGenerator<OpenPosition, String>() {
                    @Override
                    public String getValue(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
                        return getIconRemoteWork(event.getItem());
                    }

                    @Override
                    public Class<String> getType() {
                        return String.class;
                    }
                });

        iconRemoteWork.setRenderer(openPositionsTable.createRenderer(DataGrid.ImageRenderer.class));
    }

    @Install(to = "openPositionsTable.vacansyName", subject = "descriptionProvider")
    private String openPositionsTableVacansyNameDescriptionProvider(OpenPosition openPosition) {
        Date curDate = new Date();
        String QUERY = "select e.name from sec$User e, itpearls_RecrutiesTasks f " +
                "where f.reacrutier = e and f.openPosition = :openPosition and f.endDate > :currentDate";
        String returnData = "";

        List<String> recritierList = dataManager.loadValue(QUERY, String.class)
                .parameter("openPosition", openPosition)
                .parameter("currentDate", curDate)
                .list();

        if (openPosition.getShortDescription() != null) {
            returnData = returnData + "Кратко: " + openPosition.getShortDescription() + "\n";
        }

        if (recritierList.size() != 0) {
            returnData = returnData + "В работе у: ";

            for (String a : recritierList) {
                returnData = returnData + a + ",";
            }

            returnData = returnData.substring(0, returnData.length() - 1);
        }

        returnData = returnData != null ? Jsoup.parse(returnData).text() : "";

        return returnData;
    }

    @Install(to = "openPositionsTable.projectName", subject = "descriptionProvider")
    private String openPositionsTableProjectNameDescriptionProvider(OpenPosition openPosition) {
        String textReturn = openPosition.getProjectName().getProjectDescription();
        String a = textReturn != null ? Jsoup.parse(textReturn).text() : "";

        return a;
    }

    @Install(to = "openPositionsTable", subject = "detailsGenerator")
    private Component openPositionsTableDetailsGenerator(OpenPosition entity) {
        VBoxLayout mainLayout = uiComponents.create(VBoxLayout.NAME);
        mainLayout.setWidth("100%");
        mainLayout.setMargin(true);

        HBoxLayout headerBox = setHeaderBox(entity);
        HBoxLayout infoBox = setInfoOpenPositionBix(entity);
        HBoxLayout projectDescription = setProjectDescription(entity);

        mainLayout.add(headerBox);
        mainLayout.add(infoBox);
        if(projectDescription != null) mainLayout.add(projectDescription);


        return mainLayout;
    }

    private HBoxLayout setProjectDescription(OpenPosition entity) {
        return null;
    }

    private HBoxLayout setInfoOpenPositionBix(OpenPosition entity) {
        HBoxLayout retBox = uiComponents.create(HBoxLayout.NAME);
        VBoxLayout projectBox = getProjectBox(entity);

        retBox.add(projectBox);

        return retBox;
    }

    private VBoxLayout getProjectBox(OpenPosition entity) {
        VBoxLayout retBox = uiComponents.create(VBoxLayout.NAME);

        Label   title = uiComponents.create(Label.NAME);
        title.setStyleName("h3");
        title.setValue("Проект и компания");
        retBox.add(title);

        Label companyName = uiComponents.create(Label.NAME);
        companyName.setValue("Компания: " + entity.getProjectName().getProjectDepartment().getCompanyName().getComanyName());
        retBox.add(companyName);

        Label departaMentName = uiComponents.create(Label.NAME);
        departaMentName.setValue("Департамент: " + entity.getProjectName().getProjectDepartment().getDepartamentRuName());
        retBox.add(departaMentName);

        Label projectName  = uiComponents.create(Label.NAME);
        projectName.setValue("Ппроект: " + entity.getProjectName().getProjectName());
        retBox.add(projectName);

        Label projectOwner = uiComponents.create(Label.NAME);
        projectOwner.setValue("Владелец проекта: " + entity.getProjectName().getProjectOwner().getSecondName()
                + " "
                + entity.getProjectName().getProjectOwner().getFirstName());

        return retBox;
    }


    private Component createCloseButton(OpenPosition entity) {
        Button closeButton = uiComponents.create(Button.class);
        closeButton.setIcon("icons/close.png");
        BaseAction closeAction = new BaseAction("closeAction")
                .withHandler(actionPerformedEvent ->
                        openPositionsTable.setDetailsVisible(entity, false))
                .withCaption("");
        closeButton.setAction(closeAction);
        return closeButton;
    }

    private HBoxLayout setHeaderBox(OpenPosition openPosition) {
        HBoxLayout ret = uiComponents.create(HBoxLayout.NAME);

        ret.setWidth("100%");
        ret.setHeight("100%");

        Label infoLabel = uiComponents.create(Label.NAME);
        infoLabel.setHtmlEnabled(true);
        infoLabel.setStyleName("h3");
        infoLabel.setValue("Информация о вакансии:");

        Component closeButton = createCloseButton(openPosition);
        closeButton.setAlignment(Component.Alignment.TOP_RIGHT);
        ret.add(infoLabel);
        ret.add(closeButton);

        return ret;
    }

    @Install(to = "openPositionsTable", subject = "rowStyleProvider")
    private String openPositionsTableRowStyleProvider(OpenPosition openPosition) {
        Integer s = dataManager.loadValue("select count(e.reacrutier) " +
                "from itpearls_RecrutiesTasks e " +
                "where e.openPosition = :openPos and " +
                "e.endDate >= :currentDate", Integer.class)
                .parameter("openPos", openPosition)
                .parameter("currentDate", new Date())
                .one();

        if (openPosition.getInternalProject() != null) {
            if (openPosition.getInternalProject()) {
                if (s == 0) {
                    return "open-position-internal-project";
                } else {
                    return "open-position-internal-project-job-recrutier";
                }
            } else {
                if (s == 0)
                    return "open-position-empty-recrutier";
                else
                    return "open-position-job-recruitier";
            }
        } else {
            if (s == 0)
                return "open-position-empty-recrutier";
            else
                return "open-position-job-recruitier";
        }
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        checkBoxOnlyOpenedPosition.setValue(true); // только открытые позиции
        buttonExcel.setVisible(getRoleService.isUserRoles(userSession.getUser(), ROLE_MANAGER));

        setInternalProjectFilter();
        setSubcribersFilter();

        openPositionsTable.getColumn("openClose").setCollapsed(true);
        openPositionsTable.getColumn("openClose").setCollapsible(true);
        openPositionsTable.getColumn("openClose").setVisible(true);
    }

    private void setInternalProjectFilter() {
        if (getRoleService.isUserRoles(userSession.getUser(), ROLE_MANAGER) ||
                getRoleService.isUserRoles(userSession.getUser(), ROLE_ADMINISTRATOR)) {
            openPositionsDl.removeParameter("internalProject");
        } else {
            openPositionsDl.setParameter("internalProject", false);
        }

        openPositionsDl.load();
    }

    private void setSubcribersFilter() {
        if (checkBoxOnlyMySubscribe.getValue()) {
            openPositionsDl.setParameter("recrutier", userSession.getUser());
            openPositionsDl.setParameter("nowDate", new Date());
        } else {
            openPositionsDl.removeParameter("recrutier");
            openPositionsDl.removeParameter("nowDate");
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
            openPositionsTable.getColumn("openClose").setCollapsed(true);
            openPositionsTable.getColumn("openClose").setVisible(false);
        } else {
            openPositionsDl.removeParameter("openClosePos");
            openPositionsTable.getColumn("openClose").setCollapsed(false);
            openPositionsTable.getColumn("openClose").setVisible(true);
        }

        openPositionsDl.load();
    }

    private String getIconRemoteWork(OpenPosition openPosition) {
        String icon = "";

        Integer remoteWork = openPosition.getRemoteWork();

        if (remoteWork != null) {
            switch (remoteWork) {
                case 0:
                    icon = "icons/plus-btn.png";
                    break;
                case 1:
                    icon = "icons/minus.png";
                    break;
                case 2:
                    icon = "icons/to-client.png";
                    break;
                default:
                    icon = "icons/question-white.png";
                    break;
            }
        } else {
            icon = "icons/question-white.png";
        }

        return icon;
    }

    private String getIcon(OpenPosition openPosition) {
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
                .withInitializer(data -> {
                    data.setOpenPosition(openPositionsTable.getSingleSelected());
                })
                .withScreenId("itpearls_RecrutiesTasks.edit")
                .withLaunchMode(OpenMode.DIALOG)
                .build();

        opScreen.show();
    }

}

