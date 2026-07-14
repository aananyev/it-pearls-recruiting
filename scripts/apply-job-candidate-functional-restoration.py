#!/usr/bin/env python3
"""Восстанавливает ленивую функциональность вкладки «Позиции и вакансии»."""

from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
JAVA_PATH = ROOT / "modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java"
XML_PATH = ROOT / "modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml"


def replace_once(text: str, old: str, new: str, description: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(
            f"{description}: ожидалось одно совпадение, найдено {count}. "
            "Ветка изменилась — автоматическое применение остановлено."
        )
    return text.replace(old, new, 1)


def patch_java() -> None:
    java = JAVA_PATH.read_text(encoding="utf-8")

    old_fields = '''    List<Position> setPos = new ArrayList<>();
    // TODO[tabPositions]: поля positionsTabLoading/positionsTabLoaded/historyRowDataByVacancy отключены вместе с вкладкой
    // private Map<UUID, HistoryRowData> historyRowDataByVacancy;
    // private boolean positionsTabLoading;
    // private boolean positionsTabLoaded;
    // Заглушка для генераторов — вкладка отключена, генераторы не будут вызваны.
    private Map<UUID, HistoryRowData> historyRowDataByVacancy = Collections.emptyMap();
    private boolean skillsLoading;'''

    new_fields = '''    List<Position> setPos = new ArrayList<>();
    // Данные вкладки «Позиции и вакансии» загружаются один раз при первом открытии.
    private Map<UUID, HistoryRowData> historyRowDataByVacancy = Collections.emptyMap();
    private boolean positionsTabLoading;
    private boolean positionsTabLoaded;
    private boolean skillsLoading;'''

    java = replace_once(java, old_fields, new_fields, "поля состояния вкладки")

    old_init = '''        preventAutoLoadUntilReady(openPositionDl, () -> openPositionLoaderInitialized);
        preventAutoLoadUntilReady(citiesDl, () -> referenceLoadersInitialized);
        preventAutoLoadUntilReady(personPositionsLc, () -> referenceLoadersInitialized);
        // TODO[tabPositions]: loaders for positions tab disabled. Tab is commented out.
        // preventAutoLoadUntilReady(lastProjectDl, () -> positionsTabLoaded);
        // preventAutoLoadUntilReady(suggestOpenPositionDl, () -> positionsTabLoaded);

        tabSheetSocialNetworks.addSelectedTabChangeListener(selectedTabChangeEvent -> {
            initTabResume();
            initTabInteractions();
            initTabCandidate();
            initTabContactInfo();
            initTabComments();
            // TODO[tabPositions]: tab positions disabled
            // initTabPositions();
        });'''

    new_init = '''        preventAutoLoadUntilReady(openPositionDl, () -> openPositionLoaderInitialized);
        preventAutoLoadUntilReady(citiesDl, () -> referenceLoadersInitialized);
        preventAutoLoadUntilReady(personPositionsLc, () -> referenceLoadersInitialized);
        // Запросы вкладки содержат параметры кандидата и позиции, поэтому
        // блокируем автоматическую загрузку до первого открытия вкладки.
        preventAutoLoadUntilReady(lastProjectDl, () -> positionsTabLoaded);
        preventAutoLoadUntilReady(suggestOpenPositionDl, () -> positionsTabLoaded);

        tabSheetSocialNetworks.addSelectedTabChangeListener(selectedTabChangeEvent -> {
            initTabResume();
            initTabInteractions();
            initTabCandidate();
            initTabContactInfo();
            initTabComments();
            initTabPositions();
        });'''

    java = replace_once(java, old_init, new_init, "инициализация loader'ов вкладки")

    old_method = '''    // TODO[tabPositions]: Вкладка «Позиции и вакансии» отключена.
    // Полный код сохранён в git-истории. Для восстановления:
    //   git diff HEAD~2 -- modules/web/.../JobCandidateEdit.java
    // Методы: initTabPositions(), startPositionsBackgroundLoading(),
    // loadHistoryKeyValues(), buildHistoryRowData(), buildOneRow(),
    // loadSuggestedVacancies(), applyPositionsTabResult(), PositionsTabData.
    private void initTabPositions() {
        // tab positions disabled — см. TODO выше
    }'''

    new_method = '''    /**
     * Инициализирует историю рассмотрения и подходящие вакансии только при первом
     * открытии вкладки. Открытие JobCandidateEdit не выполняет эти запросы.
     */
    private void initTabPositions() {
        if (positionsTabLoaded || positionsTabLoading) {
            return;
        }

        TabSheet.Tab selectedTab = tabSheetSocialNetworks.getSelectedTab();
        if (selectedTab == null || !"tabPositions".equals(selectedTab.getName())) {
            return;
        }

        if (PersistenceHelper.isNew(getEditedEntity()) || getEditedEntity().getId() == null) {
            positionsTabLoaded = true;
            lastProjectTable.setVisible(false);
            suggestVacancyTable.setVisible(false);
            return;
        }

        startPositionsBackgroundLoading();
    }

    /**
     * В фоне агрегирует только скалярные значения взаимодействий. Entity-графы
     * кандидата, резюме и вакансий между потоками не передаются.
     */
    private void startPositionsBackgroundLoading() {
        if (positionsTabLoading || positionsTabLoaded) {
            return;
        }

        UUID candidateId = getEditedEntity().getId();
        positionsTabLoading = true;

        BackgroundTask<Void, Map<UUID, HistoryRowData>> task =
                new BackgroundTask<Void, Map<UUID, HistoryRowData>>(
                        60, TimeUnit.SECONDS, this) {
                    @Override
                    public Map<UUID, HistoryRowData> run(TaskLifeCycle<Void> taskLifeCycle) {
                        DataManager bgDataManager = AppBeans.get(DataManager.class);
                        List<KeyValueEntity> rows = bgDataManager.loadValues(
                                "select vacancy.id, vacancy.vacansyName, e.dateIteraction, " +
                                        "interactionType.iterationName, " +
                                        "interactionType.signOurInterviewAssigned, " +
                                        "interactionType.signOurInterview, " +
                                        "recruiter.name, e.recrutierName " +
                                        "from hunttech_IteractionList e " +
                                        "left join e.vacancy vacancy " +
                                        "left join e.iteractionType interactionType " +
                                        "left join e.recrutier recruiter " +
                                        "where e.candidate.id = :candidateId " +
                                        "and vacancy is not null " +
                                        "and vacancy.vacansyName not like 'Default' " +
                                        "order by e.dateIteraction desc")
                                .properties(
                                        "vacancyId",
                                        "vacancyName",
                                        "dateIteraction",
                                        "interactionName",
                                        "signResearcher",
                                        "signRecruiter",
                                        "recruiterName",
                                        "legacyRecruiterName")
                                .parameter("candidateId", candidateId)
                                .list();
                        return buildHistoryRowData(rows);
                    }

                    @Override
                    public void done(Map<UUID, HistoryRowData> result) {
                        historyRowDataByVacancy = result != null
                                ? result : Collections.emptyMap();
                        positionsTabLoading = false;
                        positionsTabLoaded = true;

                        // UI-loader'ы запускаются только на UI-потоке и только
                        // после установки обязательных параметров.
                        lastProjectDl.setParameter("candidate", getEditedEntity());
                        lastProjectDl.load();
                        setLastProjectOfCandidate();
                        setSuggestOpenPositionTable();
                        lastProjectTable.repaint();
                        suggestVacancyTable.repaint();
                    }

                    @Override
                    public boolean handleException(Exception exception) {
                        positionsTabLoading = false;
                        log.error("Не удалось загрузить вкладку позиций кандидата, candidateId={}",
                                candidateId, exception);
                        notifications.create(Notifications.NotificationType.ERROR)
                                .withCaption(messageBundle.getMessage("msgError"))
                                .withDescription("Не удалось загрузить историю позиций кандидата")
                                .show();
                        return true;
                    }
                };

        backgroundWorker.handle(task).execute();
    }

    /** Один раз агрегирует значения для генераторов колонок истории. */
    private Map<UUID, HistoryRowData> buildHistoryRowData(List<KeyValueEntity> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<UUID, HistoryAccumulator> accumulators = new LinkedHashMap<>();
        for (KeyValueEntity row : rows) {
            UUID vacancyId = row.getValue("vacancyId");
            if (vacancyId == null) {
                continue;
            }

            HistoryAccumulator accumulator = accumulators.computeIfAbsent(
                    vacancyId,
                    id -> new HistoryAccumulator(id, row.getValue("vacancyName")));

            Date interactionDate = row.getValue("dateIteraction");
            if (accumulator.maxDate == null
                    || interactionDate != null && interactionDate.after(accumulator.maxDate)) {
                accumulator.maxDate = interactionDate;
                accumulator.lastInteractionName = row.getValue("interactionName");
            }

            String employeeName = row.getValue("recruiterName");
            if (employeeName == null || employeeName.trim().isEmpty()) {
                employeeName = row.getValue("legacyRecruiterName");
            }

            if (accumulator.researcherName == null
                    && Boolean.TRUE.equals(row.getValue("signResearcher"))) {
                accumulator.researcherName = employeeName;
            }
            if (accumulator.recruiterName == null
                    && Boolean.TRUE.equals(row.getValue("signRecruiter"))) {
                accumulator.recruiterName = employeeName;
            }
        }

        Map<UUID, HistoryRowData> result = new LinkedHashMap<>();
        for (HistoryAccumulator accumulator : accumulators.values()) {
            result.put(accumulator.vacancyId, new HistoryRowData(
                    accumulator.vacancyId,
                    accumulator.vacancyName,
                    accumulator.maxDate,
                    accumulator.lastInteractionName,
                    accumulator.researcherName,
                    accumulator.recruiterName));
        }
        return result;
    }

    /** Внутренняя изменяемая модель для единственного прохода по строкам JPQL. */
    private static final class HistoryAccumulator {
        final UUID vacancyId;
        final String vacancyName;
        Date maxDate;
        String lastInteractionName;
        String researcherName;
        String recruiterName;

        HistoryAccumulator(UUID vacancyId, String vacancyName) {
            this.vacancyId = vacancyId;
            this.vacancyName = vacancyName;
        }
    }'''

    java = replace_once(java, old_method, new_method, "ленивая загрузка вкладки")

    old_disabled_comment = '''    // ── Positions tab background loading ──────────────────────────────
    // TODO[tabPositions]: Все методы вкладки «Позиции и вакансии» отключены.
    // Полный код сохранён в git-истории (HEAD~1).


'''
    java = replace_once(java, old_disabled_comment, "", "устаревший TODO вкладки")

    JAVA_PATH.write_text(java, encoding="utf-8")


def patch_xml() -> None:
    xml = XML_PATH.read_text(encoding="utf-8")

    old_tab = '''                    <!-- TODO[tabPositions]: Вкладка отключена visible="false".
                         Для включения убрать visible="false" и раскомментировать
                         Java-методы в JobCandidateEdit.java -->
                    <tab id="tabPositions"
                         caption="Позиции и вакансии"
                         visible="false"
                         spacing="true"
                         margin="false">
                        <vbox width="100%" spacing="true" stylename="job-candidate-accordion-section">
                            <hbox width="100%" align="MIDDLE_LEFT" stylename="job-candidate-accordion-header">
                                <label value="Позиции и вакансии" stylename="job-candidate-accordion-title"/>
                            </hbox>
                            <vbox width="100%" height="AUTO" spacing="true" stylename="job-candidate-accordion-content">
                                <hbox id="jobCandidatePositionsLayout"'''

    new_tab = '''                    <!-- TAB: Позиции и вакансии. Данные загружаются лениво контроллером. -->
                    <tab id="tabPositions"
                         caption="Позиции и вакансии"
                         spacing="true"
                         margin="false"
                         expand="tabPositionsSection">
                        <vbox id="tabPositionsSection"
                              width="100%"
                              height="100%"
                              spacing="true"
                              expand="tabPositionsContent"
                              stylename="job-candidate-accordion-section">
                            <hbox width="100%" align="MIDDLE_LEFT" stylename="job-candidate-accordion-header">
                                <label value="Позиции и вакансии" stylename="job-candidate-accordion-title"/>
                            </hbox>
                            <vbox id="tabPositionsContent"
                                  width="100%"
                                  height="100%"
                                  spacing="true"
                                  expand="jobCandidatePositionsLayout"
                                  stylename="job-candidate-accordion-content">
                                <hbox id="jobCandidatePositionsLayout"'''

    xml = replace_once(xml, old_tab, new_tab, "вкладка позиций")
    XML_PATH.write_text(xml, encoding="utf-8")

    # XML должен оставаться синтаксически корректным, а каждый expand — указывать
    # только на непосредственного дочернего компонента.
    tree = ET.parse(XML_PATH)
    root = tree.getroot()
    errors = []
    for element in root.iter():
        target = element.attrib.get("expand")
        if not target:
            continue
        child_ids = {child.attrib.get("id") for child in list(element)}
        if target not in child_ids:
            errors.append(
                f"{element.tag.split('}')[-1]}#{element.attrib.get('id', '<без id>')} "
                f"expand={target!r}"
            )
    if errors:
        raise RuntimeError("Некорректные expand после восстановления:\n" + "\n".join(errors))


def main() -> None:
    patch_java()
    patch_xml()
    print("Функциональность вкладки «Позиции и вакансии» восстановлена.")


if __name__ == "__main__":
    main()
