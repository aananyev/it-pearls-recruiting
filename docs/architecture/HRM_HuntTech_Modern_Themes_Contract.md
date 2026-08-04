# Визуальный контракт тем HRM HuntTech Modern

## Назначение и бизнес-смысл (What & Why)

Темы `hunttech-modern-light` и `hunttech-modern-dark` дают пользователю HRM HuntTech
единый современный интерфейс для ежедневной работы с кандидатами, вакансиями,
взаимодействиями, отчётами и административными экранами. Контракт защищает темы
от деградации до неоформленного Vaadin DOM, сохраняет читаемость плотных рабочих
экранов и определяет единые состояния компонентов без изменения бизнес-логики.

Основной риск пользовательской темы CUBA Platform 7.3 состоит в том, что наличие
`styles.scss`, `app-components` и локальных partial-файлов само по себе не даёт
стили стандартной темы. Пользовательская тема обязана импортировать и включать
родительский mixin Halo. При утрате этого слоя логотип, меню, отчёты и layout
отображаются как сырой HTML, даже если компиляция отдельных SCSS-фрагментов формально
не сообщает об ошибке.

## UI Context & Navigation

- тема выбирается в `ExtSettingsWindow` через штатное поле настройки CUBA;
- после сохранения тема применяется ко всему web-клиенту `/hrm/`, включая
  `HrmMainScreen`, главное меню, стандартные CUBA/addon-экраны и локально
  стилизованные Edit-формы;
- `hunttech-modern-light` предназначена для обычного дневного режима;
- `hunttech-modern-dark` предназначена для работы при низкой освещённости;
- экранные XML-дескрипторы, component ID, actions, loaders и bindings от темы
  не зависят и не изменяются при переключении.

## Behavior Summary

- выбор `hunttech-modern-light` → загружается Halo и светлая семантическая палитра
  → приложение получает полный layout и светлые поверхности;
- выбор `hunttech-modern-dark` → загружается Halo и тёмная семантическая палитра
  → стандартные и локальные компоненты сохраняют контраст;
- hover / focus / selected / disabled / read-only → используется единая система
  состояний темы → пользователь видит состояние без изменения CUBA-контракта;
- открытие главного экрана → логотип ограничивается размерами menubar
  → изображение не растягивает страницу;
- недоступность внешних шрифтов → используется системный font stack
  → тема остаётся работоспособной без сетевой зависимости;
- отсутствие `@import "../halo/halo"` или `@include halo`
  → контрактный тест завершается ошибкой → дефект не должен попадать в PR.

## 1. Установленная причина дефекта

До исправления обе темы подключали `app-components` и локальные partial-файлы,
но не импортировали `../halo/halo` и не выполняли `@include halo` внутри корневого
селектора темы. В результате compiled CSS не содержал полного визуального слоя
стандартных компонентов CUBA/Vaadin.

Дополнительно `hunttech-modern-dark-defaults.scss` был фактической копией светлой
палитры: белые поверхности, тёмный текст и комментарий «светлая тема». Поэтому даже
частично применённые override не образовывали тёмную тему.

## 2. Обязательная архитектура SCSS

Порядок в каждом `styles.scss` является частью контракта:

```scss
@import "<theme>-defaults";
@import "com.company.hunttech/<theme>-defaults";
@import "../halo/halo";
@import "app-components";
@import "com.company.hunttech/<theme>-ext";
@import "com.company.hunttech/modern-theme-components";
// затем локальные partial-файлы HRM HuntTech

.<theme> {
  @include halo;
  @include app_components;
  @include <theme-extension>;
  @include modern-theme-components;
  // затем локальные mixin HRM HuntTech
}
```

Причины порядка:

1. defaults должны быть объявлены до импорта Halo, чтобы Sass скомпилировал
   родительскую тему с нужной палитрой;
2. `@include halo` восстанавливает геометрию, меню, окна, формы и состояния
   стандартных Vaadin-компонентов;
3. `app_components` подключается после базового слоя;
4. legacy theme extension сохраняет совместимость существующих stylename;
5. `modern-theme-components` задаёт общий системный visual contract двух тем;
6. локальные partial-файлы экранов остаются последним слоем и не меняют
   бизнес-контракт форм.

## 3. Семантическая палитра

Обе темы используют одинаковые имена токенов:

| Токен | Назначение |
|---|---|
| `$ht-accent` | корпоративный красный акцент |
| `$ht-app-background` | фон рабочей области приложения |
| `$ht-surface` | базовая поверхность |
| `$ht-surface-raised` | карточки, окна, таблицы |
| `$ht-surface-muted` | headers, read-only и вторичные области |
| `$ht-text` | основной текст |
| `$ht-text-secondary` | подписи и вторичный текст |
| `$ht-border` / `$ht-border-strong` | разделители и control borders |
| `$ht-focus-ring` | keyboard-focus |
| `$ht-shadow` / `$ht-shadow-strong` | уровни elevation |

Legacy-алиасы `$ht-red`, `$ht-dark`, `$ht-gray`, `$ht-white`, `$ht-muted`,
`$ht-border-subtle` сохранены только для совместимости существующих partial-файлов.

### 3.1 Светлая тема

- рабочий фон: `#f3f5f8`;
- поверхность: `#ffffff`;
- основной текст: `#20242b`;
- акцент: `#c62828`;
- menubar: `#171a20`.

### 3.2 Тёмная тема

- рабочий фон: `#0f1217`;
- поверхность: `#171b22`;
- поднятая поверхность: `#1d232c`;
- основной текст: `#f2f4f7`;
- акцент: `#ef5652`;
- menubar: `#090b0f`.

## 4. Контракт компонентов

Theme extension задаёт единые правила для:

- верхнего меню и ограничения логотипа;
- TextField, TextArea, Lookup/FilterSelect, DateField и RichTextArea;
- primary, secondary, link, borderless, danger и disabled кнопок;
- Table и DataGrid, включая header, stripe, hover и selection;
- Panel, GroupBox, Window, popup и context menu;
- TabSheet;
- labels, links, notifications и loading indicator;
- scrollbar;
- `ht-oval-image`.

Селекторы находятся внутри mixin конкретной темы и компилируются под её корневым
классом. Они не должны воздействовать на `halo`, `hover`, `havana`, `helium`
или другие темы.

## 5. Ограничения

Изменение темы не разрешает менять:

- XML-компоновку экранов;
- component ID, captions, actions и invoke;
- dataContainer, property bindings, loaders, JPQL и views;
- Java lifecycle и бизнес-логику;
- сущности, БД и Liquibase.

Специализированную геометрию Edit-форм продолжают задавать локальные partial-файлы
и [общий контракт Edit-экранов](HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md).

## 6. Проверка

Обязательная статическая проверка:

```bash
./gradlew :app-core:test \
  --tests 'com.company.hunttech.core.ModernThemesFoundationContractTest' \
  --no-daemon --stacktrace

./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Обязательный runtime smoke Hermes:

1. открыть `/hrm/` в `hunttech-modern-light`;
2. проверить menubar, логотип, меню «Отчёты», Table/DataGrid, Edit-форму и popup;
3. повторить сценарий в `hunttech-modern-dark`;
4. проверить hover, focus, selected, disabled/read-only;
5. подтвердить отсутствие горизонтального переполнения и сырого HTML;
6. проверить Tomcat logs;
7. зафиксировать точный проверенный HEAD.

## 7. Rollback

Откат выполняется возвратом файлов двух theme directories к предыдущему SHA.
Изменения не затрагивают Java, XML, persistent model или БД, поэтому отдельный
data rollback не требуется.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-04 | Восстановлено обязательное наследование Halo, разделены светлая и тёмная семантические палитры, унифицирован системный слой CUBA/Vaadin и добавлен регрессионный контракт |
