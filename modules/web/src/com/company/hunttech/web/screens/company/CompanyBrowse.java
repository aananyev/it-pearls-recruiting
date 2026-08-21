package com.company.hunttech.web.screens.company;

import com.company.hunttech.entity.Company;
import com.company.hunttech.service.CompanyRequisitesIngestService;
import com.company.hunttech.service.CompanyRequisitesParsedData;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.LookupComponent;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@UiController("hunttech_Company.browse")
@UiDescriptor("company-browse.xml")
@LookupComponent("companiesTable")
@LoadDataBeforeShow
public class CompanyBrowse extends StandardLookup<Company> {
    private static final String QUERY_COMPANY_DESCRIPTIONS_BY_IDS =
            "select e.id, e.companyDescription from hunttech_Company e where e.id in :ids";

    @Inject
    private UiComponents uiComponents;

    @Install(to = "companiesTable.companyLogoColumn", subject = "columnGenerator")
    private Component companiesTableCompanyLogoColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<Company> event) {
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        retBox.setWidthFull();
        retBox.setHeightFull();

        Image image = uiComponents.create(Image.class);
        image.setDescriptionAsHtml(true);
        image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        image.setWidth("20px");
        image.setHeight("20px");
        image.setStyleName("icon-no-border-20px");
        image.setAlignment(Component.Alignment.MIDDLE_CENTER);
        image.setDescription("<h4>"
                + event.getItem().getComanyName()
                + "</h4><br><br>"
                + getCompanyDescription(event.getItem()));

        if (event.getItem().getFileCompanyLogo() != null) {
            image.setSource(FileDescriptorResource.class)
                    .setFileDescriptor(event
                            .getItem()
                            .getFileCompanyLogo());
        } else {
            image.setSource(ThemeResource.class).setPath("icons/no-company.png");
        }

        retBox.add(image);
        return retBox;
    }

    @Inject
    private TreeDataGrid<Company> companiesTable;
    @Inject
    private Metadata metadata;
    @Inject
    private Notifications notifications;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private CompanyRequisitesIngestService companyRequisitesIngestService;

    @Inject
    private CheckBox checkBoxOnlyOurClient;
    @Inject
    private CollectionLoader<Company> companiesDl;
    @Inject
    private CheckBox checkBoxOnlyLegalEntity;
    @Inject
    private DataManager dataManager;

    private Map<UUID, String> companyDescriptionCache = Collections.emptyMap();

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
                    boolean isNew = false;
                    if (company == null) {
                        company = metadata.create(Company.class);
                        isNew = true;
                    }
                    companyRequisitesIngestService.applyRequisitesToCompany(company, data);
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
                    } catch (Exception ignored) {
                    }
                    notifications.create(Notifications.NotificationType.TRAY)
                            .withCaption(isNew ? "Компания успешно создана" : "Реквизиты компании обновлены")
                            .withDescription(committedCompany.getComanyName() != null ? committedCompany.getComanyName() : "")
                            .show();
                }
            }
        });
        screen.show();
    }

    @Subscribe(id = "companiesDl", target = Target.DATA_LOADER)
    private void onCompaniesDlPostLoad(CollectionLoader.PostLoadEvent<Company> event) {
        refreshCompanyDescriptionCache(event.getLoadedEntities());
    }

    private void refreshCompanyDescriptionCache(List<Company> companies) {
        List<UUID> ids = companies.stream()
                .map(Company::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            companyDescriptionCache = Collections.emptyMap();
            return;
        }
        List<KeyValueEntity> rows = dataManager.loadValues(QUERY_COMPANY_DESCRIPTIONS_BY_IDS)
                .properties("id", "companyDescription")
                .parameter("ids", ids)
                .list();
        Map<UUID, String> cache = new HashMap<>();
        for (KeyValueEntity row : rows) {
            UUID id = row.getValue("id");
            String description = row.getValue("companyDescription");
            if (id != null) {
                cache.put(id, description);
            }
        }
        companyDescriptionCache = cache;
    }

    private String getCompanyDescription(Company company) {
        if (company == null || company.getId() == null) {
            return null;
        }
        return companyDescriptionCache.get(company.getId());
    }

    @Subscribe("checkBoxOnlyOurClient")
    public void onCheckBoxOnlyOurClientValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        filterOurClients();
    }

    @Subscribe("checkBoxOnlyLegalEntity")
    public void onCheckBoxOnlyLegalEntityValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        filterOurLegalEntity();
    }

    private void filterOurLegalEntity() {
        if (checkBoxOnlyLegalEntity.getValue()) {
            companiesDl.setParameter("setOurLegalEntity", true);
        } else {
            companiesDl.removeParameter("setOurLegalEntity");
        }

        companiesDl.load();
    }

    private void filterOurClients() {
        if (checkBoxOnlyOurClient.getValue()) {
            companiesDl.setParameter("setOurClient", true);
        } else {
            companiesDl.removeParameter("setOurClient");
        }

        companiesDl.load();
    }

    @Install(to = "companiesTable.ourCompanyIconColumn", subject = "columnGenerator")
    private Icons.Icon companiesTableOurCompanyIconColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<Company> event) {
        String returnIcon = "";

        if(event.getItem().getOurLegalEntity() != null) {
            if (event.getItem().getOurLegalEntity()) {
                returnIcon = "PLUS_CIRCLE";
            } else {
                returnIcon = "MINUS_CIRCLE";
            }
        } else {
            returnIcon = "MINUS_CIRCLE";
        }

        return CubaIcon.valueOf(returnIcon);
    }

    @Install(to = "companiesTable.ourClientIconColumn", subject = "columnGenerator")
    private Icons.Icon companiesTableOurClientIconColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<Company> event) {
        String returnIcon = "";

        if( event.getItem().getOurClient() != null) {
            if (event.getItem().getOurClient()) {
                returnIcon = "PLUS_CIRCLE";
            } else {
                returnIcon = "MINUS_CIRCLE";
            }
        } else {
            returnIcon = "MINUS_CIRCLE";
        }

        return CubaIcon.valueOf(returnIcon);
    }

    @Install(to = "companiesTable.ourCompanyIconColumn", subject = "styleProvider")
    private String companiesTableOurCompanyIconColumnStyleProvider(Company company) {
        String style = "";

        if(company.getOurLegalEntity() != null) {
            if (company.getOurLegalEntity()) {
                style = "open-position-pic-center-large-green";
            } else {
                style = "open-position-pic-center-large-red";
            }
        } else {
            style = "open-position-pic-center-large-red";
        }

        return style;
    }

    @Install(to = "companiesTable.ourClientIconColumn", subject = "styleProvider")
    private String companiesTableOurClientIconColumnStyleProvider(Company company) {
        String style = "";

        if(company.getOurClient() != null) {
            if (company.getOurClient()) {
                style = "open-position-pic-center-large-green";
            } else {
                style = "open-position-pic-center-large-red";
            }
        } else {
            style = "open-position-pic-center-large-red";
        }

        return style;
    }
}
