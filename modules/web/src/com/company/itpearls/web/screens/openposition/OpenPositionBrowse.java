package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.UiNotificationEvent;
import com.company.itpearls.entity.*;
import com.company.itpearls.service.GetRoleService;
import com.company.itpearls.web.screens.fragments.Skillsbar;
import com.company.itpearls.web.screens.recrutiestasks.RecrutiesTasksGroupSubscribeBrowse;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.web.App;
import com.haulmont.reports.entity.Report;
import com.haulmont.reports.gui.ReportGuiManager;
import com.haulmont.reports.gui.actions.list.ListPrintFormAction;
import org.apache.commons.lang3.time.DateUtils;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicReference;

@UiController("itpearls_OpenPosition.browse")
@UiDescriptor("open-position-browse.xml")
@LookupComponent("openPositionsTable")
@LoadDataBeforeShow
public class OpenPositionBrowse extends StandardLookup<OpenPosition> {
    private static final String NULL_SALARY = "0 т.р./0 т.р.";
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
    private String ROLE_RESEARCHER = "Researcher";
    private static final String MANAGEMENT_GROUP = "Менеджмент";
    private static final String HUNTING_GROUP = "Хантинг";
    private String ROLE_ADMINISTRATOR = "Administrators";
    private Map<String, Integer> remoteWork = new LinkedHashMap<>();
    private Map<String, Integer> priorityMap = new LinkedHashMap<>();
    private Map<String, Integer> mapWorkExperience = new LinkedHashMap<>();
    private List<User> users = new ArrayList<>();
    private static String QUERY_SELECT_COMMAND = "select e from itpearls_OpenPosition e where e.parentOpenPosition = :parentOpenPosition and e.openClose = false";

    public final static int PRIORITY_DRAFT = -1;
    public final static int PRIORITY_PAUSED = 0;
    public final static int PRIORITY_LOW = 1;
    public final static int PRIORITY_NORMAL = 2;
    public final static int PRIORITY_HIGH = 3;
    public final static int PRIORITY_CRITICAL = 4;

    String QUERY_COUNT_ITERACTIONS = "select e.iteractionType, count(e.iteractionType) " +
            "from itpearls_IteractionList e " +
            "where e.dateIteraction between :startDate and :endDate and " +
            "(e.vacancy = :vacancy or " +
            "(e.vacancy in (select f from itpearls_OpenPosition f where f.parentOpenPosition = :vacancy))) and " +
            "e.iteractionType.statistics = true " +
            "group by e.iteractionType";

    @Inject
    private Button groupSubscribe;
    @Inject
    private Screens screens;
    @Inject
    private CollectionContainer<OpenPosition> openPositionsDc;
    @Inject
    private CheckBox checkBoxOnlyNotPaused;
    @Inject
    private LookupField notLowerRatingLookupField;
    @Inject
    private GroupBoxLayout urgentlyPositons;
    @Inject
    private HBoxLayout urgentlyHBox;
    @Inject
    private LookupField remoteWorkLookupField;
    @Inject
    private Events events;
    @Inject
    private Notifications notifications;
    @Inject
    private Button buttonSubscribe;
    @Inject
    private Button suggestCandidateButton;
    @Inject
    private RadioButtonGroup subscribeRadioButtonGroup;
    @Inject
    private ReportGuiManager reportGuiManager;
    @Inject
    private Actions actions;
    @Inject
    private Button listBtn;
    @Inject
    private Dialogs dialogs;
    @Inject
    private Button openCloseButton;

    @Subscribe
    protected void onInit(InitEvent event) {
        addIconColumn();
        initRemoteWorkMap();

        initTableGenerator();
        initGroupSubscribeButton();

        setMapOfPriority();
        setWorkExperienceMap();

        Action listPrintFormAction = actions.create(ListPrintFormAction.class, "listPrintFormAction");
        openPositionsTable.addAction(listPrintFormAction);
        listBtn.setAction(listPrintFormAction);

        String QUERY_USER = "select e from sec$User e where e.active = true";
        users = dataManager.load(User.class)
                .query(QUERY_USER)
                .list();

    }

    @Install(to = "subscribeRadioButtonGroup", subject = "optionDescriptionProvider")
    private String subscribeRadioButtonGroupOptionDescriptionProvider(Object object) {
        String retStr = "";

        switch ((Integer) object) {
            case 0:
                retStr = "Я не подписан на эти вакансии. Для работы с ними надо подписаться";
                break;
            case 1:
                retStr = "Находится в работе у меня на определенный период времени";
                break;
            case 2:
                retStr = "Все открытые вакансии";
                break;
            case 3:
                retStr = "Не находится ни у кого в работе. Свободная вакансия";
                break;
            case 4:
                retStr = "Новые вакансии окрытые за последние 3 дня";
                break;
            case 5:
                retStr = "Новые вакансии окрытые за последнюю неделю. За 7 дней";
                break;
            case 6:
                retStr = "Новые вакансии окрытые за последний месяц. За 30 дней";
                break;
            default:
                break;
        }

        return retStr;
    }

    private void initCheckBoxOnlyOpenedPosition() {
        Map<String, Integer> onlyOpenedPositionMap = new LinkedHashMap<>();

        if (!getRoleService.isUserRoles(userSession.getUser(), ROLE_RESEARCHER)) {
            onlyOpenedPositionMap.put("Все вакансии", 2);
        }

        onlyOpenedPositionMap.put("В подписке", 1);
        onlyOpenedPositionMap.put("Не в подписке", 0);
        onlyOpenedPositionMap.put("Свободные", 3);
        onlyOpenedPositionMap.put("Открытые за 3 дня", 4);
        onlyOpenedPositionMap.put("Открытые за неделю", 5);
        onlyOpenedPositionMap.put("Открытые за месяц", 6);
        onlyOpenedPositionMap.put("На паузе", 7);

        subscribeRadioButtonGroup.setOptionsMap(onlyOpenedPositionMap);

        if (userSession.getUser().getGroup().getName().equals(MANAGEMENT_GROUP)) {
            subscribeRadioButtonGroup.setValue(2);
        } else {
            subscribeRadioButtonGroup.setValue(1);
        }

        subscribeRadioButtonGroup.addValueChangeListener(e -> {
            listBtn.setEnabled(openPositionsTable.getSingleSelected() != null);
            buttonSubscribe.setEnabled(openPositionsTable.getSingleSelected() != null);

//            buttonSubscribe.setEnabled(((Integer) subscribeRadioButtonGroup.getValue()) == 0);
            suggestCandidateButton.setVisible(((Integer) subscribeRadioButtonGroup.getValue()) == 1);

            switch ((Integer) subscribeRadioButtonGroup.getValue()) {
                case 1:
                    openPositionsDl.setParameter("subscriber", userSession.getUser());
                    openPositionsDl.removeParameter("notsubscriber");
                    openPositionsDl.removeParameter("freesubscriber");
                    openPositionsDl.removeParameter("newOpenPosition");
                    openPositionsDl.removeParameter("priority");
                    checkBoxOnlyNotPaused.setValue(true);
                    break;
                case 0:
                    openPositionsDl.removeParameter("subscriber");
                    openPositionsDl.setParameter("notsubscriber", userSession.getUser());
                    openPositionsDl.removeParameter("freesubscriber");
                    openPositionsDl.removeParameter("newOpenPosition");
                    openPositionsDl.removeParameter("priority");
                    checkBoxOnlyNotPaused.setValue(true);
                    break;
                case 2:
                    openPositionsDl.removeParameter("subscriber");
                    openPositionsDl.removeParameter("notsubscriber");
                    openPositionsDl.removeParameter("freesubscriber");
                    openPositionsDl.removeParameter("newOpenPosition");
                    openPositionsDl.removeParameter("priority");
                    checkBoxOnlyNotPaused.setValue(false);
                    break;
                case 3:
                    openPositionsDl.removeParameter("subscriber");
                    openPositionsDl.removeParameter("notsubscriber");
                    openPositionsDl.setParameter("freesubscriber", false);
                    openPositionsDl.removeParameter("newOpenPosition");
                    openPositionsDl.removeParameter("priority");
                    checkBoxOnlyNotPaused.setValue(true);
                    break;
                case 4:
                    openPositionsDl.removeParameter("subscriber");
                    openPositionsDl.removeParameter("notsubscriber");
                    openPositionsDl.removeParameter("freesubscriber");
                    openPositionsDl.setParameter("newOpenPosition", 3);
                    openPositionsDl.removeParameter("priority");
                    checkBoxOnlyNotPaused.setValue(true);
                    break;
                case 5:
                    openPositionsDl.removeParameter("subscriber");
                    openPositionsDl.removeParameter("notsubscriber");
                    openPositionsDl.removeParameter("freesubscriber");
                    openPositionsDl.setParameter("newOpenPosition", 7);
                    openPositionsDl.removeParameter("priority");
                    checkBoxOnlyNotPaused.setValue(true);
                    break;
                case 6:
                    openPositionsDl.removeParameter("subscriber");
                    openPositionsDl.removeParameter("notsubscriber");
                    openPositionsDl.removeParameter("freesubscriber");
                    openPositionsDl.setParameter("newOpenPosition", 30);
                    openPositionsDl.removeParameter("priority");
                    checkBoxOnlyNotPaused.setValue(true);
                    break;
                case 7:
                    openPositionsDl.removeParameter("subscriber");
                    openPositionsDl.removeParameter("notsubscriber");
                    openPositionsDl.removeParameter("freesubscriber");
                    openPositionsDl.removeParameter("newOpenPosition");
                    openPositionsDl.setParameter("priority", 0);
                    checkBoxOnlyNotPaused.setValue(false);
                    break;
                default:
                    break;
            }

            openPositionsDl.load();
        });
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

    @Install(to = "notLowerRatingLookupField", subject = "optionIconProvider")
    private String notLowerRatingLookupFieldOptionIconProvider(Object object) {
        String icon = null;

        switch ((int) object) {
            case PRIORITY_DRAFT:
                icon = "icons/traffic-lights_gray.png";
                break;
            case PRIORITY_PAUSED: //"Paused"
                icon = "icons/remove.png";
                break;
            case PRIORITY_LOW: //"Low"
                icon = "icons/traffic-lights_blue.png";
                break;
            case PRIORITY_NORMAL: //"Normal"
                icon = "icons/traffic-lights_green.png";
                break;
            case PRIORITY_HIGH: //"High"
                icon = "icons/traffic-lights_yellow.png";
                break;
            case PRIORITY_CRITICAL: //"Critical"
                icon = "icons/traffic-lights_red.png";
                break;
        }

        return icon;
    }

    @Install(to = "remoteWorkLookupField", subject = "optionIconProvider")
    private String remoteWorkLookupFieldOptionIconProvider(Object object) {
        String returnIcon = "";

        switch ((int) object) {
            case 1:
                returnIcon = "font-icon:PLUS_CIRCLE";
                break;
            case 0:
                returnIcon = "font-icon:MINUS_CIRCLE";
                break;
            case 2:
                returnIcon = "font-icon:QUESTION_CIRCLE";
                break;
            default:
                returnIcon = "font-icon:QUESTION_CIRCLE";
                break;
        }

        return returnIcon;
    }


    @Install(to = "openPositionsTable.remoteWork", subject = "descriptionProvider")
    private String openPositionsTableRemoteWorkDescriptionProvider(OpenPosition openPosition) {
        String retStr = String.valueOf(remoteWork.get(openPosition.getRemoteWork()));

        switch (openPosition.getRemoteWork()) {
            case 0:
                retStr = "Работа в офисе";
                break;
            case 1:
                retStr = "Удаленная работа";
                break;
            case 2:
                retStr = "Частично удаленная (офис + удаленная)";
                break;
        }

        return openPosition.getCityPosition() == null ? retStr : retStr +
                "\nЖелаемая локация: " + openPosition.getCityPosition().getCityRuName() +
                (openPosition.getRemoteComment() != null ?
                        "\nКомментарий: " + openPosition.getRemoteComment() : "");
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

    @Install(to = "openPositionsTable.testExserice", subject = "columnGenerator")
    private Object openPositionsTableTestExsericeColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        String returnIcon = "";

        if (event.getItem().getNeedExercise() != null) {
            if (event.getItem().getNeedExercise())
                returnIcon = "PLUS_CIRCLE";
            else
                returnIcon = "MINUS_CIRCLE";
        } else
            returnIcon = "MINUS_CIRCLE";

        return CubaIcon.valueOf(returnIcon);
    }

    @Install(to = "openPositionsTable.testExserice", subject = "descriptionProvider")
    private String openPositionsTableTestExsericeDescriptionProvider(OpenPosition openPosition) {
        if (openPosition.getExercise() != null)
            return Jsoup.parse(openPosition.getExercise()).text();
        else
            return "";
    }

    @Install(to = "openPositionsTable.description", subject = "columnGenerator")
    private Icons.Icon openPositionsTableDescriptionColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        String returnIcon = "";

        if (event.getItem().getComment() != null) {
            if (!event.getItem().getComment().startsWith("нет")) {
                returnIcon = "FILE_TEXT";
            } else {
                returnIcon = "FILE";
            }
        } else
            returnIcon = "FILE";

        return CubaIcon.valueOf(returnIcon);
    }

    @Install(to = "openPositionsTable.description", subject = "descriptionProvider")
    private String openPositionsTableDescriptionDescriptionProvider(OpenPosition openPosition) {
        return Jsoup.parse(openPosition.getComment()).text();
    }

    @Install(to = "openPositionsTable.description", subject = "styleProvider")
    private String openPositionsTableDescriptionStyleProvider(OpenPosition openPosition) {
        String style = "";

        if (openPosition.getComment() != null) {
            if (!openPosition.getComment().startsWith("нет")) {
                style = "open-position-pic-center-large-green";
            } else {
                style = "open-position-pic-center-large-red";
            }
        }

        return style;
    }

    @Install(to = "openPositionsTable.testExserice", subject = "styleProvider")
    private String openPositionsTableTestExsericeStyleProvider(OpenPosition openPosition) {
        if (openPosition.getNeedExercise() != null) {
            if (openPosition.getNeedExercise()) {
                return "open-position-pic-center-large-green";
            } else {
                return "open-position-pic-center-large-red";
            }
        } else
            return "open-position-pic-center-large-red";
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
                style = (openPosition.getRemoteComment() == null ?
                        "open-position-pic-center-large-green" :
                        "open-position-pic-center-large-lime");
                break;
            case 2:
                style = (openPosition.getRemoteComment() == null ?
                        "open-position-pic-center-large-red" :
                        "open-position-pic-center-large-maroon");
                break;
            case 0:
                style = (openPosition.getRemoteComment() == null ?
                        "open-position-pic-center-large-yellow" :
                        "open-position-pic-center-large-orange");
                break;
        }

        return style;
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
        String QUERY_RECRUTIER_TASK = "select e " +
                "from itpearls_RecrutiesTasks e " +
                "where e.endDate > current_date " +
                "and e.closed = false " +
                "and e.openPosition = :openPosition";

        String returnData = "";

        List<RecrutiesTasks> recrutiesTasks = dataManager.load(RecrutiesTasks.class)
                .view("recrutiesTasks-view")
                .query(QUERY_RECRUTIER_TASK)
                .parameter("openPosition", openPosition)
                .list();

        if (openPosition.getShortDescription() != null) {
            returnData = returnData + "\n<b>Кратко: </b><i>" + openPosition.getShortDescription() + "</i><br><br>";
        }

        if (recrutiesTasks.size() != 0) {
            returnData = returnData + "\n<b>В работе у:</b><br>";

            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

            for (RecrutiesTasks a : recrutiesTasks) {
                returnData = returnData + "\n" + a.getReacrutier().getName()
                        + " до <i>"
                        + sdf.format(a.getEndDate())
                        + "</i><br>";
            }

            returnData = returnData.substring(0, returnData.length() - 1);
        }

        returnData = returnData != null ? Jsoup.parse(returnData).text() : "";

        return openPosition.getVacansyName() + "\n" + returnData;
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

        return openPosition.getProjectName().getProjectName()
                + "\n\nВладелец проекта: " + projectOwner + "\n\n" + a;
    }

    @Inject
    private Fragments fragments;

    @Install(to = "openPositionsTable", subject = "detailsGenerator")
    private Component openPositionsTableDetailsGenerator(OpenPosition entity) {

        OpenPositionDetailScreenFragment openPositionDetailScreenFragment =
                fragments.create(this, OpenPositionDetailScreenFragment.class);

        openPositionDetailScreenFragment.setOpenPosition(entity);
        openPositionDetailScreenFragment.setLabels();

        GroupBoxLayout mainLayout = uiComponents.create(GroupBoxLayout.NAME);
        mainLayout.setWidth("100%");

        HBoxLayout titleBox = uiComponents.create(HBoxLayout.NAME);
        HBoxLayout buttonsHBox = uiComponents.create(HBoxLayout.NAME);

        buttonsHBox.setSpacing(true);
        buttonsHBox.setAlignment(Component.Alignment.TOP_RIGHT);

        Component suitableButton = findSuitableButton(entity);
        Component fragmentTitle = createTitleFragment(entity);
        Component closeButton = createCloseButton(entity);
        Component editButton = createEditButton(entity);
        Component priorityField = createPriorityField(entity);
        Component openCloseButton = createOpenCloseButton(entity);
        Component viewDescriptionButton = createViewDescriptionButton(entity);

        closeButton.setAlignment(Component.Alignment.TOP_RIGHT);
        openCloseButton.setAlignment(Component.Alignment.TOP_RIGHT);
        editButton.setAlignment(Component.Alignment.TOP_RIGHT);
        priorityField.setAlignment(Component.Alignment.TOP_RIGHT);

        buttonsHBox.setAlignment(Component.Alignment.BOTTOM_RIGHT);

        buttonsHBox.add(priorityField);
        buttonsHBox.add(openCloseButton);
        buttonsHBox.add(editButton);
        buttonsHBox.add(viewDescriptionButton);

        if (suitableButton != null)
            buttonsHBox.add(suitableButton);

        buttonsHBox.add(closeButton);

        titleBox.add(fragmentTitle);
        titleBox.add(buttonsHBox);
        titleBox.setWidthFull();

        mainLayout.add(titleBox);

        Fragment fragment = openPositionDetailScreenFragment.getFragment();
        fragment.setWidth("100%");
        mainLayout.add(fragment);


        Skillsbar skillBoxFragment = fragments.create(this, Skillsbar.class);
        if (skillBoxFragment.generateSkillLabels(
                openPositionsTable.getSingleSelected().getCommentEn() != null ?
                        openPositionsTable.getSingleSelected().getCommentEn() :
                        openPositionsTable.getSingleSelected().getComment())) {
            mainLayout.add(skillBoxFragment.getFragment());
        }

        closeAllAnoterDetailsScreenFragments();

        return mainLayout;
    }

    // TODO закрыть все Details другие
    private void closeAllAnoterDetailsScreenFragments() {
        for (OpenPosition op : openPositionsTable.getItems().getItems(0,
                openPositionsDc.getItems().size())) {
            openPositionsTable.setDetailsVisible(op, false);
        }
    }


    private Component createViewDescriptionButton(OpenPosition entity) {
        Button retButton = uiComponents.create(Button.NAME);
        retButton.setIcon(CubaIcon.STREET_VIEW.iconName());
        retButton.setEnabled(openPositionsTable.getSingleSelected() != null);
        retButton.setCaption("Описание");
        retButton.setDescription("Просмотр описания вакансии и описания проекта");

        retButton.addClickListener(e -> {

            if (openPositionsTable.getSingleSelected() != null) {
                QuickViewOpenPositionDescription quickViewOpenPositionDescription = screens.create(QuickViewOpenPositionDescription.class);
                quickViewOpenPositionDescription.setJobDescription(openPositionsTable.getSingleSelected() != null ?
                        openPositionsTable.getSingleSelected()
                                .getComment() : "");

                if (openPositionsTable.getSingleSelected().getProjectName().getProjectDescription() != null) {
                    quickViewOpenPositionDescription.setProjectDescription(openPositionsTable
                            .getSingleSelected()
                            .getProjectName()
                            .getProjectDescription() != null ?
                            openPositionsTable.getSingleSelected()
                                    .getProjectName()
                                    .getProjectDescription() : "");
                }

                if (openPositionsTable.getSingleSelected().getCommentEn() != null) {
                    String a = openPositionsTable.getSingleSelected().getCommentEn();

                    quickViewOpenPositionDescription.setJobDescriptionEng(openPositionsTable
                            .getSingleSelected()
                            .getCommentEn() != null ?
                            openPositionsTable.getSingleSelected().getCommentEn() : "");
                }

                if (openPositionsTable.getSingleSelected()
                        .getProjectName()
                        .getProjectDepartment()
                        .getCompanyName()
                        .getWorkingConditions() != null) {
                    quickViewOpenPositionDescription.setCompanyWorkConditions(openPositionsTable.getSingleSelected()
                            .getProjectName()
                            .getProjectDepartment()
                            .getCompanyName()
                            .getWorkingConditions());
                }

                if (openPositionsTable.getSingleSelected()
                        .getProjectName()
                        .getProjectDepartment()
                        .getCompanyName() != null) {
                    quickViewOpenPositionDescription.setCompanyDescription(openPositionsTable.getSingleSelected()
                            .getProjectName()
                            .getProjectDepartment()
                            .getCompanyName().getCompanyDescription());
                }

                quickViewOpenPositionDescription.reloadDescriptions();
                screens.show(quickViewOpenPositionDescription);

            } else {
                notifications.create(Notifications.NotificationType.WARNING)
                        .withCaption("ВНИМАНИЕ")
                        .withDescription("Куакую вакансию вы хотите просмотреть?")
                        .show();
            }

        });

        return retButton;
    }

    private Component findSuitableButton(OpenPosition entity) {

        if (dataManager.load(CandidateCV.class)
                .query("select e from itpearls_CandidateCV e where e.resumePosition = :resumePosition")
                .parameter("resumePosition", entity.getPositionType())
                .cacheable(true)
                .list().size() != 0) {

            Button suitableButton = uiComponents.create(Button.class);
            suitableButton.setDescription("Подобрать резюме по вакансии");
            suitableButton.setCaption("Подобрать");
            suitableButton.setIconFromSet(CubaIcon.EYE);

            suitableButton.setAction(new BaseAction("Suggestjobcandidate")
                    .withHandler(actionPerformedEvent -> {
                        Suggestjobcandidate suggestjobcandidate = screens.create(Suggestjobcandidate.class);
                        suggestjobcandidate.setOpenPosition(entity);

                        suggestjobcandidate.show();
                    }));

            return suitableButton;
        } else {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("ВНИМАНИЕ")
                    .withDescription("Нет кандидатов в базе для выбранной вакансии")
                    .show();

            return null;
        }
    }

    private void setOpenPositionNewsAutomatedMessage(OpenPosition editedEntity,
                                                     String subject,
                                                     String comment,
                                                     Date date,
                                                     User user) {

        OpenPositionNews openPositionNews = new OpenPositionNews();

        openPositionNews.setOpenPosition(editedEntity);
        openPositionNews.setAuthor(user);
        openPositionNews.setDateNews(date);
        openPositionNews.setSubject(subject);
        openPositionNews.setComment(comment);
        openPositionNews.setPriorityNews(true);

        CommitContext commitContext = new CommitContext();
        commitContext.addInstanceToCommit(openPositionNews);
        dataManager.commit(commitContext);
    }

    private Component createOpenCloseButton(OpenPosition entity) {
        Button retButton = uiComponents.create(Button.NAME);

        retButton.setIcon(!entity.getOpenClose() ? CubaIcon.REMOVE_ACTION.iconName() : CubaIcon.ADD_ACTION.iconName());

        retButton.setCaption(!entity.getOpenClose() ? "Закрыть" : "Открыть");
        retButton.setDescription(!entity.getOpenClose() ? "Закрыть вакансию" : "Открыть вакансию");

        retButton.addClickListener(e -> {
            openCloseButtonClickListener(entity, retButton);
        });

        return retButton;
    }

    private void openCloseButtonClickListener(OpenPosition entity, Button retButton) {
        if (!openCloseChildVacancy(entity)) {
            entity.setOpenClose(!entity.getOpenClose());

            retButton.setCaption(!entity.getOpenClose() ? "Закрыть" : "Открыть");
            retButton.setDescription(!entity.getOpenClose() ? "Закрыть вакансию" : "Открыть вакансию");

            if (entity.getOpenClose() || entity.getOpenClose() == null) {
                events.publish(new UiNotificationEvent(this, "Закрыта вакансия: "
                        + entity.getVacansyName()
                        + "<br><svg align=\"right\" width=\"100%\"><i>"
                        + userSession.getUser().getName()
                        + "</i></svg>"));

                setOpenPositionNewsAutomatedMessage(openPositionsTable.getSingleSelected(),
                        "Закрылась вакансия",
                        "Закрыта вакансия",
                        new Date(),
                        userSession.getUser());

                entity.setOwner(null);
                entity.setLastOpenDate(null);

                openPositionsTable.setDetailsVisible(entity, false);
                openPositionsDl.load();

            } else {
                events.publish(new UiNotificationEvent(this, "Открыта вакансия: "
                        + entity.getVacansyName()
                        + "<br><svg align=\"right\" width=\"100%\"><i>"
                        + userSession.getUser().getName()
                        + "</i></svg>"));

                setOpenPositionNewsAutomatedMessage(openPositionsTable.getSingleSelected(),
                        "Открылась вакансия",
                        "Открыта вакансия",
                        new Date(),
                        userSession.getUser());

                entity.setOwner(userSession.getUser());
                entity.setLastOpenDate(new Date());
                entity.setPriority(PRIORITY_NORMAL);

                if (entity.getParentOpenPosition() != null) {
                    if (entity.getParentOpenPosition().getOpenClose()) {
                        entity.getParentOpenPosition().setOpenClose(false);
                        setOpenPositionNewsAutomatedMessage(openPositionsTable.getSingleSelected().getParentOpenPosition(),
                                "Открыта дочерней вакансией "
                                        + openPositionsTable.getSingleSelected().getVacansyName(),
                                "Открыта ввиду открытия дочерней вакансии "
                                        + openPositionsTable.getSingleSelected().getVacansyName(),
                                new Date(),
                                userSession.getUser());
                    }
                }

                openPositionsTable.setDetailsVisible(entity, false);
                openPositionsDl.load();
            }

            dataManager.commit(entity);

            if (!entity.getOpenClose()) {
                entity.getProjectName().setProjectIsClosed(false);
            }

            openPositionsDl.load();
        } else {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withDescription("Невозможно закрыть вакансию верхнего уровня без закрытия вакансий входящих в ее группу")
                    .withCaption("ВНИМАНИЕ")
                    .show();
        }
    }

    private Boolean openCloseChildVacancy(OpenPosition event) {
        List<OpenPosition> openPositions = dataManager.load(OpenPosition.class)
                .query(QUERY_SELECT_COMMAND)
                .parameter("parentOpenPosition", event)
                .list();

        String magPos = "";
        AtomicReference<Boolean> flagDialog = new AtomicReference<>(false);

        if (openPositions.size() != 0) {
            for (OpenPosition a : openPositions) {
                magPos = magPos + "<li><i>" + a.getVacansyName() + "</i></li>";
            }

            dialogs.createOptionDialog()
                    .withType(Dialogs.MessageType.WARNING)
                    .withContentMode(ContentMode.HTML)
                    .withCaption("ВНИМАНИЕ")
                    .withMessage((event.getOpenClose() ? "Открыть" : "Закрыть") +
                            " вакансии группы?<br><ul>" + magPos + "</ul>")
                    .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                        for (OpenPosition a : openPositions) {
                            a.setOpenClose(event.getOpenClose());
                        }

                        flagDialog.set(true);
                    }), new DialogAction(DialogAction.Type.NO))
                    .show();

        }

        return flagDialog.get();
    }

    @Install(to = "openPositionsTable.salaryMinMax", subject = "columnGenerator")
    private String openPositionsTableSalaryMinMaxColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        String retStr = "";

        try {
            retStr = getSalaryString(event.getItem()) +
                    (event.getItem().getSalaryComment() != null ? " \ud83d\udcc3" : "");
        } catch (NullPointerException e) {
            retStr = "";
        }

        return retStr;
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

    @Install(to = "openPositionsTable.salaryMinMax", subject = "descriptionProvider")
    private String openPositionsTableSalaryMinMaxDescriptionProvider(OpenPosition openPosition) {
        String retStr = "";

        if (openPosition.getSalaryFixLimit() != null) {
            if (openPosition.getSalaryFixLimit()) {
                retStr = "Фиксированное запрлатное предложение.\n";
            }
        }

        try {
            retStr = retStr + getSalaryStringCaption(openPosition);
        } catch (NullPointerException e) {
            retStr = "";
        }

        return retStr
                + (openPosition.getSalaryComment() != null ? "\n\n" + openPosition.getSalaryComment() : "");
    }

    @Install(to = "openPositionsTable.salaryMinMax", subject = "styleProvider")
    private String openPositionsTableSalaryMinMaxStyleProvider(OpenPosition openPosition) {
        String retStr = "";

        if (openPosition.getSalaryFixLimit() != null) {
            if (openPosition.getSalaryFixLimit()) {
                retStr = "salary-fix-limit";
            }
        }

        return retStr;
    }

    @Install(to = "openPositionsTable.cityPositionList", subject = "columnGenerator")
    private Object openPositionsTableCityPositionListColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        String mainCity = "";

        if (event.getItem().getCityPosition() != null) {
            if (event.getItem().getCityPosition().getCityRuName() != null) {
                mainCity = event.getItem().getCityPosition().getCityRuName();
            }
        }

        return mainCity + ((event.getItem().getCities().size() != 0) ? " [+]" : "");

    }

    Boolean flagPriority = true;

    private Component createPriorityField(OpenPosition entity) {
        LookupField retField = uiComponents.create(LookupField.NAME);

        retField.setOptionsMap(priorityMap);
        retField.setValue(entity.getPriority());
        retField.setAlignment(Component.Alignment.TOP_RIGHT);
        retField.addValueChangeListener(f -> {
            int value = (int) retField.getValue();
            Optional<String> result = priorityMap.entrySet()
                    .stream()
                    .filter(entry -> value == entry.getValue())
                    .map(Map.Entry::getKey)
                    .findFirst();

            openPositionsTable.getSingleSelected().setPriority((int) retField.getValue());
            dataManager.commit(entity);

            events.publish(new UiNotificationEvent(this,
                    "Изменен приоритет вакансии <b>"
                            + openPositionsTable.getSingleSelected().getVacansyName()
                            + "</b> на <b>"
                            + result.get()
                            + "</b><br><svg align=\"right\" width=\"100%\"><i>"
                            + userSession.getUser().getName()
                            + "</i></svg>"));

            if (flagPriority) {
                setOpenPositionNewsAutomatedMessage(openPositionsTable.getSingleSelected(),
                        "Изменен приоритет вакансии на " + result.get(),
                        "Изменен приоритет вакансии на " + result.get(),
                        new Date(),
                        userSession.getUser());
            } else {
                flagPriority = true;
            }

            openPositionsDl.load();
        });

        retField.setLookupSelectHandler(e -> {
            openPositionsTable.getSingleSelected().setPriority((int) retField.getValue());
            dataManager.commit(entity);
            openPositionsDl.load();
        });

        retField.setOptionIconProvider(g -> {
            String icon = null;

            switch (g.hashCode()) {
                case PRIORITY_DRAFT:
                    icon = "icons/traffic-lights_gray.png";
                    break;
                case PRIORITY_PAUSED: //"Paused"
                    icon = "icons/remove.png";
                    break;
                case PRIORITY_LOW: //"Low"
                    icon = "icons/traffic-lights_blue.png";
                    break;
                case PRIORITY_NORMAL: //"Normal"
                    icon = "icons/traffic-lights_green.png";
                    break;
                case PRIORITY_HIGH: //"High"
                    icon = "icons/traffic-lights_yellow.png";
                    break;
                case PRIORITY_CRITICAL: //"Critical"
                    icon = "icons/traffic-lights_red.png";
                    break;
            }

            return icon;
        });

        return retField;
    }

    private Component createTitleFragment(OpenPosition entity) {
        Label<String> titleLabel = uiComponents.create(Label.NAME);
        titleLabel.setStyleName("h2");
        titleLabel.setDescription(entity.getVacansyName());
        titleLabel.setValue(entity.getVacansyName());
        titleLabel.setAlignment(Component.Alignment.BOTTOM_LEFT);

        return titleLabel;
    }

    private Component createEditButton(OpenPosition entity) {
        Button editButton = uiComponents.create(Button.class);
        editButton.setCaption("Изменить");
        editButton.setIcon(CubaIcon.EDIT.iconName());
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
        String returnStr = "";

        Integer s = dataManager.loadValue("select count(e.reacrutier) " +
                "from itpearls_RecrutiesTasks e " +
                "where e.openPosition = :openPos and " +
                "e.closed = false and " +
                "e.endDate >= :currentDate", Integer.class)
                .parameter("openPos", openPosition)
                .parameter("currentDate", new Date())
                .one();

        if (openPosition.getSignDraft() == null ? true : !openPosition.getSignDraft()) {
            if (openPosition.getInternalProject() != null) {
                if (openPosition.getInternalProject()) {
                    if (s == 0) {
                        returnStr = "open-position-internal-project";
                    } else {
                        returnStr = "open-position-internal-project-job-recrutier";
                    }
                } else {
                    if (s == 0)
                        if ((openPosition.getCommandCandidate() != null ? openPosition.getCommandCandidate() : 2) != 1)
                            returnStr = "open-position-empty-recrutier";
                        else
                            returnStr = "open-position-job-command";
                    else
                        returnStr = "open-position-job-recruitier";
                }
            } else {
                if (openPosition.getCommandCandidate() != 1) {
                    if (s == 0)
                        returnStr = "open-position-empty-recrutier";
                    else
                        returnStr = "open-position-job-recruitier";
                } else
                    returnStr = "open-position-job-command";
            }
        } else {
            return "open-position-draft";
        }

        if (openPosition.getOpenClose() != null) {
            if (openPosition.getOpenClose()) {
                returnStr = "open-position-close-position";
            }

        }

        return returnStr;
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        checkBoxOnlyOpenedPosition.setValue(true); // только открытые позиции
        buttonExcel.setVisible(getRoleService.isUserRoles(userSession.getUser(), ROLE_MANAGER));

        setInternalProjectFilter();
        setSubcribersFilter();
        setOpenPositionNotPaused();
        setStatusNotLower();
        setStatusRemoteWork();
        setUrgentlyPositios(3);

        clearUrgentFilter();
        setButtonsEnableDisable();

        initCheckBoxOnlyOpenedPosition();

    }

    private void setButtonsEnableDisable() {
        buttonSubscribe.setEnabled(false);
        listBtn.setEnabled(false);
        openCloseButton.setEnabled(false);
        openPositionsTable.addSelectionListener(e -> {
            if (e.getSelected() == null) {
                buttonSubscribe.setEnabled(false);
                listBtn.setEnabled(false);

                openCloseButton.setEnabled(false);
                openCloseButton.setIconFromSet(CubaIcon.CLOSE);
                openCloseButton.setCaption("Открыть / Закрыть");
            }
        });

        openPositionsTable.addItemClickListener(e -> {
            buttonSubscribe.setEnabled(true);
            listBtn.setEnabled(true);
            if (getRoleService.isUserRoles(userSession.getUser(), ROLE_MANAGER)) {
                if (e.getItem().getOpenClose()) {
                    openCloseButton.setEnabled(true);
                    openCloseButton.setIconFromSet(CubaIcon.YES);
                    openCloseButton.setCaption("Открыть");
                } else {
                    openCloseButton.setEnabled(true);
                    openCloseButton.setIconFromSet(CubaIcon.CLOSE);
                    openCloseButton.setCaption("Закрыть");
                }
            }
        });
    }

    @Subscribe("remoteWorkLookupField")
    public void onRemoteWorkLookupFieldValueChange(HasValue.ValueChangeEvent event) {
        openPositionsDl.setParameter("remoteWork", remoteWorkLookupField.getValue());
        openPositionsDl.load();
    }

    @Subscribe("notLowerRatingLookupField")
    public void onNotLowerRatingLookupFieldValueChange1(HasValue.ValueChangeEvent event) {
        removeUrgentlyLists();

        if (notLowerRatingLookupField.getValue() != null) {
            if (((int) notLowerRatingLookupField.getValue()) != PRIORITY_DRAFT) {
                setUrgentlyPositios(notLowerRatingLookupField.getValue() == null ? 0 : (int) notLowerRatingLookupField.getValue());
            } else {
                openPositionsDl.removeParameter("rating");
                openPositionsDl.setParameter("signDraft", true);
            }
        } else {
            openPositionsDl.removeParameter("rating");
        }

        openPositionsDl.load();
    }

    private void removeUrgentlyLists() {
        for (Component component : urgentlyHBox.getComponents()) {
            urgentlyHBox.remove(component);
        }
    }

    private void setUrgentlyPositios(int priority) {
        String QUERY_URGENTLY_POSITIONS = "select e from itpearls_OpenPosition e " +
                "where e.openClose = false and " +
                "e.priority >= :priority";

        List<OpenPosition> openPositions = dataManager.load(OpenPosition.class)
                .query(QUERY_URGENTLY_POSITIONS)
                .parameter("priority", priority)
                .cacheable(true)
                .view("openPosition-view")
                .list();

//        Map<Integer,String> opList = new ArrayList<Integer, String>();
        HashMap<String, Integer> opList = new HashMap<>();

        // перемешать коллекцию случайным образом
        Collections.shuffle(openPositions);

        for (OpenPosition op : openPositions) {
            if (op.getPositionType() != null) {
                if (op.getPositionType().getPositionRuName() != null) {
                    String positionName = op.getPositionType().getPositionRuName();

                    if (!opList.containsKey(positionName)) {
                        opList.put(positionName, op.getPriority());
                    } else {
                        if (opList.get(positionName) < op.getPriority()) {
                            opList.remove(positionName);
                            if (positionName != null) {
                                opList.put(positionName, op.getPriority());
                            }
                        }
                    }
                }
            }
        }

        if (opList.size() == 0) {
            urgentlyPositons.setVisible(false);
        }

        for (HashMap.Entry op : opList.entrySet()) {
            Integer countOp = 0;

            LinkButton label = uiComponents.create(LinkButton.NAME);

            String opDescriptiom = "<b><u>Проекты:</u></b><br>";

            for (OpenPosition op1 : openPositions) {
                if (op1.getPositionType() != null) {
                    if (op1.getPositionType().getPositionRuName() != null) {

                        if (op.getKey().equals(op1.getPositionType().getPositionRuName()) &&
                                (!op1.getOpenClose() || op1.getOpenClose() == null)) {
                            opDescriptiom = opDescriptiom + op1.getProjectName().getProjectName() + "<br>";

                            if (notLowerRatingLookupField.getValue() != null) {
                                if ((int) notLowerRatingLookupField.getValue() >= 0) {
                                    if (op1.getPriority() >= (int) notLowerRatingLookupField.getValue() &&
                                            !op1.getOpenClose()) {
                                        countOp = countOp + op1.getNumberPosition();
                                    }
                                }
                            } else if (!op1.getOpenClose()) {
                                if (op1.getNumberPosition() != null) {
                                    countOp = countOp + op1.getNumberPosition();
                                }
                            }
                        }
                    }
                }
            }

            label.setCaption(op.getKey().toString() + " (" + countOp.toString() + ")");
            label.setDescriptionAsHtml(true);
            label.setDescription(opDescriptiom);
            label.addClickListener(clickEvent -> {
                OpenPosition opRet = null;

                for (OpenPosition ops : openPositions) {
                    if (ops.getPositionType() != null) {
                        if (ops.getPositionType().getPositionRuName() != null) {
                            if (ops.getPositionType().getPositionRuName().equals(op.getKey())) {
                                opRet = ops;
                                break;
                            }
                        }
                    }

                }

                openPositionsDl.setParameter("rating", opRet.getPriority());
                openPositionsDl.setParameter("positionType", opRet.getPositionType());
                openPositionsDl.load();
            });

            switch ((int) op.getValue()) {
                case 1:
                    label.setStyleName("label_button_blue");
                    break;
                case 2:
                    label.setStyleName("label_button_green");
                    break;
                case 3:
                    label.setStyleName("label_button_orange");
                    break;
                case 4:
                    label.setStyleName("label_button_red");
                    break;
                default:
                    label.setStyleName("label_button_grey");
                    break;
            }

            Label label1 = uiComponents.create(Label.NAME);
            label1.setValue(" ");

            urgentlyHBox.add(label);
            urgentlyHBox.add(label1);
        }
    }

    private void setMapOfPriority() {
        priorityMap.put("Draft", PRIORITY_DRAFT);
        priorityMap.put("Paused", PRIORITY_PAUSED);
        priorityMap.put("Low", PRIORITY_LOW);
        priorityMap.put("Normal", PRIORITY_NORMAL);
        priorityMap.put("High", PRIORITY_HIGH);
        priorityMap.put("Critical", PRIORITY_CRITICAL);
    }

    private void setStatusNotLower() {
        notLowerRatingLookupField.setOptionsMap(priorityMap);
    }

    private void setStatusRemoteWork() {
        remoteWorkLookupField.setOptionsMap(remoteWork);
    }

    @Subscribe("notLowerRatingLookupField")
    public void onNotLowerRatingLookupFieldValueChange(HasValue.ValueChangeEvent event) {
        if (notLowerRatingLookupField.getValue() != null) {
            openPositionsDl.setParameter("rating", notLowerRatingLookupField.getValue());
        } else {
            openPositionsDl.removeParameter("rating");
        }

        openPositionsDl.load();
    }

    private void setOpenPositionNotPaused() {
        checkBoxOnlyNotPaused.setValue(true);
    }

    private void setInternalProjectFilter() {
        if (!getRoleService.isUserRoles(userSession.getUser(), ROLE_RESEARCHER)) {
            openPositionsDl.removeParameter("subscriber");
        } else {
            openPositionsDl.setParameter("subscriber", userSession.getUser());
        }

        openPositionsDl.load();
    }

    private void setSubcribersFilter() {
        if (checkBoxOnlyMySubscribe.getValue()) {
            openPositionsDl.setParameter("subscriber", userSession.getUser());
            openPositionsDl.removeParameter("notsubscriber");
            openPositionsDl.removeParameter("freesubscriber");
            openPositionsDl.removeParameter("newOpenPosition");
        } else {
            openPositionsDl.removeParameter("subscriber");
            openPositionsDl.setParameter("notsubscriber", userSession.getUser());
            openPositionsDl.removeParameter("freesubscriber");
            openPositionsDl.removeParameter("newOpenPosition");
        }

        openPositionsDl.load();
    }

    @Subscribe("checkBoxOnlyMySubscribe")
    public void onCheckBoxOnlyMySubscribeValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        setSubcribersFilter();
    }

    @Subscribe("checkBoxOnlyNotPaused")
    public void onCheckBoxOnlyNotPausedValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (checkBoxOnlyNotPaused.getValue()) {
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
        } else {
            openPositionsDl.removeParameter("openClosePos");
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
                case PRIORITY_DRAFT:
                    icon = "icons/traffic-lights_gray.png";
                    break;
                case PRIORITY_PAUSED: //"Paused"
                    icon = "icons/traffic-lights_gray.png";
                    break;
                case PRIORITY_LOW: //"Low"
                    icon = "icons/traffic-lights_blue.png";
                    break;
                case PRIORITY_NORMAL: //"Normal"
                    icon = "icons/traffic-lights_green.png";
                    break;
                case PRIORITY_HIGH: //"High"
                    icon = "icons/traffic-lights_yellow.png";
                    break;
                case PRIORITY_CRITICAL: //"Critical"
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

        opScreen.addAfterCloseListener(e -> {
            this.openPositionsDl.load();
        });

        opScreen.show();
    }

    public void groupSubscribe() {
        screens.create(RecrutiesTasksGroupSubscribeBrowse.class).show();
    }

    public void clearUrgentFilter() {
        if (notLowerRatingLookupField.getValue() != null) {
            openPositionsDl.setParameter("rating", (int) notLowerRatingLookupField.getValue());
        } else {
            openPositionsDl.removeParameter("rating");
        }

        openPositionsDl.removeParameter("positionType");
        openPositionsDl.load();
    }

    @Install(to = "openPositionsTable.positionType", subject = "descriptionProvider")
    private String openPositionsTablePositionTypeDescriptionProvider(OpenPosition openPosition) {
        return openPosition.getPositionType() != null ? openPosition.getPositionType().getPositionRuName()
                + (openPosition.getPositionType().getPositionEnName() != null ? "/"
                + openPosition.getPositionType().getPositionEnName() : "")
                : "";
    }

    @Install(to = "openPositionsTable.cityPositionList", subject = "descriptionProvider")
    private String openPositionsTableCityPositionListDescriptionProvider(OpenPosition openPosition) {
        String outStr = "";

        if (openPosition.getCityPosition() != null) {
            outStr = openPosition.getCityPosition().getCityRuName();
        }

        if (openPosition.getCities() != null) {
            for (City s : openPosition.getCities()) {
                if (!outStr.equals("")) {
                    outStr = outStr + ",";
                }

                outStr = outStr + s.getCityRuName();
            }
        }

        return outStr;
    }

    @Subscribe("openPositionsTable")
    public void onOpenPositionsTableEditorClose(DataGrid.EditorCloseEvent event) {
        openPositionsDl.load();
    }

    @Install(to = "openPositionsTable.queryQuestion", subject = "columnGenerator")
    private Object openPositionsTableQueryQuestionColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        String returnIcon = "";

        switch (getQueryQuestion(event)) {
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

    private int getQueryQuestion(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {

        int retInt;

        if (getTemplateLetter(event.getItem()) != "") {
            retInt = 1;
        } else {
            retInt = 0;
        }

        return retInt;
    }

    private String getTemplateLetter(OpenPosition openPosition) {
        String retStr = "";

        if (openPosition.getTemplateLetter() != null &&
                openPosition.getTemplateLetter() != "") {
            retStr = "Требования к вакансии: " + Jsoup.parse(openPosition.getTemplateLetter()).text() + "\n\n";
        }

        if (openPosition.getProjectName().getTemplateLetter() != null &&
                openPosition.getProjectName().getTemplateLetter() != "") {
            retStr = retStr +
                    "Требования проекта: " +
                    Jsoup.parse(openPosition.getProjectName().getTemplateLetter()).text() + "\n\n";
        }

        if (openPosition.getProjectName().getProjectDepartment().getTemplateLetter() != null &&
                openPosition.getProjectName().getProjectDepartment().getTemplateLetter() != "") {
            retStr = retStr +
                    "Требования департамента: " +
                    Jsoup.parse(openPosition.getProjectName().getProjectDepartment().getTemplateLetter()).text();
        }

        return retStr;
    }

    @Install(to = "openPositionsTable.queryQuestion", subject = "descriptionProvider")
    private String openPositionsTableQueryQuestionDescriptionProvider(OpenPosition openPosition) {
        return getTemplateLetter(openPosition);
    }

    @Install(to = "openPositionsTable.queryQuestion", subject = "styleProvider")
    private String openPositionsTableQueryQuestionStyleProvider(OpenPosition openPosition) {
        String style = "";

        if (!getTemplateLetter(openPosition).equals("")) {
            style = "open-position-pic-center-large-green";
        } else {
            style = "open-position-pic-center-large-red";
        }

        return style;
    }

    @Subscribe("openPositionsTable")
    public void onOpenPositionsTableSelection(DataGrid.SelectionEvent<OpenPosition> event) {
        if (openPositionsTable.getSingleSelected() != null) {
            suggestCandidateButton.setEnabled(true);
        } else {
            suggestCandidateButton.setEnabled(false);
        }

    }

    public void suggestCandidateButton() {
        Suggestjobcandidate suggestjobcandidate = screens.create(Suggestjobcandidate.class);
        suggestjobcandidate.setOpenPosition(openPositionsTable.getSingleSelected());

        suggestjobcandidate.show();
    }

    @Install(to = "openPositionsTable.memoForCandidateColumn", subject = "descriptionProvider")
    private String openPositionsTableMemoForCandidateColumnDescriptionProvider(OpenPosition openPosition) {
        if (openPosition.getMemoForInterview() != null)
            return Jsoup.parse(openPosition.getMemoForInterview()).text();
        else
            return null;
    }

    @Install(to = "openPositionsTable.memoForCandidateColumn", subject = "styleProvider")
    private String openPositionsTableMemoForCandidateColumnStyleProvider(OpenPosition openPosition) {
        String style = "open-position-pic-center-large-red";

        if (openPosition.getMemoForInterview() != null) {
            if (!openPosition.getMemoForInterview().equals("")) {
                style = "open-position-pic-center-large-green";
            } else {
                style = "open-position-pic-center-large-red";
            }
        }

        return style;
    }

    @Install(to = "openPositionsTable.lastOpenCloseColumn", subject = "columnGenerator")
    private Object openPositionsTableLastOpenCloseColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yy");
        Date lastDate = event.getItem().getLastOpenDate() != null
                ? event.getItem().getLastOpenDate() : event.getItem().getCreateTs();

        return (lastDate != null ?
                simpleDateFormat.format(lastDate) : "");
    }

    @Install(to = "openPositionsTable.lastOpenCloseColumn", subject = "styleProvider")
    private String openPositionsTableLastOpenCloseColumnStyleProvider(OpenPosition openPosition) {
        Date lastDate = openPosition.getLastOpenDate() != null
                ? openPosition.getLastOpenDate() : openPosition.getCreateTs();
        if (lastDate != null) {
            Date date = new Date();

            if (date.before(DateUtils.addMonths(lastDate, 1))) {
                return "pic-center-small-red";
            } else {
                if (date.before(DateUtils.addMonths(lastDate, 2))) {
                    return "pic-center-small-orange";
                } else {
                    if (date.before(DateUtils.addMonths(lastDate, 3))) {
                        return "pic-center-small-green";
                    } else {
                        return "pic-center-small-grey";

                    }
                }
            }
        } else {
            return "pic-center-small-gray";
        }
    }

    @Install(to = "openPositionsTable.lastOpenCloseColumn", subject = "descriptionProvider")
    private String openPositionsTableLastOpenCloseColumnDescriptionProvider(OpenPosition openPosition) {
        if (openPosition.getLastOpenDate() != null) {
            Date date = new Date();

            if (date.before(DateUtils.addMonths(openPosition.getLastOpenDate(), 1))) {
                return "Открыта более месяца назад";
            } else {
                if (date.before(DateUtils.addMonths(openPosition.getLastOpenDate(), 2))) {
                    return "Открыта более 2-х месяцев назад";
                } else {
                    if (date.before(DateUtils.addMonths(openPosition.getLastOpenDate(), 3))) {
                        return "Открыта более 3-х месяцев назад";
                    } else {
                        return "Открыта очень давно";
                    }
                }
            }
        } else {
            return "Открыта недавно";
        }
    }

    @Install(to = "openPositionsTable.memoForCandidateColumn", subject = "columnGenerator")
    private Object openPositionsTableMemoForCandidateColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        Object returnIcon = CubaIcon.MINUS_CIRCLE;

        if (event.getItem().getMemoForInterview() != null) {
            if (event.getItem().getMemoForInterview().equals("")) {
                returnIcon = CubaIcon.MINUS_CIRCLE;
            } else {
                returnIcon = CubaIcon.PLUS_CIRCLE;
            }
        }

        return returnIcon;
    }

    Integer montOfStat = 3;

    @Install(to = "openPositionsTable.idStatistics", subject = "columnGenerator")
    private Object openPositionsTableIdStatisticsColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        String retStr = "";

        GregorianCalendar gregorianCalendar = (GregorianCalendar) GregorianCalendar.getInstance();
        Date endDate = gregorianCalendar.getTime();
        gregorianCalendar.add(Calendar.MONTH, -montOfStat);
        Date startDate = gregorianCalendar.getTime();

        List<KeyValueEntity> iteractionIntegerKeyValue =
                dataManager.loadValues(QUERY_COUNT_ITERACTIONS)
                        .properties("iteractionType", "sum")
                        .parameter("startDate", startDate)
                        .parameter("vacancy", event.getItem())
                        .parameter("endDate", endDate)
                        .list();

        if (iteractionIntegerKeyValue.size() != 0) {
            for (KeyValueEntity entity : iteractionIntegerKeyValue) {
                retStr += entity.getValue("sum") + " / ";

            }

            retStr = retStr.substring(0, retStr.length() - 3);
        }

        return retStr;
    }

    @Install(to = "openPositionsTable.idStatistics", subject = "descriptionProvider")
    private String openPositionsTableIdStatisticsDescriptionProvider(OpenPosition openPosition) {
        String retStr = "Статистика за " + montOfStat + " месяца\n";

        GregorianCalendar gregorianCalendar = (GregorianCalendar) GregorianCalendar.getInstance();
        Date endDate = gregorianCalendar.getTime();
        gregorianCalendar.add(Calendar.MONTH, -montOfStat);
        Date startDate = gregorianCalendar.getTime();

        List<KeyValueEntity> iteractionIntegerKeyValue =
                dataManager.loadValues(QUERY_COUNT_ITERACTIONS)
                        .properties("iteractionType", "sum")
                        .parameter("startDate", startDate)
                        .parameter("vacancy", openPosition)
                        .parameter("endDate", endDate)
                        .list();

        if (iteractionIntegerKeyValue.size() != 0) {
            for (KeyValueEntity entity : iteractionIntegerKeyValue) {
                String iteractionName = ((Iteraction) entity.getValue("iteractionType"))
                        .getIterationName();
                retStr += iteractionName + " : " + entity.getValue("sum") + "\n";

            }

            retStr = retStr.substring(0, retStr.length() - 1);
        }

        return retStr;
    }

    String more_10_msg = "<font color=red>>10</font>";
    String clarification_required = "<font color=blue>???</font>";

    @Install(to = "openPositionsTable.numberPosition", subject = "columnGenerator")
    private Object openPositionsTableNumberPositionColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {

        if (event.getItem().getMore10NumberPosition() == null) {
            if (event.getItem().getNumberPosition() != null) {
                if (event.getItem().getNumberPosition() < 10) {
                    return event.getItem().getNumberPosition().toString();
                } else {
                    return more_10_msg;
                }
            } else {
                return clarification_required;
            }
        } else {
            if (event.getItem().getMore10NumberPosition()) {
                return more_10_msg;
            } else {
                if (event.getItem().getNumberPosition() < 10) {
                    return event.getItem().getNumberPosition().toString();
                } else {
                    return more_10_msg;
                }
            }
        }
    }

    @Install(to = "openPositionsTable.numberPosition", subject = "descriptionProvider")
    private String openPositionsTableNumberPositionDescriptionProvider(OpenPosition openPosition) {
        if (openPosition.getMore10NumberPosition() != null) {
            if (openPosition.getMore10NumberPosition()) {
                if (openPosition.getNumberPosition() == null)
                    return ">10";
                else
                    return openPosition.getNumberPosition().toString();
            } else {
                if (openPosition.getNumberPosition() != null) {
                    return openPosition.getNumberPosition().toString();
                } else {
                    return null;
                }
            }
        } else {
            return openPosition.getNumberPosition() == null ? "" : openPosition.getNumberPosition().toString();
        }
    }

    @Install(to = "openPositionsTable.owner", subject = "descriptionProvider")
    private String openPositionsTableOwnerDescriptionProvider(OpenPosition openPosition) {
        if (openPosition.getOwner() != null) {
            return (openPosition.getOwner().getName() != null ?
                    openPosition.getOwner().getName() : openPosition.getCreatedBy());
        } else {
            return null;
        }
    }

    @Install(to = "openPositionsTable.owner", subject = "columnGenerator")
    private Object openPositionsTableOwnerColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {

        return whoOwner(event);
        /* return event.getItem().getOwner() != null
                ? event.getItem().getOwner().getName()
                : event.getItem().getCreatedBy(); */
    }

    private Object whoOwner(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        String userName = null;
        String a, b, c, e;

        if (event.getItem().getOwner() == null) {
            for (User user : users) {
                a = user.getLogin();
                b = user.getName();
                c = event.getItem().getCreatedBy();
                userName = c.equals(a) ? b : null;

                if (userName != null)
                    break;

//                userName = user.getLogin().equals(event.getItem().getCreatedBy()) ? user.getName() : null;
            }
        } else {
            e = event.getItem().getOwner().getName();
            userName = event.getItem().getOwner().getName();
        }

        return userName;
    }

    @Subscribe("signDraftCheckBox")
    public void onSignDraftCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        openPositionsDl.setParameter("signDraft", (event.getValue() != null ? event.getValue() : false));
    }

    @Install(to = "openPositionsTable.workExperience", subject = "columnGenerator")
    private Object openPositionsTableWorkExperienceColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        Set<Map.Entry<String, Integer>> entrySet = mapWorkExperience.entrySet();
        Integer desiredObject = event.getItem().getWorkExperience();

        for (Map.Entry<String, Integer> pair : entrySet) {
            if (desiredObject.equals(pair.getValue())) {
                return pair.getKey();// нашли наше значение и возвращаем  ключ
            }
        }

        return null;
    }

    private void setWorkExperienceMap() {
        mapWorkExperience.put("Нет требований", 0);
        mapWorkExperience.put("Без опыта", 1);
        mapWorkExperience.put("1 год", 2);
        mapWorkExperience.put("2 года", 3);
        mapWorkExperience.put("3 года", 4);
        mapWorkExperience.put("4 года", 6);
        mapWorkExperience.put("5 лет и более", 5);
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        checkBoxOnlyMySubscribe.setValue(true);
    }

    public void getMemoForCandidate() {
        LoadContext<Report> loadContext = LoadContext.create(Report.class)
                .setQuery(LoadContext
                        .createQuery("select p from report$Report p where p.code = 'memoForCandidates'"))
                .setView("report.edit");
        Report report = dataManager.load(loadContext);

        report.setValue("openPosition", openPositionsTable.getSingleSelected(), false);

        FrameOwner window = App.getInstance().getTopLevelWindow().getFrameOwner();
        reportGuiManager.runReport(report, window);
    }

    @Install(to = "openPositionsTable.folder", subject = "columnGenerator")
    private Icons.Icon openPositionsTableFolderColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> columnGeneratorEvent) {
        String QUERY = "select e from itpearls_OpenPosition e where e.parentOpenPosition = :parentOpenPosition";
        String retStr = "QUESTION_CIRCLE";

        if (dataManager.load(OpenPosition.class)
                .query(QUERY)
                .parameter("parentOpenPosition", columnGeneratorEvent.getItem())
                .list().size() > 0) {
            retStr = "FOLDER";
        } else {
            if (columnGeneratorEvent.getItem() != null) {
                if (columnGeneratorEvent.getItem().getPriority() != null) {
                    switch (columnGeneratorEvent.getItem().getPriority()) {
                        case PRIORITY_DRAFT:
                            retStr = "REFRESH_ACTION";
                            break;
                        case PRIORITY_PAUSED:
                            retStr = "PAUSE_CIRCLE";
                            break;
                        case PRIORITY_LOW:
                            retStr = "ARROW_CIRCLE_DOWN";
                            break;
                        case PRIORITY_NORMAL:
                            retStr = "LOOKUP_OK";
                            break;
                        case PRIORITY_HIGH:
                            retStr = "ARROW_CIRCLE_UP";
                            break;
                        case PRIORITY_CRITICAL:
                            retStr = "EXCLAMATION_CIRCLE";
                            break;
                        default:
                            retStr = "LOOKUP_OK";
                            break;
                    }
                }
            }
        }

        return CubaIcon.valueOf(retStr);

    }

    @Install(to = "openPositionsTable.folder", subject = "descriptionProvider")
    private String openPositionsTableFolderDescriptionProvider(OpenPosition openPosition) {
        String QUERY = "select e from itpearls_OpenPosition e where e.parentOpenPosition = :parentOpenPosition";
        String retStr = "";

        if (dataManager.load(OpenPosition.class)
                .query(QUERY)
                .parameter("parentOpenPosition", openPosition)
                .list().size() > 0) {
            retStr = null;
        } else {
            if (openPosition.getPriority() != null) {
                switch (openPosition.getPriority()) {
                    case PRIORITY_DRAFT:
                        retStr = "DRAFT PRIORITY";
                        break;
                    case PRIORITY_PAUSED:
                        retStr = "PAUSED PRIORITY";
                        break;
                    case PRIORITY_LOW:
                        retStr = "LOW PRIORITY";
                        break;
                    case PRIORITY_NORMAL:
                        retStr = "NORMAL PRIORITY";
                        break;
                    case PRIORITY_HIGH:
                        retStr = "HIGH PRIORITY";
                        break;
                    case PRIORITY_CRITICAL:
                        retStr = "CRITICAL PRIORITY";
                        break;
                    default:
                        retStr = "NOT DEFINED";
                        break;
                }
            } else {
                retStr = "NOT DEFINED";
            }
        }

        return retStr;
    }

    @Install(to = "openPositionsTable.folder", subject = "styleProvider")
    private String openPositionsTableFolderStyleProvider(OpenPosition openPosition) {
        String QUERY = "select e from itpearls_OpenPosition e where e.parentOpenPosition = :parentOpenPosition";
        String retStr = "";

        if (dataManager.load(OpenPosition.class)
                .query(QUERY)
                .parameter("parentOpenPosition", openPosition)
                .list().size() > 0) {
            retStr = "open-position-pic-center-large-gray";
        } else {
            if (openPosition.getPriority() != null) {
                switch (openPosition.getPriority()) {
                    case PRIORITY_DRAFT:
                        retStr = "open-position-pic-center-large-gray";
                        break;
                    case PRIORITY_PAUSED:
                        retStr = "open-position-pic-center-large-gray";
                        break;
                    case PRIORITY_LOW:
                        retStr = "open-position-pic-center-large-blue";
                        break;
                    case PRIORITY_NORMAL:
                        retStr = "open-position-pic-center-large-green";
                        break;
                    case PRIORITY_HIGH:
                        retStr = "open-position-pic-center-large-orange";
                        break;
                    case PRIORITY_CRITICAL:
                        retStr = "open-position-pic-center-large-red";
                        break;
                    default:
                        retStr = "open-position-pic-center-large-gray";
                        break;
                }
            } else {
                retStr = "open-position-pic-center-large-yellow";
            }
        }

        return retStr; /*
                dataManager.load(OpenPosition.class)
                .query(QUERY)
                .parameter("parentOpenPosition", openPosition)
                .list().size() > 0
                ? "open-position-pic-center-large-gray" :
                switch(openPosition.getPriority()) {
                    case PRIOPITY_DRAFT: "open-position-pic-center-large-gray";
                    default: "open-position-pic-center-large-gray";
                }
        );*/
    }

    public void openCloseButtonInvoke() {
        if (openPositionsTable.getSingleSelected() != null) {
            openCloseButtonClickListener(openPositionsTable.getSingleSelected(), openCloseButton);
        }
    }

    @Install(to = "openPositionsTable.lastCVSend", subject = "columnGenerator")
    private Object openPositionsTableLastCVSendColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> columnGeneratorEvent) {
        Label labelRet = uiComponents.create(Label.NAME);
        String QUERY = "select e " +
                "from itpearls_IteractionList e " +
                "where e.vacancy = :vacancy " +
                "and e.iteractionType.signSendToClient = true " +
                "and e.vacancy.lastOpenDate < e.dateIteraction";
        Integer countCVsend = dataManager.load(IteractionList.class)
                .query(QUERY)
                .view("iteractionList-view")
                .parameter("vacancy", columnGeneratorEvent.getItem())
                .list().size();

        labelRet.setValue(countCVsend);
        labelRet.setWidthFull();
        labelRet.setHeightFull();
        labelRet.setAlignment(Component.Alignment.MIDDLE_CENTER);

        switch (countCVsend) {
            case 0:
            case 1:
            case 2:
                labelRet.setStyleName("label_table_blue");
                break;
            case 3:
            case 4:
                labelRet.setStyleName("label_table_green");
                break;
            case 5:
            case 6:
                labelRet.setStyleName("label_table_orange");
                break;
            case 7:
            case 8:
            case 9:
                labelRet.setStyleName("label_table_red");
                break;
            case 10:
            default:
                labelRet.setStyleName("label_table_gray");
                break;
        }

        return labelRet;
    }

}


