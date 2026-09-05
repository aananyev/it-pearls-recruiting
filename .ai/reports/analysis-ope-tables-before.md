# АНАЛИЗ ТЕКУЩЕГО СОСТОЯНИЯ 4 ВНУТРЕННИХ ТАБЛИЦ OpenPositionEdit
Сравнение с контрактом ReestrBrowse_Design_Contract.md

## Таблица 1: laborAgreementDataGrid (трудовые соглашения)

**Текущее состояние:**
- stylename: `open-position-editor-table-variant5`
- editorEnabled: true
- height: не указано (использует высоту контейнера)
- width: 100%
- columns: perhaps (editable), company, laborAgreementType
- buttonsPanel: присутствует с create/edit/remove
- dataContainer: laborAgreementDc
- view: laborAgreement-openPosition-tab-view

**Требования контракта:**
- Таблица должна иметь stylename=`borderless grid candidate-browse-grid`
- Строка 38px + word-break
- Колонки: одна резиновая с expandRatio, фиксированные ширины кратны смыслу
- Действия: стандартные edit/refresh (+create/remove где уместно)
- Zero N+1 кэши

**Расхождения:**
1. stylename: используется `open-position-editor-table-variant5` вместо `borderless grid candidate-browse-grid`
2. Отсутствует explicit height/row height specification (должно быть min-height 38px)
3. Колонки не следуют правилу резиновой колонки и фиксированных ширины
4. editorEnabled=true требует особого внимания к Data View Integrity

## Таблица 2: someFilesTable (файлы)

**Текущее состояние:**
- stylename: `open-position-editor-table-variant5`
- height: 300px (фиксированный!)
- width: 100%
- columns: fileDescription, fileType.nameFileType, fileDescriptor.size, fileOwner.name
- buttonsPanel: присутствует с add/edit/remove
- dataContainer: someFilesesDc
- view: someFilesOpenPosition-edit-view

**Требования контракта:**
- Таблица должна иметь stylename=`borderless grid candidate-browse-grid`
- Строка 38px + word-break
- Относительная высота (flex/expand контейнера), НЕ фиксированная
- Колонки: одна резиновая с expandRatio, фиксированные ширины кратны смыслу
- word-break/ellipsis в текстовых колонках

**Расхождения:**
1. stylename: используется `open-position-editor-table-variant5` вместо `borderless grid candidate-browse-grid`
2. height=300px фиксированный - нарушение п.1 задачи (должна быть относительная высота)
3. Отсутствует word-break в текстовых колонках (description может быть длинным)
4. Колонки не следуют правилу резиновой колонки

## Таблица 3: openPositionSkillsListTable (дерево навыков)

**Текущее состояние:**
- stylename: `open-position-editor-table-variant5`
- hierarchyColumn: skillName
- hierarchyProperty: skillTree
- width: 100%
- height: 100%
- columns: fileImageLogo (с componentRenderer), skillName, specialisation, wikiPage, isComment (с iconRenderer)
- dataContainer: openPositionSkillsListsDc
- view: skillTree-openPosition-tab-view

**Требования контракта:**
- Таблица должна иметь stylename=`borderless grid candidate-browse-grid`
- Строка 38px + word-break (с оговоркой для иерархии)
- Для TreeDataGrid: min-height 320px для .candidate-table-card .v-treegrid
- Сохранение читаемости иерархии (expander-отступы не ломать)
- Колонки: одна резиновая с expandRatio, фиксированные ширины кратны смыслу

**Расхождения:**
1. stylename: используется `open-position-editor-table-variant5` вместо `borderless grid candidate-browse-grid`
2. Отсутствует explicit стили для строк 38px (но height=100% может компенсироваться)
3. Требуется проверка word-break в колонках specialisation и wikiPage
4. Колонки не следуют правилу резиновой колонки

## Таблица 4: openPostionNewsDataGrid (новости)

**Текущее состояние:**
- stylename: `open-position-editor-table-variant5`
- width: 100%
- height: 100%
- columns: dateNews (200px), subject, candidates, author (250px)
- buttonsPanel: присутствует с create/remove + кастомная кнопка addOpenPositionNewsButton
- dataContainer: openPositionNewsDc
- view: openPositionNews-edit-view

**Требования контракта:**
- Таблица должна иметь stylename=`borderless grid candidate-browse-grid`
- Строка 38px + word-break
- Колонки: одна резиновая с expandRatio (subject), фиксированные ширины кратны смыслу
- word-break/ellipsis в текстовых колонках (subject новостей)
- Кнопки: стиль кнопок по п.4 контракта (candidate-btn-паттерн: иконка+текст, 40px/600/4px)

**Расхождения:**
1. stylename: используется `open-position-editor-table-variant5` вместо `borderless grid candidate-browse-grid`
2. Отсутствует word-break в колонке subject (может быть длинным)
3. Фиксированные ширины колонок могут не соответствовать паттерну "резиновая + фиксированные кратны смыслу"
4. Кастомная кнопка addOpenPositionNewsButton требует проверки стиля

## SCSS анализ

Проверяем файл: `/Users/alekseyananyev/StudioProjects/hrm-hermes2/modules/web/themes/halo/com.company.hunttech/open-position-editor.scss`

Класс .open-position-editor-table-variant5 не найден в SCSS

## Выводы

Все 4 таблицы требуют обновления стилизации для соответствия контракту ReestrBrowse_Design_Contract.md:

1. Заменить stylename с `open-position-editor-table-variant5` на `borderless grid candidate-browse-grid`
2. Убрать фиксированный height=300px у someFilesTable и сделать высоту относительной
3. Добавить word-break/ellipsis в текстовые колонки где необходимо
4. Убедиться, что высота строк составляет минимум 38px с ростом по содержимому
5. Проверить, что колонки следуют паттерну: одна резиновая колонка с expandRatio, остальные с фиксированными ширинами кратными смыслу
6. Обновить стили в SCSS для variant5 класса чтобы соответствовать candidate-browse-grid
7. Убедиться в соблюдении Data View Integrity для всех геттеров, используемых в колонках
8. Для someFilesTable: если снятие высоты меняет компоновку - зафиксировать и эскалировать

## План действий

Каждый логический кусок - отдельный коммит:
1. Обновление stylename и базовых свойств всех 4 таблиц
2. Убрать фиксированную высоту у someFilesTable
3. Добавить word-break в текстовые колонки
4. Обновить SCSS стили для variant5 класса
5. Проверить и исправить ширины колонок
6. Проверить Data View Integrity
7. Запустить тесты и визуальную приемку
