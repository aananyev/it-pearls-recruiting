package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.service.SmartCvIngestService;
import com.company.hunttech.service.SmartCvParsedData;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.screen.*;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Диалог интеллектуального сканирования резюме и сопоставления контактных данных с карточкой кандидата.
 */
@UiController("hunttech_SmartCvScanDialog")
@UiDescriptor("smart-cv-scan-dialog.xml")
public class SmartCvScanDialog extends Screen {
    private static final Logger log = LoggerFactory.getLogger(SmartCvScanDialog.class);

    @Inject
    private SmartCvIngestService smartCvIngestService;
    @Inject
    private BackgroundWorker backgroundWorker;
    @Inject
    private DataManager dataManager;
    @Inject
    private UiComponents uiComponents;

    @Inject
    private Label<String> cvCandidateNameLabel;
    @Inject
    private Label<String> cvDatePostLabel;
    @Inject
    private Label<String> cvFileNameLabel;
    @Inject
    private Label<String> cvToVacancyLabel;

    @Inject
    private VBoxLayout progressBox;
    @Inject
    private ProgressBar progressBar;
    @Inject
    private Label<String> statusLabel;

    @Inject
    private VBoxLayout errorBox;
    @Inject
    private Label<String> errorDetailsLabel;

    @Inject
    private VBoxLayout noUpdatesBox;

    @Inject
    private VBoxLayout comparisonCard;
    @Inject
    private VBoxLayout comparisonRowsBox;

    @Inject
    private Button selectAllBtn;
    @Inject
    private Button deselectAllBtn;
    @Inject
    private Button applyBtn;
    @Inject
    private Button cancelBtn;

    private CandidateCV candidateCv;
    private JobCandidate candidate;
    private SmartCvParsedData parsedData;
    private final List<CvScanFieldComparison> comparisons = new ArrayList<>();
    private final Map<String, CheckBox> checkBoxesByField = new LinkedHashMap<>();

    /**
     * Инициализация входных параметров диалога.
     */
    public void initScan(CandidateCV cv, JobCandidate cand) {
        this.candidateCv = cv;
        this.candidate = cand;

        populateCvInfo();
    }

    private void populateCvInfo() {
        if (candidateCv == null) return;

        if (cvCandidateNameLabel != null) {
            String candName = candidate != null ? candidate.getFullName() : null;
            cvCandidateNameLabel.setValue(candName != null && !candName.trim().isEmpty() ? candName : "—");
        }

        if (cvDatePostLabel != null) {
            Date dp = candidateCv.getDatePost();
            if (dp != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
                cvDatePostLabel.setValue(sdf.format(dp));
            } else {
                cvDatePostLabel.setValue("—");
            }
        }

        if (cvFileNameLabel != null) {
            String fileName = null;
            if (candidateCv.getOriginalFileCV() != null) {
                fileName = candidateCv.getOriginalFileCV().getName();
            } else if (candidateCv.getFileCV() != null) {
                fileName = candidateCv.getFileCV().getName();
            }
            cvFileNameLabel.setValue(fileName != null ? fileName : "Текстовое резюме");
        }

        if (cvToVacancyLabel != null) {
            String vacName = candidateCv.getToVacancy() != null ? candidateCv.getToVacancy().getVacansyName() : null;
            cvToVacancyLabel.setValue(vacName != null ? vacName : "—");
        }
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        startScan();
    }

    private void startScan() {
        if (candidateCv == null) {
            showError("Не выбрано резюме для анализа");
            return;
        }

        progressBox.setVisible(true);
        progressBar.setVisible(true);
        statusLabel.setValue("Подготовка резюме к анализу...");
        errorBox.setVisible(false);
        noUpdatesBox.setVisible(false);
        comparisonCard.setVisible(false);
        applyBtn.setEnabled(false);

        final CandidateCV inputCv = this.candidateCv;

        BackgroundTask<Integer, SmartCvParsedData> task =
                new BackgroundTask<Integer, SmartCvParsedData>(300, this) {
                    @Override
                    public SmartCvParsedData run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
                        // 1. Чтение резюме и извлечение текста
                        CandidateCV loadedCv = dataManager.load(CandidateCV.class)
                                .id(inputCv.getId())
                                .view("candidateCV-scan-view")
                                .optional()
                                .orElse(inputCv);

                        FileDescriptor fd = loadedCv.getOriginalFileCV() != null
                                ? loadedCv.getOriginalFileCV() : loadedCv.getFileCV();

                        String rawText = null;
                        if (fd != null) {
                            try {
                                rawText = smartCvIngestService.extractTextFromFile(fd, null);
                            } catch (Exception e) {
                                log.warn("Не удалось прочитать файл резюме id=" + fd.getId(), e);
                            }
                        }

                        if (rawText == null || rawText.trim().isEmpty()) {
                            rawText = loadedCv.getTextCV();
                        }

                        if ((rawText == null || rawText.trim().isEmpty()) && loadedCv.getLinkOriginalCv() != null) {
                            try {
                                rawText = fetchTextFromUrl(loadedCv.getLinkOriginalCv());
                            } catch (Exception ignored) {
                            }
                        }

                        if (rawText == null || rawText.trim().isEmpty()) {
                            throw new IllegalArgumentException("Не удалось прочитать выбранное резюме. В выбранной записи отсутствуют прикрепленный файл и текст резюме.");
                        }

                        // 2. Интеллектуальный AI/LLM-парсинг контактных данных
                        return smartCvIngestService.parseCvText(rawText);
                    }

                    @Override
                    public void done(SmartCvParsedData result) {
                        progressBar.setVisible(false);
                        progressBox.setVisible(false);
                        parsedData = result;

                        if (parsedData == null) {
                            showNoUpdatesFound();
                            return;
                        }

                        renderComparisons();
                    }

                    @Override
                    public boolean handleException(Exception ex) {
                        progressBar.setVisible(false);
                        progressBox.setVisible(false);
                        log.error("Ошибка при интеллектуальном сканировании резюме", ex);

                        String msg = ex.getMessage();
                        if (ex instanceof IllegalArgumentException) {
                            showError(msg != null ? msg : "Не удалось прочитать выбранное резюме.");
                        } else {
                            showError("Не удалось выполнить интеллектуальное сканирование. Сервис анализа временно недоступен.<br/>" +
                                    "<span style='color: #64748b; font-size: 11px;'>Технические подробности: " +
                                    escapeHtml(msg != null ? msg : ex.getClass().getSimpleName()) + "</span>");
                        }
                        return true;
                    }
                };

        backgroundWorker.handle(task).execute();
    }

    private void renderComparisons() {
        comparisons.clear();
        checkBoxesByField.clear();
        comparisonRowsBox.removeAll();

        comparisons.addAll(SmartCvScanHelper.buildComparisons(candidate, parsedData));

        long actionableCount = comparisons.stream()
                .filter(c -> c.getStatus() == CvScanFieldComparison.Status.NEW || c.getStatus() == CvScanFieldComparison.Status.REPLACE)
                .count();

        if (actionableCount == 0) {
            showNoUpdatesFound();
            return;
        }

        // Рендеринг таблицы строк сравнения
        for (CvScanFieldComparison comp : comparisons) {
            HBoxLayout row = uiComponents.create(HBoxLayout.class);
            row.setWidth("100%");
            row.setSpacing(true);
            row.setAlignment(Component.Alignment.MIDDLE_LEFT);
            row.setStyleName("cv-scan-comparison-row");
            row.setMargin(new MarginInfo(true, false, true, false));

            // Чекбокс выбора
            CheckBox cb = uiComponents.create(CheckBox.class);
            cb.setValue(comp.isSelected());
            cb.setEnabled(comp.isEnabled());
            cb.addValueChangeListener(e -> {
                comp.setSelected(Boolean.TRUE.equals(e.getValue()));
                updateApplyButtonState();
            });
            checkBoxesByField.put(comp.getFieldId(), cb);
            row.add(cb);

            // Название поля
            Label<String> captionLabel = uiComponents.create(Label.TYPE_STRING);
            captionLabel.setValue(comp.getFieldCaption());
            captionLabel.setWidth("130px");
            captionLabel.setStyleName("bold");
            row.add(captionLabel);

            // Текущее значение
            Label<String> currValLabel = uiComponents.create(Label.TYPE_STRING);
            currValLabel.setValue(comp.getCurrentValue());
            currValLabel.setWidth("180px");
            currValLabel.setStyleName("edit-help");
            row.add(currValLabel);

            // Стрелка-разделитель
            Label<String> arrowLabel = uiComponents.create(Label.TYPE_STRING);
            arrowLabel.setValue("→");
            arrowLabel.setWidth("20px");
            arrowLabel.setStyleName("bold");
            row.add(arrowLabel);

            // Найденное значение
            Label<String> foundValLabel = uiComponents.create(Label.TYPE_STRING);
            foundValLabel.setValue(comp.getFoundValue());
            foundValLabel.setStyleName("bold");
            row.add(foundValLabel);
            row.expand(foundValLabel);

            // Статус бейдж
            Label<String> statusBadge = uiComponents.create(Label.TYPE_STRING);
            statusBadge.setHtmlEnabled(true);
            statusBadge.setWidth("100px");
            statusBadge.setValue(formatStatusBadge(comp.getStatus()));
            row.add(statusBadge);

            comparisonRowsBox.add(row);
        }

        comparisonCard.setVisible(true);
        updateApplyButtonState();
    }

    private String formatStatusBadge(CvScanFieldComparison.Status status) {
        switch (status) {
            case NEW:
                return "<span style='background: #dcfce7; color: #15803d; border-radius: 4px; padding: 2px 8px; font-weight: 600; font-size: 11px;'>Новое</span>";
            case REPLACE:
                return "<span style='background: #dbeafe; color: #1d4ed8; border-radius: 4px; padding: 2px 8px; font-weight: 600; font-size: 11px;'>Замена</span>";
            case MATCH:
                return "<span style='background: #f1f5f9; color: #64748b; border-radius: 4px; padding: 2px 8px; font-size: 11px;'>Совпадает</span>";
            case EMPTY:
            default:
                return "<span style='background: #f8fafc; color: #94a3b8; border-radius: 4px; padding: 2px 8px; font-size: 11px;'>Не найдено</span>";
        }
    }

    private void updateApplyButtonState() {
        boolean hasSelected = comparisons.stream().anyMatch(CvScanFieldComparison::isSelected);
        applyBtn.setEnabled(hasSelected);
    }

    private void showNoUpdatesFound() {
        noUpdatesBox.setVisible(true);
        comparisonCard.setVisible(false);
        applyBtn.setEnabled(false);
    }

    private void showError(String message) {
        errorDetailsLabel.setValue(message);
        errorBox.setVisible(true);
        progressBox.setVisible(false);
        noUpdatesBox.setVisible(false);
        comparisonCard.setVisible(false);
        applyBtn.setEnabled(false);
    }

    @Subscribe("selectAllBtn")
    public void onSelectAllBtnClick(Button.ClickEvent event) {
        for (CvScanFieldComparison comp : comparisons) {
            if (comp.isEnabled()) {
                comp.setSelected(true);
                CheckBox cb = checkBoxesByField.get(comp.getFieldId());
                if (cb != null) {
                    cb.setValue(true);
                }
            }
        }
        updateApplyButtonState();
    }

    @Subscribe("deselectAllBtn")
    public void onDeselectAllBtnClick(Button.ClickEvent event) {
        for (CvScanFieldComparison comp : comparisons) {
            comp.setSelected(false);
            CheckBox cb = checkBoxesByField.get(comp.getFieldId());
            if (cb != null) {
                cb.setValue(false);
            }
        }
        updateApplyButtonState();
    }

    @Subscribe("applyBtn")
    public void onApplyBtnClick(Button.ClickEvent event) {
        close(StandardOutcome.COMMIT);
    }

    @Subscribe("cancelBtn")
    public void onCancelBtnClick(Button.ClickEvent event) {
        close(StandardOutcome.CLOSE);
    }

    /**
     * Получить список сопоставлений полей, выбранных пользователем для обновления.
     * Возвращаются только действия со статусом NEW или REPLACE.
     */
    public List<CvScanFieldComparison> getSelectedUpdates() {
        return comparisons.stream()
                .filter(c -> c.isSelected() && (c.getStatus() == CvScanFieldComparison.Status.NEW || c.getStatus() == CvScanFieldComparison.Status.REPLACE))
                .collect(Collectors.toList());
    }

    public SmartCvParsedData getParsedData() {
        return parsedData;
    }

    private String fetchTextFromUrl(String urlString) throws Exception {
        if (urlString == null || urlString.trim().isEmpty()) return null;
        String cleanUrl = urlString.trim();
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://" + cleanUrl;
        }

        java.net.URI uri = new java.net.URI(cleanUrl);
        String host = uri.getHost();
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Некорректный адрес ссылки: " + urlString);
        }

        // Защита от SSRF: блокировка localhost, link-local, private IP и cloud metadata
        java.net.InetAddress addr = java.net.InetAddress.getByName(host);
        if (addr.isLoopbackAddress() || addr.isLinkLocalAddress() || addr.isSiteLocalAddress()
                || addr.isMulticastAddress() || addr.isAnyLocalAddress()
                || "169.254.169.254".equals(addr.getHostAddress())
                || "localhost".equalsIgnoreCase(host)) {
            throw new SecurityException("Доступ к внутренним сетевым адресам заблокирован: " + host);
        }

        org.jsoup.nodes.Document doc = Jsoup.connect(cleanUrl)
                .userAgent("Mozilla/5.0")
                .timeout(15000)
                .get();
        doc.select("script, style, noscript, svg, nav, footer, header").remove();
        return doc.body() != null ? doc.body().text() : doc.text();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
