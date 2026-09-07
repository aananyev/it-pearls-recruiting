# Отчёт Аналитика и UI/UX-дизайнера: Рефакторинг правой рабочей области и навигации ExtSettingsWindow

> **Дата**: 2026-09-07  
> **Роли**: Аналитик, UI/UX-дизайнер, Java Backend-разработчик  
> **Экран**: `ExtSettingsWindow` (CUBA screen ID `settings`)  
> **Файлы дескрипторов**: `ext-settings-window.xml`, `ext-settings-window-email-navigation.xml`, `ext-settings-window-main-background.xml`  
> **Контроллер**: `ExtSettingsWindowEmailNavigation.java` (базовый класс `ExtSettingsWindow.java`)  
> **Нормативная основа**: [HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md](../../docs/architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md), [hunttech-edit-screen-design](../../.agents/skills/hunttech-edit-screen-design/SKILL.md).

---

## 1. Анализ постановки задачи

### 1.1. Требования Заказчика
1. **Улучшить дизайн правой части экрана `ExtSettingsWindow`**:
   - Привести компоновку **первой вкладки** (`msgMyInfo` — «Обо мне / Профессиональный ИИ-профиль») и **второй вкладки** (`msgInterface` — «Интерфейс») к стандарту контрактного документа по Edit-экранам HRM HuntTech.
2. **Ограничения по Sidebar**:
   - Сам сайдбар (ширина 270px, аватар 176px с OvaFallbackImage, имя, статус, сводка, предупреждения о безопасности) **изменять запрещается**.
   - В сайдбаре **разрешено изменить label-навигацию** и сделать её **по вкладкам элемента tabsheet** правой части экрана.
3. **Редеплой и перезапуск**:
   - Провести полную пересборку и развертывание приложения на локальном сервере Tomcat с проверкой доступности `http://localhost:8080/hrm/`.

---

## 2. Архитектурное и UI/UX решение

### 2.1. Покладочная Label-навигация в левом Сайдбаре
- В левом сайдбаре внедряется единый навигационный блок `settingsTabsNavigation` (`stylename="settings-tabs-navigation label-navigation"`), соответствующий эталону `OpenPositionEdit`:
  - Заголовок: `msg://tabsNavigationTitle` («ВКЛАДКИ НАСТРОЕК», `stylename="label-nav-title job-candidate-section-title settings-section-navigation-title"`);
  - Кнопки навигации по вкладкам (`stylename="borderless label-nav-item"`):
    1. `navTabMyInfo` — «Обо мне» (вкладка `msgMyInfo`);
    2. `navTabInterface` — «Интерфейс» (вкладка `msgInterface`);
    3. `navTabMail` — «Почта» (вкладка `mailAccessTab`);
    4. `navTabAi` — «AI» (вкладка `aiAccessTab`);
    5. `navTabGeo` — «Гео-API» (вкладка `geoApiAccessTab`).
- При клике по кнопке контроллер вызывает `settingsTabSheet.setSelectedTab("<tabId>")`.
- При переключении вкладок активной кнопке назначается класс `label-nav-item-active` (золотой акцент `#ffb11b` с левой полосой-маркером).
- Существующие блоки секционной навигации (`userAiProfileSectionNavigation`, `interfaceSettingsNavigation`, `emailSettingsNavigation`, `aiSettingsNavigation`, `geoSettingsNavigation`) сохраняются в XML в скрытом режиме (`visible="false"`), гарантируя 100% совместимость с инъекциями контроллера и контрактными тестами.

### 2.2. Рефакторинг Вкладки 1 («Обо мне / Профессиональный ИИ-профиль», `msgMyInfo`)
- **Тулбар (`userAiProfileToolbar`)**:
  - `label` заголовка получает класс `edit-toolbar-title`;
  - `label` описания получает `edit-toolbar-description edit-help`;
  - Кнопки действий: `previewAiContextBtn` (`icon="EYE"`, `stylename="primary"`), `clearAiProfileBtn` (`icon="TRASH"`, `stylename="danger"`).
- **Секции аккордеона (`edit-accordion-section`)**:
  - Каждая группа полей оформляется через `<groupBox>` с `showAsPanel="true" collapsable="true"` и `stylename="user-ai-profile-section edit-accordion-section"`.
  - Устраняется нестабильная разметка `grid flex="1"`, приводившая к наложению многострочных полей и отсутствию единого ритма.
  - Применяется модульная схема строк `hbox width="100%" spacing="true"`:
    - Парные поля (50/50) с `box.expandRatio="1"` и `width="100%"`;
    - Полноширинные многострочные текстовые поля (`aboutMeField`, `recruitingExperienceYearsField`, `industriesField`) на всю ширину;
    - Всем полям ввода назначается обязательный семантический класс `stylename="edit-form-control"`.

### 2.3. Рефакторинг Вкладки 2 («Интерфейс», `msgInterface`)
- **Тулбар (`interfaceSettingsToolbar`)**:
  - Заголовок `edit-toolbar-title`, описание `edit-toolbar-description edit-help`;
  - Кнопки тулбара: `changePasswordBtn` (`caption="msg://changePassw" icon="icons/change-pass.png" stylename="secondary"`), `resetScreenSettingsBtn` (`caption="msg://resetScreenSettings" icon="icons/trash.png" stylename="danger"`).
- **Карточка «Рабочее пространство» (`interfaceAppearanceCard`)**:
  - Оформление карточки: `edit-card`, заголовок `edit-card-title`, описание `edit-help`;
  - Элементы управления в сетке (`appThemeField`, `appLangField`, `timeZoneLookup`, `defaultScreenField`) получают `stylename="edit-form-control"`;
  - Текстовые метки получают класс `bold` для четкой контрастности.
  - Контракт геометрии `ExtSettingsWindowInterfaceLayout` (`INTERFACE_LABEL_WIDTH = 190px`, `timeZoneBox.expand(timeZoneLookup)`) сохраняется на 100%.

---

## 3. Data View Integrity и безопасность
- Никакие entity, datasource (`userSettingsDs`, `extUserDs`, `userAiProfileDs`, `userAiConfigsDs`), binding, свойства, загрузчики и валидаторы не затрагиваются.
- Все геттеры и обращения к полям соответствуют объявленным view (`userSettings-view`, `extUser-view`, `userAiProfile-view`, `userAiConfiguration-view`).
- Логика коммита и изменения данных остаётся интактной.
