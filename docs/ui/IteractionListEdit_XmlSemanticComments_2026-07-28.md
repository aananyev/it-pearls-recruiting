# IteractionListEdit — смысловые комментарии XML

> Проект: **HRM HuntTech**  
> Screen ID: `hunttech_IteractionList.edit`  
> Descriptor: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`  
> Дата: `2026-07-28`

## 1. Назначение и бизнес-смысл (What & Why)

XML-дескриптор `IteractionListEdit` объединяет data binding CUBA, фильтруемые loaders, sidebar-контекст и четыре блока ввода. До этой задачи часть компонентов имела комментарии только на уровне крупных секций, поэтому назначение отдельных loader, query condition, action, layout, field, row и column требовало чтения Java-controller.

Задача вводит inline living-документацию: перед каждым открывающим XML-элементом расположен отдельный комментарий, объясняющий его роль, источник данных или причину присутствия. Комментарии не заменяют business-spec и не изменяют поведение формы.

## 2. UI Context & Navigation

Экран открывается как editor факта взаимодействия кандидата и вакансии. XML состоит из следующих контекстов:

- `data` — редактируемый `IteractionList`, типы взаимодействий, вакансии и пользователи;
- `iteractionListSidebar` — кандидат, вакансия, статус, приоритет, label-навигация и служебные карточки;
- `iteractionListWorkspace` — toolbar, частые взаимодействия, четыре постоянных блока и footer;
- `participantsAccordion` — кандидат и вакансия;
- `interactionAccordion` — тип действия и dynamic fields;
- `resultAccordion` — оценка, рекрутёр и способ связи;
- `commentAccordion` — комментарий;
- `editActions` — подписка, сохранение и отмена.

Комментарии расположены непосредственно перед описываемым элементом, поэтому разработчик видит назначение компонента в месте его изменения.

## 3. Behavior Summary

| Действие | Условие | Результат |
|---|---|---|
| открыть XML | требуется понять элемент | перед тегом находится комментарий о назначении и данных |
| добавить новый XML-элемент | изменяется descriptor | разработчик обязан добавить отдельный смысловой комментарий |
| удалить или перенести элемент | меняется layout | связанный комментарий удаляется или переносится вместе с элементом |
| изменить binding/action/query | меняется контракт элемента | комментарий актуализируется в том же commit |
| запустить профильный тест | комментарий отсутствует или формальный | тест завершится ошибкой с номером строки и тегом |

## 4. Область покрытия

Комментарии обязательны для всех открывающих элементов, включая:

- `window`, `data`, `instance`, `collection`, `loader`;
- `query`, `condition`, `and`, `c:jpql`, `c:where`;
- `dialogMode`, `layout`, `vbox`, `hbox`, `scrollBox`, `grid`, `buttonsPanel`;
- `columns`, `column`, `rows`, `row`;
- `label`, `image`, `ovaFallbackImage`;
- `textField`, `dateField`, `textArea`, `checkBox`, `lookupField`, `lookupPickerField`, `suggestionPickerField`;
- `actions`, `action`, `button`.

CDATA-текст JPQL и закрывающие теги не являются XML-компонентами и отдельного комментария не требуют.

## 5. Требования к содержанию комментария

Комментарий должен:

1. объяснять бизнес- или UI-назначение элемента;
2. при наличии binding указывать, какие данные читает или сохраняет компонент;
3. для loader/query пояснять фильтр и источник параметров;
4. для action/button описывать пользовательское действие;
5. для layout объяснять, какие элементы он группирует и зачем;
6. находиться непосредственно перед открывающим тегом;
7. содержать не менее 24 символов содержательного текста.

Недопустимые комментарии:

```xml
<!-- Элемент vbox -->
<!-- Поле -->
<!-- TODO -->
```

Допустимый комментарий:

```xml
<!-- LookupPickerField выбирает вакансию из отфильтрованного openPositionDc. -->
<lookupPickerField id="vacancyFiels" ...>
```

## 6. Автоматическая защита

Тест:

```text
modules/core/test/com/company/hunttech/core/IteractionListXmlSemanticCommentsTest.java
```

Проверки:

- каждый открывающий тег имеет предыдущую непустую строку-комментарий;
- комментарий оформлен как `<!-- ... -->`;
- текст не короче 24 символов;
- отсутствуют формальные шаблоны `Элемент vbox`, `Элемент label` и `TODO`;
- присутствуют контрольные комментарии data, sidebar, vacancy picker и comment field.

## 7. Сохранённые CUBA-контракты

Задача не изменяет:

- структуру XML и порядок элементов;
- component ID;
- `dataContainer`, `property`, `optionsContainer`;
- views и loaders;
- JPQL и параметры запросов;
- actions и `invoke`;
- required/visible/editable;
- размеры, stylename и layout geometry;
- Java-controller;
- entity, БД и Liquibase;
- business lifecycle и side effects.

## 8. Проверка Hermes

Hermes должен выполнить:

```bash
./gradlew :app-core:test \
  --tests 'com.company.hunttech.core.IteractionListXmlSemanticCommentsTest' \
  --tests 'com.company.hunttech.core.IteractionListVisualAlignmentTest' \
  --tests 'com.company.hunttech.core.IteractionListEditAccordionLayoutTest' \
  --no-daemon --stacktrace

./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Поскольку runtime-компоновка не изменена, browser smoke подтверждает отсутствие регрессии открытия, навигации, сохранения и отмены.

## 9. История изменений

| Дата | Изменение |
|---|---|
| 2026-07-28 | Перед каждым XML-элементом `IteractionListEdit` добавлен смысловой комментарий; введён автоматический тест полноты комментариев. |
