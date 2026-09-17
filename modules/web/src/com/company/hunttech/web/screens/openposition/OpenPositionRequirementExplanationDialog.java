package com.company.hunttech.web.screens.openposition;

import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.service.OpenPositionExplanationService;
import com.company.hunttech.service.dto.OpenPositionExplanationResult;
import com.google.gson.Gson;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.ProgressBar;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.vaadin.ui.JavaScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.UUID;

/**
 * Экран диалога: интеллектуальное объяснение требований вакансии для рекрутера.
 * Поддерживает стандартный анализ AI и житейские аналогии через Hermes hrm-viewer с доступом в интернет.
 */
@UiController("hunttech_OpenPositionRequirementExplanationDialog")
@UiDescriptor("open-position-requirement-explanation-dialog.xml")
public class OpenPositionRequirementExplanationDialog extends Screen {
    private static final Logger log = LoggerFactory.getLogger(OpenPositionRequirementExplanationDialog.class);

    @Inject
    private Label<String> vacancySubTitle;
    @Inject
    private Label<String> explanationContentLabel;
    @Inject
    private Label<String> metaInfoLabel;
    @Inject
    private Label<String> statusLabel;
    @Inject
    private Label<String> errorDetailsLabel;
    @Inject
    private VBoxLayout progressBox;
    @Inject
    private VBoxLayout errorBox;
    @Inject
    private ProgressBar progressBar;
    @Inject
    private Button explainSimplifiedBtn;
    @Inject
    private Button refreshStandardBtn;
    @Inject
    private Button copyTextBtn;
    @Inject
    private Button closeBtn;

    @Inject
    private OpenPositionExplanationService openPositionExplanationService;
    @Inject
    private BackgroundWorker backgroundWorker;
    @Inject
    private Notifications notifications;

    private OpenPosition openPosition;
    private UUID openPositionId;
    private String currentRawText;
    private final Gson gson = new Gson();

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
        if (openPosition != null) {
            this.openPositionId = openPosition.getId();
        }
    }

    public void setOpenPositionId(UUID openPositionId) {
        this.openPositionId = openPositionId;
    }

    @Subscribe
    public void onInit(InitEvent event) {
        explainSimplifiedBtn.addClickListener(e -> startSimplifiedExplanation());
        refreshStandardBtn.addClickListener(e -> startStandardExplanation());
        copyTextBtn.addClickListener(e -> copyExplanationToClipboard());
        closeBtn.addClickListener(e -> closeWithDefaultAction());
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        updateHeaderInfo();
        startStandardExplanation();
    }

    private void updateHeaderInfo() {
        if (openPosition != null) {
            StringBuilder sb = new StringBuilder();
            if (openPosition.getVacansyName() != null) {
                sb.append("<b>").append(escapeHtml(openPosition.getVacansyName())).append("</b>");
            }
            if (openPosition.getGrade() != null && openPosition.getGrade().getGradeName() != null) {
                sb.append(" • ").append(escapeHtml(openPosition.getGrade().getGradeName()));
            }
            if (openPosition.getProjectName() != null && openPosition.getProjectName().getProjectName() != null) {
                sb.append(" • Проект: ").append(escapeHtml(openPosition.getProjectName().getProjectName()));
            }
            vacancySubTitle.setValue(sb.toString());
        } else if (openPositionId != null) {
            vacancySubTitle.setValue("Вакансия ID: " + openPositionId);
        }
    }

    private void startStandardExplanation() {
        if (openPositionId == null) {
            showError("Не указан идентификатор вакансии.");
            return;
        }

        setBusyState(true, "Анализируем требования вакансии и карту поиска через AI...");
        final UUID targetId = this.openPositionId;

        BackgroundTask<Integer, OpenPositionExplanationResult> task =
                new BackgroundTask<Integer, OpenPositionExplanationResult>(120, this) {
                    @Override
                    public OpenPositionExplanationResult run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
                        return openPositionExplanationService.explainRequirements(targetId);
                    }

                    @Override
                    public void done(OpenPositionExplanationResult result) {
                        handleExplanationResult(result, false);
                        setBusyState(false, null);
                    }

                    @Override
                    public boolean handleException(Exception ex) {
                        setBusyState(false, null);
                        log.error("Сбой при вызове explainRequirements", ex);
                        showError("Не удалось получить объяснение требований: " + ex.getMessage());
                        return true;
                    }
                };

        backgroundWorker.handle(task).execute();
    }

    private void startSimplifiedExplanation() {
        if (openPositionId == null) {
            showError("Не указан идентификатор вакансии.");
            return;
        }

        setBusyState(true, "Генерируем житейские аналогии и ищем контекст в интернете через Hermes hrm-viewer...");
        final UUID targetId = this.openPositionId;
        final String prevText = this.currentRawText;

        BackgroundTask<Integer, OpenPositionExplanationResult> task =
                new BackgroundTask<Integer, OpenPositionExplanationResult>(150, this) {
                    @Override
                    public OpenPositionExplanationResult run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
                        return openPositionExplanationService.explainSimplifiedWithWebSearch(targetId, prevText);
                    }

                    @Override
                    public void done(OpenPositionExplanationResult result) {
                        handleExplanationResult(result, true);
                        setBusyState(false, null);
                    }

                    @Override
                    public boolean handleException(Exception ex) {
                        setBusyState(false, null);
                        log.error("Сбой при вызове explainSimplifiedWithWebSearch", ex);
                        showError("Не удалось получить житейское объяснение от Hermes: " + ex.getMessage());
                        return true;
                    }
                };

        backgroundWorker.handle(task).execute();
    }

    private void handleExplanationResult(OpenPositionExplanationResult result, boolean isSimplified) {
        if (result == null || !result.isSuccess() || result.getExplanationText() == null
                || result.getExplanationText().trim().isEmpty()) {
            String err = result != null && result.getErrorMessage() != null
                    ? result.getErrorMessage() : "Нейросеть не вернула текст ответа";
            showError(err);
            return;
        }

        errorBox.setVisible(false);
        this.currentRawText = result.getExplanationText();
        String formattedHtml = markdownToHtml(result.getExplanationText());
        explanationContentLabel.setValue(formattedHtml);

        String serviceTitle = isSimplified
                ? "Житейские аналогии (Hermes hrm-viewer + Web)"
                : "Стандартный анализ (AiExecutionService)";

        double sec = result.getDurationMs() != null ? (result.getDurationMs() / 1000.0) : 0.0;
        int tokens = result.getTotalTokens() != null ? result.getTotalTokens() : 0;
        String model = result.getModelName() != null ? result.getModelName() : "default";
        String provider = result.getProviderCode() != null ? result.getProviderCode() : "ai";

        String meta = String.format(
                "💡 <b>Режим:</b> %s &nbsp;|&nbsp; <b>Модель:</b> %s (%s) &nbsp;|&nbsp; <b>Токены:</b> %d &nbsp;|&nbsp; <b>Время:</b> %.1f с",
                escapeHtml(serviceTitle), escapeHtml(model), escapeHtml(provider), tokens, sec);

        metaInfoLabel.setValue(meta);

        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption(isSimplified ? "Житейское объяснение готово" : "Объяснение требований готово")
                .withDescription("Сгенерировано моделью " + model + " за " + String.format("%.1f", sec) + " с")
                .show();
    }

    private void setBusyState(boolean busy, String statusMessage) {
        progressBox.setVisible(busy);
        progressBar.setVisible(busy);
        if (statusMessage != null) {
            statusLabel.setValue(statusMessage);
        }
        explainSimplifiedBtn.setEnabled(!busy);
        refreshStandardBtn.setEnabled(!busy);
        copyTextBtn.setEnabled(!busy && currentRawText != null && !currentRawText.trim().isEmpty());
    }

    private void showError(String message) {
        errorBox.setVisible(true);
        errorDetailsLabel.setValue(escapeHtml(message));
        metaInfoLabel.setValue("<span style='color:#ef4444;'>Произошла ошибка при обращении к нейросети</span>");
    }

    private void copyExplanationToClipboard() {
        if (currentRawText == null || currentRawText.trim().isEmpty()) {
            return;
        }

        try {
            String jsLiteral = gson.toJson(currentRawText);
            JavaScript.getCurrent().execute(
                    "if (navigator.clipboard && window.isSecureContext) { navigator.clipboard.writeText(" + jsLiteral + "); }");

            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption("Буфер обмена")
                    .withDescription("Текст объяснения успешно скопирован")
                    .show();
        } catch (Exception e) {
            log.warn("Ошибка копирования в буфер обмена: {}", e.getMessage());
        }
    }

    private String markdownToHtml(String md) {
        if (md == null) return "";

        StringBuilder sb = new StringBuilder();
        String[] lines = md.split("\r?\n");
        boolean inList = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (inList) {
                    sb.append("</ul>\n");
                    inList = false;
                }
                sb.append("<br/>\n");
                continue;
            }

            // Заголовки
            if (trimmed.startsWith("### ")) {
                if (inList) { sb.append("</ul>\n"); inList = false; }
                sb.append("<h4 style='color:#1e40af; margin-top:12px; margin-bottom:6px;'>")
                  .append(formatInline(trimmed.substring(4))).append("</h4>\n");
            } else if (trimmed.startsWith("## ")) {
                if (inList) { sb.append("</ul>\n"); inList = false; }
                sb.append("<h3 style='color:#0f172a; margin-top:16px; margin-bottom:8px; border-bottom: 1px solid #e2e8f0; padding-bottom:4px;'>")
                  .append(formatInline(trimmed.substring(3))).append("</h3>\n");
            } else if (trimmed.startsWith("# ")) {
                if (inList) { sb.append("</ul>\n"); inList = false; }
                sb.append("<h2 style='color:#0f172a; margin-top:20px; margin-bottom:10px;'>")
                  .append(formatInline(trimmed.substring(2))).append("</h2>\n");
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                if (!inList) {
                    sb.append("<ul style='margin-top:4px; margin-bottom:4px; padding-left:20px;'>\n");
                    inList = true;
                }
                sb.append("<li style='margin-bottom:4px;'>").append(formatInline(trimmed.substring(2))).append("</li>\n");
            } else if (trimmed.matches("^\\d+\\.\\s+.*")) {
                if (inList) { sb.append("</ul>\n"); inList = false; }
                sb.append("<p style='margin-top:6px; margin-bottom:4px;'><b>")
                  .append(formatInline(trimmed)).append("</b></p>\n");
            } else {
                if (inList) {
                    sb.append("</ul>\n");
                    inList = false;
                }
                sb.append("<p style='margin-top:4px; margin-bottom:6px;'>")
                  .append(formatInline(trimmed)).append("</p>\n");
            }
        }

        if (inList) {
            sb.append("</ul>\n");
        }

        return sb.toString();
    }

    private String formatInline(String text) {
        if (text == null) return "";
        String escaped = escapeHtml(text);
        escaped = escaped.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
        escaped = escaped.replaceAll("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)", "<i>$1</i>");
        escaped = escaped.replaceAll("`(.*?)`", "<code style='background:#f1f5f9; padding:2px 4px; border-radius:4px; font-size:12px; color:#0f172a;'>$1</code>");
        return escaped;
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
