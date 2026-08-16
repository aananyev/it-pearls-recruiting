# Интеграция сервиса анализа навыков и сущность «Навыки кандидата» (`CandidateSkill`)

## 1. Общие сведения

* **Новая сущность:** `hunttech_CandidateSkill` (`com.company.hunttech.entity.CandidateSkill`)
* **Перечисление уровней критичности:** `com.company.hunttech.entity.CandidateSkillPriority` (MAIN / 10, SECONDARY / 20, TERTIARY / 30)
* **Используемый сервис анализа навыков:** `hunttech_SkillAnalysisService` (`SkillAnalysisService`)
* **Точки интеграции в UI:**
  * Экран редактирования резюме `hunttech_CandidateCV.edit` (`CandidateCVEdit.java`, `candidate-cv-edit.xml`), вкладка «Текст резюме» (`tabCV`), кнопка **«Сканировать навыки»** (`scanCandidateSkillsButton`).
  * Сайдбар карточки кандидата `hunttech_JobCandidate.edit` (`JobCandidateEdit.java`, `job-candidate-edit.xml`): новый раздел **«ОСНОВНЫЕ НАВЫКИ»** (`candidateProfileSkills`) с цветными бейджами навыков кандидата.
  * Сайдбар Split-View экранов `hunttech_JobCandidateTest1.browse` и `hunttech_JobCandidateTest.browse`: блок **«ОСНОВНЫЕ НАВЫКИ»** (`detailSkillsVBox`) с динамическим выводом бейджей навыков выбранного кандидата.
* **Всплывающее уведомление:** TRAY-нотификация с подробной статистикой обнаруженных/сохраненных навыков и автоматическим скрытием через **5 секунд** (`withHideDelayMs(5000)`).
* **Миграции базы данных:**
  * Liquibase: `modules/core/db/changelog/260815-2-addCandidateSkillEntity.xml` (включена в `db.changelog-master.xml`)
  * SQL: `modules/core/db/update/postgres/26/260815-2-addCandidateSkillEntity.sql`
* **Автотесты:** `modules/core/test/com/company/hunttech/core/CandidateSkillEntityContractTest.java` (пройден успешно).

---

## 2. Модель данных: сущность `CandidateSkill`

Таблица базы данных: `HUNTTECH_CANDIDATE_SKILL`

| Поле | Тип | Описание | Ограничения |
|---|---|---|---|
| `ID` | `uuid` | Первичный ключ StandardEntity | PK |
| `CANDIDATE_ID` | `uuid` | Ссылка на кандидата (`HUNTTECH_JOB_CANDIDATE`) | NOT NULL, FK, Индекс |
| `SKILL_ID` | `uuid` | Ссылка на навык из справочника (`HUNTTECH_SKILL_TREE`) | NOT NULL, FK, Индекс |
| `PRIORITY` | `integer` | Признак критичности: 10 (Основной), 20 (Второстепенный), 30 (Третьестепенный) | Nullable, Enum `CandidateSkillPriority` |

### Защита от дубликатов
На уровне базы данных и бизнес-логики гарантируется уникальность пары `(CANDIDATE_ID, SKILL_ID)`:
```sql
CREATE UNIQUE INDEX IF NOT EXISTS IDX_HUNTTECH_CANDIDATE_SKILL_UNQ
    ON HUNTTECH_CANDIDATE_SKILL (CANDIDATE_ID, SKILL_ID)
    WHERE DELETE_TS IS NULL;
```

---

## 3. Бизнес-логика сканирования навыков (`scanCandidateSkills`)

При нажатии на кнопку **«Сканировать навыки»** во вкладке «Текст резюме» (`candidateCVRichTextArea`):

1. **Валидация контекста:**
   * Проверяется наличие и непустота текста в `candidateCVRichTextArea`.
   * Извлекается привязанный кандидат (`getEditedEntity().getCandidate()`).
   * HTML-разметка резюме очищается с помощью `Jsoup.parse(...).text()`.

2. **Загрузка существующих навыков:**
   * Выполняется запрос к БД для получения уже сохраненных навыков кандидата (`select e from hunttech_CandidateSkill e where e.candidate = :candidate`).
   * Формируется множество `Set<UUID> existingSkillIds` для исключения дублей.

3. **Вызов сервиса `SkillAnalysisService` по уровням:**
   * `skillAnalysisService.analyzeMain(inputText)` — извлечение основных/ключевых навыков (`CandidateSkillPriority.MAIN`).
   * `skillAnalysisService.analyzeSecondary(inputText)` — извлечение второстепенных/желательных навыков (`CandidateSkillPriority.SECONDARY`).
   * `skillAnalysisService.analyzeTertiary(inputText)` — извлечение третьестепенных навыков (`CandidateSkillPriority.TERTIARY`).
   * В случае классического fallback (если списки по уровням пусты) вызывается `analyzeAll(inputText)` с присвоением уровня `MAIN`.

4. **Дедупликация и сохранение:**
   * Навыки сохраняются в строгом приоритетном порядке (MAIN → SECONDARY → TERTIARY), дубликаты среди уровней исключаются.
   * Новые записи `CandidateSkill` атомарно сохраняются через `DataManager.commit()`.

5. **Всплывающая статистика (5 секунд):**
   * Выводится структурированное HTML-оповещение с таймером автоскрытия `5000 мс`:
     * Всего обнаружено навыков: `N`
     * • Основных: `X`
     * • Второстепенных: `Y`
     * • Третьестепенных: `Z`
     * ✅ Сохранено новых: `K` (Уже присутствуют: `M`).

---

## 4. Отображение основных навыков в Sidebar (`JobCandidateEdit` и `JobCandidateTestBrowse`)

1. **Раздел в сайдбаре `JobCandidateEdit`:**
   * Стилизованный заголовок `msg://msgMainSkillsTitle` («Основные навыки») с фирменными линиями сверху и снизу (`job-candidate-section-title`).
   * Контейнер с цветными бейджами (`chip-pills`), где основные навыки выделены маркировкой `★`, а каждый бейдж имеет уникальный оттенок из гармоничной палитры.
2. **Раздел в Split-View экранах `JobCandidateTest1Browse` и `JobCandidateTestBrowse`:**
   * Динамическое обновление списка навыков при выборе кандидата в таблице справа.
   * Автоматический сброс при очистке выбора.
