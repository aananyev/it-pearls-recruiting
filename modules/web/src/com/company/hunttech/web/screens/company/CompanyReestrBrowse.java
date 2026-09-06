package com.company.hunttech.web.screens.company;

import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.Project;
import com.hunttech.hrm.gui.components.OvaFallbackImage;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.company.hunttech.service.CompanyRequisitesIngestService;
import com.company.hunttech.service.CompanyRequisitesParsedData;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.actions.list.CreateAction;
import com.haulmont.cuba.gui.actions.list.EditAction;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.LookupComponent;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@UiController("hunttech_CompanyReestr.browse")
@UiDescriptor("company-reestr-browse.xml")
@LookupComponent("companiesTable")
@LoadDataBeforeShow
public class CompanyReestrBrowse extends StandardLookup<Company> {

    private static final Logger log = LoggerFactory.getLogger(CompanyReestrBrowse.class);

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
    private Metadata metadata;
    @Inject
    private Messages messages;
    @Inject
    private Notifications notifications;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private CompanyRequisitesIngestService companyRequisitesIngestService;

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
        setupTableActions();
        setupTableSelection();
        setupFilterActions();
        setupSidebarButtons();

        // Выравнивание заголовков профиля (под OvalFallbackImage) по центру
        detailTitle.setAlignment(Component.Alignment.MIDDLE_CENTER);
        detailSubtitle.setAlignment(Component.Alignment.MIDDLE_CENTER);
        detailLocation.setAlignment(Component.Alignment.MIDDLE_CENTER);
    }

    private void setupTableActions() {
        Action editAction = companiesTable.getAction("edit");
        if (editAction instanceof EditAction) {
            ((EditAction<Company>) editAction).setScreenClass(CompanyReestrEdit.class);
            ((EditAction<Company>) editAction).setScreenId("hunttech_CompanyReestr.edit");
            ((EditAction<Company>) editAction).setOpenMode(OpenMode.DIALOG);
        }
        Action createAction = companiesTable.getAction("create");
        if (createAction instanceof CreateAction) {
            ((CreateAction<Company>) createAction).setScreenClass(CompanyReestrEdit.class);
            ((CreateAction<Company>) createAction).setScreenId("hunttech_CompanyReestr.edit");
            ((CreateAction<Company>) createAction).setOpenMode(OpenMode.DIALOG);
        }
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
            String textHtml = "<div style='text-align: left;'><div style='font-weight: 600; color: #1e293b; font-size: 13px; white-space: normal; word-break: break-word; line-height: 1.35;'>" + name + "</div>" +
                    (sub.length() > 0 ? "<div style='font-size: 11px; color: #64748b; white-space: normal; word-break: break-word; line-height: 1.35;'>" + sub.toString() + "</div>" : "") + "</div>";
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
                        .withScreenClass(CompanyReestrEdit.class)
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

    @Subscribe("smartUploadBtn")
    public void onSmartUploadBtnClick(Button.ClickEvent event) {
        openSmartCompanyUploadDialog();
    }

    public void openSmartCompanyUploadDialog() {
        SmartCompanyRequisitesUploadScreen screen = screenBuilders.screen(this)
                .withScreenClass(SmartCompanyRequisitesUploadScreen.class)
                .withOpenMode(OpenMode.DIALOG)
                .build();
        screen.addAfterCloseListener(closeEvent -> {
            if (closeEvent.closedWith(StandardOutcome.COMMIT)) {
                CompanyRequisitesParsedData data = screen.getParsedData();
                if (data != null) {
                    Company company = null;
                    if (data.getInn() != null && !data.getInn().trim().isEmpty()) {
                        company = dataManager.load(Company.class)
                                .query("select c from hunttech_Company c where c.inn = :inn")
                                .parameter("inn", data.getInn().trim())
                                .view("company-edit-view")
                                .optional()
                                .orElse(null);
                    }
                    if (company == null) {
                        String nameToFind = null;
                        if (data.getLegalEntityName() != null && !data.getLegalEntityName().trim().isEmpty()) {
                            nameToFind = data.getLegalEntityName().trim();
                        } else if (data.getCompanyShortName() != null && !data.getCompanyShortName().trim().isEmpty()) {
                            nameToFind = data.getCompanyShortName().trim();
                        }
                        if (nameToFind != null) {
                            List<Company> matches = dataManager.load(Company.class)
                                    .query("select c from hunttech_Company c where lower(c.comanyName) = lower(:name) or lower(c.companyShortName) = lower(:name)")
                                    .parameter("name", nameToFind)
                                    .view("company-edit-view")
                                    .list();
                            if (!matches.isEmpty()) {
                                company = matches.get(0);
                            }
                        }
                    }
                    boolean isNew = false;
                    if (company == null) {
                        company = metadata.create(Company.class);
                        isNew = true;
                    }
                    company = companyRequisitesIngestService.applyRequisitesToCompany(company, data);
                    if (company.getComanyName() == null || company.getComanyName().trim().isEmpty()) {
                        if (data.getLegalEntityName() != null && !data.getLegalEntityName().trim().isEmpty()) {
                            company.setComanyName(data.getLegalEntityName().trim());
                        } else if (data.getCompanyShortName() != null && !data.getCompanyShortName().trim().isEmpty()) {
                            company.setComanyName(data.getCompanyShortName().trim());
                        } else {
                            company.setComanyName("Новая компания");
                        }
                    }
                    Company committedCompany = dataManager.commit(company);
                    companiesDl.load();
                    try {
                        companiesTable.setSelected(committedCompany);
                        updateSidebarDetails(committedCompany);
                    } catch (Exception e) {
                        log.warn("Failed to select or refresh company details after smart upload", e);
                    }
                    notifications.create(Notifications.NotificationType.TRAY)
                            .withCaption(messages.getMessage(CompanyReestrBrowse.class, isNew ? "msgCompanyCreatedSuccess" : "msgCompanyUpdatedSuccess"))
                            .withDescription(committedCompany.getComanyName() != null ? committedCompany.getComanyName() : "")
                            .show();
                }
            }
        });
        screen.show();
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
            detailOurClient.setValue("✓ Да (Клиент)");
        } else {
            detailOurClient.setValue("Нет");
        }

        if (Boolean.TRUE.equals(company.getOurLegalEntity())) {
            detailOurLegalEntity.setValue("✓ Наше юр. лицо");
        } else {
            detailOurLegalEntity.setValue("Нет");
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
            detailDescription.setValue("Описание не заполнено");
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
        detailDescription.setValue("Компания не выбрана");
    }
}
