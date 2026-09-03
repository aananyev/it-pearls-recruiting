---
name: hunttech-reestr-screen
description: >-
  Руководство и стандарт разработки экранных форм Реестров (Browse/Reestr) в HRM HuntTech (CUBA 7.3 / Vaadin):
  Split-View с полновысотным сайдбаром 312px (scrollBox, аватар 120px, 4 уровня шапки профиля, быстрые действия,
  секции «Готовность и рейтинг», «Условия/Контакты», 3-уровневые чипы навыков), адаптивный командный тулбар,
  мастер «Умная загрузка» (файлы, текст, ссылка), оптимизация Zero N+1 и адаптация под 7 SCSS-тем.
---

# Стандарт и руководство по разработке форм Реестров (HuntTech Reestr Screen)

Данный навык фиксирует проверенный архитектурный стандарт, XML-разметку, Java-контроллеры и визуальное оформление для создания и модернизации экранов **Реестров** в системе **HuntTech HRM** на основе эталонных реализаций (*Реестр кандидатов* — `JobCandidateReestr` и *Реестр открытых вакансий* — `OpenPositionReestrBrowse`).

---

## 1. Архитектурная концепция Split-View

Экран реестра строится по структуре двухпанельного Split-View с полновысотным левым сайдбаром (312px) и адаптивной рабочей областью таблицы:

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Главное меню приложения / Заголовок реестра (42px)                                               │
├───────────────────────┬──────────────────────────────────────────────────────────────────────────┤
│ ЛЕВЫЙ САЙДБАР (312px) │ ПРАВАЯ РАБОЧАЯ ОБЛАСТЬ (margin="true,true,true,false")                   │
│ (job-candidate-sidebar│ ┌──────────────────────────────────────────────────────────────────────┐ │
│  + edit-sidebar)      │ │ Командный тулбар (candidate-filter-bar):                             │ │
│                       │ │ [Создать] [Умная загрузка] [Ред.] [Удал.]  ...  [Фильтр ▼] [Действия]│ │
│ • Аватар / Лого 120px │ ├──────────────────────────────────────────────────────────────────────┤ │
│ • Заголовок (h2 bold) │ │ Generic Filter (collapsable="true" collapsed="true")                 │ │
│ • Подзаголовок 1 (h4) │ ├──────────────────────────────────────────────────────────────────────┤ │
│ • Компания/Город (bold) │ Карточка таблицы (candidate-table-card):                             │ │
│ • Локация / Формат    │ │ ┌───┬──────┬──────────────┬──────────────┬──────────────┬──────────┐ │ │
│ • Быстрые действия    │ │ │ ! │ Лого │ Наименование │ Специализ-я  │ Зарплата     │ Навыки   │ │ │
│ • Готовность & Рейтинг│ │ ├───┼──────┼──────────────┼──────────────┼──────────────┼──────────┤ │ │
│   - Чеклист           │ │ │ 🟢│  💼  │ Lead ML Eng. │ Разработка   │ до 350 000 ₽ │ ★ Python │ │ │
│   - Звезды ★★★★☆      │ │ └───┴──────┴──────────────┴──────────────┴──────────────┴──────────┘ │ │
│ • Условия / Контакты  │ │ RowsCount (компактный счетчик 24px)                                  │ │
│ • Навыки (3 уровня)   │ └──────────────────────────────────────────────────────────────────────┘ │
│   - Обязательные (★)  │                                                                          │
│   - Желательные       │                                                                          │
│   - Прочие            │                                                                          │
└───────────────────────┴──────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Стандарт оформления левого Сайдбара (Sidebar Concept)

Сайдбар реестров обеспечивает мгновенный обзор сущности без необходимости открывать форму редактирования. Сайдбар полностью размещается внутри `<scrollBox id="detailScroll" width="100%" height="100%" orientation="vertical">`, предотвращая переполнение и обрезание данных на компактных экранах.

### 2.1. Шапка профиля и визуальная идентичность (Profile Header & Identity)

Шапка профиля выравнивается по центру (`align="TOP_CENTER"`, stylename `job-candidate-profile-header edit-sidebar-visual`) и состоит из:

1. **Круглого аватара / логотипа (120×120px)**:
   - Компонент: `ovaFallbackImage` (`WebOvaFallbackImage`)
   - Габариты: `width="120px" height="120px" ovalWidth="120px" ovalHeight="120px"`
   - Масштабирование: `scaleMode="SCALE_DOWN"` (гарантирует отсутствие искажений пропорций)
   - Стиль: `stylename="job-candidate-avatar"`
   - **Таблица стандартных Fallback-ресурсов**:

| Сущность / Реестр | Fallback-путь (`fallbackThemePath`) | Описание иконки |
|---|---|---|
| **Кандидаты** (`JobCandidateReestr`) | `icons/no-programmer.jpeg` | Фото разработчика/кандидата |
| **Вакансии** (`OpenPositionReestr`) | `icons/briefcase.png` | Портфель / проект вакансии |
| **Резюме / Взаимодействия / Персоны** | `icons/no-candidate.png` | Контурный силуэт человека |
| **Компании / Департаменты / Группы** | `icons/no-company.png` | Здание компании / корпорация |
| **Регионы / Города / Страны** | `icons/map.png` / `icons/building.png` | Карта региона / герб города |

2. **Четырёхуровневой типографики идентичности** (`edit-sidebar-identity`):
   - **Уровень 1 (Главный заголовок)**: `stylename="edit-sidebar-title h2 candidate-sidebar-fullname bold"` (ФИО кандидата / Название вакансии / Имя компании).
   - **Уровень 2 (Подзаголовок)**: `stylename="edit-sidebar-subtitle h4 candidate-sidebar-position bold"` (Текущая должность / Проект).
   - **Уровень 3 (Компания / Город)**: `stylename="edit-help candidate-sidebar-city bold"` (Город проживания / Компания заказчика).
   - **Уровень 4 (Локация / Формат работы)**: `stylename="edit-help candidate-sidebar-city"` (Формат: «Удалённо / Офис», тип занятости).

### 2.2. Панель быстрых действий (Sidebar Quick Actions)

Контейнер: `<vbox width="100%" spacing="true" stylename="edit-sidebar-summary">`.
- **Основная кнопка** (открыть форму): `caption="Открыть карточку" icon="EDIT_ACTION" stylename="primary" width="100%"`
- **Контекстные кнопки**:
  - Вакансии: «Подобрать кандидатов» (`icon="font-icon:MAGIC"`), «Подписаться» (`icon="font-icon:BELL"`).
  - Кандидаты: «Добавить взаимодействие» (`icon="PLUS"`), «Добавить резюме» (`icon="FILE_TEXT_O"`).

### 2.3. Секция: Готовность и рейтинг (Readiness & Rating)

Контейнер: `<vbox id="readinessCard" width="100%" spacing="true" stylename="job-candidate-navigation label-navigation">`.
- **Заголовок**: `<label value="ГОТОВНОСТЬ И РЕЙТИНГ" width="100%" stylename="label-nav-title job-candidate-section-title" align="MIDDLE_CENTER"/>`
- **Элементы**:
  - Чеклист готовности (светофоры 🟢🟡⚪): `stylename="job-candidate-readiness edit-help"`
  - Звёздный рейтинг (★) и оценка: `stylename="job-candidate-rating edit-help"`
  - Числовые показатели / воронка: количество отправленных кандидатов, статус откликов.

### 2.4. Секция: Условия / Контакты и реквизиты

Контейнер: `<vbox id="termsCard" width="100%" spacing="true" stylename="job-candidate-navigation label-navigation">`.
- **Заголовок**: `<label value="УСЛОВИЯ И РЕКВИЗИТЫ" width="100%" stylename="label-nav-title job-candidate-section-title" align="MIDDLE_CENTER"/>` (или `"КОНТАКТЫ И РЕКВИЗИТЫ"`).
- **Сетка полей**: `<grid id="termsGrid" spacing="true" width="100%" stylename="edit-sidebar-summary">` с 2 колонками:
  - Левая колонка (название): `<label value="Вилка по ТК:" stylename="bold"/>`
  - Правая колонка (значение): `<label id="detailSalaryTk" value="-"/>` (чистый текст через NVL-логику без сырого HTML).

### 2.5. Секция: Ключевые навыки (3-уровневые чипы)

Контейнер: `<vbox id="skillsCard" width="100%" spacing="true" stylename="job-candidate-navigation label-navigation">`.
- **Заголовок**: `<label value="ОСНОВНЫЕ НАВЫКИ" width="100%" stylename="label-nav-title job-candidate-section-title" align="MIDDLE_CENTER"/>`
- **Чипы навыков**: `<label id="detailSkillsLabels" width="100%" htmlEnabled="true" stylename="candidate-skills-chips"/>`
- **Иерархия чипов**:
  1. **Primary (★)** — Обязательные навыки (золотой бейдж с маркой `★`).
  2. **Secondary** — Желательные навыки (синий/серебряный бейдж).
  3. **Other** — Дополнительные технологии (нейтральный серый бейдж).

---

## 3. Эталонный XML-дескриптор Сайдбара

```xml
<!-- ЛЕВАЯ ПАНЕЛЬ (САЙДБАР): 312px Split-View -->
<vbox id="detailPane" width="312px" height="100%" spacing="true" stylename="job-candidate-sidebar edit-sidebar">
    <scrollBox id="detailScroll" width="100%" height="100%" orientation="vertical">
        <vbox width="100%" spacing="true">

            <!-- 1. Шапка профиля: Аватар 120px + 4-уровневая типографика -->
            <vbox id="profileHeader" width="100%" spacing="true" align="TOP_CENTER" stylename="job-candidate-profile-header edit-sidebar-visual">
                <ovaFallbackImage id="logoPic"
                                  width="120px" height="120px"
                                  ovalWidth="120px" ovalHeight="120px"
                                  align="TOP_CENTER"
                                  stylename="job-candidate-avatar"
                                  fallbackThemePath="icons/briefcase.png"
                                  scaleMode="SCALE_DOWN"/>
                <vbox width="100%" spacing="false" align="MIDDLE_CENTER" stylename="edit-sidebar-identity">
                    <label id="detailTitle" value="Выберите запись" stylename="edit-sidebar-title h2 candidate-sidebar-fullname bold" width="100%" align="MIDDLE_CENTER"/>
                    <label id="detailSubtitle" value="" stylename="edit-sidebar-subtitle h4 candidate-sidebar-position bold" width="100%" align="MIDDLE_CENTER"/>
                    <label id="detailCompany" value="" stylename="edit-help candidate-sidebar-city bold" width="100%" align="MIDDLE_CENTER"/>
                    <label id="detailLocation" value="" stylename="edit-help candidate-sidebar-city" width="100%" align="MIDDLE_CENTER"/>
                </vbox>
            </vbox>

            <!-- 2. Панель быстрых действий -->
            <vbox id="sidebarActionsCard" width="100%" spacing="true" stylename="edit-sidebar-summary">
                <button id="openEditCardBtn" caption="Открыть карточку" icon="EDIT_ACTION" stylename="primary" enabled="false" width="100%"/>
                <button id="suggestBtn" caption="Подобрать подходящие" icon="font-icon:MAGIC" enabled="false" width="100%"/>
            </vbox>

            <!-- 3. Готовность и рейтинг -->
            <vbox id="readinessCard" width="100%" spacing="true" stylename="job-candidate-navigation label-navigation">
                <label value="ГОТОВНОСТЬ И РЕЙТИНГ" width="100%" stylename="label-nav-title job-candidate-section-title" align="MIDDLE_CENTER"/>
                <label id="detailReadiness" width="100%" stylename="job-candidate-readiness edit-help"/>
                <label id="detailRating" width="100%" stylename="job-candidate-rating edit-help"/>
            </vbox>

            <!-- 4. Условия и реквизиты -->
            <vbox id="termsCard" width="100%" spacing="true" stylename="job-candidate-navigation label-navigation">
                <label value="УСЛОВИЯ И РЕКВИЗИТЫ" width="100%" stylename="label-nav-title job-candidate-section-title" align="MIDDLE_CENTER"/>
                <grid id="termsGrid" spacing="true" width="100%" stylename="edit-sidebar-summary">
                    <columns count="2"/>
                    <rows>
                        <row><label value="Вилка по ТК:" stylename="bold"/><label id="detailSalaryTk" value="-"/></row>
                        <row><label value="Ставка по ИП:" stylename="bold"/><label id="detailSalaryIe" value="-"/></row>
                        <row><label value="Опыт работы:" stylename="bold"/><label id="detailExperience" value="-"/></row>
                        <row><label value="Грейд:" stylename="bold"/><label id="detailGrade" value="-"/></row>
                        <row><label value="Формат:" stylename="bold"/><label id="detailFormat" value="-"/></row>
                    </rows>
                </grid>
            </vbox>

            <!-- 5. Ключевые навыки -->
            <vbox id="skillsCard" width="100%" spacing="true" stylename="job-candidate-navigation label-navigation">
                <label value="ОСНОВНЫЕ НАВЫКИ" width="100%" stylename="label-nav-title job-candidate-section-title" align="MIDDLE_CENTER"/>
                <label id="detailSkillsLabels" width="100%" htmlEnabled="true" stylename="candidate-skills-chips"/>
            </vbox>
        </vbox>
    </scrollBox>
</vbox>
```

---

## 4. Паттерны Java-контроллера для Сайдбара

1. **Безопасное обновление данных (`updateSidebarWithEntity`)**:
   - При выборе записи в таблице (`SelectionEvent`): вызывать обновление сайдбара и разблокировку кнопок действий.
   - При `entity == null`: выводить дефолтные плейсхолдеры («Выберите запись», прочерки `-`) и отключать (`enabled="false"`) кнопки действий.
2. **Data View Integrity**:
   - Все геттеры Java-кода, используемые при заполнении сайдбара (`entity.getProjectName()`, `entity.getCity()`, `entity.getSkills()`), **обязаны быть задекларированы во View** контейнера/загрузчика (`jobCandidatesDc`, `openPositionsDc`), исключая `UnfetchedAttributeException` и `LazyInitializationException`.
3. **Чистый вывод текста без HTML-инъекций**:
   - Для значений сетки реквизитов использовать null-safe метод:
   ```java
   private String nvl(String val) {
       return (val != null && !val.trim().isEmpty()) ? val.trim() : "-";
   }
   ```
4. **Рендеринг чипов навыков**:
   - Формировать чипы с CSS-классами `.skill-chip-primary`, `.skill-chip-secondary`, `.skill-chip-other`.

---

## 5. Поддержка 7 SCSS-тем и проверка качества

1. **Единые классы в темах**:
   - `.candidate-sidebar-fullname` (`color: #f8fafc`, `text-align: center`)
   - `.candidate-sidebar-position` (`color: #93c5fd`, `text-align: center`)
   - `.candidate-sidebar-city` (`color: #cbd5e1`, `text-align: center`)
   - `.job-candidate-avatar` (`width: 120px`, `height: 120px`, `border-radius: 50%`)
2. **Сериализация Gradle и Code Review**:
   - Все сборки и тесты — через `bash ../hunttech_recruiting/scripts/agent-gradle.sh`.
   - Code review перед коммитом — через Alibaba `ocr review --audience agent`.
