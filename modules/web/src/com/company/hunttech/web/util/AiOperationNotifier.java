package com.company.hunttech.web.util;

import com.company.hunttech.service.AiCredentialOwner;
import com.company.hunttech.service.AiExecutionResult;
import com.company.hunttech.web.screens.AiProgressDialog;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.ContentMode;
import com.haulmont.cuba.gui.screen.FrameOwner;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.gui.screen.Screen;

/**
 * Единая точка показа пользователю фидбека об AI-операции: исчезающие
 * TRAY-нотификации (старт/итог) и модальный диалог прогресса «крутилка»
 * на время фонового выполнения.
 *
 * <p><b>Контракт</b> (полный текст — {@code docs/architecture/HRM_HuntTech_AI_User_Notification_Contract.md}):
 * каждая реальная AI-операция, инициированная из UI, завершается исчезающей
 * TRAY-нотификацией стандартными средствами CUBA Platform ({@link Notifications}),
 * в которой указано, какая модель что сделала и кто является собственником
 * использованного API: корпоративное (административное) подключение
 * ({@link AiCredentialOwner#ADMIN}) или личное подключение пользователя
 * ({@link AiCredentialOwner#USER}).</p>
 *
 * <p>Нотификация автоматически исчезает через {@value #HIDE_DELAY_MS} мс (правый
 * нижний угол). Заголовок («что сделала») передаёт вызывающий экран; блок «какая
 * модель + собственник API» строится из {@link AiExecutionResult}.</p>
 */
public final class AiOperationNotifier {

    /** Время автоскрытия нотификации, мс (контракт пользовательской нотификации). */
    public static final int HIDE_DELAY_MS = 5000;

    private static final String LABEL_MODEL = "Модель";
    private static final String LABEL_PROVIDER = "Провайдер";
    private static final String LABEL_OWNER = "Собственник API";
    private static final String LABEL_OWNER_ADMIN = "корпоративный (администратора)";
    private static final String LABEL_OWNER_USER = "личный (пользователя)";
    private static final String LABEL_UNKNOWN = "не указана";

    private AiOperationNotifier() {
    }

    /**
     * Показывает исчезающую TRAY-нотификацию об успешно выполненной AI-операции.
     *
     * @param notifications     API нотификаций текущего экрана (CUBA)
     * @param result            результат AI-выполнения с метаданными (модель, собственник API)
     * @param operationCaption  что сделала модель, например «Краткое описание сгенерировано»
     * @param operationDetail   дополнительные детали операции или {@code null}
     */
    public static void show(Notifications notifications, AiExecutionResult result,
                            String operationCaption, String operationDetail) {
        notifications.create(Notifications.NotificationType.TRAY)
                .withPosition(Notifications.Position.BOTTOM_RIGHT)
                .withCaption(operationCaption)
                .withDescription(buildDescription(result, operationDetail))
                .withContentMode(ContentMode.HTML)
                .withHideDelayMs(HIDE_DELAY_MS)
                .show();
    }

    /**
     * Показывает исчезающую TRAY-нотификацию о НАЧАЛЕ AI-операции («AI-нотификации
     * 2 раза» — при старте и по завершении, контракт пользовательской нотификации).
     *
     * <p>Если {@code operationDetail} не передан, в описании указывается, что по
     * завершении будут показаны использованная модель и собственник API — так
     * пользователь сразу понимает, что итоговая нотификация ещё придёт.</p>
     *
     * @param notifications     API нотификаций текущего экрана (CUBA)
     * @param operationCaption  что запускается, например «AI генерирует краткое описание…»
     * @param operationDetail   дополнительные детали операции или {@code null}
     */
    public static void showStarted(Notifications notifications, String operationCaption,
                                   String operationDetail) {
        String detail = (operationDetail == null || operationDetail.trim().isEmpty())
                ? "После завершения будет указана модель и собственник API."
                : operationDetail;
        notifications.create(Notifications.NotificationType.TRAY)
                .withPosition(Notifications.Position.BOTTOM_RIGHT)
                .withCaption(operationCaption)
                .withDescription(detail)
                .withContentMode(ContentMode.HTML)
                .withHideDelayMs(HIDE_DELAY_MS)
                .show();
    }

    /**
     * Показывает модальный диалог «крутилка» (indeterminate ProgressBar) на время
     * выполнения фоновой AI-операции и возвращает его для последующего закрытия
     * в {@link #closeProgress(Screen)} из завершающего обработчика
     * {@code BackgroundTask} ({@code done} / {@code handleException} /
     * {@code handleTimeoutException}).
     *
     * <p>Порядок показа по контракту: сначала {@link #showStarted(Notifications, String, String)}
     * (нотификация о начале), затем этот диалог, по завершении —
     * {@link #show(Notifications, AiExecutionResult, String, String)} (нотификация
     * об итоге с кратким отчётом).</p>
     *
     * @param owner   экран-владелец (должен быть открыт)
     * @param message текст под «крутилкой», например «Анализ навыков резюме…»
     * @return открытый диалог прогресса; закрывается методом {@link #closeProgress(Screen)}
     */
    public static Screen showProgress(FrameOwner owner, String message) {
        AiProgressDialog dialog = AppBeans.get(ScreenBuilders.class).screen(owner)
                .withScreenClass(AiProgressDialog.class)
                .withOpenMode(OpenMode.DIALOG)
                .build();
        dialog.setMessage(message);
        dialog.show();
        return dialog;
    }

    /**
     * Закрывает диалог прогресса, открытый {@link #showProgress(FrameOwner, String)}.
     * Безопасен для {@code null} (диалог не открывался или уже закрыт).
     */
    public static void closeProgress(Screen progressDialog) {
        if (progressDialog != null) {
            progressDialog.closeWithDefaultAction();
        }
    }

    /**
     * HTML-блок «какая модель что сделала + собственник API» для нотификации:
     *
     * <pre>Модель: deepseek-v4-flash · Провайдер: deepseek
     * Собственник API: корпоративный (администратора)</pre>
     *
     * <p>Если AI-выполнение не состоялось ({@code result == null} — классический
     * fallback сервиса, AI недоступен), возвращается только {@code operationDetail}
     * без AI-блока — контракт пользовательской нотификации (см. {@code docs/architecture/HRM_HuntTech_AI_User_Notification_Contract.md}).</p>
     *
     * @param result          метаданные AI-выполнения или {@code null} (fallback)
     * @param operationDetail детали операции (первая строка) или {@code null}
     * @return HTML-описание для {@code withDescription(...)}
     */
    public static String buildDescription(AiExecutionResult result, String operationDetail) {
        if (result == null) {
            return operationDetail == null ? "" : operationDetail;
        }
        StringBuilder description = new StringBuilder();
        if (operationDetail != null && !operationDetail.trim().isEmpty()) {
            description.append(operationDetail).append("<br/>");
        }
        description.append(LABEL_MODEL).append(": ").append(safe(result.getModelName()))
                .append(" · ").append(LABEL_PROVIDER).append(": ").append(safe(result.getProviderCode()))
                .append("<br/>").append(LABEL_OWNER).append(": ")
                .append(AiCredentialOwner.USER == result.getCredentialOwner()
                        ? LABEL_OWNER_USER : LABEL_OWNER_ADMIN);
        return description.toString();
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? LABEL_UNKNOWN : value;
    }
}
