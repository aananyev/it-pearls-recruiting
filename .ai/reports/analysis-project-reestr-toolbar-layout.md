# Анализ тулбара реестра проектов (ProjectReestrBrowse)

**Дата:** 2026-09-03  
**Экран:** project-reestr-browse.xml  
**Эталон:** open-position-reestr-browse.xml (реестр открытых вакансий)

---

## Корневая причина

Текущая верстка тулбара в `project-reestr-browse.xml` (строки 117-140) использует **hbox** контейнеры:
- `hbox tableFilterBar` — корневой контейнер
- `hbox leftActionButtons` — левая группа кнопок
- `hbox toolbarSpacer` — спейсер (width="100%")
- `hbox rightActionButtons` — правая группа кнопок

**Проблемы hbox:**
1. Нет `flex-wrap: wrap` — кнопки не переносятся на новую строку при нехватке ширины
2. Нет адаптивных брейкпоинтов — при сужении экрана кнопки выходят за границы или обрезаются
3. `toolbarSpacer` с `width="100%"` работает только при достаточной ширине, при нехватке ломает верстку
4. Отсутствует `justify-content: space-between` для правильного распределения пространства

Эталон `open-position-reestr-browse.xml` использует **cssLayout** с классом `.candidate-filter-bar`, который имеет полную поддержку flexbox и адаптивности через SCSS в `job-candidate-editor.scss` (строки 1208-1484).

---

## Рекомендации по исправлению

### XML изменения (project-reestr-browse.xml)

1. **Заменить `hbox tableFilterBar` на `cssLayout`** с `stylename="candidate-filter-bar edit-card"`
2. **Заменить `hbox leftActionButtons` на `cssLayout`** с `stylename="filter-buttons-panel left-action-buttons"`
3. **Удалить `hbox toolbarSpacer`** — больше не нужен (flexbox сам распределит пространство)
4. **Заменить `hbox rightActionButtons` на `cssLayout`** с `stylename="filter-buttons-panel right-action-buttons"`
5. **Добавить стили кнопкам:** `stylename="candidate-btn"` для единообразия с эталоном
6. **Кнопке создания:** добавить `candidate-create-btn` (primary стиль)
7. **PopupButton'ам:** добавить `candidate-btn` и соответствующие классы (`candidate-filter-scope-btn` и т.д.)

### SCSS изменения
SCSS **не требует изменений** — в `job-candidate-editor.scss` уже есть полная поддержка:
- `.candidate-filter-bar` с flexbox и адаптивностью
- `.filter-buttons-panel` с flex-wrap
- `.left-action-buttons` / `.right-action-buttons` с правильным flex-поведением
- Брейкпоинты: 1440px, 1240px (правые кнопки под левые), 900px
- Стили `.candidate-btn`, `.candidate-btn.primary`, `.candidate-btn.secondary`
- Стили для popupButton внутри тулбара

### Контроллер (ProjectReestrBrowse.java)
Изменения в Java **не требуются** — @Inject полей по id останутся рабочими, так как id компонентов сохраняются.

---

## Файлы к модификации

1. `/Users/alekseyananyev/StudioProjects/hunttech_recruiting/modules/web/src/com/company/hunttech/web/screens/project/project-reestr-browse.xml` — основной XML тулбара

---

## Ожидаемый результат

После изменений тулбар реестра проектов будет иметь:
- ✅ Адаптивную верстку как в реестре открытых вакансий
- ✅ Правильный перенос кнопок на мобильных/узких экранах
- ✅ Единообразные стили кнопок (candidate-btn)
- ✅ Правильное поведение popupButton'ов
- ✅ Брейкпоинты 1440px / 1240px / 900px

Sidebar, таблица, бизнес-логика, сущности — **не затрагиваются**.