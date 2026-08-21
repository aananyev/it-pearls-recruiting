package com.company.hunttech.web.screens.companydepartament;

import com.company.hunttech.entity.CompanyDepartament;
import com.company.hunttech.entity.Person;
import com.hunttech.hrm.gui.components.OvaFallbackImage;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.LookupComponent;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@UiController("hunttech_CompanyDepartamentReestr.browse")
@UiDescriptor("company-departament-reestr-browse.xml")
@LookupComponent("companyDepartamentsTable")
@LoadDataBeforeShow
public class CompanyDepartamentReestrBrowse extends StandardLookup<CompanyDepartament> {

    private static final String QUERY_PROJECTS_COUNT_BY_DEPTS =
            "select p.projectDepartment.id, count(p) from hunttech_Project p " +
            "where p.projectDepartment.id in :deptIds group by p.projectDepartment.id";

    private static final String QUERY_DEPT_DESCRIPTIONS_BY_IDS =
            "select e.id, e.departamentDescription from hunttech_CompanyDepartament e where e.id in :ids";

    @Inject
    private CollectionContainer<CompanyDepartament> companyDepartamentsDc;
    @Inject
    private CollectionLoader<CompanyDepartament> companyDepartamentsDl;
    @Inject
    private DataGrid<CompanyDepartament> companyDepartamentsTable;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private DataManager dataManager;
    @Inject
    private ScreenBuilders screenBuilders;

    @Inject
    private OvaFallbackImage logoPic;
    @Inject
    private Label<String> detailTitle;
    @Inject
    private Label<String> detailSubtitle;
    @Inject
    private Button openEditCardBtn;
    @Inject
    private Label<String> detailDirector;
    @Inject
    private Label<String> detailHrDirector;
    @Inject
    private Label<String> detailStaffCount;
    @Inject
    private Label<String> detailProjectsCount;
    @Inject
    private Label<String> detailDescription;

    private Map<UUID, Integer> deptProjectsCountCache = Collections.emptyMap();
    private Map<UUID, String> deptDescriptionCache = Collections.emptyMap();

    @Subscribe
    public void onInit(InitEvent event) {
        setupTableSelection();
        setupSidebarButtons();
    }

    private void setupTableSelection() {
        companyDepartamentsTable.addSelectionListener(e -> {
            Set<CompanyDepartament> selected = e.getSelected();
            if (selected != null && !selected.isEmpty()) {
                CompanyDepartament single = selected.iterator().next();
                updateSidebarDetails(single);
            } else {
                clearSidebarDetails();
            }
        });
    }

    private void setupSidebarButtons() {
        openEditCardBtn.addClickListener(e -> {
            CompanyDepartament selected = companyDepartamentsTable.getSingleSelected();
            if (selected != null) {
                screenBuilders.editor(companyDepartamentsTable)
                        .editEntity(selected)
                        .withOpenMode(OpenMode.DIALOG)
                        .show();
            }
        });
    }

    @Subscribe("actionsPopupButton.refreshAction")
    public void onRefreshAction(Action.ActionPerformedEvent event) {
        companyDepartamentsDl.load();
    }

    @Subscribe("actionsPopupButton.excelExportAction")
    public void onExcelExportAction(Action.ActionPerformedEvent event) {
        Action excel = companyDepartamentsTable.getAction("excel");
        if (excel != null) {
            excel.actionPerform(companyDepartamentsTable);
        }
    }

    @Subscribe(id = "companyDepartamentsDl", target = Target.DATA_LOADER)
    private void onCompanyDepartamentsDlPostLoad(CollectionLoader.PostLoadEvent<CompanyDepartament> event) {
        refreshCaches(event.getLoadedEntities());

        CompanyDepartament current = companyDepartamentsTable.getSingleSelected();
        if (current != null) {
            updateSidebarDetails(current);
        } else if (!event.getLoadedEntities().isEmpty()) {
            companyDepartamentsTable.setSelected(event.getLoadedEntities().get(0));
        } else {
            clearSidebarDetails();
        }
    }

    private void refreshCaches(List<CompanyDepartament> items) {
        List<UUID> ids = items.stream()
                .map(CompanyDepartament::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (ids.isEmpty()) {
            deptProjectsCountCache = Collections.emptyMap();
            deptDescriptionCache = Collections.emptyMap();
            return;
        }

        // Projects count
        List<KeyValueEntity> projectCounts = dataManager.loadValues(QUERY_PROJECTS_COUNT_BY_DEPTS)
                .properties("deptId", "count")
                .parameter("deptIds", ids)
                .list();
        Map<UUID, Integer> projMap = new HashMap<>();
        for (KeyValueEntity r : projectCounts) {
            UUID deptId = r.getValue("deptId");
            Long count = r.getValue("count");
            if (deptId != null && count != null) {
                projMap.put(deptId, count.intValue());
            }
        }
        deptProjectsCountCache = projMap;

        // Descriptions
        List<KeyValueEntity> descRows = dataManager.loadValues(QUERY_DEPT_DESCRIPTIONS_BY_IDS)
                .properties("id", "departamentDescription")
                .parameter("ids", ids)
                .list();
        Map<UUID, String> descMap = new HashMap<>();
        for (KeyValueEntity r : descRows) {
            UUID id = r.getValue("id");
            String desc = r.getValue("departamentDescription");
            if (id != null && desc != null) {
                descMap.put(id, desc);
            }
        }
        deptDescriptionCache = descMap;
    }

    private void updateSidebarDetails(CompanyDepartament dept) {
        openEditCardBtn.setEnabled(true);

        if (dept.getCompanyName() != null && dept.getCompanyName().getFileCompanyLogo() != null) {
            logoPic.setSource(FileDescriptorResource.class).setFileDescriptor(dept.getCompanyName().getFileCompanyLogo());
        } else {
            logoPic.setSource(ThemeResource.class).setPath("icons/no-company.png");
        }

        detailTitle.setValue(dept.getDepartamentRuName() != null ? dept.getDepartamentRuName() : "Без названия");
        detailSubtitle.setValue(dept.getCompanyName() != null ? dept.getCompanyName().getComanyName() : "-");

        detailDirector.setValue(dept.getDepartamentDirector() != null ? dept.getDepartamentDirector().getInstanceName() : "-");
        detailHrDirector.setValue(dept.getDepartamentHrDirector() != null ? dept.getDepartamentHrDirector().getInstanceName() : "-");
        detailStaffCount.setValue(dept.getDepartamentNumberOfProgrammers() != null ? String.valueOf(dept.getDepartamentNumberOfProgrammers()) : "-");

        int projCount = dept.getId() != null ? deptProjectsCountCache.getOrDefault(dept.getId(), 0) : 0;
        detailProjectsCount.setValue(String.valueOf(projCount));

        String desc = dept.getId() != null ? deptDescriptionCache.get(dept.getId()) : null;
        if (desc != null && !desc.trim().isEmpty()) {
            String plain = Jsoup.parse(desc).text();
            detailDescription.setValue(plain.length() > 300 ? plain.substring(0, 300) + "..." : plain);
        } else {
            detailDescription.setValue("<span style='color: #94a3b8;'>Описание подразделения отсутствует</span>");
        }
    }

    private void clearSidebarDetails() {
        openEditCardBtn.setEnabled(false);
        logoPic.setSource(ThemeResource.class).setPath("icons/no-company.png");
        detailTitle.setValue("Выберите департамент");
        detailSubtitle.setValue("-");
        detailDirector.setValue("-");
        detailHrDirector.setValue("-");
        detailStaffCount.setValue("-");
        detailProjectsCount.setValue("0");
        detailDescription.setValue("<span style='color: #94a3b8;'>Департамент не выбран</span>");
    }

    @Install(to = "companyDepartamentsTable.projectsCountColumn", subject = "columnGenerator")
    private Component companyDepartamentsTableProjectsCountColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<CompanyDepartament> event) {
        CompanyDepartament dept = event.getItem();
        int count = (dept != null && dept.getId() != null) ? deptProjectsCountCache.getOrDefault(dept.getId(), 0) : 0;

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
    }
}
