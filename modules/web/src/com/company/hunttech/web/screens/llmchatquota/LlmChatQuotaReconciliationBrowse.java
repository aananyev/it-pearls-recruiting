package com.company.hunttech.web.screens.llmchatquota;

import com.company.hunttech.entity.ai.LlmChatQuotaReservation;
import com.company.hunttech.service.LlmChatService;
import com.haulmont.cuba.core.global.Security;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.app.core.inputdialog.InputParameter;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogOutcome;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.StandardOutcome;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

/** Административная сверка неизвестного usage без повторного вызова провайдера. */
@UiController("hunttech_LlmChatQuotaReconciliation.browse")
@UiDescriptor("llm-chat-quota-reconciliation-browse.xml")
@LookupComponent("reservationsTable")
public class LlmChatQuotaReconciliationBrowse extends StandardLookup<LlmChatQuotaReservation> {

    @Inject
    private DataGrid<LlmChatQuotaReservation> reservationsTable;
    @Inject
    private CollectionLoader<LlmChatQuotaReservation> reservationsDl;
    @Inject
    private LlmChatService llmChatService;
    @Inject
    private Dialogs dialogs;
    @Inject
    private Notifications notifications;
    @Inject
    private Security security;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        // The loader is manual so owner/dialog metadata cannot be fetched before this permission check.
        if (!security.isSpecificPermitted(LlmChatService.RECONCILE_CHAT_QUOTA_PERMISSION)) {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Нет права сверки usage LLM-чата")
                    .show();
            close(StandardOutcome.CLOSE);
            return;
        }
        reservationsDl.load();
    }

    @Subscribe("settledBtn")
    public void onSettled() {
        LlmChatQuotaReservation reservation = selectedReservation();
        if (reservation == null) {
            notifySelectionRequired();
            return;
        }
        dialogs.createInputDialog(this)
                .withCaption("Фактическое usage провайдера")
                .withParameters(InputParameter.stringParameter("actualTokens")
                        .withCaption("Списано токенов")
                        .withRequired(true))
                .withCloseListener(event -> {
                    if (event.closedWith(DialogOutcome.OK)) {
                        reconcileCharged(reservation, event.getValue("actualTokens"));
                    }
                })
                .show();
    }

    @Subscribe("releasedBtn")
    public void onReleased() {
        LlmChatQuotaReservation reservation = selectedReservation();
        if (reservation == null) {
            notifySelectionRequired();
            return;
        }
        try {
            llmChatService.reconcileUnknown(reservation.getRequestId(), 0, false);
            showReconciled();
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    private void reconcileCharged(LlmChatQuotaReservation reservation, String actualTokensText) {
        try {
            int actualTokens = Integer.parseInt(actualTokensText == null ? "" : actualTokensText.trim());
            llmChatService.reconcileUnknown(reservation.getRequestId(), actualTokens, true);
            showReconciled();
        } catch (NumberFormatException ex) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Укажите целое число токенов")
                    .show();
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    private LlmChatQuotaReservation selectedReservation() {
        return reservationsTable.getSingleSelected();
    }

    private void showReconciled() {
        reservationsDl.load();
        notifications.create(Notifications.NotificationType.TRAY)
                .withCaption("Резерв сверён")
                .withDescription("Повторный вызов провайдера не выполнялся.")
                .show();
    }

    private void notifySelectionRequired() {
        notifications.create(Notifications.NotificationType.WARNING)
                .withCaption("Выберите зависший запрос")
                .show();
    }

    private void showError(RuntimeException ex) {
        notifications.create(Notifications.NotificationType.ERROR)
                .withCaption("Не удалось выполнить сверку")
                .withDescription(ex.getMessage() == null ? "Проверьте права и состояние reservation." : ex.getMessage())
                .show();
    }
}
