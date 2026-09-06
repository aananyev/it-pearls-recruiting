package com.company.hunttech.web.screens.company;

import com.company.hunttech.app.ProcessedImage;
import com.company.hunttech.app.ProjectLogoImageProcessingService;
import com.company.hunttech.entity.City;
import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.Country;
import com.company.hunttech.entity.Ownershup;
import com.company.hunttech.entity.Person;
import com.company.hunttech.entity.Region;
import com.company.hunttech.service.CompanyRequisitesIngestService;
import com.company.hunttech.service.CompanyRequisitesParsedData;
import com.company.hunttech.service.CompanySearchAiService;
import com.haulmont.cuba.core.app.FileStorageService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.ViewBuilder;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.DataContext;
import com.haulmont.cuba.gui.screen.*;
import com.hunttech.hrm.web.components.WebOvaFallbackImage;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Окно редактирования компании из Реестра компаний (CompanyReestrEdit).
 * Полноэкранный двухпанельный редактор: строго неизменяемый сайдбар 270px
 * с функционалом эталона CompanyEdit и адаптивная правая часть (Workspace)
 * с поддержкой различных разрешений экрана.
 */
@UiController("hunttech_CompanyReestr.edit")
@UiDescriptor("company-reestr-edit.xml")
@EditedEntityContainer("companyDc")
@PrimaryEditorScreen(Company.class)
@LoadDataBeforeShow
public class CompanyReestrEdit extends StandardEditor<Company> {
    private static final Logger log = LoggerFactory.getLogger(CompanyReestrEdit.class);

    @Inject
    private BackgroundWorker backgroundWorker;
    @Inject
    private WebOvaFallbackImage companyLogoFileImage;
    @Inject
    private FileUploadField companyLogoFileUpload;
    @Inject
    private ProjectLogoImageProcessingService projectLogoImageProcessingService;
    @Inject
    private FileLoader fileLoader;
    @Inject
    private FileStorageService fileStorageService;
    @Inject
    private Metadata metadata;
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
    private CompanySearchAiService companySearchAiService;
    @Inject
    private Notifications notifications;

    private boolean addressLoaded;
    private boolean companyDescriptionLoaded;
    private boolean departmentsLoaded;

    /** Соответствие «имя вкладки TabSheet -> пункт label-навигации». */
    private static final Map<String, String> TAB_TO_NAV_BUTTON =
            Collections.unmodifiableMap(new HashMap<String, String>() {{
                put("tabConpanyDetails", "companyEditorNavMain");
                put("companyRequisitesTab", "companyEditorNavRequisites");
                put("companyDescriptionTab", "companyEditorNavDescription");
                put("tabCompanyDepartament", "companyEditorNavDepartments");
            }});

    /** Вкладки с поддержкой sidebar-навигации: все вкладки формы компании. */
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
        updateSidebarTitle();
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

    @Subscribe("smartFillCompanyBtn")
    public void onSmartFillCompanyBtnClick(Button.ClickEvent event) {
        openSmartCompanyWizard();
    }

    @Subscribe("smartUploadRequisitesBtn")
    public void onSmartUploadRequisitesBtnClick(Button.ClickEvent event) {
        openSmartCompanyWizard();
    }

    private void openSmartCompanyWizard() {
        SmartCompanyRequisitesUploadScreen screen = screens.create(
                SmartCompanyRequisitesUploadScreen.class,
                OpenMode.DIALOG);
        screen.setInitialSearchParams(getEditedEntity().getComanyName(), getEditedEntity().getInn());
        screen.addAfterCloseListener(afterCloseEvent -> {
            if (afterCloseEvent.closedWith(StandardOutcome.COMMIT)) {
                CompanyRequisitesParsedData data = screen.getParsedData();
                if (data != null) {
                    Company target = getEditedEntity();
                    Ownershup oldOwnership = target.getCompanyOwnership();
                    Person oldDirector = target.getCompanyDirector();
                    City oldCity = target.getCityOfCompany();
                    Region oldRegion = target.getRegionOfCompany();
                    Country oldCountry = target.getCountryOfCompany();

                    Company applied = companySearchAiService.applyCompanyData(target, data);

                    if (applied.getCompanyOwnership() != null && !java.util.Objects.equals(applied.getCompanyOwnership(), oldOwnership)) {
                        companyOwnershipsLc.load();
                    }
                    if (applied.getCompanyDirector() != null && !java.util.Objects.equals(applied.getCompanyDirector(), oldDirector)) {
                        companyDirectorsLc.load();
                    }
                    if (applied.getCityOfCompany() != null && !java.util.Objects.equals(applied.getCityOfCompany(), oldCity)) {
                        cityOfCompaniesLc.load();
                    }
                    if (applied.getRegionOfCompany() != null && !java.util.Objects.equals(applied.getRegionOfCompany(), oldRegion)) {
                        regionOfCompaniesLc.load();
                    }
                    if (applied.getCountryOfCompany() != null && !java.util.Objects.equals(applied.getCountryOfCompany(), oldCountry)) {
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

                    if (applied.getCompanyDescription() != null && !applied.getCompanyDescription().trim().isEmpty()) {
                        target.setCompanyDescription(applied.getCompanyDescription().trim());
                        companyDescriptionLoaded = true;
                    }
                    if (applied.getWorkingConditions() != null && !applied.getWorkingConditions().trim().isEmpty()) {
                        target.setWorkingConditions(applied.getWorkingConditions().trim());
                        companyDescriptionLoaded = true;
                    }

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

                    if (data.getLogoUrl() != null && !data.getLogoUrl().trim().isEmpty() && target.getFileCompanyLogo() == null) {
                        downloadAndApplyLogo(target, data.getLogoUrl());
                    }

                    updateSidebarTitle();

                    notifications.create(Notifications.NotificationType.TRAY)
                            .withCaption("Данные компании обновлены")
                            .withDescription(target.getComanyName() != null ? target.getComanyName() : (target.getLegalEntityName() != null ? target.getLegalEntityName() : ""))
                            .show();
                }
            }
        });
        screen.show();
    }

    private void downloadAndApplyLogo(Company company, String logoUrl) {
        if (logoUrl == null || logoUrl.trim().isEmpty() || company.getFileCompanyLogo() != null) {
            return;
        }
        String cleanUrl = logoUrl.trim();
        java.net.URI uri;
        try {
            uri = new java.net.URI(cleanUrl);
        } catch (Exception ex) {
            return;
        }

        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            return;
        }
        int port = uri.getPort();
        if (port != -1 && port != 80 && port != 443) {
            return;
        }
        String host = uri.getHost();
        if (host == null || host.trim().isEmpty()) {
            return;
        }

        BackgroundTask<Integer, FileDescriptor> task = new BackgroundTask<Integer, FileDescriptor>(15, this) {
            @Override
            public FileDescriptor run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
                java.net.InetAddress addr = java.net.InetAddress.getByName(host.trim());
                if (!isSafePublicIp(addr)) {
                    log.warn("Отклонен небезопасный целевой IP адрес для логотипа: {}", addr.getHostAddress());
                    return null;
                }

                java.net.HttpURLConnection conn = null;
                try {
                    int targetPort = port != -1 ? port : ("https".equalsIgnoreCase(scheme) ? 443 : 80);
                    String pathAndQuery = (uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath())
                            + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");
                    java.net.URL pinnedUrl = new java.net.URL(scheme, addr.getHostAddress(), targetPort, pathAndQuery);

                    conn = (java.net.HttpURLConnection) pinnedUrl.openConnection();
                    conn.setRequestProperty("Host", host);
                    conn.setRequestProperty("User-Agent", "HuntTech-HRM/1.0");
                    conn.setConnectTimeout(4000);
                    conn.setReadTimeout(4000);
                    conn.setInstanceFollowRedirects(false);

                    if (conn instanceof javax.net.ssl.HttpsURLConnection) {
                        javax.net.ssl.HttpsURLConnection httpsConn = (javax.net.ssl.HttpsURLConnection) conn;
                        httpsConn.setHostnameVerifier((h, session) ->
                                javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier().verify(host, session));
                    }

                    int code = conn.getResponseCode();
                    if (code >= 200 && code < 300) {
                        byte[] bytes;
                        try (InputStream is = conn.getInputStream()) {
                            bytes = readBoundedStream(is, 5 * 1024 * 1024);
                        }
                        if (bytes != null && bytes.length >= 128) {
                            String ext = detectImageExtension(bytes);
                            if (ext != null) {
                                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
                                if (img != null && img.getWidth() > 0 && img.getHeight() > 0) {
                                    FileDescriptor fd = metadata.create(FileDescriptor.class);
                                    fd.setName("company-logo-" + company.getId() + "." + ext);
                                    fd.setExtension(ext);
                                    fd.setSize((long) bytes.length);
                                    fd.setCreateDate(new java.util.Date());
                                    fileStorageService.saveFile(fd, bytes);
                                    return dataManager.commit(fd);
                                }
                            }
                        }
                    }
                } finally {
                    if (conn != null) {
                        try {
                            conn.disconnect();
                        } catch (Exception ignored) {
                        }
                    }
                }
                return null;
            }

            @Override
            public void done(FileDescriptor committed) {
                if (committed != null && getEditedEntity() == company && company.getFileCompanyLogo() == null) {
                    company.setFileCompanyLogo(dataContext.merge(committed));
                    notifications.create(Notifications.NotificationType.TRAY)
                            .withCaption("Логотип компании успешно загружен")
                            .show();
                }
            }

            @Override
            public boolean handleException(Exception ex) {
                log.warn("Ошибка при фоновой загрузке логотипа компании по URL {}: {}", cleanUrl, ex.getMessage(), ex);
                return true;
            }
        };

        backgroundWorker.handle(task).execute();
    }

    private boolean isSafePublicIp(java.net.InetAddress addr) {
        if (addr == null) return false;
        if (addr.isLoopbackAddress() || addr.isAnyLocalAddress() || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress() || addr.isMulticastAddress()) {
            return false;
        }
        byte[] raw = addr.getAddress();
        if (raw.length == 4) {
            int b0 = raw[0] & 0xFF;
            int b1 = raw[1] & 0xFF;
            if (b0 == 10 || b0 == 127 || b0 == 0) return false;
            if (b0 == 172 && (b1 >= 16 && b1 <= 31)) return false;
            if (b0 == 192 && b1 == 168) return false;
            if (b0 == 169 && b1 == 254) return false;
            if (b0 == 100 && (b1 >= 64 && b1 <= 127)) return false;
        }
        return true;
    }

    private byte[] readBoundedStream(InputStream is, int maxBytes) throws java.io.IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        int total = 0;
        while ((read = is.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new java.io.IOException("Размер файла превышает лимит " + maxBytes + " байт");
            }
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }

    private String detectImageExtension(byte[] bytes) {
        if (bytes == null || bytes.length < 8) return null;
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return "png";
        }
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes.length > 12 && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "webp";
        }
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return "gif";
        }
        return null;
    }

    private void updateSidebarTitle() {
        if (companySidebarTitle != null) {
            String name = getEditedEntity().getComanyName();
            if (name != null && !name.trim().isEmpty()) {
                companySidebarTitle.setValue(name);
            } else {
                companySidebarTitle.setValue(messages.getMessage(getClass(), "browseCaption"));
            }
        }
    }

    private final Set<FileDescriptor> pendingRemovalLogoDescriptors = new HashSet<>();

    @Subscribe
    public void onAfterCommitChanges(AfterCommitChangesEvent event) {
        if (!pendingRemovalLogoDescriptors.isEmpty()) {
            for (FileDescriptor descriptor : pendingRemovalLogoDescriptors) {
                try {
                    fileStorageService.removeFile(descriptor);
                    dataManager.remove(descriptor);
                } catch (Exception ex) {
                    // non-fatal
                }
            }
            pendingRemovalLogoDescriptors.clear();
        }
    }

    @Subscribe("enhanceCompanyLogoBtn")
    public void onEnhanceCompanyLogoBtnClick(Button.ClickEvent event) {
        Company company = getEditedEntity();
        FileDescriptor logoDescriptor = company.getFileCompanyLogo();
        if (logoDescriptor == null) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Логотип отсутствует")
                    .withDescription("Сначала выберите или загрузите изображение логотипа компании")
                    .show();
            return;
        }

        try {
            byte[] originalBytes;
            try (InputStream is = fileLoader.openStream(logoDescriptor)) {
                originalBytes = IOUtils.toByteArray(is);
            }

            ProcessedImage processed = projectLogoImageProcessingService.process(
                    originalBytes, logoDescriptor.getName(), false);

            if (processed != null && processed.isProcessed() && processed.getData() != null) {
                FileDescriptor newDescriptor = metadata.create(FileDescriptor.class);
                String uniqueName = processed.getName() + "-" + newDescriptor.getId() + "." + processed.getExtension();
                newDescriptor.setName(uniqueName);
                newDescriptor.setExtension(processed.getExtension());
                newDescriptor.setSize((long) processed.getData().length);
                newDescriptor.setCreateDate(new java.util.Date());

                fileStorageService.saveFile(newDescriptor, processed.getData());
                FileDescriptor committedDescriptor = dataManager.commit(newDescriptor);

                company.setFileCompanyLogo(dataContext.merge(committedDescriptor));
                pendingRemovalLogoDescriptors.add(logoDescriptor);

                notifications.create(Notifications.NotificationType.TRAY)
                        .withCaption("Логотип успешно обработан")
                        .withDescription("Улучшено качество, удален фон и выполнено вписывание в круг")
                        .show();
            } else {
                notifications.create(Notifications.NotificationType.HUMANIZED)
                        .withCaption("Обработка не выполнена")
                        .withDescription("Файл не является поддерживаемым растровым изображением или уже оптимизирован")
                        .show();
            }
        } catch (Exception e) {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Ошибка обработки изображения")
                    .withDescription(e.getMessage())
                    .show();
        }
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
