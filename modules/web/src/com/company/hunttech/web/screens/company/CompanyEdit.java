package com.company.hunttech.web.screens.company;

import com.company.hunttech.entity.City;
import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.Country;
import com.company.hunttech.entity.Ownershup;
import com.company.hunttech.entity.Person;
import com.company.hunttech.entity.Region;
import com.company.hunttech.service.CompanyRequisitesIngestService;
import com.company.hunttech.service.CompanyRequisitesParsedData;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.ViewBuilder;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.screen.*;
import com.hunttech.hrm.web.components.WebOvaFallbackImage;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@UiController("hunttech_Company.edit")
@UiDescriptor("company-edit.xml")
@EditedEntityContainer("companyDc")
@LoadDataBeforeShow
public class CompanyEdit extends StandardEditor<Company> {
    @Inject
    private WebOvaFallbackImage companyLogoFileImage;
    @Inject
    private FileUploadField companyLogoFileUpload;
    @Inject
    private DataManager dataManager;
    @Inject
    private DataContext dataContext;
    @Inject
    private CollectionLoader<Ownershup> companyOwnershipsLc;
    @Inject
    private CollectionLoader<Person> companyDirectorsLc;
    @Inject
    private CollectionLoader<City> cityOfCompaniesLc;
    @Inject
    private CollectionLoader<Region> regionOfCompaniesLc;
    @Inject
    private CollectionLoader<Country> countryOfCompaniesLc;
    @Inject
    private TabSheet mainTab;
    @Inject
    private Messages messages;
    @Inject
    private Label companySidebarTitle;
    @Inject
    private Button companyEditorNavMain;
    @Inject
    private Button companyEditorNavRequisites;
    @Inject
    private Button companyEditorNavDescription;
    @Inject
    private Button companyEditorNavDepartments;
    @Inject
    private VBoxLayout companyEditorSidebarNavigation;
    @Inject
    private Screens screens;
    @Inject
    private CompanyRequisitesIngestService companyRequisitesIngestService;
    @Inject
    private Notifications notifications;

    private boolean addressLoaded;
    private boolean companyDescriptionLoaded;
    private boolean departmentsLoaded;

    /** Соответствие «имя вкладки TabSheet → пункт label-навигации». */
    private static final Map<String, String> TAB_TO_NAV_BUTTON =
            Collections.unmodifiableMap(new HashMap<String, String>() {{
                put("tabConpanyDetails", "companyEditorNavMain");
                put("companyRequisitesTab", "companyEditorNavRequisites");
                put("companyDescriptionTab", "companyEditorNavDescription");
                put("tabCompanyDepartament", "companyEditorNavDepartments");
            }});

    /** Вкладки с поддержкой sidebar-навигации: все 4 вкладки компании. */
    private static final Set<String> TABS_WITH_SIDEBAR_NAVIGATION =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    "tabConpanyDetails", "companyRequisitesTab", "companyDescriptionTab", "tabCompanyDepartament")));

    @Subscribe("mainTab")
    public void onMainTabSelectedTabChange(TabSheet.SelectedTabChangeEvent event) {
        if (event.getSelectedTab() == null || PersistenceHelper.isNew(getEditedEntity())) {
            return;
        }
        String tabName = event.getSelectedTab().getName();
        if ("tabConpanyDetails".equals(tabName) && !addressLoaded) {
            loadAddress();
            addressLoaded = true;
        }
        if ("companyDescriptionTab".equals(tabName) && !companyDescriptionLoaded) {
            loadCompanyDescriptions();
            companyDescriptionLoaded = true;
        }
        if ("tabCompanyDepartament".equals(tabName) && !departmentsLoaded) {
            loadDepartments();
            departmentsLoaded = true;
        }
    }

    private void loadAddress() {
        Company reloaded = dataManager.reload(getEditedEntity(), ViewBuilder.of(Company.class)
                .add("addressOfCompany")
                .build());
        getEditedEntity().setAddressOfCompany(reloaded.getAddressOfCompany());
    }

    private void loadCompanyDescriptions() {
        Company reloaded = dataManager.reload(getEditedEntity(), ViewBuilder.of(Company.class)
                .add("companyDescription")
                .add("workingConditions")
                .build());
        getEditedEntity().setCompanyDescription(reloaded.getCompanyDescription());
        getEditedEntity().setWorkingConditions(reloaded.getWorkingConditions());
    }

    private void loadDepartments() {
        Company reloaded = dataManager.reload(getEditedEntity(), ViewBuilder.of(Company.class)
                .add("departmentOfCompany", "companyDepartament-department-child-view")
                .build());
        getEditedEntity().setDepartmentOfCompany(reloaded.getDepartmentOfCompany());
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if (PersistenceHelper.isNew(getEditedEntity())) {
            getEditedEntity().setOurClient(false);
        } else if (!addressLoaded) {
            loadAddress();
            addressLoaded = true;
        }
    }

    @Subscribe("cityOfCompanyField")
    public void onCityOfCompanyFieldValueChange(HasValue.ValueChangeEvent<City> event) {
        handleCityChange(event.getValue());
    }

    @Subscribe("cityOfCompanyRequisitesField")
    public void onCityOfCompanyRequisitesFieldValueChange(HasValue.ValueChangeEvent<City> event) {
        handleCityChange(event.getValue());
    }

    private void handleCityChange(City city) {
        if (city == null) {
            return;
        }
        City cityLoaded = dataManager.reload(city, "city-location-view");
        Region region = cityLoaded.getCityRegion();
        getEditedEntity().setRegionOfCompany(region);
        if (region != null) {
            Region regionLoaded = dataManager.reload(region, "region-browse-view");
            getEditedEntity().setCountryOfCompany(regionLoaded.getRegionCountry());
        }
    }

    @Subscribe("regionOfCompanyField")
    public void onRegionOfCompanyFieldValueChange(HasValue.ValueChangeEvent<Region> event) {
        handleRegionChange(event.getValue());
    }

    @Subscribe("regionOfCompanyRequisitesField")
    public void onRegionOfCompanyRequisitesFieldValueChange(HasValue.ValueChangeEvent<Region> event) {
        handleRegionChange(event.getValue());
    }

    private void handleRegionChange(Region region) {
        if (region == null) {
            return;
        }
        Region regionLoaded = dataManager.reload(region, "region-browse-view");
        getEditedEntity().setCountryOfCompany(regionLoaded.getRegionCountry());
    }

    // ===== Presentation-only: sidebar-навигация «Разделы» (контракт Edit-форм) =====

    @Subscribe
    public void onBeforeShowSidebar(BeforeShowEvent event) {
        // Динамический title sidebar: наименование компании, иначе общий заголовок формы.
        if (getEditedEntity().getComanyName() != null) {
            companySidebarTitle.setValue(getEditedEntity().getComanyName());
        } else {
            companySidebarTitle.setValue(messages.getMessage(getClass(), "browseCaption"));
        }
    }

    @Subscribe("companyEditorNavMain")
    public void onCompanyEditorNavMainClick(Button.ClickEvent event) {
        setNavigationActive(companyEditorNavMain);
        mainTab.setSelectedTab("tabConpanyDetails");
    }

    @Subscribe("companyEditorNavRequisites")
    public void onCompanyEditorNavRequisitesClick(Button.ClickEvent event) {
        setNavigationActive(companyEditorNavRequisites);
        mainTab.setSelectedTab("companyRequisitesTab");
    }

    @Subscribe("companyEditorNavDescription")
    public void onCompanyEditorNavDescriptionClick(Button.ClickEvent event) {
        setNavigationActive(companyEditorNavDescription);
        mainTab.setSelectedTab("companyDescriptionTab");
    }

    @Subscribe("companyEditorNavDepartments")
    public void onCompanyEditorNavDepartmentsClick(Button.ClickEvent event) {
        setNavigationActive(companyEditorNavDepartments);
        mainTab.setSelectedTab("tabCompanyDepartament");
    }

    @Subscribe("smartUploadRequisitesBtn")
    public void onSmartUploadRequisitesBtnClick(Button.ClickEvent event) {
        SmartCompanyRequisitesUploadScreen screen = screens.create(
                SmartCompanyRequisitesUploadScreen.class,
                OpenMode.DIALOG);
        screen.addAfterCloseListener(afterCloseEvent -> {
            if (afterCloseEvent.closedWith(StandardOutcome.COMMIT)) {
                CompanyRequisitesParsedData data = screen.getParsedData();
                if (data != null) {
                    // applied возвращается из middleware как десериализованный объект
                    Company applied = companyRequisitesIngestService.applyRequisitesToCompany(getEditedEntity(), data);
                    Company target = getEditedEntity();

                    // Перезагрузка коллекций опций только при изменении связанных сущностей
                    if (applied.getCompanyOwnership() != null && !java.util.Objects.equals(applied.getCompanyOwnership(), target.getCompanyOwnership())) {
                        companyOwnershipsLc.load();
                    }
                    if (applied.getCompanyDirector() != null && !java.util.Objects.equals(applied.getCompanyDirector(), target.getCompanyDirector())) {
                        companyDirectorsLc.load();
                    }
                    if (applied.getCityOfCompany() != null && !java.util.Objects.equals(applied.getCityOfCompany(), target.getCityOfCompany())) {
                        cityOfCompaniesLc.load();
                    }
                    if (applied.getRegionOfCompany() != null && !java.util.Objects.equals(applied.getRegionOfCompany(), target.getRegionOfCompany())) {
                        regionOfCompaniesLc.load();
                    }
                    if (applied.getCountryOfCompany() != null && !java.util.Objects.equals(applied.getCountryOfCompany(), target.getCountryOfCompany())) {
                        countryOfCompaniesLc.load();
                    }
                    if (applied.getInn() != null) target.setInn(applied.getInn());
                    if (applied.getKpp() != null) target.setKpp(applied.getKpp());
                    if (applied.getOgrn() != null) target.setOgrn(applied.getOgrn());
                    if (applied.getOkpo() != null) target.setOkpo(applied.getOkpo());
                    if (applied.getOktmo() != null) target.setOktmo(applied.getOktmo());
                    if (applied.getOkved() != null) target.setOkved(applied.getOkved());
                    if (applied.getLegalEntityName() != null) target.setLegalEntityName(applied.getLegalEntityName());
                    if (applied.getLegalAddress() != null) target.setLegalAddress(applied.getLegalAddress());
                    if (applied.getActualAddress() != null) target.setActualAddress(applied.getActualAddress());
                    if (applied.getPostalAddress() != null) target.setPostalAddress(applied.getPostalAddress());
                    if (applied.getAddressOfCompany() != null) target.setAddressOfCompany(applied.getAddressOfCompany());
                    if (applied.getBik() != null) target.setBik(applied.getBik());
                    if (applied.getBankName() != null) target.setBankName(applied.getBankName());
                    if (applied.getSettlementAccount() != null) target.setSettlementAccount(applied.getSettlementAccount());
                    if (applied.getCorrespondentAccount() != null) target.setCorrespondentAccount(applied.getCorrespondentAccount());
                    if (applied.getPhone() != null) target.setPhone(applied.getPhone());
                    if (applied.getEmail() != null) target.setEmail(applied.getEmail());
                    if (applied.getWebsite() != null) target.setWebsite(applied.getWebsite());

                    if (target.getComanyName() == null || target.getComanyName().trim().isEmpty()) {
                        if (applied.getComanyName() != null && !applied.getComanyName().trim().isEmpty()) {
                            target.setComanyName(applied.getComanyName());
                        }
                    }
                    if (target.getCompanyShortName() == null || target.getCompanyShortName().trim().isEmpty()) {
                        if (applied.getCompanyShortName() != null && !applied.getCompanyShortName().trim().isEmpty()) {
                            target.setCompanyShortName(applied.getCompanyShortName());
                        }
                    }

                    // Привязка сущностей через DataContext для исключения Unfetched/Detached
                    if (applied.getCompanyDirector() != null) {
                        target.setCompanyDirector(dataContext.merge(applied.getCompanyDirector()));
                    }
                    if (applied.getCompanyOwnership() != null) {
                        target.setCompanyOwnership(dataContext.merge(applied.getCompanyOwnership()));
                    }
                    if (applied.getCountryOfCompany() != null) {
                        target.setCountryOfCompany(dataContext.merge(applied.getCountryOfCompany()));
                    }
                    if (applied.getRegionOfCompany() != null) {
                        target.setRegionOfCompany(dataContext.merge(applied.getRegionOfCompany()));
                    }
                    if (applied.getCityOfCompany() != null) {
                        target.setCityOfCompany(dataContext.merge(applied.getCityOfCompany()));
                    }

                    notifications.create(Notifications.NotificationType.TRAY)
                            .withCaption("Реквизиты применены")
                            .withDescription(target.getComanyName() != null ? target.getComanyName() : (target.getLegalEntityName() != null ? target.getLegalEntityName() : ""))
                            .show();
                }
            }
        });
        screen.show();
    }

    @Subscribe("mainTab")
    public void onMainTabSelectedTabChangeNav(TabSheet.SelectedTabChangeEvent event) {
        TabSheet.Tab selectedTab = event.getSelectedTab();
        if (selectedTab == null) {
            return;
        }
        companyEditorSidebarNavigation.setVisible(
                TABS_WITH_SIDEBAR_NAVIGATION.contains(selectedTab.getName()));
        updateActiveNavigation(selectedTab);
    }

    private void updateActiveNavigation(TabSheet.Tab selectedTab) {
        if (selectedTab == null) {
            return;
        }
        String navButtonId = TAB_TO_NAV_BUTTON.get(selectedTab.getName());
        if (navButtonId == null) {
            return;
        }
        switch (navButtonId) {
            case "companyEditorNavMain":
                setNavigationActive(companyEditorNavMain);
                break;
            case "companyEditorNavRequisites":
                setNavigationActive(companyEditorNavRequisites);
                break;
            case "companyEditorNavDescription":
                setNavigationActive(companyEditorNavDescription);
                break;
            case "companyEditorNavDepartments":
                setNavigationActive(companyEditorNavDepartments);
                break;
            default:
                break;
        }
    }

    private void resetNavigationActiveStyles() {
        companyEditorNavMain.removeStyleName("label-nav-item-active");
        if (companyEditorNavRequisites != null) {
            companyEditorNavRequisites.removeStyleName("label-nav-item-active");
        }
        companyEditorNavDescription.removeStyleName("label-nav-item-active");
        companyEditorNavDepartments.removeStyleName("label-nav-item-active");
    }

    private void setNavigationActive(Button activeButton) {
        resetNavigationActiveStyles();
        activeButton.addStyleName("label-nav-item-active");
    }
}
