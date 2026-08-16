# Интеграция сервиса анализа навыков и сущность «Навыки кандидата» (`CandidateSkill`)

## 1. Общие сведения

* **Новая сущность:** `hunttech_CandidateSkill` (`com.company.hunttech.entity.CandidateSkill`)
* **Перечисление уровней критичности:** `com.company.hunttech.entity.CandidateSkillPriority` (MAIN / 10, SECONDARY / 20, TERTIARY / 30)
* **Используемый сервис анализа навыков:** `hunttech_SkillAnalysisService` (`SkillAnalysisService`)
* **Точки интеграции в UI:**
  * Экран редактирования резюме `hunttech_CandidateCV.edit` (`CandidateCVEdit.java`, `candidate-cv-edit.xml`):
    * Кнопки **«Сканировать навыки»** (`scanCandidateSkillsButton` и `scanCandidateSkillsTreeButton`) выделены красным цветом (`stylename="danger"`, `icon="font-icon:MAGIC"`).
    * Кнопки размещены на вкладках «Текст резюме» (`tabCV`) и «Навыки» (`tabSkillTree`).
    * Улучшенная компоновка сайдбара (`edit-sidebar candidate-cv-sidebar`) в стиле Edit-форм (320px, центрированная шапка профиля, блок «Резюме для вакансии» над навигацией, блок «ОСНОВНЫЕ НАВЫКИ» с цветными бейджами).
  * Сайдбар карточки кандидата `hunttech_JobCandidate.edit` (`JobCandidateEdit.java`, `job-candidate-edit.xml`): раздел **«ОСНОВНЫЕ НАВЫКИ»** (`candidateProfileSkills`) с цветными бейджами навыков кандидата и автообновлением при работе с резюме.
  * Сайдбар Split-View экранов `hunttech_JobCandidateTest1.browse` и `hunttech_JobCandidateTest.browse`: блок **«ОСНОВНЫЕ НАВЫКИ»** (`detailSkillsVBox`) с динамическим выводом бейджей навыков выбранного кандидата.
* **Всплывающее уведомление:** TRAY-нотификация с подробной статистикой обнаруженных/сохраненных навыков и автоматическим скрытием через **5 секунд** (`withHideDelayMs(5000)`).
* **Миграции базы данных:**
  * Liquibase: `modules/core/db/changelog/260815-2-addCandidateSkillEntity.xml` (включена в `db.changelog-master.xml`)
  * SQL: `modules/core/db/update/postgres/26/260815-2-addCandidateSkillEntity.sql`
* **Автотесты:**
  * `modules/core/test/com/company/hunttech/core/CandidateSkillEntityContractTest.java` (100% пройден)
  * `modules/core/test/com/company/hunttech/core/SkillAnalysisServiceBeanTest.java` (100% пройден).

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

При нажатии на красную кнопку **«Сканировать навыки»** (`stylename="danger"`, `icon="font-icon:MAGIC"`):

1. **Валидация контекста и извлечение текста:**
   * Проверяется текст из всех доступных источников (`candidateCVRichTextArea.getValue()`, `getEditedEntity().getTextCV()`, `textResumeStringBuffer`).
   * Проверяется привязка кандидата (`getEditedEntity().getCandidate()`, `candidateField.getValue()`).
   * HTML-разметка резюме очищается с помощью `Jsoup.parse(...).text()`.

2. **Загрузка существующих навыков:**
   * Выполняется запрос к БД для получения уже сохраненных навыков кандидата (`select e from hunttech_CandidateSkill e where e.candidate = :candidate`).
   * Формируется множество `Set<UUID> existingSkillIds` для исключения дублей.

3. **Вызов сервиса `SkillAnalysisService` по уровням:**
   * `skillAnalysisService.analyzeMain(inputText)` — извлечение основных/ключевых навыков (`CandidateSkillPriority.MAIN`).
   * `skillAnalysisService.analyzeSecondary(inputText)` — извлечение второстепенных/желательных навыков (`CandidateSkillPriority.SECONDARY`).
   * `skillAnalysisService.analyzeTertiary(inputText)` — извлечение третьестепенных навыков (`CandidateSkillPriority.TERTIARY`).
   * При fallback (если списки пусты) вызывается `analyzeAll(inputText)` с присвоением уровня `MAIN`.

4. **Дедупликация и сохранение:**
   * Навыки сохраняются в строгом приоритетном порядке (MAIN → SECONDARY → TERTIARY), дубликаты исключаются.
   * Новые записи `CandidateSkill` сохраняются через `DataManager.commit()`.
   * Синхронизируются навыки в `CandidateCV.skillTree` и контейнере `skillTreesDc` для мгновенного отображения во вкладке «Навыки».

5. **Всплывающая статистика (5 секунд):**
   * Выводится структурированное HTML-оповещение с таймером автоскрытия `5000 мс`:
     * Всего обнаружено навыков: `N`
     * • Основных: `X`
     * • Второстепенных: `Y`
     * • Третьестепенных: `Z`
     * ✅ Сохранено новых: `K` (Уже присутствуют: `M`).

---

## 4. Стилизация и компоновка UI

1. **Кнопка AI-определения навыков:**
   * Выделена красным цветом `stylename="danger"` с иконкой волшебной палочки `icon="font-icon:MAGIC"`.
   * Доступна как на вкладке резюме `tabCV`, так и на панели таблицы вкладки `tabSkillTree`.
2. **Сайдбар `CandidateCVEdit`:**
   * Блок целевой вакансии поднят выше навигации.
   * Раздел «ОСНОВНЫЕ НАВЫКИ» с цветными бейджами динамически обновляется при сканировании.
