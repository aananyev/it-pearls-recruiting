package com.company.hunttech.web.screens.company;

import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.Project;
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

@UiController("hunttech_CompanyReestr.browse")
@UiDescriptor("company-reestr-browse.xml")
@LookupComponent("companiesTable")
@LoadDataBeforeShow
public class CompanyReestrBrowse extends StandardLookup<Company> {

    private static final String QUERY_COMPANY_DESCRIPTIONS_BY_IDS =
            "select e.id, e.companyDescription, e.workingConditions from hunttech_Company e where e.id in :ids";

    private static final String QUERY_PROJECTS_COUNT_BY_COMPANIES =
            "select d.companyName.id, count(p) from hunttech_Project p join p.projectDepartment d " +
            "where d.companyName.id in :companyIds group by d.companyName.id";

    private static final String QUERY_OPEN_POSITIONS_COUNT_BY_COMPANIES =
            "select d.companyName.id, count(op) from hunttech_OpenPosition op join op.projectName p join p.projectDepartment d " +
            "where not (op.openClose = true) and d.companyName.id in :companyIds group by d.companyName.id";

    @Inject
    private CollectionContainer<Company> companiesDc;
    @Inject
    private CollectionLoader<Company> companiesDl;
    @Inject
    private GroupTable<Company> companiesTable;
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
    private Label<String> detailLocation;
    @Inject
    private Button openEditCardBtn;
    @Inject
    private Button createProjectFromCompanyBtn;
    @Inject
    private Label<String> detailOurClient;
    @Inject
    private Label<String> detailOurLegalEntity;
    @Inject
    private Label<String> detailCompanyGroup;
    @Inject
    private Label<String> detailOwnership;
    @Inject
    private Label<String> detailDirector;
    @Inject
    private Label<String> detailProjectsCount;
    @Inject
    private Label<String> detailPositionsCount;
    @Inject
    private Label<String> detailDescription;

    @Inject
    private PopupButton filterPopupButton;

    private Map<UUID, String> companyDescriptionCache = Collections.emptyMap();
    private Map<UUID, String> companyWorkingConditionsCache = Collections.emptyMap();
    private Map<UUID, Integer> projectsCountCache = Collections.emptyMap();
    private Map<UUID, Integer> openPositionsCountCache = Collections.emptyMap();

    @Subscribe
    public void onInit(InitEvent event) {
        setupTableColumns();
        setupTableSelection();
        setupFilterActions();
        setupSidebarButtons();
    }

    private void setupTableColumns() {
        companiesTable.addGeneratedColumn("companyLogoColumn", company -> {
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

            String desc = (company != null && company.getId() != null) ? companyDescriptionCache.get(company.getId()) : null;
            image.setDescription("<h4>" + ((company != null && company.getComanyName() != null) ? company.getComanyName() : "") + "</h4>" +
                    (desc != null ? "<br>" + Jsoup.parse(desc).text() : ""));

            if (company != null && company.getFileCompanyLogo() != null) {
                image.setSource(FileDescriptorResource.class).setFileDescriptor(company.getFileCompanyLogo());
            } else {
                image.setSource(ThemeResource.class).setPath("icons/no-company.png");
            }

            retBox.add(image);
            return retBox;
        });

        companiesTable.addGeneratedColumn("comanyName", company -> {
            String name = (company != null && company.getComanyName() != null) ? company.getComanyName() : "Без наименования";
            StringBuilder sub = new StringBuilder();
            if (company != null) {
                if (company.getCompanyShortName() != null && !company.getCompanyShortName().trim().isEmpty()
                        && !company.getCompanyShortName().trim().equals(name)) {
                    sub.append("🏷 ").append(company.getCompanyShortName().trim());
                }
                if (company.getCompanyGroup() != null && company.getCompanyGroup().getCompanyRuGroupName() != null) {
                    if (sub.length() > 0) sub.append(" • ");
                    sub.append("🌐 ").append(company.getCompanyGroup().getCompanyRuGroupName().trim());
                }
            }
            String textHtml = "<div style='text-align: left;'><div style='font-weight: 600; color: #1e293b; font-size: 13px;'>" + name + "</div>" +
                    (sub.length() > 0 ? "<div style='font-size: 11px; color: #64748b;'>" + sub.toString() + "</div>" : "") + "</div>";
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            lbl.setWidth("100%");
            lbl.setValue(textHtml);
            return lbl;
        });

        companiesTable.addGeneratedColumn("ourClientIconColumn", company -> {
            HBoxLayout box = uiComponents.create(HBoxLayout.class);
            box.setWidthFull();
            box.setHeightFull();
            box.setAlignment(Component.Alignment.MIDDLE_CENTER);

            Label label = uiComponents.create(Label.class);
            label.setHtmlEnabled(true);
            label.setAlignment(Component.Alignment.MIDDLE_CENTER);

            if (company != null && Boolean.TRUE.equals(company.getOurClient())) {
                label.setValue("<span style='background: #dcfce7; color: #15803d; border: 1px solid #bbf7d0; padding: 2px 6px; border-radius: 4px; font-size: 10.5px; font-weight: 600;'>✓ Клиент</span>");
            } else {
                label.setValue("<span style='color: #cbd5e1; font-size: 11px;'>—</span>");
            }

            box.add(label);
            return box;
        });

        companiesTable.addGeneratedColumn("ourCompanyIconColumn", company -> {
            HBoxLayout box = uiComponents.create(HBoxLayout.class);
            box.setWidthFull();
            box.setHeightFull();
            box.setAlignment(Component.Alignment.MIDDLE_CENTER);

            Label label = uiComponents.create(Label.class);
            label.setHtmlEnabled(true);
            label.setAlignment(Component.Alignment.MIDDLE_CENTER);

            if (company != null && Boolean.TRUE.equals(company.getOurLegalEntity())) {
                label.setValue("<span style='background: #dbeafe; color: #1d4ed8; border: 1px solid #bfdbfe; padding: 2px 6px; border-radius: 4px; font-size: 10.5px; font-weight: 600;'>⚡ Наше ЮЛ</span>");
            } else {
                label.setValue("<span style='color: #cbd5e1; font-size: 11px;'>—</span>");
            }

            box.add(label);
            return box;
        });

        companiesTable.addGeneratedColumn("cityOfCompany", company -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            String city = (company != null && company.getCityOfCompany() != null && company.getCityOfCompany().getCityRuName() != null)
                    ? company.getCityOfCompany().getCityRuName() : "—";
            lbl.setValue("<span style='font-size: 12px; color: #334155;'>" + ("—".equals(city) ? "—" : "📍 " + city) + "</span>");
            return lbl;
        });

        companiesTable.addGeneratedColumn("countryOfCompany", company -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            String country = (company != null && company.getCountryOfCompany() != null && company.getCountryOfCompany().getCountryRuName() != null)
                    ? company.getCountryOfCompany().getCountryRuName() : "—";
            lbl.setValue("<span style='font-size: 12px; color: #334155;'>" + ("—".equals(country) ? "—" : "🌍 " + country) + "</span>");
            return lbl;
        });
    }

    private void setupTableSelection() {
        companiesTable.addSelectionListener(e -> {
            Set<Company> selected = e.getSelected();
            if (selected != null && !selected.isEmpty()) {
                Company single = selected.iterator().next();
                updateSidebarDetails(single);
            } else {
                clearSidebarDetails();
            }
        });
    }

    private void setupSidebarButtons() {
        openEditCardBtn.addClickListener(e -> {
            Company selected = companiesTable.getSingleSelected();
            if (selected != null) {
                screenBuilders.editor(companiesTable)
                        .editEntity(selected)
                        .withOpenMode(OpenMode.DIALOG)
                        .show();
            }
        });

        createProjectFromCompanyBtn.addClickListener(e -> {
            Company selected = companiesTable.getSingleSelected();
            if (selected != null) {
                Project newProject = dataManager.create(Project.class);
                screenBuilders.editor(Project.class, this)
                        .newEntity(newProject)
                        .withOpenMode(OpenMode.DIALOG)
                        .show();
            }
        });
    }

    private void setupFilterActions() {
        // Действия фильтрации
    }

    @Subscribe("filterPopupButton.filterAll")
    public void onFilterAll(Action.ActionPerformedEvent event) {
        companiesDl.removeParameter("setOurClient");
        companiesDl.removeParameter("setOurLegalEntity");
        filterPopupButton.setCaption("Все компании");
        companiesDl.load();
    }

    @Subscribe("filterPopupButton.filterClientsOnly")
    public void onFilterClientsOnly(Action.ActionPerformedEvent event) {
        companiesDl.setParameter("setOurClient", true);
        companiesDl.removeParameter("setOurLegalEntity");
        filterPopupButton.setCaption("Только наши клиенты");
        companiesDl.load();
    }

    @Subscribe("filterPopupButton.filterLegalEntitiesOnly")
    public void onFilterLegalEntitiesOnly(Action.ActionPerformedEvent event) {
        companiesDl.setParameter("setOurLegalEntity", true);
        companiesDl.removeParameter("setOurClient");
        filterPopupButton.setCaption("Только наши юр. лица");
        companiesDl.load();
    }

    @Subscribe("actionsPopupButton.refreshAction")
    public void onRefreshAction(Action.ActionPerformedEvent event) {
        companiesDl.load();
    }

    @Subscribe("actionsPopupButton.excelExportAction")
    public void onExcelExportAction(Action.ActionPerformedEvent event) {
        Action excel = companiesTable.getAction("excel");
        if (excel != null) {
            excel.actionPerform(companiesTable);
        }
    }

    @Subscribe(id = "companiesDl", target = Target.DATA_LOADER)
    private void onCompaniesDlPostLoad(CollectionLoader.PostLoadEvent<Company> event) {
        refreshCaches(event.getLoadedEntities());
        // Синхронизация сайдбара
        Company current = companiesTable.getSingleSelected();
        if (current != null) {
            updateSidebarDetails(current);
        } else if (!event.getLoadedEntities().isEmpty()) {
            companiesTable.setSelected(event.getLoadedEntities().get(0));
        } else {
            clearSidebarDetails();
        }
    }

    private void refreshCaches(List<Company> companies) {
        List<UUID> ids = companies.stream()
                .map(Company::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (ids.isEmpty()) {
            companyDescriptionCache = Collections.emptyMap();
            companyWorkingConditionsCache = Collections.emptyMap();
            projectsCountCache = Collections.emptyMap();
            openPositionsCountCache = Collections.emptyMap();
            return;
        }

        // 1. Описания
        List<KeyValueEntity> descRows = dataManager.loadValues(QUERY_COMPANY_DESCRIPTIONS_BY_IDS)
                .properties("id", "companyDescription", "workingConditions")
                .parameter("ids", ids)
                .list();
        Map<UUID, String> descMap = new HashMap<>();
        Map<UUID, String> condMap = new HashMap<>();
        for (KeyValueEntity row : descRows) {
            UUID id = row.getValue("id");
            String desc = row.getValue("companyDescription");
            String cond = row.getValue("workingConditions");
            if (id != null) {
                if (desc != null) descMap.put(id, desc);
                if (cond != null) condMap.put(id, cond);
            }
        }
        companyDescriptionCache = descMap;
        companyWorkingConditionsCache = condMap;

        // 2. Количество проектов
        List<KeyValueEntity> projRows = dataManager.loadValues(QUERY_PROJECTS_COUNT_BY_COMPANIES)
                .properties("companyId", "count")
                .parameter("companyIds", ids)
                .list();
        Map<UUID, Integer> projMap = new HashMap<>();
        for (KeyValueEntity row : projRows) {
            UUID cid = row.getValue("companyId");
            Long count = row.getValue("count");
            if (cid != null && count != null) {
                projMap.put(cid, count.intValue());
            }
        }
        projectsCountCache = projMap;

        // 3. Количество открытых вакансий
        List<KeyValueEntity> posRows = dataManager.loadValues(QUERY_OPEN_POSITIONS_COUNT_BY_COMPANIES)
                .properties("companyId", "count")
                .parameter("companyIds", ids)
                .list();
        Map<UUID, Integer> posMap = new HashMap<>();
        for (KeyValueEntity row : posRows) {
            UUID cid = row.getValue("companyId");
            Long count = row.getValue("count");
            if (cid != null && count != null) {
                posMap.put(cid, count.intValue());
            }
        }
        openPositionsCountCache = posMap;
    }

    private void updateSidebarDetails(Company company) {
        openEditCardBtn.setEnabled(true);
        createProjectFromCompanyBtn.setEnabled(true);

        // Логотип
        if (company.getFileCompanyLogo() != null) {
            logoPic.setSource(FileDescriptorResource.class).setFileDescriptor(company.getFileCompanyLogo());
        } else {
            logoPic.setSource(ThemeResource.class).setPath("icons/no-company.png");
        }

        // Заголовки
        detailTitle.setValue(company.getComanyName() != null ? company.getComanyName() : "Без названия");
        detailSubtitle.setValue(company.getCompanyShortName() != null ? company.getCompanyShortName() : "-");

        StringBuilder loc = new StringBuilder();
        if (company.getCountryOfCompany() != null && company.getCountryOfCompany().getCountryRuName() != null) {
            loc.append(company.getCountryOfCompany().getCountryRuName());
        }
        if (company.getCityOfCompany() != null && company.getCityOfCompany().getCityRuName() != null) {
            if (loc.length() > 0) loc.append(", ");
            loc.append(company.getCityOfCompany().getCityRuName());
        }
        detailLocation.setValue(loc.length() > 0 ? loc.toString() : "-");

        // Реквизиты и статус
        if (Boolean.TRUE.equals(company.getOurClient())) {
            detailOurClient.setValue("<span style='color: #16a34a; font-weight: 600;'>✓ Да (Клиент)</span>");
        } else {
            detailOurClient.setValue("<span style='color: #94a3b8;'>Нет</span>");
        }

        if (Boolean.TRUE.equals(company.getOurLegalEntity())) {
            detailOurLegalEntity.setValue("<span style='color: #2563eb; font-weight: 600;'>✓ Наше юр. лицо</span>");
        } else {
            detailOurLegalEntity.setValue("<span style='color: #94a3b8;'>Нет</span>");
        }

        detailCompanyGroup.setValue(company.getCompanyGroup() != null ? company.getCompanyGroup().getCompanyRuGroupName() : "-");
        detailOwnership.setValue(company.getCompanyOwnership() != null ? company.getCompanyOwnership().getLongType() : "-");

        if (company.getCompanyDirector() != null) {
            detailDirector.setValue(company.getCompanyDirector().getInstanceName());
        } else {
            detailDirector.setValue("-");
        }

        // Статистика
        int projCount = company.getId() != null ? projectsCountCache.getOrDefault(company.getId(), 0) : 0;
        int posCount = company.getId() != null ? openPositionsCountCache.getOrDefault(company.getId(), 0) : 0;
        detailProjectsCount.setValue(String.valueOf(projCount));
        detailPositionsCount.setValue(String.valueOf(posCount));

        // Описание
        String desc = company.getId() != null ? companyDescriptionCache.get(company.getId()) : null;
        if (desc != null && !desc.trim().isEmpty()) {
            String plain = Jsoup.parse(desc).text();
            detailDescription.setValue(plain.length() > 300 ? plain.substring(0, 300) + "..." : plain);
        } else {
            detailDescription.setValue("<span style='color: #94a3b8;'>Описание не заполнено</span>");
        }
    }

    private void clearSidebarDetails() {
        openEditCardBtn.setEnabled(false);
        createProjectFromCompanyBtn.setEnabled(false);
        logoPic.setSource(ThemeResource.class).setPath("icons/no-company.png");
        detailTitle.setValue("Выберите компанию");
        detailSubtitle.setValue("-");
        detailLocation.setValue("-");
        detailOurClient.setValue("-");
        detailOurLegalEntity.setValue("-");
        detailCompanyGroup.setValue("-");
        detailOwnership.setValue("-");
        detailDirector.setValue("-");
        detailProjectsCount.setValue("0");
        detailPositionsCount.setValue("0");
        detailDescription.setValue("<span style='color: #94a3b8;'>Компания не выбрана</span>");
    }
}
