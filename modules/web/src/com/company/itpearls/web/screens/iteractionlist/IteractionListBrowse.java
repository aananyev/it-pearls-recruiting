package com.company.itpearls.web.screens.iteractionlist;

import com.company.itpearls.core.StarsAndOtherService;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.service.GetRoleService;
import com.company.itpearls.web.StandartRoles;
import com.company.itpearls.web.screens.iteractionlist.iteractionlistbrowse.IteractionListSimpleBrowse;
import com.company.itpearls.web.screens.jobcandidate.JobCandidateEdit;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Iteraction;
import com.company.itpearls.entity.IteractionList;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.global.UserSession;
import com.vaadin.ui.JavaScript;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@UiController("itpearls_IteractionList.browse")
@UiDescriptor("iteraction-list-browse.xml")
@LookupComponent("iteractionListsTable")
public class IteractionListBrowse extends StandardLookup<IteractionList> {
    @Inject
    private UserSession userSession;
    @Inject
    private CollectionLoader<IteractionList> iteractionListsDl;
    @Inject
    private CheckBox checkBoxShowOnlyMy;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private Button buttonCopy;
    @Inject
    private Button buttonExcel;
    @Inject
    private GetRoleService getRoleService;
    @Inject
    private DataGrid<IteractionList> iteractionListsTable;
    @Inject
    private Screens screens;
    @Inject
    private StarsAndOtherService starsAndOtherService;
    @Inject
    private Button jobCandidateCardButton;
    @Inject
    private Notifications notifications;
    @Inject
    private Button clipBtn;
    @Inject
    private Button itercationListButton;
    @Inject
    private DataManager dataManager;
    @Inject
    private DateField<Date> dateFromField;

    private static final int DEFAULT_DATE_FILTER_DAYS = 90;

    private static final String QUERY_OUTSTAFFING_TYPES =
            "select e from itpearls_Iteraction e where e.outstaffingSign = true";

    private static final String QUERY_ACTIVE_RECRUITER_TASKS_BY_VACANCIES =
            "select e.openPosition, count(e) from itpearls_RecrutiesTasks e "
                    + "where e.openPosition in :vacancies and e.closed = false and e.endDate >= :currentDate "
                    + "group by e.openPosition";

    private static final ThreadLocal<SimpleDateFormat> ADD_DATE_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("dd-MM-yyyy H:m"));

    private Map<UUID, Integer> vacancyRecruiterTaskCountCache = Collections.emptyMap();
    private Set<UUID> outstaffingTypeIdsCache;
    private boolean suppressDateFromReload;
    private boolean listenersAttached;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        suppressDateFromReload = true;
        try {
            buttonExcel.setVisible(getRoleService.isUserRoles(userSession.getUser(), StandartRoles.MANAGER));
            dateFromField.setValue(getDefaultDateFrom());
            applyBrowseFilters();
        } finally {
            suppressDateFromReload = false;
        }
    }

    private Date getDefaultDateFrom() {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.add(GregorianCalendar.DAY_OF_MONTH, -DEFAULT_DATE_FILTER_DAYS);
        return calendar.getTime();
    }

    private void applyDateFromFilter() {
        Date dateFrom = dateFromField.getValue();
        if (dateFrom != null) {
            iteractionListsDl.setParameter("dateFrom", dateFrom);
        } else {
            iteractionListsDl.removeParameter("dateFrom");
        }
    }

    private Set<UUID> getOutstaffingTypeIds() {
        if (outstaffingTypeIdsCache == null) {
            outstaffingTypeIdsCache = dataManager.load(Iteraction.class)
                    .query(QUERY_OUTSTAFFING_TYPES)
                    .view("_minimal")
                    .cacheable(true)
                    .list()
                    .stream()
                    .map(Iteraction::getId)
                    .collect(Collectors.toSet());
        }
        return outstaffingTypeIdsCache;
    }

    private void applyBrowseFilters() {
        if (userSession.getUser().getGroup().getName().equals(StandartRoles.STAGER)) {
            iteractionListsDl.setParameter("userName",
                    "%" + userSession.getUser().getLogin() + "%");
            checkBoxShowOnlyMy.setValue(true);
            checkBoxShowOnlyMy.setEditable(false);
        }

        if (userSession.getUser().getGroup().getName().equals(StandartRoles.MANAGER)) {
            Set<UUID> outstaffingTypeIds = getOutstaffingTypeIds();
            if (outstaffingTypeIds.isEmpty()) {
                iteractionListsDl.removeParameter("outstaffingTypeIds");
            } else {
                iteractionListsDl.setParameter("outstaffingTypeIds", outstaffingTypeIds);
            }
        } else {
            iteractionListsDl.removeParameter("outstaffingTypeIds");
        }

        applyDateFromFilter();
        applyInternalProjectFilter();
        iteractionListsDl.load();
    }

    private void applyInternalProjectFilter() {
        if (getRoleService.isUserRoles(userSession.getUser(), StandartRoles.MANAGER) ||
                getRoleService.isUserRoles(userSession.getUser(), StandartRoles.ADMINISTRATOR)) {
            iteractionListsDl.removeParameter("internalProject");
        } else {
            iteractionListsDl.setParameter("internalProject", false);
        }
    }

    @Subscribe("dateFromField")
    public void onDateFromFieldValueChange(HasValue.ValueChangeEvent<Date> event) {
        if (suppressDateFromReload) {
            return;
        }
        applyDateFromFilter();
        iteractionListsDl.load();
    }

    @Subscribe(id = "iteractionListsDl", target = Target.DATA_LOADER)
    private void onIteractionListsDlPostLoad(CollectionLoader.PostLoadEvent<IteractionList> event) {
        refreshVacancyRecruiterTaskCountCache(event.getLoadedEntities());
    }

    private void refreshVacancyRecruiterTaskCountCache(List<IteractionList> items) {
        List<OpenPosition> vacancies = items.stream()
                .map(IteractionList::getVacancy)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (vacancies.isEmpty()) {
            vacancyRecruiterTaskCountCache = Collections.emptyMap();
            return;
        }

        List<KeyValueEntity> counts = dataManager.loadValues(QUERY_ACTIVE_RECRUITER_TASKS_BY_VACANCIES)
                .properties("openPosition", "count")
                .parameter("vacancies", vacancies)
                .parameter("currentDate", new Date())
                .list();

        Map<UUID, Integer> cache = new HashMap<>();
        for (KeyValueEntity row : counts) {
            OpenPosition vacancy = row.getValue("openPosition");
            Long taskCount = row.getValue("count");
            if (vacancy != null && vacancy.getId() != null && taskCount != null) {
                cache.put(vacancy.getId(), taskCount.intValue());
            }
        }
        vacancyRecruiterTaskCountCache = cache;
    }

    private int getRecruiterTaskCount(OpenPosition openPosition) {
        if (openPosition == null || openPosition.getId() == null) {
            return 0;
        }
        return vacancyRecruiterTaskCountCache.getOrDefault(openPosition.getId(), 0);
    }

    @Install(to = "iteractionListsTable.iteractionType", subject = "descriptionProvider")
    private String iteractionListsTableIteractionTypeDescriptionProvider(IteractionList iteractionList) {
        String add = "";

        if (iteractionList.getAddDate() != null) {
            add = ADD_DATE_FORMAT.get().format(iteractionList.getAddDate());
        }

        if (iteractionList.getAddString() != null)
            add = iteractionList.getAddString();

        if (iteractionList.getAddInteger() != null)
            add = iteractionList.getAddInteger().toString();

        return new StringBuilder
                (iteractionList.getAddString() != null ? iteractionList.getAddString() : "")
                .append(add)
                .toString();
    }

    @Subscribe("checkBoxShowOnlyMy")
    public void onCheckBoxShowOnlyMyValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (checkBoxShowOnlyMy.getValue()) {
            iteractionListsDl.setParameter("userName", new StringBuilder()
                    .append("%")
                    .append(userSession.getUser().getLogin())
                    .append("%")
                    .toString());
        } else {
            iteractionListsDl.removeParameter("userName");
        }

        iteractionListsDl.load();
    }

    public void onButtonCopyClick() {
        Screen screen = screenBuilders.editor(iteractionListsTable)
                .newEntity()
                .withInitializer(data -> {
                    if (iteractionListsTable.getSingleSelected() != null) {
                        data.setCandidate(iteractionListsTable.getSingleSelected().getCandidate());
                        data.setVacancy(iteractionListsTable.getSingleSelected().getVacancy());
                    }
                })
                .withScreenClass(IteractionListEdit.class)
                .withLaunchMode(OpenMode.THIS_TAB)
                .build();

        screen.show();
    }

    private void addIconColumn() {
        DataGrid.Column iconColumn = iteractionListsTable.addGeneratedColumn("icon",
                new DataGrid.ColumnGenerator<IteractionList, String>() {
                    @Override
                    public String getValue(DataGrid.ColumnGeneratorEvent<IteractionList> event) {
                        return getIcon(event.getItem());
                    }

                    @Override
                    public Class<String> getType() {
                        return String.class;
                    }
                });

        iconColumn.setRenderer(iteractionListsTable.createRenderer(DataGrid.ImageRenderer.class));
    }


    private String getIcon(IteractionList item) {
        if (item.getIteractionType() != null) {
            if (item.getIteractionType().getPic() != null) {
                return item.getIteractionType().getPic();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Subscribe
    public void onInit(InitEvent event) {
        addIconColumn();
        addTableListeners();
    }

    private void addTableListeners() {
        if (listenersAttached) {
            return;
        }
        listenersAttached = true;

        jobCandidateCardButton.setEnabled(false);

        iteractionListsTable.addSelectionListener(event -> {
            if (event.getSelected() == null) {
                jobCandidateCardButton.setEnabled(false);
                clipBtn.setEnabled(false);
                itercationListButton.setEnabled(false);
                buttonCopy.setEnabled(false);

            } else {
                jobCandidateCardButton.setEnabled(true);
                clipBtn.setEnabled(true);
                itercationListButton.setEnabled(true);
                buttonCopy.setEnabled(true);
            }
        });
    }

    public void onJobCandidateButtonClick() {
        JobCandidateEdit jobCandidateEdit = screens.create(JobCandidateEdit.class);

        jobCandidateEdit.setEntityToEdit(iteractionListsTable.getSingleSelected().getCandidate());
        screens.show(jobCandidateEdit);

    }

    @Install(to = "iteractionListsTable.rating", subject = "columnGenerator")
    private String iteractionListsTableRatingColumnGenerator(DataGrid.ColumnGeneratorEvent<IteractionList> event) {

        return event.getItem().getRating() != null ?
                starsAndOtherService.setStars(event.getItem().getRating() + 1) : "";
    }

    @Install(to = "iteractionListsTable.rating", subject = "styleProvider")
    private String iteractionListsTableRatingStyleProvider(IteractionList iteractionList) {
        return "rating_box";
    }

    public void onButtonCopyToClibboard() {
//        String clipboardText = "";
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");

        if (iteractionListsTable.getSelected() != null) {
            for (IteractionList i : iteractionListsTable.getSelected()) {
                sb.append(i.getCandidate().getFullName())
                        .append(",")
                        .append(simpleDateFormat.format(i.getDateIteraction()))
                        .append(",")
                        .append(i.getVacancy().getVacansyName())
                        .toString();
/*                clipboardText = clipboardText + i.getCandidate().getFullName() + "," +
                        simpleDateFormat.format(i.getDateIteraction()) + "," +
                        i.getVacancy().getVacansyName(); */
            }
        }

        JavaScript.getCurrent().execute(new StringBuilder()
                .append("navigator.clipboard.writeText('")
                .append(sb)
                .append("');")
                .toString());
        notifications.create().withCaption("Скопировано в буфер обмена").show();
    }

    public void onIteractionListButton() {
        IteractionListSimpleBrowse iteractionListSimpleBrowse = screens.create(IteractionListSimpleBrowse.class);
        iteractionListSimpleBrowse.setSelectedCandidate(iteractionListsTable.getSingleSelected().getCandidate());
        screens.show(iteractionListSimpleBrowse);
    }

    @Install(to = "iteractionListsTable.currentOpenCloseColumn", subject = "columnGenerator")
    private Icons.Icon iteractionListsTableCurrentOpenCloseColumnColumnGenerator(
            DataGrid.ColumnGeneratorEvent<IteractionList> columnGeneratorEvent) {

        if (columnGeneratorEvent.getItem().getCurrentOpenClose() != null) {
            return columnGeneratorEvent.getItem().getCurrentOpenClose()
                    ? CubaIcon.MINUS_CIRCLE : CubaIcon.PLUS_CIRCLE;
        } else {
            if (columnGeneratorEvent.getItem().getVacancy() != null) {
                return columnGeneratorEvent.getItem().getVacancy().getOpenClose() != null ?
                        (columnGeneratorEvent.getItem().getVacancy().getOpenClose() ?
                                CubaIcon.MINUS_CIRCLE : CubaIcon.PLUS_CIRCLE) : CubaIcon.PLUS_CIRCLE;
            } else {
                return CubaIcon.CANCEL;
            }
        }
    }

    @Install(to = "iteractionListsTable.currentOpenCloseColumn", subject = "styleProvider")
    private String iteractionListsTableCurrentOpenCloseColumnStyleProvider(
            IteractionList iteractionList) {

        if (iteractionList.getCurrentOpenClose() != null) {
            return iteractionList.getCurrentOpenClose()
                    ? "pic-center-large-red" : "pic-center-large-green";
        } else {
            if (iteractionList.getVacancy() != null) {
                return iteractionList.getVacancy().getOpenClose() ?
                        "pic-center-large-red" : "pic-center-large-green";
            } else {
                return "pic-center-large-gray";
            }
        }
    }

    @Install(to = "iteractionListsTable.currentOpenCloseColumn", subject = "descriptionProvider")
    private String iteractionListsTableCurrentOpenCloseColumnDescriptionProvider(IteractionList iteractionList) {

        if (iteractionList.getCurrentOpenClose() != null) {
            return iteractionList.getCurrentOpenClose()
                    ? "Закрыта на момент создания взаимодействия" : "Открыта на момент создания взаимодействия";
        } else {
            if (iteractionList.getVacancy() != null) {
                return iteractionList.getVacancy().getOpenClose() ?
                        "Закрыта на текущий момент" : "Открыта на текущий момент";
            } else {
                return null;
            }
        }
    }

    @Install(to = "iteractionListsTable", subject = "rowStyleProvider")
    private String iteractionListsTableRowStyleProvider(IteractionList iteractionList) {
        return OpenPositionRowStyleHelper.resolveStyle(
                iteractionList.getVacancy(),
                getRecruiterTaskCount(iteractionList.getVacancy()));
    }
}