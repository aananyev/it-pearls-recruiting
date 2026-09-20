package com.company.hunttech.web.screens.useraifunctionoverride;

import com.company.hunttech.entity.ai.UserAiFunctionOverride;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("hunttech_UserAiFunctionOverride.browse")
@UiDescriptor("user-ai-function-override-browse.xml")
@LookupComponent("overridesTable")
public class UserAiFunctionOverrideBrowse extends StandardLookup<UserAiFunctionOverride> {
    @Inject
    private DataGrid<UserAiFunctionOverride> overridesTable;
    @Inject
    private Button openEditCardBtn;
    @Inject
    private Label<String> detailTitle;
    @Inject
    private Label<String> detailSubtitle;
    @Inject
    private Label<String> detailStatusBadge;
    @Inject
    private Label<String> detailEnabled;
    @Inject
    private Label<String> detailFunctionCode;
    @Inject
    private Label<String> detailProvider;
    @Inject
    private Label<String> detailModel;
    @Inject
    private Label<String> detailOverrideModel;
    @Inject
    private CollectionLoader<UserAiFunctionOverride> overridesDl;
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private Messages messages;
    @Inject
    private ScreenBuilders screenBuilders;

    @Subscribe
    public void onInit(InitEvent event) {
        openEditCardBtn.setEnabled(false);
        overridesTable.addSelectionListener(selectionEvent -> {
            UserAiFunctionOverride selected = overridesTable.getSingleSelected();
            openEditCardBtn.setEnabled(selected != null);
            updateSidebar(selected);
        });
        openEditCardBtn.addClickListener(clickEvent -> openSelectedEditor());
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        overridesDl.setParameter("user", userSessionSource.getUserSession().getUser());
        overridesDl.load();
    }

    private void updateSidebar(UserAiFunctionOverride selected) {
        if (selected != null) {
            String fnName = selected.getAiFunction() != null && selected.getAiFunction().getName() != null
                    ? selected.getAiFunction().getName() : "";
            String fnCode = selected.getAiFunction() != null && selected.getAiFunction().getCode() != null
                    ? selected.getAiFunction().getCode() : "";
            detailTitle.setValue(!fnName.isEmpty() ? fnName : messages.getMessage(getClass(), "sidebarSubtitle"));
            detailSubtitle.setValue(!fnCode.isEmpty() ? fnCode : "-");

            boolean isEnabled = Boolean.TRUE.equals(selected.getEnabled());
            detailStatusBadge.setValue(isEnabled ? ("🟢 " + messages.getMessage(getClass(), "statusEnabled")) : ("⚪ " + messages.getMessage(getClass(), "statusDisabled")));
            detailEnabled.setValue(isEnabled ? messages.getMessage(getClass(), "statusEnabled") : messages.getMessage(getClass(), "statusDisabled"));
            detailFunctionCode.setValue(!fnCode.isEmpty() ? fnCode : "-");

            String provider = selected.getUserAiConfiguration() != null && selected.getUserAiConfiguration().getProviderCode() != null
                    ? selected.getUserAiConfiguration().getProviderCode() : "-";
            String defaultModel = selected.getUserAiConfiguration() != null && selected.getUserAiConfiguration().getDefaultModelName() != null
                    ? selected.getUserAiConfiguration().getDefaultModelName() : "-";
            String overrideModel = selected.getModelName() != null ? selected.getModelName() : "По умолчанию (" + defaultModel + ")";

            detailProvider.setValue(provider);
            detailModel.setValue(defaultModel);
            detailOverrideModel.setValue(overrideModel);
        } else {
            detailTitle.setValue(messages.getMessage(getClass(), "sidebarDefaultTitle"));
            detailSubtitle.setValue(messages.getMessage(getClass(), "sidebarSubtitle"));
            detailStatusBadge.setValue("");
            detailEnabled.setValue("-");
            detailFunctionCode.setValue("-");
            detailProvider.setValue("-");
            detailModel.setValue("-");
            detailOverrideModel.setValue("-");
        }
    }

    private void openSelectedEditor() {
        UserAiFunctionOverride selected = overridesTable.getSingleSelected();
        if (selected != null) {
            screenBuilders.editor(overridesTable)
                    .editEntity(selected)
                    .withScreenClass(UserAiFunctionOverrideEdit.class)
                    .show();
        }
    }
}

