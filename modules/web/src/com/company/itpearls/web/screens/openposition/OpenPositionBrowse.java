package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.entity.RecrutiesTasks;
import com.company.itpearls.service.GetRoleService;
import com.company.itpearls.web.screens.recrutiestasks.RecrutiesTasksGroupSubscribeBrowse;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.global.UserSession;
import org.jsoup.Jsoup;

import javax.inject.Inject;
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
    @Inject
    private UiComponents uiComponents;

    private String ROLE_MANAGER = "Manager";
    private static final String MANAGEMENT_GROUP = "Менеджмент";
    private static final String HUNTING_GROUP = "Хантинг";
    private String ROLE_ADMINISTRATOR = "Administrators";
    private Map<String, Integer> remoteWork = new LinkedHashMap<>();
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private Button groupSubscribe;
    @Inject
    private Screens screens;
    @Inject
    private CollectionContainer<OpenPosition> openPositionsDc;
    @Inject
    private CheckBox checkBoxOnlyNotPaused;


    @Subscribe
    protected void onInit(InitEvent event) {
        addIconColumn();
        // addIconRemoteWork();
        initRemoteWorkMap();

        initTableGenerator();
        initGroupSubscribeButton();
    }

    private void initGroupSubscribeButton() {
        if (userSession.getUser().getGroup().getName().equals(MANAGEMENT_GROUP) ||
                userSession.getUser().getGroup().getName().equals(HUNTING_GROUP)) {
            groupSubscribe.setVisible(true);
        } else {
            groupSubscribe.setVisible(false);
        }
    }

    private void initTableGenerator() {
        openPositionsTable.setItemClickAction(new BaseAction("itemClickAction")
                .withHandler(actionPerformedEvent ->
                        openPositionsTable.setDetailsVisible(openPositionsTable.getSingleSelected(), true)));

    }

    @Install(to = "openPositionsTable.remoteWork", subject = "descriptionProvider")
    private String openPositionsTableRemoteWorkDescriptionProvider(OpenPosition openPosition) {
        String retStr = String.valueOf(remoteWork.get(openPosition.getRemoteWork()));

        switch (openPosition.getRemoteWork()) {
            case 0:
                retStr = "Нет";
                break;
            case 1:
                retStr = "Удаленная работа";
                break;
            case 2:
                retStr = "Частично 50/50";
                break;
        }

        return retStr;
    }

    private void initRemoteWorkMap() {
        remoteWork.put("Нет", 0);
        remoteWork.put("Удаленная работа", 1);
        remoteWork.put("Частично 50/50", 2);
    }

    @Install(to = "openPositionsTable.remoteWork", subject = "columnGenerator")
    private Icons.Icon openPositionsTableRemoteWorkColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        String returnIcon = "";

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

    @Install(to = "openPositionsTable.description", subject = "columnGenerator")
    private Icons.Icon openPositionsTableDescriptionColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        String returnIcon = "";

        if (!event.getItem().getComment().startsWith("нет")) {
            returnIcon = "FILE_TEXT";
        } else {
            returnIcon = "FILE";
        }

        return CubaIcon.valueOf(returnIcon);
    }

    @Install(to = "openPositionsTable.description", subject = "descriptionProvider")
    private String openPositionsTableDescriptionDescriptionProvider(OpenPosition openPosition) {
        return Jsoup.parse(openPosition.getComment()).text();
    }

    @Install(to = "openPositionsTable.description", subject = "styleProvider")
    private String openPositionsTableDescriptionStyleProvider(OpenPosition openPosition) {
        String style = "";

        if (!openPosition.getComment().startsWith("нет")) {
            style = "open-position-pic-center-large-green";
        } else {
            style = "open-position-pic-center-large-red";
        }

        return style;
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
        DataGrid.Column<OpenPosition> iconColumn = openPositionsTable.addGeneratedColumn("icon",
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
        DataGrid.Column<OpenPosition> iconRemoteWork = openPositionsTable.addGeneratedColumn("remoteWork",
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
        String projectOwner = "";

        try {
            projectOwner = openPosition.getProjectName().getProjectOwner().getSecondName() + " " +
                    openPosition.getProjectName().getProjectOwner().getFirstName();
        } catch (NullPointerException e) {
            return a;
        }

        return "Владелец проекта: " + projectOwner + " \n" + a;
    }

    @Inject
    private Fragments fragments;

    @Install(to = "openPositionsTable", subject = "detailsGenerator")
    private Component openPositionsTableDetailsGenerator(OpenPosition entity) {

        OpenPositionDetailScreenFragment openPositionDetailScreenFragment =
                fragments.create(this, OpenPositionDetailScreenFragment.class);

        GroupBoxLayout mainLayout = uiComponents.create(GroupBoxLayout.NAME);
        mainLayout.setWidth("100%");

        HBoxLayout titleBox = uiComponents.create(HBoxLayout.NAME);
        HBoxLayout buttonsHBox = uiComponents.create(HBoxLayout.NAME);

        buttonsHBox.setSpacing(true);
        buttonsHBox.setAlignment(Component.Alignment.TOP_RIGHT);

        Component fragmentTitle = createTitleFragment(entity);
        Component closeButton = createCloseButton(entity);
        Component editButton = createEditButton(entity);
        closeButton.setAlignment(Component.Alignment.TOP_RIGHT);
        editButton.setAlignment(Component.Alignment.TOP_RIGHT);

        buttonsHBox.setAlignment(Component.Alignment.BOTTOM_RIGHT);
        buttonsHBox.add(editButton);
        buttonsHBox.add(closeButton);

        titleBox.add(fragmentTitle);
        titleBox.add(buttonsHBox);
        titleBox.setWidthFull();

        mainLayout.add(titleBox);

        Fragment fragment = openPositionDetailScreenFragment.getFragment();
        fragment.setWidth("100%");

        mainLayout.add(fragment);

        return mainLayout;
    }

    private Component createTitleFragment(OpenPosition entity) {
        Label<String> titleLabel = uiComponents.create(Label.NAME);
        titleLabel.setStyleName("h2");
        titleLabel.setValue(entity.getVacansyName());
        titleLabel.setAlignment(Component.Alignment.BOTTOM_LEFT);

        return titleLabel;
    }

    private Component createEditButton(OpenPosition entity) {
        Button editButton = uiComponents.create(Button.class);
        editButton.setCaption("Изменить");
        editButton.setIcon("EDIT");
        editButton.setAlignment(Component.Alignment.TOP_RIGHT);

        BaseAction editAction = new BaseAction("edit")
                .withHandler(actionPerformedEvent -> {
                    screenBuilders.editor(OpenPosition.class, this)
                            .editEntity(entity)
                            .build()
                            .show();
                })
                .withCaption("");
        editButton.setAction(editAction);
        return editButton;
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
                    if ((openPosition.getCommandCandidate() != null ? openPosition.getCommandCandidate() : 2) != 1)
                        return "open-position-empty-recrutier";
                    else
                        return "open-position-job-command";
                else
                    return "open-position-job-recruitier";
            }
        } else {
            if (openPosition.getCommandCandidate() != 1) {
                if (s == 0)
                    return "open-position-empty-recrutier";
                else
                    return "open-position-job-recruitier";
            } else
                return "open-position-job-command";
        }
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        checkBoxOnlyOpenedPosition.setValue(true); // только открытые позиции
        buttonExcel.setVisible(getRoleService.isUserRoles(userSession.getUser(), ROLE_MANAGER));

        setInternalProjectFilter();
        setSubcribersFilter();
        setOpenPositionNotPaused();

        openPositionsTable.getColumn("openClose").setCollapsed(true);
        openPositionsTable.getColumn("openClose").setCollapsible(true);
        openPositionsTable.getColumn("openClose").setVisible(true);
    }

    private void setOpenPositionNotPaused() {
        checkBoxOnlyNotPaused.setValue(true);
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

    @Subscribe("checkBoxOnlyNotPaused")
    public void onCheckBoxOnlyNotPausedValueChange(HasValue.ValueChangeEvent<Boolean> event) {
       if(checkBoxOnlyNotPaused.getValue()) {
           openPositionsDl.setParameter("paused", 0);
       } else {
           openPositionsDl.removeParameter("paused");
       }

       openPositionsDl.load();
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

    public void groupSubscribe() {
        screens.create(RecrutiesTasksGroupSubscribeBrowse.class).show();
    }
}

