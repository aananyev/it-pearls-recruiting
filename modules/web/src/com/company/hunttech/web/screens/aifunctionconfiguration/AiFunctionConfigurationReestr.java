package com.company.hunttech.web.screens.aifunctionconfiguration;

import com.company.hunttech.entity.ai.AiFunctionConfiguration;
import com.company.hunttech.web.screens.aicalllog.AiCallLogBrowse;
import com.google.gson.Gson;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.Target;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.hunttech.hrm.web.components.WebOvaFallbackImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Контроллер экрана «Реестр функций AI» (Split-View Master-Detail).
 * Реализован в стандарте HuntTech Reestr: сайдбар 312px с быстрым обзором параметров,
 * безопасный safe-view без prompt LOB, поддержка быстрых действий и Zero N+1.
 */
@UiController("hunttech_AiFunctionConfiguration.reestr")
@UiDescriptor("ai-function-configuration-reestr.xml")
@LookupComponent("aiFunctionsTable")
@LoadDataBeforeShow
public class AiFunctionConfigurationReestr extends StandardLookup<AiFunctionConfiguration> {

    private static final Logger log = LoggerFactory.getLogger(AiFunctionConfigurationReestr.class);
    private static final Gson gson = new Gson();

    @Inject
    private DataGrid<AiFunctionConfiguration> aiFunctionsTable;
    @Inject
    private CollectionContainer<AiFunctionConfiguration> aiFunctionsDc;
    @Inject
    private CollectionLoader<AiFunctionConfiguration> aiFunctionsDl;
    @Inject
    private DataManager dataManager;
    @Inject
    private Messages messages;
    @Inject
    private Notifications notifications;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private UiComponents uiComponents;

    // Компоненты левого сайдбара (312px)
    @Inject
    private WebOvaFallbackImage detailPic;
    @Inject
    private Label<String> detailName;
    @Inject
    private Label<String> detailCode;
    @Inject
    private Label<String> detailCapability;
    @Inject
    private Label<String> detailStatus;

    @Inject
    private Button editBtn;
    @Inject
    private Button copyCodeBtn;
    @Inject
    private Button callLogsBtn;
    @Inject
    private Button toggleActiveBtn;

    // Секция 1: Маршрутизация и исполнение
    @Inject
    private Label<String> detailExecutionPolicy;
    @Inject
    private Label<String> detailFallbackPolicy;
    @Inject
    private Label<String> detailAdminConfig;
    @Inject
    private Label<String> detailAdminModel;
    @Inject
    private Label<String> detailIncludeUserContext;
    @Inject
    private Label<String> detailAllowOverride;

    // Секция 2: Параметры LLM и лимиты
    @Inject
    private Label<String> detailTemperature;
    @Inject
    private Label<String> detailMaxTokens;
    @Inject
    private Label<String> detailMonthlyQuota;
    @Inject
    private Label<String> detailConfigVersion;

    // Секция 3: Описание
    @Inject
    private Label<String> detailDescription;

    @Subscribe
    public void onInit(Screen.InitEvent event) {
        initStatusColumn();
    }

    private void initStatusColumn() {
        aiFunctionsTable.addGeneratedColumn("activeStatus", new DataGrid.ColumnGenerator<AiFunctionConfiguration, Component>() {
            @Override
            public Component getValue(DataGrid.ColumnGeneratorEvent<AiFunctionConfiguration> event) {
                Label<String> label = uiComponents.create(Label.NAME);
                label.setHtmlEnabled(true);
                label.setValue(renderStatusHtml(event.getItem().getActive()));
                return label;
            }

            @Override
            public Class<Component> getType() {
                return Component.class;
            }
        });
    }

    @Subscribe(id = "aiFunctionsDc", target = Target.DATA_CONTAINER)
    public void onAiFunctionsDcItemChange(InstanceContainer.ItemChangeEvent<AiFunctionConfiguration> event) {
        updateSidebar(event.getItem());
    }

    private void updateSidebar(AiFunctionConfiguration item) {
        if (item == null) {
            detailPic.setDescription(null);
            detailName.setValue(messages.getMessage(getClass(), "selectFunctionPrompt"));
            detailCode.setValue("");
            detailCapability.setValue("");
            detailStatus.setValue("");

            detailExecutionPolicy.setValue("-");
            detailFallbackPolicy.setValue("-");
            detailAdminConfig.setValue("-");
            detailAdminModel.setValue("-");
            detailIncludeUserContext.setValue("-");
            detailAllowOverride.setValue("-");

            detailTemperature.setValue("-");
            detailMaxTokens.setValue("-");
            detailMonthlyQuota.setValue("-");
            detailConfigVersion.setValue("-");

            detailDescription.setValue(messages.getMessage(getClass(), "noDescription"));

            editBtn.setEnabled(false);
            copyCodeBtn.setEnabled(false);
            callLogsBtn.setEnabled(false);
            toggleActiveBtn.setEnabled(false);
            return;
        }

        String displayName = item.getName() != null ? item.getName() : "—";
        detailPic.setDescription(item.getName() != null ? item.getName() : item.getCode());
        detailName.setValue(escapeHtml(displayName));
        detailCode.setValue(item.getCode() != null ? "<code>" + escapeHtml(item.getCode()) + "</code>" : "");
        detailCapability.setValue(item.getCapability() != null
                ? messages.getMessage(getClass(), "labelCapability") + " <b>" + escapeHtml(item.getCapability().getId()) + "</b>" : "—");

        boolean isActive = Boolean.TRUE.equals(item.getActive());
        detailStatus.setValue(renderStatusHtml(item.getActive()));
        if (isActive) {
            toggleActiveBtn.setCaption(messages.getMessage(getClass(), "btnDeactivate"));
            toggleActiveBtn.setIcon("font-icon:PAUSE");
        } else {
            toggleActiveBtn.setCaption(messages.getMessage(getClass(), "btnActivate"));
            toggleActiveBtn.setIcon("font-icon:PLAY");
        }

        // Реквизиты маршрутизации
        detailExecutionPolicy.setValue(item.getExecutionPolicy() != null ? item.getExecutionPolicy().getId() : "—");
        detailFallbackPolicy.setValue(item.getFallbackPolicy() != null ? item.getFallbackPolicy().getId() : "—");
        detailAdminConfig.setValue(item.getAdminConfiguration() != null && item.getAdminConfiguration().getName() != null
                ? item.getAdminConfiguration().getName() : messages.getMessage(getClass(), "notLinked"));
        detailAdminModel.setValue(item.getAdminModelName() != null
                ? item.getAdminModelName() : messages.getMessage(getClass(), "byDefault"));

        if (item.getIncludeUserContext() != null) {
            detailIncludeUserContext.setValue(Boolean.TRUE.equals(item.getIncludeUserContext())
                    ? messages.getMessage(getClass(), "contextEnabled")
                    : messages.getMessage(getClass(), "contextDisabled"));
        } else {
            detailIncludeUserContext.setValue(messages.getMessage(getClass(), "contextByCapability"));
        }

        detailAllowOverride.setValue(Boolean.TRUE.equals(item.getAllowModelOverride())
                ? messages.getMessage(getClass(), "overrideAllowed")
                : messages.getMessage(getClass(), "overrideForbidden"));

        // Реквизиты LLM
        detailTemperature.setValue(item.getTemperature() != null ? String.valueOf(item.getTemperature()) : "—");
        detailMaxTokens.setValue(item.getMaxTokens() != null ? String.valueOf(item.getMaxTokens()) : "—");
        if (item.getDefaultMonthlyTokenQuota() != null) {
            detailMonthlyQuota.setValue(String.format("%,d %s", item.getDefaultMonthlyTokenQuota(), messages.getMessage(getClass(), "tokenUnits")));
        } else {
            detailMonthlyQuota.setValue(messages.getMessage(getClass(), "noQuotaLimit"));
        }
        detailConfigVersion.setValue(item.getConfigurationVersion() != null ? "v" + item.getConfigurationVersion() : "v1");

        // Описание
        if (item.getDescription() != null && !item.getDescription().trim().isEmpty()) {
            detailDescription.setValue(escapeHtml(item.getDescription().trim()));
        } else {
            detailDescription.setValue("<em>" + escapeHtml(messages.getMessage(getClass(), "noDescription")) + "</em>");
        }

        editBtn.setEnabled(true);
        copyCodeBtn.setEnabled(true);
        callLogsBtn.setEnabled(true);
        toggleActiveBtn.setEnabled(true);
    }

    @Subscribe("editBtn")
    public void onEditBtnClick(Button.ClickEvent event) {
        AiFunctionConfiguration selected = aiFunctionsTable.getSingleSelected();
        if (selected != null) {
            screenBuilders.editor(aiFunctionsTable)
                    .editEntity(selected)
                    .show();
        }
    }

    @Subscribe("copyCodeBtn")
    public void onCopyCodeBtnClick(Button.ClickEvent event) {
        AiFunctionConfiguration selected = aiFunctionsTable.getSingleSelected();
        if (selected != null && selected.getCode() != null) {
            String jsLiteral = gson.toJson(selected.getCode().trim());
            String copyScript = String.format(
                    "var txt = %s;\n" +
                    "if (navigator.clipboard && window.isSecureContext) {\n" +
                    "    navigator.clipboard.writeText(txt);\n" +
                    "} else {\n" +
                    "    var ta = document.createElement('textarea');\n" +
                    "    ta.value = txt;\n" +
                    "    ta.style.position = 'fixed';\n" +
                    "    ta.style.opacity = '0';\n" +
                    "    document.body.appendChild(ta);\n" +
                    "    ta.focus();\n" +
                    "    ta.select();\n" +
                    "    try { document.execCommand('copy'); } catch (err) {}\n" +
                    "    document.body.removeChild(ta);\n" +
                    "}", jsLiteral);
            com.vaadin.ui.JavaScript.getCurrent().execute(copyScript);
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption(messages.getMessage(getClass(), "codeCopied"))
                    .withDescription(selected.getCode())
                    .show();
        }
    }

    @Subscribe("callLogsBtn")
    public void onCallLogsBtnClick(Button.ClickEvent event) {
        AiFunctionConfiguration selected = aiFunctionsTable.getSingleSelected();
        if (selected == null) {
            return;
        }
        AiCallLogBrowse screen = screenBuilders.screen(this)
                .withScreenClass(AiCallLogBrowse.class)
                .withOpenMode(OpenMode.NEW_TAB)
                .build();
        if (selected.getCode() != null) {
            screen.setFunctionCodeFilter(selected.getCode().trim());
        }
        screen.show();
    }

    @Subscribe("toggleActiveBtn")
    public void onToggleActiveBtnClick(Button.ClickEvent event) {
        AiFunctionConfiguration selected = aiFunctionsTable.getSingleSelected();
        if (selected != null) {
            try {
                AiFunctionConfiguration entityToCommit = dataManager.load(AiFunctionConfiguration.class)
                        .id(selected.getId())
                        .view("ai-function-configuration-browse-view")
                        .one();
                boolean targetState = !Boolean.TRUE.equals(entityToCommit.getActive());
                entityToCommit.setActive(targetState);
                AiFunctionConfiguration committed = dataManager.commit(entityToCommit);
                aiFunctionsDc.replaceItem(committed);
                aiFunctionsTable.setSelected(committed);
                updateSidebar(committed);
                notifications.create(Notifications.NotificationType.TRAY)
                        .withCaption(targetState
                                ? messages.getMessage(getClass(), "msgFunctionActivated")
                                : messages.getMessage(getClass(), "msgFunctionDeactivated"))
                        .withDescription(committed.getName() != null ? committed.getName() : committed.getCode())
                        .show();
            } catch (Exception e) {
                log.error("Failed to toggle active state for AI function: {}", selected.getCode(), e);
                aiFunctionsDl.load();
                notifications.create(Notifications.NotificationType.ERROR)
                        .withCaption(messages.getMessage(getClass(), "msgSaveError"))
                        .withDescription(e.getMessage())
                        .show();
            }
        }
    }

    private String renderStatusHtml(Boolean active) {
        if (Boolean.TRUE.equals(active)) {
            return "<span style='color: #27ae60; font-weight: bold;'>🟢 "
                    + escapeHtml(messages.getMessage(getClass(), "statusActive")) + "</span>";
        } else {
            return "<span style='color: #e74c3c; font-weight: bold;'>⚪ "
                    + escapeHtml(messages.getMessage(getClass(), "statusDisabled")) + "</span>";
        }
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
