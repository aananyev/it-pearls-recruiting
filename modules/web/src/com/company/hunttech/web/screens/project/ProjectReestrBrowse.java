package com.company.hunttech.web.screens.project;

import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.Person;
import com.company.hunttech.entity.Project;
import com.hunttech.hrm.gui.components.OvaFallbackImage;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.LookupComponent;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;
import java.util.stream.Collectors;

@UiController("hunttech_ProjectReestr.browse")
@UiDescriptor("project-reestr-browse.xml")
@LookupComponent("projectsTable")
@LoadDataBeforeShow
public class ProjectReestrBrowse extends StandardLookup<Project> {

    private static final String QUERY_OPEN_POSITION_COUNT_BY_PROJECTS =
            "select e.projectName, count(e) from hunttech_OpenPosition e "
                    + "where not e.openClose = true and e.projectName in :projects group by e.projectName";

    private static final String QUERY_PROJECT_DESCRIPTIONS_BY_IDS =
            "select e.id, e.projectDescription from hunttech_Project e where e.id in :ids";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    @Inject
    private CollectionContainer<Project> projectsDc;
    @Inject
    private CollectionLoader<Project> projectsDl;
    @Inject
    private GroupTable<Project> projectsTable;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private DataManager dataManager;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private MessageTools messageTools;

    @Inject
    private PopupButton filterPopupButton;

    @Inject
    private OvaFallbackImage logoPic;
    @Inject
    private Label<String> detailTitle;
    @Inject
    private Label<String> detailSubtitle;
    @Inject
    private Label<String> detailLocation;
    @Inject
    private Button openEditCardBtn;
    @Inject
    private Button createPositionForProjectBtn;
    @Inject
    private Label<String> detailStatus;
    @Inject
    private Label<String> detailStartDate;
    @Inject
    private Label<String> detailEndDate;
    @Inject
    private Label<String> detailOpenPositionsCount;
    @Inject
    private Label<String> detailCuratorName;
    @Inject
    private Label<String> detailCuratorPosition;
    @Inject
    private Label<String> detailCuratorDept;
    @Inject
    private Label<String> detailCuratorContacts;
    @Inject
    private Label<String> detailDescription;

    private Map<UUID, Integer> openPositionCountCache = Collections.emptyMap();
    private Map<UUID, String> projectDescriptionCache = Collections.emptyMap();

    @Subscribe
    public void onInit(InitEvent event) {
        initDefaultFilters();
        setupTableColumns();
        setupTableSelection();
        setupSidebarButtons();
    }

    private void initDefaultFilters() {
        projectsDl.setParameter("projectClosed", false);
        projectsDl.setParameter("withOpenPosition", true);
    }

    private void setupTableColumns() {
        projectsTable.addGeneratedColumn("projectLogoColumn", project -> {
            HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
            retBox.setWidthFull();
            retBox.setHeightFull();
            retBox.setAlignment(Component.Alignment.MIDDLE_CENTER);

            Image image = uiComponents.create(Image.class);
            image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
            image.setWidth("24px");
            image.setHeight("24px");
            image.setStyleName("icon-no-border-20px");
            image.setAlignment(Component.Alignment.MIDDLE_CENTER);

            if (project.getProjectLogo() != null) {
                image.setSource(FileDescriptorResource.class).setFileDescriptor(project.getProjectLogo());
            } else {
                image.setSource(ThemeResource.class).setPath("icons/no-company.png");
            }

            retBox.add(image);
            return retBox;
        });

        projectsTable.addGeneratedColumn("projectName", project -> {
            HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
            retHBox.setWidthFull();
            retHBox.setAlignment(Component.Alignment.MIDDLE_LEFT);
            retHBox.setHeightFull();
            retHBox.setSpacing(true);

            // Бейдж «Новый»
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            gregorianCalendar.setTime(new Date());
            gregorianCalendar.add(Calendar.DAY_OF_MONTH, -14);
            if (project.getStartProjectDate() != null && project.getStartProjectDate().after(gregorianCalendar.getTime())) {
                Label newBadge = uiComponents.create(Label.class);
                newBadge.setHtmlEnabled(true);
                newBadge.setValue("<span style='background: #fef2f2; color: #dc2626; border: 1px solid #fecaca; padding: 1px 6px; border-radius: 8px; font-size: 10px; font-weight: 700;'>НОВЫЙ</span>");
                newBadge.setAlignment(Component.Alignment.MIDDLE_LEFT);
                retHBox.add(newBadge);
            }

            Label nameLabel = uiComponents.create(Label.class);
            nameLabel.setValue(project.getProjectName() != null ? project.getProjectName() : "");
            nameLabel.setStyleName("bold");
            nameLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);
            retHBox.add(nameLabel);
            retHBox.expand(nameLabel);

            return retHBox;
        });

        projectsTable.addGeneratedColumn("projectStatus", project -> {
            HBoxLayout box = uiComponents.create(HBoxLayout.class);
            box.setWidthFull();
            box.setHeightFull();
            box.setAlignment(Component.Alignment.MIDDLE_CENTER);

            Label label = uiComponents.create(Label.class);
            label.setHtmlEnabled(true);
            label.setAlignment(Component.Alignment.MIDDLE_CENTER);

            if (Boolean.TRUE.equals(project.getProjectIsClosed())) {
                label.setValue("<span style='background: #fee2e2; color: #b91c1c; border: 1px solid #fecaca; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600;'>Закрыт</span>");
            } else {
                label.setValue("<span style='background: #dcfce7; color: #15803d; border: 1px solid #bbf7d0; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600;'>Открыт</span>");
            }

            box.add(label);
            return box;
        });

        projectsTable.addGeneratedColumn("openPositionsCountColumn", project -> {
            int count = (project != null && project.getId() != null) ? openPositionCountCache.getOrDefault(project.getId(), 0) : 0;

            HBoxLayout box = uiComponents.create(HBoxLayout.class);
            box.setWidthFull();
            box.setHeightFull();
            box.setAlignment(Component.Alignment.MIDDLE_CENTER);

            Label label = uiComponents.create(Label.class);
            label.setHtmlEnabled(true);
            label.setAlignment(Component.Alignment.MIDDLE_CENTER);

            if (count > 0) {
                label.setValue("<span style='background: #eff6ff; color: #2563eb; border: 1px solid #bfdbfe; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 700;'>" + count + "</span>");
            } else {
                label.setValue("<span style='color: #94a3b8; font-size: 11px;'>0</span>");
            }

            box.add(label);
            return box;
        });
    }

    private void setupTableSelection() {
        projectsTable.addSelectionListener(e -> {
            Set<Project> selected = e.getSelected();
            if (selected != null && !selected.isEmpty()) {
                Project single = selected.iterator().next();
                updateSidebarDetails(single);
            } else {
                clearSidebarDetails();
            }
        });
    }

    private void setupSidebarButtons() {
        openEditCardBtn.addClickListener(e -> {
            Project selected = projectsTable.getSingleSelected();
            if (selected != null) {
                screenBuilders.editor(projectsTable)
                        .editEntity(selected)
                        .withOpenMode(OpenMode.DIALOG)
                        .show();
            }
        });

        createPositionForProjectBtn.addClickListener(e -> {
            Project selected = projectsTable.getSingleSelected();
            if (selected != null) {
                OpenPosition newPosition = dataManager.create(OpenPosition.class);
                newPosition.setProjectName(selected);
                screenBuilders.editor(OpenPosition.class, this)
                        .newEntity(newPosition)
                        .withOpenMode(OpenMode.DIALOG)
                        .show();
            }
        });
    }

    @Subscribe("filterPopupButton.filterWithOpenPositions")
    public void onFilterWithOpenPositions(Action.ActionPerformedEvent event) {
        projectsDl.setParameter("projectClosed", false);
        projectsDl.setParameter("withOpenPosition", true);
        filterPopupButton.setCaption("С открытыми вакансиями");
        projectsDl.load();
    }

    @Subscribe("filterPopupButton.filterOnlyOpen")
    public void onFilterOnlyOpen(Action.ActionPerformedEvent event) {
        projectsDl.setParameter("projectClosed", false);
        projectsDl.removeParameter("withOpenPosition");
        filterPopupButton.setCaption("Только открытые проекты");
        projectsDl.load();
    }

    @Subscribe("filterPopupButton.filterAll")
    public void onFilterAll(Action.ActionPerformedEvent event) {
        projectsDl.removeParameter("projectClosed");
        projectsDl.removeParameter("withOpenPosition");
        filterPopupButton.setCaption("Все проекты");
        projectsDl.load();
    }

    @Subscribe("actionsPopupButton.refreshAction")
    public void onRefreshAction(Action.ActionPerformedEvent event) {
        projectsDl.load();
    }

    @Subscribe("actionsPopupButton.excelExportAction")
    public void onExcelExportAction(Action.ActionPerformedEvent event) {
        Action excel = projectsTable.getAction("excel");
        if (excel != null) {
            excel.actionPerform(projectsTable);
        }
    }

    @Subscribe(id = "projectsDl", target = Target.DATA_LOADER)
    private void onProjectsDlPostLoad(CollectionLoader.PostLoadEvent<Project> event) {
        refreshOpenPositionCountCache(event.getLoadedEntities());
        refreshProjectDescriptionCache(event.getLoadedEntities());

        Project current = projectsTable.getSingleSelected();
        if (current != null) {
            updateSidebarDetails(current);
        } else if (!event.getLoadedEntities().isEmpty()) {
            projectsTable.setSelected(event.getLoadedEntities().get(0));
        } else {
            clearSidebarDetails();
        }
    }

    private void refreshOpenPositionCountCache(List<Project> projects) {
        if (projects.isEmpty()) {
            openPositionCountCache = Collections.emptyMap();
            return;
        }
        List<KeyValueEntity> counts = dataManager.loadValues(QUERY_OPEN_POSITION_COUNT_BY_PROJECTS)
                .properties("project", "count")
                .parameter("projects", projects)
                .list();
        Map<UUID, Integer> cache = new HashMap<>();
        for (KeyValueEntity row : counts) {
            Project project = row.getValue("project");
            Long count = row.getValue("count");
            if (project != null && project.getId() != null && count != null) {
                cache.put(project.getId(), count.intValue());
            }
        }
        openPositionCountCache = cache;
    }

    private void refreshProjectDescriptionCache(List<Project> projects) {
        List<UUID> ids = projects.stream()
                .map(Project::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            projectDescriptionCache = Collections.emptyMap();
            return;
        }
        List<KeyValueEntity> rows = dataManager.loadValues(QUERY_PROJECT_DESCRIPTIONS_BY_IDS)
                .properties("id", "projectDescription")
                .parameter("ids", ids)
                .list();
        Map<UUID, String> cache = new HashMap<>();
        for (KeyValueEntity row : rows) {
            UUID id = row.getValue("id");
            String description = row.getValue("projectDescription");
            if (id != null && description != null) {
                cache.put(id, description);
            }
        }
        projectDescriptionCache = cache;
    }

    private void updateSidebarDetails(Project project) {
        openEditCardBtn.setEnabled(true);
        createPositionForProjectBtn.setEnabled(true);

        // Логотип
        if (project.getProjectLogo() != null) {
            logoPic.setSource(FileDescriptorResource.class).setFileDescriptor(project.getProjectLogo());
        } else {
            logoPic.setSource(ThemeResource.class).setPath("icons/no-company.png");
        }

        // Заголовки
        detailTitle.setValue(project.getProjectName() != null ? project.getProjectName() : "Без названия");

        if (project.getProjectDepartment() != null) {
            String deptStr = project.getProjectDepartment().getDepartamentRuName();
            if (project.getProjectDepartment().getCompanyName() != null) {
                deptStr = project.getProjectDepartment().getCompanyName().getComanyName() + " (" + deptStr + ")";
            }
            detailSubtitle.setValue(deptStr);
        } else {
            detailSubtitle.setValue("-");
        }

        if (project.getProjectOwner() != null) {
            detailLocation.setValue("Куратор: " + project.getProjectOwner().getInstanceName());
        } else {
            detailLocation.setValue("Куратор не назначен");
        }

        // Сроки и статус
        if (Boolean.TRUE.equals(project.getProjectIsClosed())) {
            detailStatus.setValue("<span style='background: #fee2e2; color: #b91c1c; border: 1px solid #fecaca; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600;'>Закрыт</span>");
        } else {
            detailStatus.setValue("<span style='background: #dcfce7; color: #15803d; border: 1px solid #bbf7d0; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600;'>Открыт</span>");
        }

        detailStartDate.setValue(project.getStartProjectDate() != null ? DATE_FORMAT.format(project.getStartProjectDate()) : "Не указана");
        detailEndDate.setValue(project.getEndProjectDate() != null ? DATE_FORMAT.format(project.getEndProjectDate()) : "Не указана");

        int posCount = project.getId() != null ? openPositionCountCache.getOrDefault(project.getId(), 0) : 0;
        detailOpenPositionsCount.setValue(String.valueOf(posCount));

        // Куратор
        Person curator = project.getProjectOwner();
        if (curator != null) {
            detailCuratorName.setValue(curator.getInstanceName());
            detailCuratorPosition.setValue(curator.getPersonPosition() != null ? curator.getPersonPosition().getPositionRuName() : "Должность не указана");
            detailCuratorDept.setValue(curator.getCompanyDepartment() != null ? curator.getCompanyDepartment().getDepartamentRuName() : "Отдел не указан");

            StringBuilder contacts = new StringBuilder();
            String ph = curator.getMobPhone() != null ? curator.getMobPhone() : curator.getPhone();
            if (ph != null) contacts.append("📞 ").append(ph);
            if (curator.getEmail() != null) {
                if (contacts.length() > 0) contacts.append(" | ");
                contacts.append("✉ ").append(curator.getEmail());
            }
            detailCuratorContacts.setValue(contacts.length() > 0 ? contacts.toString() : "-");
        } else {
            detailCuratorName.setValue("Куратор не назначен");
            detailCuratorPosition.setValue("-");
            detailCuratorDept.setValue("-");
            detailCuratorContacts.setValue("-");
        }

        // Описание
        String desc = project.getId() != null ? projectDescriptionCache.get(project.getId()) : null;
        if (desc != null && !desc.trim().isEmpty()) {
            String plain = Jsoup.parse(desc).text();
            detailDescription.setValue(plain.length() > 300 ? plain.substring(0, 300) + "..." : plain);
        } else {
            detailDescription.setValue("<span style='color: #94a3b8;'>Описание проекта не заполнено</span>");
        }
    }

    private void clearSidebarDetails() {
        openEditCardBtn.setEnabled(false);
        createPositionForProjectBtn.setEnabled(false);
        logoPic.setSource(ThemeResource.class).setPath("icons/no-company.png");
        detailTitle.setValue("Выберите проект");
        detailSubtitle.setValue("-");
        detailLocation.setValue("-");
        detailStatus.setValue("-");
        detailStartDate.setValue("-");
        detailEndDate.setValue("-");
        detailOpenPositionsCount.setValue("0");
        detailCuratorName.setValue("-");
        detailCuratorPosition.setValue("-");
        detailCuratorDept.setValue("-");
        detailCuratorContacts.setValue("-");
        detailDescription.setValue("<span style='color: #94a3b8;'>Проект не выбран</span>");
    }
}
