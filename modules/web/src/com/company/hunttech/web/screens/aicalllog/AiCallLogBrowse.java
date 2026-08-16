package com.company.hunttech.web.screens.aicalllog;

import com.company.hunttech.entity.ai.AiCallLog;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.GroupTable;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.components.TextArea;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import java.math.BigDecimal;

@UiController("hunttech_AiCallLog.browse")
@UiDescriptor("ai-call-log-browse.xml")
@LookupComponent("aiCallLogsTable")
@LoadDataBeforeShow
public class AiCallLogBrowse extends StandardLookup<AiCallLog> {

    @Inject
    private GroupTable<AiCallLog> aiCallLogsTable;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private TextArea<String> promptTextArea;
    @Inject
    private TextArea<String> responseTextArea;
    @Inject
    private TextArea<String> errorTextArea;

    @Subscribe
    public void onInit(Screen.InitEvent event) {
        aiCallLogsTable.addGeneratedColumn("userDisplay", log -> {
            Label<String> label = uiComponents.create(Label.NAME);
            String name = log.getUserName() != null ? log.getUserName() : log.getUserLogin();
            label.setValue(name != null ? name : "—");
            return label;
        });

        aiCallLogsTable.addGeneratedColumn("tokensDisplay", log -> {
            Label<String> label = uiComponents.create(Label.NAME);
            label.setHtmlEnabled(true);
            int prompt = log.getPromptTokens() != null ? log.getPromptTokens() : 0;
            int completion = log.getCompletionTokens() != null ? log.getCompletionTokens() : 0;
            int total = log.getTotalTokens() != null ? log.getTotalTokens() : (prompt + completion);
            if (total > 0) {
                label.setValue("<span style='font-size: 11px;'><b>" + total + "</b> (" + prompt + " / " + completion + ")</span>");
            } else {
                label.setValue("<span style='color: #7f8c8d; font-size: 11px;'>—</span>");
            }
            return label;
        });

        aiCallLogsTable.addGeneratedColumn("costDisplay", log -> {
            Label<String> label = uiComponents.create(Label.NAME);
            label.setHtmlEnabled(true);
            BigDecimal cost = log.getEstimatedCost();
            String currency = log.getCurrency() != null ? log.getCurrency() : "USD";
            if (cost != null && cost.compareTo(BigDecimal.ZERO) > 0) {
                label.setValue("<span style='color: #27ae60; font-weight: 600; font-size: 11px;'>"
                        + cost.toPlainString() + " " + currency + "</span>");
            } else {
                label.setValue("<span style='color: #7f8c8d; font-size: 11px;'>—</span>");
            }
            return label;
        });

        aiCallLogsTable.addGeneratedColumn("durationDisplay", log -> {
            Label<String> label = uiComponents.create(Label.NAME);
            Long ms = log.getDurationMs();
            if (ms != null) {
                label.setValue(String.format("%.2f с", ms / 1000.0));
            } else {
                label.setValue("—");
            }
            return label;
        });

        aiCallLogsTable.addGeneratedColumn("statusDisplay", log -> {
            Label<String> label = uiComponents.create(Label.NAME);
            label.setHtmlEnabled(true);
            String status = log.getStatus();
            if ("SUCCESS".equalsIgnoreCase(status)) {
                label.setValue("<span style='background: rgba(39, 174, 96, 0.15); color: #27ae60; padding: 2px 8px; border-radius: 4px; font-weight: 600; font-size: 11px;'>OK</span>");
            } else if ("ERROR".equalsIgnoreCase(status)) {
                label.setValue("<span style='background: rgba(231, 76, 60, 0.15); color: #e74c3c; padding: 2px 8px; border-radius: 4px; font-weight: 600; font-size: 11px;'>ERROR</span>");
            } else {
                label.setValue("<span style='font-size: 11px;'>" + (status != null ? status : "—") + "</span>");
            }
            return label;
        });
    }

    @Subscribe("aiCallLogsTable")
    public void onAiCallLogsTableSelection(Table.SelectionEvent<AiCallLog> event) {
        AiCallLog selected = aiCallLogsTable.getSingleSelected();
        if (selected != null) {
            promptTextArea.setValue(selected.getPromptText() != null ? selected.getPromptText() : "");
            responseTextArea.setValue(selected.getResponseText() != null ? selected.getResponseText() : "");
            errorTextArea.setValue(selected.getErrorMessage() != null ? selected.getErrorMessage() : "");
        } else {
            promptTextArea.setValue("");
            responseTextArea.setValue("");
            errorTextArea.setValue("");
        }
    }
}
