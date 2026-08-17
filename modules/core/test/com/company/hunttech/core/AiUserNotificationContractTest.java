package com.company.hunttech.core;

import com.company.hunttech.app.ProcessedImage;
import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.service.AiCredentialOwner;
import com.company.hunttech.service.AiExecutionResult;
import com.company.hunttech.service.AiExecutionService;
import com.company.hunttech.service.HrmAiService;
import com.company.hunttech.service.ProjectAiService;
import com.company.hunttech.service.SkillAnalysisResult;
import com.company.hunttech.service.SkillAnalysisService;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Контракт пользовательской нотификации об AI-операциях HRM HuntTech.
 *
 * <p>Полный текст контракта — {@code docs/architecture/HRM_HuntTech_AI_User_Notification_Contract.md}.
 * Тест фиксирует его кодом: (1) каждый сервис, вызывающий AI-функции, возвращает метаданные
 * выполнения — модель, провайдер и собственника API ({@link AiCredentialOwner}: корпоративное
 * подключение администратора или личное пользователя); (2) экраны показывают исчезающую
 * TRAY-нотификацию стандартными средствами CUBA (web-утилита {@code AiOperationNotifier},
 * автоскрытие 5 с), в которой указано, какая модель что сделала и чей API использован.</p>
 */
public class AiUserNotificationContractTest {

    // ── Слой 1: AiExecutionService возвращает метаданные вместо сырого payload ──────────

    @Test
    public void aiExecutionServiceReturnsExecutionMetadata() throws Exception {
        Method executeText = AiExecutionService.class.getMethod("executeText", String.class, Map.class);
        Method executeImage = AiExecutionService.class.getMethod(
                "executeImage", String.class, Map.class, byte[].class, String.class);
        assertTrue("executeText обязан возвращать AiExecutionResult",
                AiExecutionResult.class == executeText.getReturnType());
        assertTrue("executeImage обязан возвращать AiExecutionResult",
                AiExecutionResult.class == executeImage.getReturnType());
    }

    @Test
    public void executionResultExposesModelProviderAndOwner() throws Exception {
        for (String getter : new String[]{"getFunctionCode", "getFunctionName", "getModelName",
                "getProviderCode", "getCredentialOwner"}) {
            AiExecutionResult.class.getMethod(getter);
        }
        AiExecutionResult.class.getMethod("getText");
        AiExecutionResult.class.getMethod("getImage");
    }

    @Test
    public void credentialOwnerEnumDeclaresAdminAndUser() {
        List<AiCredentialOwner> owners = Arrays.asList(AiCredentialOwner.values());
        assertTrue("Должен быть собственник ADMIN (корпоративное/административное API)",
                owners.contains(AiCredentialOwner.ADMIN));
        assertTrue("Должен быть собственник USER (личное API пользователя)",
                owners.contains(AiCredentialOwner.USER));
    }

    @Test
    public void executionBeanTagsUserAndAdminPathsWithOwner() throws IOException {
        String bean = read("modules/core/src/com/company/hunttech/service/AiExecutionServiceBean.java");
        // Путь пользовательского override помечает результат собственником USER,
        // административный путь — собственником ADMIN (включая image-пути).
        assertTrue("Нет owner=USER в пользовательском пути", bean.contains("AiCredentialOwner.USER"));
        assertTrue("Нет owner=ADMIN в административном пути", bean.contains("AiCredentialOwner.ADMIN"));
        assertTrue("USER-путь не собирает результат с метаданными",
                bean.contains("AiExecutionResult.textResult(function.getCode(), function.getName(), function.getCapability(),"));
        assertTrue("IMAGE-путь не собирает результат с метаданными",
                bean.contains("AiExecutionResult.imageResult(function.getCode(), function.getName(), function.getCapability(),"));
    }

    // ── Слой 2: фасады, вызывающие AI-функции, распространяют метаданные ────────────────

    @Test
    public void projectAiFacadePropagatesExecutionMetadata() throws Exception {
        Method upload = ProjectAiService.class.getMethod("processUploadedDescription",
                String.class, String.class, String.class);
        Method shortDescription = ProjectAiService.class.getMethod("generateShortDescription",
                String.class, String.class);
        assertTrue(AiExecutionResult.class == upload.getReturnType());
        assertTrue(AiExecutionResult.class == shortDescription.getReturnType());
    }

    @Test
    public void skillAnalysisPropagatesExecutionMetadata() throws Exception {
        for (String method : new String[]{"analyzeAll", "analyzeMain", "analyzeSecondary", "analyzeTertiary"}) {
            Method analyze = SkillAnalysisService.class.getMethod(method, String.class);
            assertTrue("Метод " + method + " обязан возвращать SkillAnalysisResult",
                    SkillAnalysisResult.class == analyze.getReturnType());
        }
        // Результат: навыки + метаданные AI-выполнения; при классическом fallback
        // метаданные null (isAiUsed()==false) — экран не показывает «обработано ИИ».
        SkillAnalysisResult.class.getMethod("getSkills");
        SkillAnalysisResult.class.getMethod("getAiExecution");
        SkillAnalysisResult.class.getMethod("isAiUsed");
    }

    @Test
    public void legacyHrmAiFacadeKeepsStringContractForExternalConsumers() throws Exception {
        // Legacy-фасад vacancy-сценариев возвращает String (внешние потребители —
        // боты): контракт метаданных соблюдается на уровне AiExecutionService,
        // который этот фасад вызывает. Сознательное исключение, зафиксированное в доке.
        Method standardize = HrmAiService.class.getMethod("standardizeVacancyDescription", String.class);
        assertTrue(String.class == standardize.getReturnType());
    }

    @Test
    public void connectionTestIsRealAiCallWithContractNotification() throws Exception {
        // testConnection выполняет реальный AI-вызов (проверка provider/key/model)
        // и, по контракту, несёт метаданные выполнения, а экраны «Управление AI»
        // показывают исчезающую нотификацию «какая модель что делала + чей API».
        Method testConnection = HrmAiService.class.getMethod("testConnection", UserAiConfiguration.class);
        assertTrue("testConnection обязан возвращать AiExecutionResult",
                AiExecutionResult.class == testConnection.getReturnType());

        String bean = read("modules/core/src/com/company/hunttech/service/HrmAiServiceBean.java");
        assertTrue("testConnection не строит результат с метаданными",
                bean.contains("AiExecutionResult.textResult("));
        assertTrue("testConnection не помечает собственник = личный ключ пользователя",
                bean.contains("AiCredentialOwner.USER"));
        assertTrue("testConnection не проставляет модель/провайдер в метаданные",
                bean.contains("configuration.getDefaultModelName()")
                        && bean.contains("configuration.getProviderCode()"));

        String browse = read("modules/web/src/com/company/hunttech/web/screens/useraiconfiguration/UserAiConfigurationBrowse.java");
        String settings = read("modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindow.java");
        assertTrue("UserAiConfigurationBrowse не показывает контрактную нотификацию",
                browse.contains("AiOperationNotifier.show(notifications, result"));
        assertTrue("ExtSettingsWindow не показывает контрактную нотификацию",
                settings.contains("AiOperationNotifier.show(notifications, result"));
    }

    @Test
    public void processedImageCarriesAiExecutionMetadata() throws Exception {
        ProcessedImage.class.getMethod("getAiExecution");
    }

    @Test
    public void logoProcessingPassesAiExecutionToResult() throws IOException {
        String bean = read("modules/core/src/com/company/hunttech/app/ProjectLogoImageProcessingServiceBean.java");
        assertTrue("AI-этап логотипа не сохраняет метаданные выполнения",
                bean.contains("aiExecutionInfo = aiExecution"));
        assertTrue("Метаданные не переданы в ProcessedImage",
                bean.contains("aiProcessed, aiExecutionInfo"));
    }

    // ── Слой 3: web-нотификация — исчезающая TRAY, модель + собственник API ─────────────

    @Test
    public void webNotifierIsDisappearingTrayStatingModelAndOwner() throws IOException {
        String notifier = read("modules/web/src/com/company/hunttech/web/util/AiOperationNotifier.java");

        // Исчезающее оповещение стандартными средствами CUBA Platform.
        assertTrue("Нотификация не TRAY-типа", notifier.contains("NotificationType.TRAY"));
        assertTrue("Нотификация не в правом нижнем углу",
                notifier.contains("Position.BOTTOM_RIGHT"));
        assertTrue("Нет автоскрытия", notifier.contains("withHideDelayMs"));
        assertTrue("Автоскрытие не 5 секунд (контракт)", notifier.contains("HIDE_DELAY_MS = 5000"));

        // Пользователю передаётся «какая модель что делала».
        assertTrue("Нет блока «Модель»", notifier.contains("LABEL_MODEL = \"Модель\""));
        assertTrue("Нет блока «Провайдер»", notifier.contains("LABEL_PROVIDER = \"Провайдер\""));

        // Собственник API: административное API или API пользователя.
        assertTrue("Нет подписи «Собственник API»", notifier.contains("LABEL_OWNER = \"Собственник API\""));
        assertTrue("Нет подписи корпоративного (административного) API",
                notifier.contains("корпоративный (администратора)"));
        assertTrue("Нет подписи личного API пользователя",
                notifier.contains("личный (пользователя)"));
        assertTrue("Подпись собственника не зависит от AiCredentialOwner",
                notifier.contains("AiCredentialOwner.USER == result.getCredentialOwner()"));
    }

    @Test
    public void aiNotificationsAreShownTwiceStartedAndCompleted() throws IOException {
        String notifier = read("modules/web/src/com/company/hunttech/web/util/AiOperationNotifier.java");
        String projectEdit = read("modules/web/src/com/company/hunttech/web/screens/project/ProjectEdit.java");
        String candidateCv = read("modules/web/src/com/company/hunttech/web/screens/candidatecv/CandidateCVEdit.java");
        String uploadField = read("modules/web/src/com/company/hunttech/web/gui/components/WebProjectLogoFileUploadField.java");

        // Единая точка стартовой нотификации — та же исчезающая TRAY (5 с), что и итоговая.
        assertTrue("AiOperationNotifier не имеет стартовой нотификации",
                notifier.contains("public static void showStarted"));
        assertTrue("Стартовая нотификация не исчезающая", notifier.contains("withHideDelayMs"));
        assertTrue("Старт не обещает итоговую нотификацию с моделью и собственником API",
                notifier.contains("После завершения будет указана модель и собственник API."));

        // ProjectEdit: «Кратко» и обработка описания — старт + завершение.
        assertTrue("ProjectEdit «Кратко» не показывает стартовую нотификацию",
                projectEdit.contains("AiOperationNotifier.showStarted(notifications,"));
        assertTrue("ProjectEdit upload-описания не показывает стартовую нотификацию",
                countOccurrences(projectEdit, "AiOperationNotifier.showStarted(notifications,") >= 2);
        assertTrue("ProjectEdit «Кратко» не показывает итоговую нотификацию",
                projectEdit.contains("AiOperationNotifier.show(notifications, result"));

        // CandidateCVEdit: старт анализа + итоговая статистика с моделью/собственником.
        assertTrue("CandidateCVEdit не показывает стартовую нотификацию",
                candidateCv.contains("AiOperationNotifier.showStarted(notifications, \"Запущен AI-анализ навыков резюме…\", null)"));
        assertTrue("CandidateCVEdit не добавляет модель/собственника API в итоговую нотификацию",
                candidateCv.contains("AiOperationNotifier.buildDescription(aiExecution, statsDescription)"));

        // Загрузка изображений: старт только при включённом нейросетевом этапе.
        assertTrue("WebProjectLogoFileUploadField не показывает стартовую нотификацию",
                uploadField.contains("AiOperationNotifier.showStarted(appUI.getNotifications(), caption, detail)"));
        assertTrue("Старт логотипа не завязан на hunttech.projectLogo.ai.enabled",
                uploadField.contains("config.getAiProcessingEnabled()"));
        assertTrue("Старт фото не завязан на hunttech.projectLogo.rembg.enabled",
                uploadField.contains("config.getRembgEnabled()"));
        assertTrue("WebProjectLogoFileUploadField не показывает итоговую нотификацию",
                uploadField.contains("AiOperationNotifier.show(appUI.getNotifications(), processedAiExecution"));
    }

    @Test
    public void aiOperationsFollowReferencePatternWithProgressDialog() throws IOException {
        // Эталонный паттерн (§4.1 контракта, эталон — CandidateCVEdit «Сканировать навыки»):
        // showStarted → showProgress («крутилка») → BackgroundTask → closeProgress во всех
        // терминальных путях (done/handleException/handleTimeoutException) → итоговая нотификация.
        String projectEdit = read("modules/web/src/com/company/hunttech/web/screens/project/ProjectEdit.java");
        String candidateCv = read("modules/web/src/com/company/hunttech/web/screens/candidatecv/CandidateCVEdit.java");
        String openPosition = read("modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionEdit.java");
        String userBrowse = read("modules/web/src/com/company/hunttech/web/screens/useraiconfiguration/UserAiConfigurationBrowse.java");
        String extSettings = read("modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindow.java");

        // ProjectEdit: обе AI-операции показывают «крутилку» и закрывают её во всех путях.
        assertTrue("ProjectEdit не показывает «крутилку»",
                countOccurrences(projectEdit, "AiOperationNotifier.showProgress(this,") >= 2);
        assertTrue("ProjectEdit не закрывает «крутилку» в done/exception/timeout",
                countOccurrences(projectEdit, "AiOperationNotifier.closeProgress(progressDialog)") >= 6);
        assertTrue("ProjectEdit не обрабатывает таймаут (закрытие крутилки)",
                countOccurrences(projectEdit, "AiOperationNotifier.closeProgress(progressDialog);\n" +
                        "                projectDescriptionShortButton.setEnabled(true);") >= 1);

        // CandidateCVEdit: анализ навыков — эталон (старт → крутилка → итог).
        assertTrue("CandidateCVEdit не показывает «крутилку» анализа навыков",
                candidateCv.contains("AiOperationNotifier.showProgress(this, \"Анализ навыков резюме…\")"));
        assertTrue("CandidateCVEdit не закрывает «крутилку» в handleException",
                candidateCv.contains("AiOperationNotifier.closeProgress(progressDialog);\n" +
                        "                        notifications.create(Notifications.NotificationType.ERROR)"));
        assertTrue("CandidateCVEdit не обрабатывает таймаут анализа навыков",
                candidateCv.contains("public boolean handleTimeoutException()"));

        // OpenPositionEdit: AI-анализ требований — тот же паттерн.
        assertTrue("OpenPositionEdit не показывает «крутилку»",
                openPosition.contains("AiOperationNotifier.showProgress(this, \"Анализ требований вакансии…\")"));
        assertTrue("OpenPositionEdit не обрабатывает таймаут",
                openPosition.contains("public boolean handleTimeoutException()"));

        // Диагностика testConnection в обоих экранах «Управление AI» — тоже фоновый
        // паттерн (синхронный вызов на UI-потоке недопустим: обе нотификации одной пачкой).
        for (String screen : new String[]{userBrowse, extSettings}) {
            assertTrue("Экран «Управление AI» не показывает стартовую нотификацию testConnection",
                    screen.contains("AiOperationNotifier.showStarted(notifications, \"Проверка AI-подключения…\", null)"));
            assertTrue("Экран «Управление AI» не показывает «крутилку» testConnection",
                    screen.contains("AiOperationNotifier.showProgress(this, \"Проверка AI-подключения…\")"));
            assertTrue("Экран «Управление AI» не выполняет testConnection в BackgroundTask",
                    screen.contains("new BackgroundTask<Integer, AiExecutionResult>(60, this)"));
            assertTrue("Экран «Управление AI» не закрывает «крутилку» в done",
                    screen.contains("AiOperationNotifier.closeProgress(progressDialog);"));
            assertTrue("Экран «Управление AI» не обрабатывает таймаут testConnection",
                    screen.contains("public boolean handleTimeoutException()"));
        }
    }

    @Test
    public void screensShowDisappearingNotificationForAiOperations() throws IOException {
        String projectEdit = read("modules/web/src/com/company/hunttech/web/screens/project/ProjectEdit.java");
        String candidateCv = read("modules/web/src/com/company/hunttech/web/screens/candidatecv/CandidateCVEdit.java");
        String uploadField = read("modules/web/src/com/company/hunttech/web/gui/components/WebProjectLogoFileUploadField.java");

        // ProjectEdit: «Кратко» и обработка загруженного описания — через AiOperationNotifier.
        assertTrue("ProjectEdit «Кратко» не использует AiOperationNotifier",
                projectEdit.contains("AiOperationNotifier.show(notifications, result"));
        assertTrue("ProjectEdit upload-описания не использует AiOperationNotifier",
                countOccurrences(projectEdit, "AiOperationNotifier.show(notifications, result") >= 2);

        // CandidateCVEdit: статистика анализа навыков — исчезающая нотификация (5 с)
        // с блоком «модель + собственник API» от AiOperationNotifier.
        assertTrue("CandidateCVEdit не добавляет модель/собственника API в нотификацию",
                candidateCv.contains("AiOperationNotifier.buildDescription(aiExecution, statsDescription)"));
        assertTrue("CandidateCVEdit нотификация не исчезающая (5 с)",
                candidateCv.contains("withHideDelayMs(5000)"));

        // Загрузка логотипа: нотификация при реальном применении AI-функции.
        assertTrue("WebProjectLogoFileUploadField не использует AiOperationNotifier",
                uploadField.contains("AiOperationNotifier.show(appUI.getNotifications(), processedAiExecution"));
    }

    @Test
    public void skillAnalysisBeanPropagatesMetadataOrNullFallback() throws IOException {
        String bean = read("modules/core/src/com/company/hunttech/service/SkillAnalysisServiceBean.java");
        assertTrue("AI-путь не возвращает метаданные выполнения",
                bean.contains("SkillAnalysisResult.of(matched, execution)"));
        assertTrue("Классический fallback не обнуляет метаданные (AI не выполнялся)",
                bean.contains("SkillAnalysisResult.of(SkillNameMatcher.matchText(loadDictionary(), normalizedText), null)"));
    }

    private int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(projectRoot().resolve(relativePath)), StandardCharsets.UTF_8);
    }

    private Path projectRoot() {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertNotNull("Не найден корень проекта HRM HuntTech", root);
        return root;
    }
}
