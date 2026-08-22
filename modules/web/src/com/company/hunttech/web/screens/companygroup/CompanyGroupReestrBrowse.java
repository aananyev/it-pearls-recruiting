package com.company.hunttech.web.screens.companygroup;

import com.company.hunttech.entity.CompanyGroup;
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

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@UiController("hunttech_CompanyGroupReestr.browse")
@UiDescriptor("company-group-reestr-browse.xml")
@LookupComponent("companyGroupsTable")
@LoadDataBeforeShow
public class CompanyGroupReestrBrowse extends StandardLookup<CompanyGroup> {

    private static final String QUERY_COMPANIES_BY_GROUP_IDS =
            "select c.companyGroup.id, c.comanyName from hunttech_Company c " +
            "where c.companyGroup.id in :groupIds order by c.comanyName";

    @Inject
    private CollectionContainer<CompanyGroup> companyGroupsDc;
    @Inject
    private CollectionLoader<CompanyGroup> companyGroupsDl;
    @Inject
    private GroupTable<CompanyGroup> companyGroupsTable;
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
    private Label<String> detailCompaniesCount;
    @Inject
    private Label<String> detailCompaniesList;

    private Map<UUID, List<String>> groupCompaniesCache = Collections.emptyMap();

    @Subscribe
    public void onInit(InitEvent event) {
        setupTableColumns();
        setupTableSelection();
        setupSidebarButtons();
    }

    private void setupTableColumns() {
        companyGroupsTable.addGeneratedColumn("companyRuGroupName", group -> {
            String name = (group != null && group.getCompanyRuGroupName() != null) ? group.getCompanyRuGroupName() : "Без наименования";
            List<String> list = (group != null && group.getId() != null) ? groupCompaniesCache.getOrDefault(group.getId(), Collections.emptyList()) : Collections.emptyList();
            
            StringBuilder sub = new StringBuilder();
            if (!list.isEmpty()) {
                sub.append("🏢 ").append(String.join(", ", list.subList(0, Math.min(list.size(), 3))));
                if (list.size() > 3) {
                    sub.append(" (+").append(list.size() - 3).append(")");
                }
            }

            String textHtml = "<div style='text-align: left;'><div style='font-weight: 600; color: #1e293b; font-size: 13px; white-space: normal; word-break: break-word; line-height: 1.35;'>" + name + "</div>" +
                    (sub.length() > 0 ? "<div style='font-size: 11px; color: #64748b; white-space: normal; word-break: break-word; line-height: 1.35;'>" + sub.toString() + "</div>" : "") + "</div>";
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            lbl.setWidth("100%");
            lbl.setValue(textHtml);
            return lbl;
        });

        companyGroupsTable.addGeneratedColumn("companiesCountColumn", group -> {
            List<String> list = (group != null && group.getId() != null) ? groupCompaniesCache.getOrDefault(group.getId(), Collections.emptyList()) : Collections.emptyList();
            int count = list.size();

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
        companyGroupsTable.addSelectionListener(e -> {
            Set<CompanyGroup> selected = e.getSelected();
            if (selected != null && !selected.isEmpty()) {
                CompanyGroup single = selected.iterator().next();
                updateSidebarDetails(single);
            } else {
                clearSidebarDetails();
            }
        });
    }

    private void setupSidebarButtons() {
        openEditCardBtn.addClickListener(e -> {
            CompanyGroup selected = companyGroupsTable.getSingleSelected();
            if (selected != null) {
                screenBuilders.editor(companyGroupsTable)
                        .editEntity(selected)
                        .withOpenMode(OpenMode.DIALOG)
                        .show();
            }
        });
    }

    @Subscribe("actionsPopupButton.refreshAction")
    public void onRefreshAction(Action.ActionPerformedEvent event) {
        companyGroupsDl.load();
    }

    @Subscribe("actionsPopupButton.excelExportAction")
    public void onExcelExportAction(Action.ActionPerformedEvent event) {
        Action excel = companyGroupsTable.getAction("excel");
        if (excel != null) {
            excel.actionPerform(companyGroupsTable);
        }
    }

    @Subscribe(id = "companyGroupsDl", target = Target.DATA_LOADER)
    private void onCompanyGroupsDlPostLoad(CollectionLoader.PostLoadEvent<CompanyGroup> event) {
        refreshCompaniesCache(event.getLoadedEntities());

        CompanyGroup current = companyGroupsTable.getSingleSelected();
        if (current != null) {
            updateSidebarDetails(current);
        } else if (!event.getLoadedEntities().isEmpty()) {
            companyGroupsTable.setSelected(event.getLoadedEntities().get(0));
        } else {
            clearSidebarDetails();
        }
    }

    private void refreshCompaniesCache(List<CompanyGroup> groups) {
        List<UUID> ids = groups.stream()
                .map(CompanyGroup::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (ids.isEmpty()) {
            groupCompaniesCache = Collections.emptyMap();
            return;
        }

        List<KeyValueEntity> rows = dataManager.loadValues(QUERY_COMPANIES_BY_GROUP_IDS)
                .properties("groupId", "companyName")
                .parameter("groupIds", ids)
                .list();

        Map<UUID, List<String>> cache = new HashMap<>();
        for (KeyValueEntity r : rows) {
            UUID gId = r.getValue("groupId");
            String cName = r.getValue("companyName");
            if (gId != null && cName != null) {
                cache.computeIfAbsent(gId, k -> new ArrayList<>()).add(cName);
            }
        }
        groupCompaniesCache = cache;
    }

    private void updateSidebarDetails(CompanyGroup group) {
        openEditCardBtn.setEnabled(true);
        logoPic.setSource(ThemeResource.class).setPath("icons/no-company.png");

        detailTitle.setValue(group.getCompanyRuGroupName() != null ? group.getCompanyRuGroupName() : "Без названия");
        detailSubtitle.setValue("Холдинг / Группа компаний");

        List<String> companies = group.getId() != null ? groupCompaniesCache.getOrDefault(group.getId(), Collections.emptyList()) : Collections.emptyList();
        detailCompaniesCount.setValue(String.valueOf(companies.size()));

        if (!companies.isEmpty()) {
            StringBuilder sb = new StringBuilder("<ul style='padding-left: 16px; margin: 0;'>");
            for (String comp : companies) {
                sb.append("<li style='margin-bottom: 4px;'><b>").append(comp).append("</b></li>");
            }
            sb.append("</ul>");
            detailCompaniesList.setValue(sb.toString());
        } else {
            detailCompaniesList.setValue("<span style='color: #94a3b8;'>В группу пока не добавлено ни одной компании</span>");
        }
    }

    private void clearSidebarDetails() {
        openEditCardBtn.setEnabled(false);
        logoPic.setSource(ThemeResource.class).setPath("icons/no-company.png");
        detailTitle.setValue("Выберите группу");
        detailSubtitle.setValue("Холдинг / Группа компаний");
        detailCompaniesCount.setValue("0");
        detailCompaniesList.setValue("<span style='color: #94a3b8;'>Группа не выбрана</span>");
    }
}
