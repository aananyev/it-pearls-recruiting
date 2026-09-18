# Отчёт: Почему PR #229 не содержит изменений форм CompanyEdit и ExtSettingsWindow

**Дата:** 2026-09-04
**Автор:** hrm-dev (Antigravity)
**Статус:** Расследование завершено

---

## 1. Главный вывод

**PR #229 (commit 2fc96ead) — это ТОЛЬКО merge-коммит с протоколом защиты форм и фиксом теста.**

**В самом PR #229 НЕТ изменений XML/SCSS форм CompanyEdit и ExtSettingsWindow.**

Фактические исправления компоновки форм сделаны в **предыдущих PR #225, #226, #227**, которые **уже замерджены в master ДО PR #229**.

---

## 2. Хронология мерджей (от старых к новым)

| PR | Commit | Дата мерджа | Что содержит |
|----|--------|-------------|--------------|
| **#225** | 480b4a05 | 2026-09-04 04:00 | **ExtSettingsWindow**: убран height=100% у settingsTabSheet и main-background; ai-settings-content перенесён на scrollBox вкладок; сняты дубли edit-workspace; margin убрано с 9 groupBox; captionwrap-фикс; toolbar унифицированы (min-height 64px, MIDDLE_RIGHT); спейсеры → CSS |
| **#226** | 494fe1bc | 2026-09-04 04:24 | **CompanyEdit**: expand снят с 4 вкладок; дубль edit-workspace → company-tab-scroll (без height); overflow:hidden → overflow-x:hidden в 7 темах; добавлен CompanyEditTabLayoutContractTest (3 теста) |
| **#227** | bc302e6c | 2026-09-04 04:37 | **CompanyEdit (дополнение)**: overflow:hidden → overflow-x:hidden на tabsheetpanel в shared edit-screen-shared-styles (7 тем); height:100%!important снят с .v-scrollable в company-editor.scss |
| **#229** | 2fc96ead | 2026-09-04 22:16 | **Только**: защита протокола (запрет чужих правок CompanyEdit/ExtSettingsWindow, обязательный OCR review) + фикс теста ExtSettingsWindowCoreBeanLookupTest (параметр `limit`) |

---

## 3. Что в PR #229 (фактический diff)

```diff
# build.gradle
- version = '0.433'
+ version = '0.434'

# ExtSettingsWindowCoreBeanLookupTest.java
- assertTrue(navigationController.contains("UserAiContextBuilder.buildPreview(profile)"))
+ assertTrue(navigationController.contains("UserAiContextBuilder.buildPreview(profile, limit)"))
```

**Никаких изменений в:**
- `modules/web/src/com/company/hunttech/web/screens/company/company-edit.xml`
- `modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window.xml`
- `modules/web/themes/*/com.company.hunttech/company-editor.scss`
- `modules/web/themes/*/com.company.hunttech/settings-window-sections.scss`
- `modules/web/themes/*/com.company.hunttech/edit-screen-shared-styles.scss`

---

## 4. Текущее состояние master (commit 2fc96ead)

**Master ВКЛЮЧАЕТ все изменения PR #225, #226, #227** (так как 2fc96ead — merge-коммит с базой bc302e6c, которая уже содержит #225, #226, #227).

```bash
git log --oneline master | head -10
2fc96ead Merge pull request #229...
52fb8058 тест: поправить assert...
6792209e протокол: защита CompanyEdit...
bc302e6c Merge pull request #227...     ← #227 ЗДЕСЬ
c5967a15 CompanyEdit (дополнение...)    ← #227 changes
494fe1bc Merge pull request #226...     ← #226 ЗДЕСЬ
8f425f0f CompanyEdit: восстановление...  ← #226 changes
480b4a05 Merge pull request #225...     ← #225 ЗДЕСЬ
b16358ff ExtSettingsWindow: доведение...  ← #225 changes
```

**Деплой master (0.434-SNAPSHOT) ДОЛЖЕН содержать все исправления форм.**

---

## 5. Почему изменения могут не видеться в UI

### Вариант А: Кэш браузера / Vaadin widgetset
```bash
# Очистить кэш браузера (Ctrl+Shift+R) или открыть в инкогнито
# Vaadin может кэшировать widgetset — проверить версии JAR
```

### Вариант Б: Тема не пересобралась
```bash
# Проверить, что SCSS скомпилировался в CSS
ls -la deploy/tomcat/webapps/hrm/VAADIN/themes/hover/com.company.hunttech/
# Должны быть .css файлы с изменениями overflow-x:hidden
```

### Вариант В: Browser deployment cache
```bash
# Tomcat может кэшировать статику в work/
rm -rf deploy/tomcat/work/Catalina/localhost/hrm
# Перезапуск: bash scripts/start-app.sh --force
```

### Вариант Г: Worktree Antigravity (0.437) перекрыл
Branch-деплой 22:23:31 развернул worktree Antigravity (версия 0.437). Master-деплой 23:43:14 вернул 0.434. Если проверяли между 22:23 и 23:43 — видели версию worktree.

---

## 6. Как верифицировать, что исправления В master

### 6.1 Проверить XML CompanyEdit (mainTab structure)
```bash
# В master (текущий деплой):
grep -A 5 "companyDetailsScroll\|companyRequisitesScroll" modules/web/src/com/company/hunttech/web/screens/company/company-edit.xml
# Должно быть: stylename="company-tab-scroll" (БЕЗ edit-workspace)
```

### 6.2 Проверить SCSS edit-screen-shared-styles (overflow-x:hidden)
```bash
grep "overflow-x:hidden" modules/web/themes/hover/com.company.hunttech/edit-screen-shared-styles.scss
# Должно быть на .edit-tabs .v-tabsheet-tabsheetpanel
```

### 6.3 Проверить SCSS company-editor.scss (height:100%!important убран)
```bash
grep "height:100%!important" modules/web/themes/hover/com.company.hunttech/company-editor.scss
# Должно быть ПУСТО (убрано в PR #227)
```

### 6.4 Проверить ExtSettingsWindow (нет height=100% на settingsTabSheet)
```bash
grep 'settingsTabSheet.*height.*100%' modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window.xml
# Должно быть ПУСТО (убрано в PR #225)
```

### 6.5 Проверить контрактные тесты (должны проходить)
```bash
bash ../hunttech_recruiting/scripts/agent-gradle.sh :app-core:test --tests com.company.hunttech.core.CompanyEditTabLayoutContractTest --tests com.company.hunttech.core.CompanyEditLayoutContractTest --tests com.company.hunttech.core.ExtSettingsWindowLayoutContractTest --tests com.company.hunttech.core.ExtSettingsWindowCoreBeanLookupTest
```

---

## 7. Варианты исправления / действий

### Если исправления НЕ работают в UI (после верификации 6.1-6.4):

| # | Действие | Команда | Ожидаемый результат |
|---|----------|---------|---------------------|
| 1 | **Полная пересборка тем + деплой** | `bash scripts/start-app.sh --force` | SCSS → CSS, очистка work/, перезапуск Tomcat |
| 2 | **Проверить тесты компоновки** | `agent-gradle.sh :app-core:test --tests "*LayoutContractTest*"` | Все зелёные (3+8+4+4 теста) |
| 3 | **CDP-проверка форм в браузере** | Авторизация → открыть CompanyEdit / ExtSettingsWindow → проверить DOM | Нет `edit-workspace` дублей, `overflow-x:hidden` на tabsheetpanel |
| 4 | **Если тесты падают** | Анализ падений → фикс в worktree Antigravity → PR | Новый PR с исправлением регрессии |

### Если исправления В master, но ожидались в PR #229:

**Понимание:** PR #229 — это **защита протокола** (запрет чужих правок, OCR review), а не PR с исправлениями форм. Исправления форм уже в master через PR #225-#227.

---

## 8. Рекомендация

1. **Запустить верификацию (пункт 6)** — подтвердить, что код в master правильный
2. **Сделать `bash scripts/start-app.sh --force`** — гарантированно пересобрать темы и перезапустить Tomcat
3. **Проверить UI в инкогнито** — исключить кэш браузера
4. **Если тесты проходят, а UI не обновился** — проблема в кэше/деплое, а не в коде PR #229

---

## 9. Субагенты

В расследовании участвовали:
- **Основной агент (hrm-dev)**: анализ git-истории, PR diff, XML/SCSS файлов, понимание последовательности мерджей
- Никакие дополнительные субагенты не требовались — задача чисто диагностическая (git-археология)

---

## Приложение: Ключевые коммиты с исправлениями форм

```
# PR #225 — ExtSettingsWindow (b16358ff)
- settingsTabSheet: убран height="100%"
- main-background.xml: убран height="100%"
- ai-settings-content: перенесён на scrollBox вкладок
- edit-workspace дубли удалены
- margin=true убрано с 9 groupBox
- captionwrap margin-top: 0 !important
- toolbar min-height: 64px, actions MIDDLE_RIGHT

# PR #226 — CompanyEdit (8f425f0f)
- 4 вкладки: expand снят
- scrollBox вкладок: edit-workspace → company-tab-scroll (без height)
- overflow:hidden → overflow-x:hidden (7 тем)
- CompanyEditTabLayoutContractTest (3 теста)

# PR #227 — CompanyEdit дополнение (c5967a15)
- edit-screen-shared-styles: .edit-tabs .v-tabsheet-tabsheetpanel overflow:hidden → overflow-x:hidden
- company-editor.scss: убран height:100%!important с .v-scrollable
```

**Все эти изменения УЖЕ в master (commit 2fc96ead).**